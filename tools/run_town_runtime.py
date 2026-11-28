"""Run remapped Towns and its probe with unmodified installed dependency jars."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
import socket
import time

sys.path.insert(0, str(Path(__file__).resolve().parent))
from prepare_town_fixture import ROOT, metadata, sha

OUT = ROOT / "build" / "town-runtime-evidence"

def gradle():
    # Reuse the verified CMobs production tooling version and checksum.
    temp = Path(os.environ["RUNNER_TEMP"])
    root = temp / "town-production-gradle"
    binary = root / "gradle-8.12.1/bin/gradle.bat"
    if not binary.is_file():
        archive = temp / "town-gradle-8.12.1.zip"
        with urllib.request.urlopen("https://services.gradle.org/distributions/gradle-8.12.1-bin.zip", timeout=120) as r, archive.open("wb") as f:
            shutil.copyfileobj(r, f)
        if sha(archive) != "8d97a97984f6cbd2b85fe4c60a743440a347544bf18818048e611f5288d46c94":
            raise RuntimeError("Gradle checksum mismatch")
        with zipfile.ZipFile(archive) as z:
            z.extractall(root)
    return binary

def execute(command, cwd, log, timeout):
    with log.open("w", encoding="utf-8") as output:
        proc = subprocess.Popen(command, cwd=cwd, stdout=output, stderr=subprocess.STDOUT)
        try:
            if proc.wait(timeout=timeout):
                raise RuntimeError("Fixture process failed")
        finally:
            if proc.poll() is None:
                subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"],
                               stdout=output, stderr=subprocess.STDOUT, timeout=30)
                proc.wait(timeout=30)

def connected_probe(root, mods, binary, project):
    # A disposable server and client on the same interactive runner; no external access.
    shared = root / "connected"
    shared.mkdir()
    server_dir = root / "server"
    server_dir.mkdir()
    shutil.copytree(mods, server_dir / "mods")
    launcher = server_dir / "server.jar"
    with urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.19.5/1.0.3/server/jar", timeout=90) as r, launcher.open("wb") as f:
        shutil.copyfileobj(r, f)
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        port = listener.getsockname()[1]
    (server_dir / "eula.txt").write_text("eula=true\n")
    (server_dir / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=3\nsimulation-distance=3\nspawn-protection=0\n")
    live = root / "live"
    live.mkdir()
    (live / "options.txt").write_text("version:3465\nonboardAccessibility:false\npauseOnLostFocus:false\n", encoding="utf-8")
    server_log = OUT / "connected-server.log"
    client_log = OUT / "connected-client.log"
    with server_log.open("w", encoding="utf-8") as output:
        server = subprocess.Popen(["java", "-Xmx3G", "-Dtown.test.connected=" + str(shared),
                                   "-jar", str(launcher), "nogui"], cwd=server_dir,
                                  stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT)
        try:
            for _ in range(180):
                if "Done (" in server_log.read_text(encoding="utf-8", errors="replace"):
                    break
                if server.poll() is not None:
                    raise RuntimeError("Connected fixture server exited")
                time.sleep(1)
            else:
                raise RuntimeError("Connected fixture startup timeout")
            command = [str(binary), "-p", str(project), "--no-daemon", "--console=plain",
                       "-PtownRuntimeMods=" + str(mods), "-PtownRunDir=" + str(live),
                       "-PtownConnectedDir=" + str(shared), "-PtownServerAddress=127.0.0.1:" + str(port),
                       "runTownClient"]
            # Retry only an explicitly observed pre-join login timeout; never
            # replay a failed gameplay phase or a partially mutated world.
            for attempt in (1, 2):
                try:
                    execute(command, root, client_log, 360)
                    break
                except (subprocess.TimeoutExpired, RuntimeError):
                    server_text = server_log.read_text(encoding="utf-8", errors="replace")
                    client_text = client_log.read_text(encoding="utf-8", errors="replace")
                    if (attempt == 2 or server.poll() is not None
                            or "TownVisualCI joined the game" in server_text
                            or (shared / "phase").exists()
                            or "ReadTimeoutException" not in client_text):
                        raise
                    shutil.copy2(client_log, OUT / "connected-client-first-attempt.log")
                    print("TOWN_CONNECTED_RETRY pre-join read timeout", flush=True)
            server_text = server_log.read_text(encoding="utf-8", errors="replace")
            if "TOWN_CONNECTED_PHASE owner_complete" not in server_text:
                raise RuntimeError("Owner client did not complete before visitor")
            visitor_dir = root / "visitor"
            visitor_dir.mkdir()
            (visitor_dir / "options.txt").write_text("version:3465\nonboardAccessibility:false\npauseOnLostFocus:false\n", encoding="utf-8")
            visitor_log = OUT / "connected-visitor.log"
            visitor_command = [str(binary), "-p", str(project), "--no-daemon", "--console=plain",
                               "-PtownRuntimeMods=" + str(mods), "-PtownRunDir=" + str(visitor_dir),
                               "-PtownConnectedDir=" + str(shared), "-PtownServerAddress=127.0.0.1:" + str(port),
                               "-PtownProbePlayer=TownVisitorCI", "runTownClient"]
            for attempt in (1, 2):
                try:
                    execute(visitor_command, root, visitor_log, 360)
                    break
                except (subprocess.TimeoutExpired, RuntimeError):
                    server_text = server_log.read_text(encoding="utf-8", errors="replace")
                    visitor_text = visitor_log.read_text(encoding="utf-8", errors="replace")
                    if (attempt == 2 or server.poll() is not None or "TownVisitorCI joined the game" in server_text
                            or "TOWN_CONNECTED_PHASE visitor_" in server_text or "ReadTimeoutException" not in visitor_text):
                        raise
                    shutil.copy2(visitor_log, OUT / "connected-visitor-first-attempt.log")
                    print("TOWN_CONNECTED_VISITOR_RETRY pre-join read timeout", flush=True)
            server_text = server_log.read_text(encoding="utf-8", errors="replace")
            if "TOWN_CONNECTED_VISITOR_MAP_OK" not in visitor_log.read_text(encoding="utf-8", errors="replace"):
                raise RuntimeError("Visitor read-only UI proof missing")
            for marker in ("FOUND_DENIED", "FOUNDED", "MCA_BOUND", "INFRASTRUCTURE_DENIED", "CAMPFIRE", "REGISTER", "DUPLICATE", "RESIDENCE", "DUPLICATE_HOME", "SERVER", "VISITOR"):
                if "TOWN_CONNECTED_" + marker + "_OK" not in server_text:
                    raise RuntimeError("Missing connected server marker: " + marker)
            if not (shared / "town-connected-map.png").is_file() or not (shared / "ack-map").is_file():
                raise RuntimeError("No server-fed map evidence")
            print("TOWN_CONNECTED_END_TO_END_OK")
        finally:
            if server.poll() is None:
                try:
                    server.communicate(b"stop\n", timeout=45)
                except subprocess.TimeoutExpired:
                    subprocess.run(["taskkill", "/PID", str(server.pid), "/T", "/F"], timeout=30)
                    server.wait(timeout=30)
            if (shared / "town-connected-map.png").is_file():
                shutil.copy2(shared / "town-connected-map.png", OUT)
            for log in (server_log, client_log, OUT / "connected-visitor.log"):
                if log.is_file():
                    value = log.read_text(encoding="utf-8", errors="replace")
                    print(log.name, value[:4000], "\n--- tail ---\n", value[-20000:])
            for name, directory in (("connected-client-crashes", live), ("connected-server-crashes", server_dir)):
                if (directory / "crash-reports").is_dir():
                    shutil.copytree(directory / "crash-reports", OUT / name, dirs_exist_ok=True)
    if server.returncode != 0:
        raise RuntimeError("Connected fixture server did not stop cleanly")

def main():
    mode = sys.argv[1]
    if mode not in ("server", "client"):
        raise ValueError(mode)
    OUT.mkdir(parents=True, exist_ok=True)
    selected = []
    for mid in ("mcatowns", "mcatowns_gametest"):
        matches = [p for p in (ROOT / "build/libs").glob("*.jar")
                   if not p.name.endswith(("-dev.jar", "-sources.jar")) and metadata(p).get("id") == mid]
        if len(matches) != 1:
            raise RuntimeError("Expected one remapped jar for " + mid)
        selected.extend(matches)
    fixture = ROOT / "build/town-desktop-inputs"
    manifest = json.loads((fixture / "manifest.json").read_text(encoding="utf-8"))
    installed = list((fixture / "jars").glob("*.jar"))
    if mode == "server":
        names = set(manifest["baseline_jars"])
        assert names and names <= {p.name for p in installed}, "Missing baseline dependency"
        installed = [p for p in installed if p.name in names]
        print("TOWN_BASELINE_RUNTIME", sorted(names))
    elif "numismatics" not in manifest["roots"]:
        raise RuntimeError("Connected founding requires installed Numismatics")
    selected.extend(installed)
    if mode == "server":
        game_test_jars = [p for p in (ROOT / "build/town-test-runtime").glob("*.jar")
                          if metadata(p).get("id") == "fabric-gametest-api-v1"]
        if len(game_test_jars) != 1:
            raise RuntimeError("Expected standalone production Fabric GameTest module")
        selected.extend(game_test_jars)
        print("TOWN_GAMETEST_RUNTIME_STAGED", game_test_jars[0].name, sha(game_test_jars[0]))
    (OUT / "runtime.json").write_text(json.dumps({p.name: sha(p) for p in selected}, indent=2))
    with tempfile.TemporaryDirectory(prefix="town-", dir=os.environ["RUNNER_TEMP"], ignore_cleanup_errors=True) as tmp:
        root = Path(tmp)
        run = root / "game"
        mods = run / "mods"
        mods.mkdir(parents=True)
        for p in selected:
            shutil.copy2(p, mods / p.name)
        log = OUT / (mode + ".log")
        try:
            if mode == "server":
                (run / "eula.txt").write_text("eula=true\n")
                (run / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=0\nonline-mode=true\n")
                launcher = run / "server.jar"
                with urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.19.5/1.0.3/server/jar", timeout=90) as r, launcher.open("wb") as f:
                    shutil.copyfileobj(r, f)
                report = run / "gametest-results.xml"
                execute(["java", "-Xmx3G", "-Dfabric-api.gametest", "-Dtown.test.mca=true",
                         "-Dfabric-api.gametest.report-file=" + str(report), "-jar", str(launcher), "nogui"], run, log, 600)
                import xml.etree.ElementTree as ET
                if not report.is_file():
                    raise RuntimeError("No production GameTest report")
                xml = ET.parse(report)
                cases = xml.findall(".//testcase")
                if len(cases) < 13 or xml.findall(".//failure") or xml.findall(".//error"):
                    raise RuntimeError("Production GameTest failure or incomplete report")
                if "TOWN_MCA_WORLD_OK" not in log.read_text(encoding="utf-8", errors="replace"):
                    raise RuntimeError("MCA world marker absent")
                print("TOWN_PRODUCTION_SERVER_OK", len(cases))
                # Separate normal server world, same remapped production jars; two genuine launches.
                restart = root / "restart"
                (restart / "mods").mkdir(parents=True)
                for p in selected:
                    if metadata(p).get("id") != "fabric-gametest-api-v1":
                        shutil.copy2(p, restart / "mods" / p.name)
                shutil.copy2(launcher, restart / "server.jar")
                (restart / "eula.txt").write_text("eula=true\n")
                (restart / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=0\nonline-mode=true\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=3\nsimulation-distance=3\n")
                for phase in ("create", "verify"):
                    phase_log = OUT / ("restart-" + phase + ".log")
                    try:
                        execute(["java", "-Xmx3G", "-Dtown.test.restart=" + phase, "-jar",
                                 str(restart / "server.jar"), "nogui"], restart, phase_log, 240)
                        if "TOWN_RESTART_" + phase.upper() + "_OK" not in phase_log.read_text(encoding="utf-8", errors="replace"):
                            raise RuntimeError("Missing restart marker " + phase)
                    finally:
                        if phase_log.is_file():
                            print(phase_log.read_text(encoding="utf-8", errors="replace")[-12000:])
                restart_text = (OUT / "restart-verify.log").read_text(encoding="utf-8", errors="replace")
                for marker in ("TOWN_RESTART_UNLOADED_FOOTPRINT_OK", "TOWN_RESTART_UNLOADED_DERIVED_OK"):
                    if marker not in restart_text:
                        raise RuntimeError("Missing restart proof: " + marker)
                print("TOWN_PRODUCTION_RESTART_OK")
            else:
                (run / "options.txt").write_text("version:3465\nonboardAccessibility:false\npauseOnLostFocus:false\n", encoding="utf-8")
                project = root / "probe"
                shutil.copytree(ROOT / "tools/production-client", project)
                binary = gradle()
                command = [str(binary), "-p", str(project), "--no-daemon", "--console=plain",
                           "-PtownRuntimeMods=" + str(mods), "-PtownRunDir=" + str(run), "runTownClient"]
                execute(command, root, log, 600)
                marker = run / "town-title-ready.txt"
                if not marker.is_file() or not (run / "town-title.png").is_file():
                    raise RuntimeError("Client title evidence missing")
                if not marker.read_text().startswith("TOWN_MCA_CLIENT_TITLE_OK"):
                    raise RuntimeError("Invalid client marker")
                map_marker = run / "town-map-ready.txt"
                if not map_marker.is_file() or not (run / "town-map.png").is_file():
                    raise RuntimeError("Blueprint map screenshot evidence missing")
                if not map_marker.read_text().startswith("TOWN_BLUEPRINT_MAP_OK"):
                    raise RuntimeError("Invalid blueprint map marker")
                print(marker.read_text())
                print(map_marker.read_text())
                connected_probe(root, mods, binary, project)
        finally:
            for name in ("gametest-results.xml", "town-title-ready.txt", "town-title.png", "town-map-ready.txt", "town-map.png", "logs/latest.log"):
                src = run / name
                if src.is_file():
                    shutil.copy2(src, OUT / src.name)
            if (run / "crash-reports").is_dir():
                shutil.copytree(run / "crash-reports", OUT / "crash-reports", dirs_exist_ok=True)
            if log.is_file():
                value = log.read_text(encoding="utf-8", errors="replace")
                print(value[:5000], "\n--- tail ---\n", value[-20000:])

if __name__ == "__main__":
    main()

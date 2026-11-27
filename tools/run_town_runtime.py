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
    selected.extend((ROOT / "build/town-desktop-inputs/jars").glob("*.jar"))
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
                if len(cases) < 5 or xml.findall(".//failure") or xml.findall(".//error"):
                    raise RuntimeError("Production GameTest failure or incomplete report")
                if "TOWN_MCA_WORLD_OK" not in log.read_text(encoding="utf-8", errors="replace"):
                    raise RuntimeError("MCA world marker absent")
                print("TOWN_PRODUCTION_SERVER_OK", len(cases))
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
                print(marker.read_text())
        finally:
            for name in ("gametest-results.xml", "town-title-ready.txt", "town-title.png", "logs/latest.log"):
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

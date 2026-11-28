"""Desktop-only read-only dependency discovery, based on the verified CMobs harness."""
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "town-desktop-inputs"
# Core dependencies already pinned by the existing Loom build.
BUILTINS = {"minecraft", "java", "fabricloader"}
OPTIONAL = {"farmersdelight", "farm_and_charm", "bountifulfares", "immersive_weathering", "numismatics"}

def sha(path):
    with path.open("rb") as f:
        return hashlib.file_digest(f, "sha256").hexdigest()

def metadata(path):
    try:
        with zipfile.ZipFile(path) as z:
            return json.loads(z.read("fabric.mod.json"))
    except (KeyError, ValueError, zipfile.BadZipFile):
        return {}

def discover():
    configured = os.environ.get("TOWN_MOD_SOURCE", "").strip()
    override = os.environ.get("ADADV_INSTANCE_PATH", "").strip()
    roots = [Path(configured)] if configured else []
    if not configured:
        if override:
            roots.append(Path(override))
        else:
            # DevHub background service may run under a different Windows account.
            drive = Path(os.environ.get("SystemDrive", "C:") + "/")
            profiles = {Path.home(), Path(os.environ.get("PUBLIC", drive / "Users/Public")).parent}
            try:
                profiles.update(p for p in (drive / "Users").iterdir() if p.is_dir())
            except OSError:
                pass
            parents = {drive / "CurseForge/Minecraft/Instances"}
            for profile in profiles:
                parents.update(profile / part for part in (
                    "curseforge/minecraft/Instances", "Documents/Curse/Minecraft/Instances",
                    "Documents/CurseForge/Minecraft/Instances", "CurseForge/Minecraft/Instances"))
            matches = set()
            for parent in parents:
                try:
                    matches.update(p.resolve() for p in parent.iterdir()
                                   if p.name.casefold() == "adadv-2.0" and (p / "mods").is_dir())
                except OSError:
                    continue
            if len(matches) > 1:
                raise RuntimeError("Multiple ADADV-2.0 instances; set TOWN_MOD_SOURCE to the intended instance.")
            roots.extend(matches)
    jars = {}
    for root in roots:
        if not root.is_dir():
            continue
        dirs = [root] if root.name.lower() in {"mods", "jars"} else [root / "mods"]
        for folder in ("automodpack", ".automodpack"):
            base = root / folder
            dirs.append(base / "mods")
            try:
                dirs.extend(p / "mods" for p in (base / "modpacks").iterdir())
            except OSError:
                pass
        # A separately configured dependency directory may contain JARs directly.
        if configured and root == Path(configured):
            dirs.append(root)
        for directory in dirs:
            try:
                for p in directory.glob("*.jar"):
                    jars[str(p.resolve())] = p
            except OSError:
                continue
    print("TOWN_DISCOVERY", len(roots), "source roots,", len(jars), "candidate JARs")
    return list(jars.values())


def ids_in(jar):
    def walk(z):
        try:
            meta = json.loads(z.read("fabric.mod.json"))
        except KeyError:
            return set()
        ids = {meta["id"], *meta.get("provides", [])}
        for entry in meta.get("jars", []):
            with zipfile.ZipFile(io.BytesIO(z.read(entry["file"]))) as nested:
                ids.update(walk(nested))
        return ids
    with zipfile.ZipFile(jar) as z:
        return walk(z)


def resolve_closure(records, roots):
    required = set(roots)
    provided = set(BUILTINS)
    chosen = {}
    while required - provided:
        mid = min(required - provided, key=lambda x: (x != "fabric-api", x))
        matches = [(p, m) for p, m in records if mid == m["id"] or mid in m.get("provides", [])]
        if not matches:
            matches = [(p, m) for p, m in records if m.get("jars") and mid in ids_in(p)]
        if not matches or len({sha(p) for p, _ in matches}) != 1:
            raise RuntimeError("Missing or ambiguous fixture dependency: " + mid)
        p, m = matches[0]
        if p.name in chosen and sha(chosen[p.name]) != sha(p):
            raise RuntimeError("Conflicting fixture filename: " + p.name)
        chosen[p.name] = p
        provided.update(ids_in(p))
        required.update(m.get("depends", {}))
    return chosen


def main():
    records = [(p, metadata(p)) for p in discover()]
    records = [(p, m) for p, m in records if m and m.get("environment") != "client"
               and m.get("id") not in {"automodpack", "mcatowns"}]
    roots = {"mca", "fabric-api"} | {m["id"] for _, m in records if m["id"] in OPTIONAL}
    # Create brings its own GameTests via Numismatics. Keep the original
    # farming baseline separate; connected gameplay still includes Numismatics.
    baseline = resolve_closure(records, roots - {"numismatics"})
    chosen = resolve_closure(records, roots)
    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT / "jars").mkdir(parents=True)
    manifest = {"roots": sorted(roots), "baseline_jars": sorted(baseline), "jars": {}}
    for name, src in chosen.items():
        target = OUT / "jars" / name
        shutil.copy2(src, target)
        manifest["jars"][name] = {"sha256": sha(target), "id": metadata(target)["id"], "version": metadata(target)["version"]}
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps(manifest, indent=2))
    print("TOWN_DESKTOP_INPUTS_READY")

if __name__ == "__main__":
    main()

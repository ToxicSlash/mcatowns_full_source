"""Verify the short-lived private artifact before either runner uses it."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "build" / "town-desktop-inputs"
manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
jars = {p.name: p for p in (root / "jars").glob("*.jar")}
assert set(jars) == set(manifest["jars"]), "Fixture file set differs"
assert "mca" in manifest["roots"], "MCA absent"
for name, p in jars.items():
    with p.open("rb") as f:
        assert hashlib.file_digest(f, "sha256").hexdigest() == manifest["jars"][name]["sha256"], name
print("TOWN_FIXTURE_HASHES_OK")

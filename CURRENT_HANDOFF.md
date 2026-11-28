# MCA Towns — current handoff

Branch: dev only. Keep main unchanged. Fabric 1.20.1 / Java 17 compilation; Java 21 reduced production fixture.

## Verified checkpoint
- b222c8f: standard run 46108448778 and DevHub run 46108448797 both passed. All 8 required production GameTests, 13 modded crop types and 6 farmland types; Immersive Weathering weeds excluded. Actual shutdown/restart preserved registered farm UUID, bounds, crop count, blocks and duplicate guard. Client actual map pixels verified (156258 background, 4410 outline) using synthetic map data. Rectangular/L/U and door/window/slab overhang world fixtures passed.
- Both DevHub runners and desktop artifact transfer verified. Read shared DEVHUB_RUNNER_STANDARD.md before modifying workflows. Keep installed runtime jars unchanged, compile with original Loom, stage standalone GameTest module, wait for overlays and validate screenshot pixels. Original pack untouched.
- Earlier baseline 25b4234 passed compilation and startup (not MCA gameplay).

## Active batch
- Added connected disposable loopback production server/client probe using actual normal blueprint C2S packets and real survival player. Numismatics added to desktop dependency discovery when installed; actual crown required. Verify rejected founding without crown, successful founding with exact crown/scrap payment, real farm inspection, registration payment, second-end duplicate denial without payment, server-fed map footprint and screenshot. Record actual MCA link diagnostics; successful player settlement does not alone prove MCA native village binding. New run pending. Connected probe uses test-only fixture player and world, no changes to user saves. MCA onboarding may interfere; inspect logs rather than bypassing gameplay requirements.
- Registration now checks target town boundary and loaded target after inspection, not just player location.
- Outstanding: actual MCA native building recognition and residence validation, multiplayer non-owner authority, multi-room/elevation footprints, user interface interactions beyond packet probe, actual chunk unload/anchor repair and full pack compatibility.

## Stages
1. Essential regression infrastructure and real MCA/player founding, authority and gameplay.
2. Farm and building detection / map correctness, existing systems verification.
3. Profile town ticks, building scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 modpack integration (exclude AutoModpack itself).
A queued build is progress, not completion. Keep passing and pending evidence distinct.

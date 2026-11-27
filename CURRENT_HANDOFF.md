# MCA Towns — current handoff

Branch: dev only. Keep main unchanged. Fabric 1.20.1 / Java 17.

## Baseline
- 25b4234: GitHub run 45926609707 passed build, unit tests, dedicated server startup and headless client startup.
- These startup tests are not proof of MCA gameplay: local MCA runtime jar is optional in build.gradle.
- New DevHub background/visual runners reported by user; exact status not accessible from the runner admin endpoint. Other ADADV workflows use Windows self-hosted labels background / visual-test (variable overrides); verify labels here before dispatch.

## Active batch
- Farm registration: connected field traversal, irrigation/path gaps, stepped rows, crop/soil selection, duplicate farm guard, loaded-chunk safety and regression tests. New tests pending CI.
- Blueprint now uses traced enclosed floor cells instead of MCA's raw all-height block projection. Render/hover/selection share a wall-boundary geometry helper; fallback is a location marker, not a fabricated rectangle. Registered and detected farm tiles feed the same geometry. Unit tests cover rectangle/L/U/duplicate cells. Still need live fixtures for overhangs, doors, furniture, multi-room buildings, multiple elevations, loaded/unloaded chunks, and exact visual appearance.
- Confirm actual crop varieties and farmland types from ADADV pack; avoid broadening crop rules to weeds. Test fields with different irrigation and paths, chunk edges, partial unload/reload, existing registered farm persistence and duplicate attempts.

## Stages
1. Essential regression infrastructure: unit tests, real MCA dedicated server and client, gameplay fixture, background/visual runner evidence, save/restart, authority and town founding.
2. Farm and building detection / map correctness, then existing systems verification.
3. Profile town ticks, building scans, saved state, network and unloaded activity.
4. UI consolidation; then military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 modpack integration (exclude AutoModpack itself); preserve original instance/saves.

No gameplay tests passed for this new batch yet. Keep pending CI and live verification separate from completed work.

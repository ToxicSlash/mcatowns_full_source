# MCA Towns — current handoff

Branch: dev only. Keep main unchanged. Fabric 1.20.1 / Java 17.

## Baseline
- 25b4234: GitHub run 45926609707 passed build, unit tests, dedicated server startup and headless client startup.
- These startup tests are not proof of MCA gameplay: local MCA runtime jar is optional in build.gradle.
- Shared DEVHUB_RUNNER_STANDARD.md read. New DevHub integration workflow uses background / visual-test variable routing, CMD and copied portable Python provisioning, desktop dependency discovery and one-day hash-verified private dependency artifact. Run 45984816479 verified both runner identities, interactive session, installed MCA 7.6.26, four farming mods and hash transfer. Both runtime jobs failed before launch because an installed jar uses Loom 1.16.3 and this project uses 1.8.13. Compile separately and run unmodified production jars; added remapped test-only jar, direct Fabric server and isolated Loom production client. New runtime run pending. No original pack files are changed.

## Active batch
- Farm registration: connected field traversal, irrigation/path gaps, stepped rows, crop/soil selection, duplicate farm guard, loaded-chunk safety and regression tests. Run 45973460812 compiled but failed six FarmFieldScanner tests during static initialization (registry-dependent crop tag). Deferred initialization to actual world queries; run 45973724988 passed compilation and unit tests; startup checks were still running at last inspection.
- Blueprint now uses traced enclosed floor cells instead of MCA's raw all-height block projection. Render/hover/selection share a wall-boundary geometry helper; fallback is a location marker, not a fabricated rectangle. Registered and detected farm tiles feed the same geometry. Unit tests cover rectangle/L/U/duplicate cells. Added isolated Fabric GameTest fixtures for real-world irrigated/pathed fields, a separating fence, rectangular and concave rooms with overhanging roofs. All four world fixtures passed in run 45974024473, as did unit tests and server/client startup. Still need doors, furniture, multi-room buildings, multiple elevations, loaded/unloaded chunks, actual MCA integration and exact visual appearance.
- Confirm actual crop varieties and farmland types from ADADV pack; avoid broadening crop rules to weeds. Test fields with different irrigation and paths, chunk edges, partial unload/reload, existing registered farm persistence and duplicate attempts.

## Stages
1. Essential regression infrastructure: unit tests, real MCA dedicated server and client, gameplay fixture, background/visual runner evidence, save/restart, authority and town founding.
2. Farm and building detection / map correctness, then existing systems verification.
3. Profile town ticks, building scans, saved state, network and unloaded activity.
4. UI consolidation; then military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 modpack integration (exclude AutoModpack itself); preserve original instance/saves.

Baseline world fixtures passed. New DevHub tests require installed MCA blueprint registration and capture a client title screenshot; this is startup/registry integration, not proof of town UI or full gameplay. Installed farming block inventory is logged. Installed crop implementations are now exercised against farmland in GameTest; report required. Full modpack, actual MCA registration, farm restart persistence and live UI tests remain outstanding. Keep pending CI and live verification separate from completed work. Test mod sources are isolated in src/gametest; run ./gradlew runGametest. CI uploads reports and logs.

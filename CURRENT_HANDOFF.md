# MCA Towns — current handoff

Branch dev only; keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture. Founding requires one Numismatics Crown currency and ten scraps; retain.

## Verified checkpoint
- de23adc: Linux 46131195587 and DevHub 46131195585 passed all 14 production world tests, connected owner/visitor gameplay, restart/no chunk force-load. Sparse 1024-town trade test: reference 61.0236 ms, indexed build+query 9.2939 ms (~6.6x); results identical. Not whole-server TPS.
- 6e719cc: 67 change-only setters, farm cache world isolation and scan optimisations passed. Prior verified 13 crop/6 soil types, weeds excluded; rectangular/L/U, furnished, connected/disconnected rooms, doors and overhangs, persisted footprints, anchor recovery, actual Crown founding, duplicate rejection and ownership. MCA bell candidate bound mca_0; native house lookup not-detected and mature native village unproved. External Create GameTests remain unresolved (split runtime verified).
- Read shared DEVHUB_RUNNER_STANDARD.md; original pack untouched.

## Active batch
- Spread each existing 200-tick town cycle over the first 20 ticks, contiguous balanced slices preserving original ordering. Reuse one context/data/index snapshot per cycle, drop incomplete cycle on world unload/time jump, skip deleted player towns. Daily/stat/tax guards retained, business eligibility uses original cycle start, guard/weed cycle numbers unchanged. Actual processing may occur up to 19 ticks later; newly added towns enter next cycle. Real-world cadence and performance pending verification.
- Add 15th production world test checking exact once-only coverage and balanced slices from zero to 10,000 towns. At 1024 towns expected peak 52 rather than 1024; this is work distribution, not MSPT. Connected fixture now requires actual live tick profile marker with total and peak duration, includes cycle preparation. World-specific villager visit cooldown and cleanup, fix first-cleanup Long.MIN_VALUE subtraction overflow, clear on unload; remove redundant disconnect callback already present in ModNetworking.
- Outstanding: native MCA house recognition, multi-level/stepped outlines, profiling representative full pack, blueprint packet/geometry optimisation and full-pack integration. Core Stages 1/2 verified but native/vertical gaps open.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

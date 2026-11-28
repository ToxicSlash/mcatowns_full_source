# MCA Towns — current handoff

Branch dev only; keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture. Founding requires one Numismatics Crown currency and ten scraps; retain.

## Verified checkpoint
- 6e719cc: Linux 46129628623 and DevHub 46129628605 passed. All 13 production world tests, remote restart/no chunk force-load, owner/visitor permission and duplicate protection, server-fed map. Change-only setters, world-specific farm weed caches and early blueprint scan optimisations passed.
- 8d4df69 verified original 13 world tests and remote derived infrastructure updates. 13 modded crop and 6 soil types; weeds excluded; rectangle/L/U, furnished, connected/disconnected rooms, doors and overhangs, persistent footprints and anchor recovery. MCA bell candidate binding mca_0 passed; native house lookup not-detected; mature native village unproved. External Create GameTests remain unresolved (split fixture verified).
- Read shared DEVHUB_RUNNER_STANDARD.md; original pack untouched.

## Active batch
- Per-cycle trade link spatial index uses 3x3 neighbor buckets; preserves 3D distance and reads live eligibility, avoiding stale cached posts. Reference comparison across negative positions, ranges and mutations; real production stress regression with 1024 sparse towns, records reference and indexed timings including index construction (no hardware-dependent speed threshold). 14 required tests.
- Session state bound to actual ServerWorld and cleared on disconnect/world unload. Reject out-of-bound/unloaded inspection targets before native building validation. No change to founding costs or trade range. Pending verification.
- Outstanding: native MCA house recognition, multi-level/stepped geometry, broader stage 3 tick scheduling/profile/packet optimization, full-pack integration. Stages 1/2 core acceptance verified but native/vertical gaps still open.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

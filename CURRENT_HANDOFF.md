# MCA Towns — current handoff

Branch dev only; keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture. Founding requires one Numismatics Crown currency and ten scraps; user confirmed retain.

## Verified checkpoint
- 4a2b62b: Linux 46121911950 passed; DevHub 46121911910 visual passed actual owner and visitor sequential login, all founding/costs, campfire/farm/residence, duplicate farm and second-room duplicate residence rejection, server-fed map, visitor read-only and denial of founding, rename, tax, remove town, inspect/register/remove building, no currency loss. Background all 12 world tests passed, but remote restart failed after saved outline/no-load check passed: full building record changed after refresh. Same failure in 46121795657. Prior d82ca3e ordinary restart passed saved footprint and 12 world tests. MCA bell mca_0 binding verified; native house lookup still not-detected; mature native village unproved.
- Verified crop compatibility: 13 crop and 6 soil types; weeds excluded. Rectangular/L/U, furnished, connected/disconnected rooms, doors and roof overhangs verified. Anchor recovery 17 crops, UUID preserved. External Create tests TestFluids.thresholdSwitch and TestItems.attributeFilters remain unresolved; split runtime fixture verified.
- Read shared DEVHUB_RUNNER_STANDARD.md before modifying workflows. Original pack untouched.

## Active batch
- Remote restart created an ACTIVE farm with no community/workers; town-wide refresh legitimately changed its derived status. Initialize fixture with normal derived status before save, retain exact full-record and footprint equality when unchanged, add positive offline infrastructure transition assertion (blocked to understaffed, all other fields unchanged, no chunk load). Require both markers. Not a confirmed chunk-loading defect.
- Production inspection skips all buildings with unloaded bounds, reuses farm scan for persisted geometry instead of scanning twice, only draws farm tiles for farms, excludes open structures from enclosure outlines; new open-structure marker regression. 13 required production world tests. Pending verification.
- Outstanding: native MCA house recognition, mature village, multi-level/stepped outlines, broader stage 3 profiling and full-pack integration. Continue from latest actual run, not an earlier green run.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

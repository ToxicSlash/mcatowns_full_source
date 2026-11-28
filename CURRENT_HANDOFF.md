# MCA Towns — current handoff

Branch: dev only. Keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture.

## Verified checkpoint
- 1839cd2: runs 46117524414 and 46117524408 passed. All 11 production world regressions (incl connected/disconnected rooms, furnishing, rectangle/L/U, roof overhangs). Actual survival founding, MCA bell candidate binding (mca_0), campfire/farm/duplicate rejection, furnished residence registration/capacity, exact 21-cell server-fed outline screenshot. Native house lookup at sample block returned not-detected; local enclosure fallback registered it. Mature MCA village and native residence recognition not proved.
- 9940671: 13 modded crop and 6 soil types; weeds excluded. Real normal-server restart verified farm UUID, bounds, crop count and duplicate guard.
- Shared DEVHUB_RUNNER_STANDARD.md read; verified split dependency fixture. External Create tests TestFluids.thresholdSwitch and TestItems.attributeFilters previously failed, remain unresolved. Original user pack unchanged.

## Active batch
- Save version 3 stores bounded/deduplicated verified geometry per building UUID (legacy saves without footprint still read). Use known geometry when saved bounds are unloaded; loaded broken enclosure gets location marker, not fabricated outline. Registration checks exact 3D shape overlap to prevent duplicate houses from another room. Daily inspection stores verified shape; farm anchor recovery limited to previously saved field tiles and avoids other registered buildings. Standard run 46119421316 passed; background job 140900623070 in 46119421308 passed all 12 world tests and normal restart with saved footprint. Visual title/synthetic map passed, but loopback login timed out before player join (ReadTimeoutException, null profile ID), no gameplay phase executed.
- Added world shape overlap (connected/disconnected rooms), saved shape roundtrip, broken-anchor regression (17 crops, identity preserved), actual restart checks persisted tiles. 12 world tests required. New connected probe tests second-room inspection/duplicate rejection and exact costs. Added one bounded retry only for documented pre-join ReadTimeoutException while server remains healthy and no phase started; preserve first attempt log. New restart fixture persists a real remote field, checks saved outline and inspection without loading the distant chunk, then loads it and checks the real crops. Pending verification. Crown currency and 10 scraps remain required for founding per user clarification.
- Outstanding: native MCA house recognition, non-owner multiplayer authority, multi-level/stepped floor outlines, real chunk unloading, performance profiling, full-pack integration.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

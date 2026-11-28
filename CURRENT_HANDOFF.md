# MCA Towns — current handoff

Branch dev only; keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture. Founding requires one Numismatics Crown currency and ten scraps; retain.

## Verified checkpoint
- 8d4df69: standard 46125938131 and DevHub 46125938080 both passed. All 13 production GameTests, actual remote farm restart with no chunk load while viewing or refreshing geometry, derived infrastructure transition while unloaded, real crops after load. Owner and visitor client runs passed including native bell candidate binding mca_0, founding costs, community/farm/residence progression, duplicate field and house rejection, real server-fed map, non-owner mutation denial. Native MCA house lookup still not-detected; mature village not verified.
- 13 modded crop and 6 soil types; weeds excluded; rect/L/U, furnished, connected/disconnected rooms, doors and overhangs, persistent footprints, 17-crop anchor recovery with UUID preserved. External Create tests TestFluids.thresholdSwitch and TestItems.attributeFilters remain unresolved; split fixture verified.
- Read shared DEVHUB_RUNNER_STANDARD.md; original pack untouched.

## Active batch
- Stage 3 first optimisation pass: 67 primitive/String setters mark dirty only for actual changes; added dirty-state unit regression. Periodic detection checks blueprint before ownership lookup; nearest farm selection returns soil seed without redundant connected scan; nearby marker/furniture queries skip unloaded/out-of-border positions. Minecraft profiler sections for blueprint detection/town tick/town simulation. Staffed farm weed cache keyed by actual ServerWorld instead of reused dimension name, cleared on world unload; reuse refreshed list for maintenance rather than rebuilding it twice.
- Pending consolidated tests. No measured TPS improvement yet; profiler sections enable next measurement. No change to weed chance, crop thresholds, town progression, currency or maintenance interval.
- Outstanding: mature/native MCA building recognition, multi-level/stepped floor geometry, further profiling and cache invalidation, actual full-pack integration. Stages 1/2 core acceptance verified but native/vertical gaps still open.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

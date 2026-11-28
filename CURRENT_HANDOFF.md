# MCA Towns — current handoff

Branch: dev only. Keep main unchanged. Fabric 1.20.1 / Java 17 compile, Java 21 reduced production fixture.

## Verified checkpoint
- 9940671: standard run 46115465507 and DevHub run 46115465515 both passed. 9 real-world production tests (including furnished-room, rectangular/L/U, exterior door/window/slab overhang), 13 modded crop and 6 soil types, weeds excluded. Real normal-server restart preserved farm UUID, bounds, crops, blocks and duplicate guard.
- Same run connected actual survival player via loopback: crown denial and exact founding payment, real MCA bell candidate id 0 (isVillage=false), farm inspection, infrastructure denial, campfire registration, farm registration, duplicate rejection without currency consumption, server-fed farm map screenshot. All passed. Standalone baseline dependency closure separated from Numismatics/Create connected closure; external Create tests TestFluids.thresholdSwitch and TestItems.attributeFilters previously failed and remain unresolved.
- Read shared DEVHUB_RUNNER_STANDARD.md; both runners, original production dependency jars, artifact hash transfer, standalone GameTest module, real framebuffer validation verified. Original user pack untouched.
- Earlier baseline b222c8f passed 8 production GameTests, restart and synthetic map rendering.

## Active batch
- New bounded door-connected room tracing merges only independently enclosed rooms and their internal partitions. Exterior door/roof overhang excluded, unconnected rooms separate. Added two world fixtures; 11 required GameTests. Guard loaded/height/border for roof and solid queries. Retest required.
- Connected survival probe now asserts MCA bell candidate mapping to founded player settlement, builds a furnished two-room home after founding, inspects/registers residence through normal packets, verifies exact cost, capacity and 21-cell server-fed outline, logs actual MCA house recognition. MCA mature native village (isVillage=true) not yet proved. Pending live results; do not conflate player-founded settlement with mature MCA village.
- Outstanding: native MCA residence recognition if not verified, multiplayer non-owner authority, multi-level/stepped buildings, duplicate residence prevention, persistent footprint for unloaded chunks, anchor repair, full pack integration.

## Stages
1. Essential regression infrastructure, real MCA/player founding, authority and gameplay.
2. Farm and building detection/map correctness and existing systems verification.
3. Profile town ticks, scans, saved state, network and unloaded activity.
4. UI consolidation, military/bandits, trade, specialists per docs/IMPLEMENTATION_PLAN.md.
5. Disposable ADADV-2.0 integration (exclude AutoModpack itself).
Queued runs are progress, not verified completion.

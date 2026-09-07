# MCA Towns TODO

This file tracks planned systems and design questions that are not yet final implementation decisions. See `docs/IMPLEMENTATION_PLAN.md` for the current approved direction and existing-code alignment.

## Initial pre-End release scope

- [ ] Prioritize town upgrading, workers, specialists/research, basic Bandit Activity, festivals, travelling caravans and the upcoming economy revision.
- [ ] Develop progression/content through roughly Town Stage 3 for this release; later town stages may remain present in legacy code but do not need complete content yet.
- [ ] Defer Harbour/Shipments, simulated town-to-town trade, Adventurer's Guild, player co-mayor/member permissions, villager-trade town interaction and advanced raid integration.

## Bounties and Requests

- [x] Bounties come only from the Bountiful Bounty Board; MCA Towns no longer treats generic hostile kills as bounties.
- [x] Retire the legacy MCA Towns 30-hostile-kill bounty counter from player-facing gameplay while keeping old save data readable.
- [ ] Design/implement an Architect-provided Bounty Decree (or equivalent Bountiful integration) that enables town-themed bounties in the board.
- [ ] Bandit-specific board bounties should reduce Bandit Activity by an additional fixed amount.
- [ ] Decide exact town rewards for completed board bounties.
- [ ] Expand Town Requests beyond Storehouse material deliveries into community/civic needs.
- [x] Keep normal ignored Town Requests non-punitive or only very mildly consequential.
- [x] Present Town Requests, festivals and disaster/problem events together in the Requests / Events UI.
- [ ] Allow only one normal disaster/problem event at a time; Bandit Activity may occur independently.

## Prosperity and Threats

- [x] Add an effective Prosperity Base layer so temporary threats can suppress the current floor without destroying the permanent civic Base. Current suppression is zero until threat rules are approved.
- [ ] Decide exact Prosperity thresholds and mild modifiers.
- [ ] Bandit Activity should mildly reduce town Happiness and Building Output at higher tiers; exact values still need balancing.
- [ ] Do not implement Monster Pressure for this release.

## Bandit Activity

- [x] Use a hidden 0-120 Bandit Activity score with player-facing tiers: Minimal (0-15), Low (16-40), Moderate (41-70), High (71-99), Extreme (100-120).
- [x] Base growth check: every 20 minutes, +5 Activity. Defence reduces the growth chance by 2.5 percentage points per Defence, with a 40% minimum chance.
- [x] If the town owner is offline, natural growth cannot pass High (99); Extreme progression is online-only.
- [x] Tagged MCA Towns bandit kills reduce Activity by 4 by default. Future special bandits can carry a larger reduction value.
- [x] Add saved Wild Band encounter reservations around roughly 300 blocks from a player town without loading their chunks.
  - Capacity: Minimal 1, Low 1, Moderate 2, High 3, Extreme 4.
  - [ ] Physically generate ordinary Pillagers only when a player approaches.
  - [x] If never physically generated within 15 minutes, remove the reservation.
  - [x] Once physically spawned, release its capacity slot after 10 minutes even if survivors unload/despawn; the survivors remain independently taggable for later Activity reductions.
- [x] Add saved Town Band reservations around roughly 70 blocks from the town.
  - Capacity: Minimal 0, Low 1, Moderate 1, High 2, Extreme 4.
  - [ ] Physically spawn ordinary Pillagers only while the relevant town/player area is loaded; custom bandit mobs can replace them later.
- [x] Wild/Town band generation gets one 60% attempt every 5 minutes per town and fills at most one missing slot per successful attempt.
- [x] Add persistent placeholder Bandit Camp site reservations without force-loading distant chunks.
  - Camp roll every 40 minutes: Low 20%, Moderate 40%, High/Extreme 70%.
  - Camp capacities: Low 1, Moderate 2, High/Extreme 3.
  - Candidate sites are chosen around 1,800-2,200 blocks away and must be at least 1,500 blocks from every player town.
  - [ ] Materialise the current placeholder camp as a physical Pillager group when approached.
  - [ ] Future real camp structures should snap/revalidate against the structure-spacing system rather than force-loading arbitrary terrain.
  - [x] Clearing a camp reduces Activity by 20 and places camp replacement on an 80-minute cooldown.
- [x] Track encounter IDs/member counts so clearing a spawned group can immediately free its reservation; Wild reservations also self-release after their 10-minute spawned lifetime.
- [ ] Extreme should later enable a true raid through the custom raid mod. MCA Towns owns the Activity/trigger state; the custom raid mod owns the actual raid waves.
- [ ] Winning a true bandit raid should heavily reduce Activity and may trigger a festival.

## Defence and Guards

- [x] Rename the lightweight town stat from Security to Defence for the new infrastructure system.
- [x] Defence slows Bandit Activity growth and grants affiliated Guard Villagers +1.5 Armour per Defence point.
- [ ] Initial Defence buildings:
  - Barracks: +1 Defence, +6 Guard Capacity at T1 and +6 capacity per building tier; maximum 3 per town.
  - Watchtower: +2 Defence; maximum 10 per town.
  - Outpost: +2 Defence; maximum 10 per town.
- [x] Enforce the Barracks/Watchtower/Outpost per-town cap logic in the building-registration path; Watchtower/Outpost still need their actual building definitions/inspection rules.
- [ ] Barracks upgrades: +1 Guard Attack Damage for each tier above T1.
- [ ] Watchtower/Outpost upgrades: +2 Guard Max Health for each tier above T1.
- [ ] Exact furniture, Prosperity and town-stage requirements for Defence building upgrades still need to be decided.
- [x] Guard Villagers hired by the town consume normal population slots.
- [x] Residence capacity backend: T1 Residence provides +2 population capacity; T2+ provides +4.
- [ ] Add the easy T2 Residence upgrade flow and decide its exact furniture requirements.
- [x] Barracks GUI recruits Guard Villagers using the configured town currency via **Search for Hires**; survival searches take 60 seconds, Creative spawns instantly.
- [ ] Barracks should later allow town-wide Guard stat training/upgrades with daily food upkeep from Town Food reserves.
  - Example: Strength +3 costs 3 Food/day per guard per Strength level = 9 Food/day per guard.
  - Decide the exact trainable stats, maximum levels, purchase costs, downgrade/suspension behaviour when food runs short, and whether upkeep is charged only for living/affiliated guards.

## Festivals

- [ ] Festival opportunities can be triggered by winning a bandit raid, completing a major Town Request/large supply request, or randomly while Prosperity is high.
- [ ] Add a cooldown so festivals remain occasional/special.
- [ ] Decide exact festival types and bonuses later; keep the trigger framework lightweight.

## Trade

### Wandering Caravans

- [x] Rework the current random caravan system as themed Wandering Trader-style encounters.
- [x] Preserve normal merchant interaction/functionality as the base behaviour.
- [ ] Give each caravan theme/type its own custom trade pool.
- [x] Despawn temporary caravan NPCs after a few Minecraft days.
- [ ] Decide exact themed trade pools.

### Town Caravans — deferred

- [ ] Add simulated town-to-town trade separate from Wandering Caravans later.
- [ ] Only one active town trade at a time initially.
- [ ] Add cached/simple export resource pools rather than scanning every building at trade time.
- [ ] Decide final pool categories (candidate ideas: Agriculture, Food/Livestock, Minerals, Manufactured, Luxury, Specialist).
- [ ] Add Trade Capacity and decide its values/sources.
- [ ] Add route risk/success affected by Bandit Activity, Defence and escorts.
- [ ] Add uncommon caravan interruption/rescue events.
- [ ] Allow up to two Guard Villagers as simple caravan escorts.
- [ ] Decide travel time, success formula, guard bonuses and interruption frequency.

### Harbour / Shipping — deferred

- [ ] Add Ports / Harbour infrastructure.
- [ ] Add player-directed shipping requests / shipment contracts.
- [ ] Let fishing progression or fishing-related town bonuses improve shipping rewards.

## Residents and Guards

- [ ] Residents are all permanent town-affiliated NPCs, including MCA villagers and Guard Villagers.
- [x] Guard Villagers can be tracked as explicit town residents and consume population capacity.
- [ ] Let Guard Villagers use residences/home assignment and Barracks as workplace.
- [ ] Add resident status fields such as Working, On Patrol, Caravan Escort and Idle.
- [ ] Expand resident detail UI with Name, Happiness, Occupation, Home, Workplace and status.
- [x] Use Guard Villagers mod NPCs as the standard guard type for standard guard detection/stat integration.
- [ ] Keep military NPC hierarchy beyond standard Guard Villagers unresolved.

## Player Access and Permissions — deferred

- [ ] Use the Rules / Management area for human-player town access and permissions later.
- [ ] Keep human town membership separate from NPC Residents and population capacity; invited players never consume population slots.
- [ ] Add an Owner role with full control, including invitations and destructive town-management actions.
- [ ] Add a Co-Mayor role for trusted players who can manage ordinary town systems without transferring ownership.
- [ ] Add a Town Member role for players who belong to the town but do not receive mayor-level management permissions.
- [ ] Add invite / accept / remove flows rather than silently adding nearby players.
- [ ] Decide exact permission matrix for building registration/removal, worker assignment, treasury withdrawal, rules/tax changes and town deletion.
- [ ] Keep town deletion and ownership transfer Owner-only unless explicitly changed later.

## Specialists

- [x] Keep direct NPC interaction as the specialist service/research entry point; no central Town Research tab.
- [x] Generalize the Architect-only research UI/service into a specialist-specific framework. Existing research entries remain Architect-owned until other trees are approved.
- [ ] Add Carpenter specialist after its workplace/role is finalized.
- [ ] Add Stonemason specialist after its workplace/role is finalized.
- [ ] Decide Scholar's long-term role.
- [ ] Decide whether duplicate specialists are allowed and how duplicates behave.
- [ ] Carpenter service direction: Store tab for logs/furniture/building materials.
- [ ] Stonemason service direction: Store tab for stone/masonry/decorative materials.
- [ ] Blacksmith service direction: modular Store + Modify tabs (repair, rarity reforge, gem socketing).

## Adventurer's Guild — deferred

- [ ] Add later as a lightweight adventure/hunting/scouting building/system.
- [ ] Keep it distinct from ordinary Bounty Board bounties.
- [ ] Potential functions: special hunting contracts, bandit scouting, discovery/intelligence.
- [ ] Do not turn it into a second full quest framework.

## Buildings and UI

- [x] Canonical main tabs: Overview/Map, Town, Buildings, Residents, Requests/Events, Trade, Rules/Management.
- [x] Fold the standalone Building Catalogue into the Buildings area rather than keeping it as a top-level tab.
- [x] Buildings exposes explicit Building List / Catalogue sub-tabs.
- [x] Catalogue includes a Detect Here step before inspection/registration, while map detection remains available for spatial use.
- [x] Remove duplicate map-side first-four building buttons/text and use direct map selection + compact right summary + Info button.
- [x] Buildings detail page shows actual assigned worker/resident names where available.
- [ ] Keep simple building upgrades; exact upgrade costs, town-stage gates, Prosperity gates and furniture requirements remain TODO.

## Economy

- [ ] Economy revision is planned for this release.
- [ ] Replace generic currency abstraction with actual Create: Numismatics currency integration when the final denomination flow is decided.
- [ ] Defer ordinary villager-trade/town-economy interaction for now.
- [ ] Clarify specialist store/service pricing and progression.

## Other deferred design

- [ ] Exact Prosperity effects on happiness/services/Output.
- [ ] Exact Town Request generation rules/content.
- [ ] Exact Bountiful Decree integration.
- [ ] Final Adventurer's Guild progression.
- [ ] Final advanced Barracks training/loadout system.

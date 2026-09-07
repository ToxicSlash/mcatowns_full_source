# MCA Towns TODO

This file tracks planned systems and design questions that are not yet final implementation decisions. See `docs/IMPLEMENTATION_PLAN.md` for the current approved direction and existing-code alignment.

## Bounties and Requests

- [x] Bounties come only from the Bountiful Bounty Board; MCA Towns no longer treats generic hostile kills as bounties.
- [x] Retire the legacy MCA Towns 30-hostile-kill bounty counter from player-facing gameplay while keeping old save data readable.
- [ ] Design/implement an Architect-provided Bounty Decree (or equivalent Bountiful integration) that enables town-themed bounties in the board.
- [ ] Decide exact town rewards for completed board bounties.
- [ ] Expand Town Requests beyond Storehouse material deliveries into community/civic needs.
- [x] Keep normal ignored Town Requests non-punitive or only very mildly consequential.
- [x] Present Town Requests, festivals and disaster/problem events together in the Requests / Events UI.
- [ ] Allow only one normal disaster/problem event at a time; Bandit Activity may occur independently. Existing random events already use one active slot; independent Bandit Activity is still future work.

## Prosperity and Threats

- [x] Add an effective Prosperity Base layer so temporary threats can suppress the current floor without destroying the permanent civic Base. Current suppression is zero until threat rules are approved.
- [ ] Decide exact Prosperity thresholds and mild modifiers.
- [ ] Add Town Threat events such as Bandit Activity, Monster Pressure, Food Shortage and Trade Disruption.
- [ ] Prefer gradual pressure/world escalation over repeated large instant Prosperity losses.

## Bandits and Scouting

- [ ] Add hidden per-town Bandit Pressure.
- [ ] Add persistent latent bandit sites that do not generate until a player approaches.
  - Store candidate/site state in saved data.
  - Revalidate before generation.
  - Avoid towns and meaningful player builds.
  - Never force-load chunks.
  - Support long cooldown after clearing.
- [ ] Potential site progression: Camp -> Outpost -> Tower -> Fortified Stronghold.
- [ ] Add small bandit camp scouting/detection before the full Adventurer's Guild if useful.
- [ ] Keep player-facing risk qualitative (Safe / Low Risk / Risky / Dangerous) unless a success percentage is more useful.

## Trade

### Wandering Caravans

- [x] Rework the current random caravan system as themed Wandering Trader-style encounters.
- [x] Preserve normal merchant interaction/functionality as the base behaviour.
- [ ] Give each caravan theme/type its own custom trade pool.
- [x] Despawn temporary caravan NPCs after a few Minecraft days.
- [ ] Decide exact themed trade pools.

### Town Caravans

- [ ] Add simulated town-to-town trade separate from Wandering Caravans.
- [ ] Only one active town trade at a time initially.
- [ ] Add cached/simple export resource pools rather than scanning every building at trade time.
- [ ] Decide final pool categories (candidate ideas: Agriculture, Food/Livestock, Minerals, Manufactured, Luxury, Specialist).
- [ ] Add Trade Capacity and decide its values/sources.
- [ ] Add route risk/success affected by Bandit Pressure, Security and escorts.
- [ ] Add uncommon caravan interruption/rescue events.
- [ ] Allow up to two Guard Villagers as simple caravan escorts.
- [ ] Decide travel time, success formula, guard bonuses and interruption frequency.

### Harbour / Shipping

- [ ] Add Ports / Harbour infrastructure.
- [ ] Add player-directed shipping requests / shipment contracts.
- [ ] Let fishing progression or fishing-related town bonuses improve shipping rewards.

## Residents and Guards

- [ ] Redefine Residents as all permanent town-affiliated NPCs, including MCA villagers and Guard Villagers.
- [ ] Let Guard Villagers live in residences and use Barracks as workplace.
- [ ] Add resident status fields such as Working, On Patrol, Caravan Escort and Idle.
- [ ] Expand resident detail UI with Name, Happiness, Occupation, Home, Workplace and status.
- [x] Use Guard Villagers mod NPCs as the standard guard type for standard guard detection/stat integration. Guard resident/hiring management is still TODO.
- [ ] Keep military NPC hierarchy beyond standard Guard Villagers unresolved.

## Player Access and Permissions

- [ ] Use the Rules / Management area for human-player town access and permissions.
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

## Adventurer's Guild

- [ ] Add later as a lightweight adventure/hunting/scouting building/system.
- [ ] Keep it distinct from ordinary Bounty Board bounties.
- [ ] Potential functions: special hunting contracts, bandit scouting, discovery/intelligence.
- [ ] Do not turn it into a second full quest framework.

## Barracks

- [ ] Expand Barracks into Guard Villager management/hiring.
- [ ] Add guard assignments and caravan escort selection.
- [ ] Add guard loadouts / military upgrades after progression is finalized.
- [ ] Barracks food/morale buff remains brainstorm-only until approved.

## Buildings and UI

- [x] Canonical main tabs: Overview/Map, Town, Buildings, Residents, Requests/Events, Trade, Rules/Management.
- [x] Fold the standalone Building Catalogue into the Buildings area rather than keeping it as a top-level tab.
- [x] Buildings exposes explicit Building List / Catalogue sub-tabs.
- [x] Catalogue includes a Detect Here step before inspection/registration, while map detection remains available for spatial use.
- [x] Remove duplicate map-side first-four building buttons/text and use direct map selection + compact right summary + Info button.
- [x] Buildings detail page shows actual assigned worker/resident names where available.
- [ ] Keep simple building upgrades; exact upgrade rules/costs remain TODO.

## Economy

- [ ] Replace generic currency abstraction with actual Create: Numismatics currency integration.
- [ ] Clarify vanilla/MCA villager trade balance and how normal trading interacts with town economy.
- [ ] Clarify specialist store/service pricing and progression.

## Other deferred design

- [ ] Exact Prosperity effects on happiness/trades/services/Output.
- [ ] Exact Town Request generation rules/content.
- [ ] Exact Bountiful Decree integration.
- [ ] Final Adventurer's Guild progression.
- [ ] Final Barracks upgrade/loadout system.

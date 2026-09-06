# MCA Towns Implementation Plan

This document tracks the currently approved direction for MCA Towns and separates implemented behaviour from planned work. It is intentionally conservative: unresolved balance/formula questions remain TODO rather than being guessed in code.

## Core Principle

MCA Towns is a lightweight settlement-management layer for the larger modpack. It should enrich building, exploration, combat, farming, trading and villager interaction without becoming a spreadsheet-heavy city builder or requiring constant babysitting.

Implementation should remain server-authoritative, event-driven or periodic where possible, avoid forced chunk loading and large repeated world scans, keep network payloads bounded, and preserve save compatibility.

## Current Alignment

### Existing foundations to keep

- Prosperity, Prosperity Base, Town Tokens, ranks and rank checklists.
- Registered building footprints, tiers, Output, workers and infrastructure.
- Building Output derived from staffing, tier, furniture and nearby synergies.
- One active Town Request at a time.
- Residents and Specialists views in the town UI.
- Direct specialist interaction as the entry point for specialist services/research.
- Trading-post town discovery/link checks.
- Guard Villagers optional integration and periodic guard stat updates.
- Dedicated-server, headless-client and unit-test CI checks.

### Existing behaviour to revise

- The old global 30-hostile-kill "bounty" counter is not a bounty system and should be retired from player-facing gameplay. Bounties should come only from a Bountiful Bounty Board.
- Town-related bounty generation should later use an Architect-provided Bounty Decree or equivalent Bountiful integration. Exact decree behaviour remains TODO.
- The current random Merchant/Supply/Military/Immigrant `TownCaravanSystem` is conceptually a Wandering Caravan system, not simulated town-to-town trade.
- Architect-only research code should become a generic specialist service/research framework before new specialist trees are added.
- Town management currently has overlapping old/new screens. The Blueprint town screen should become the canonical compact town-management UI.

## UI Direction

Canonical main navigation:

- Overview / Map
- Town
- Buildings
- Residents
- Requests / Events
- Trade
- Rules / Management

Do not add a central Research tab.

### Overview / Map

Keep the existing black map and registered building bounding boxes. Normal outlines are off-white and the selected building outline is brighter white. Clicking a building selects it. The right side shows a compact summary and an Info button that opens the Buildings page with that exact building selected.

### Town

Show rank, expanded core town stats, Prosperity and Prosperity Base, and the next-rank checklist. Keep hidden formulas hidden.

### Buildings

Use a scrollable building list. The selected building shows type/name, tier, state, Output, actual assigned worker names where practical, bonuses, infrastructure contribution/use, and upgrade information when upgrades are implemented.

The old standalone Catalog top-level page should eventually be folded into this area rather than remaining a competing main navigation page.

### Residents

Use Residents / Specialists sub-tabs.

Residents should eventually include permanent MCA villagers and Guard Villagers. Resident detail should support Name, Happiness, Occupation, Home, Workplace and current status (Working, On Patrol, Caravan Escort, Idle, etc.).

Specialist list should show Name, Tier, Occupation and assigned building. Major specialist interaction remains through the NPC itself.

### Requests / Events

This becomes the single lightweight town activity board. It should list:

- Town Requests
- Festivals
- Disaster / town problem events

Only one normal disaster/problem event should be active at a time. Bandit Activity / Bandit Pressure is exempt from that single-disaster rule and may occur independently.

Normal Town Requests are community needs/resolutions and should not strongly punish the player when ignored. Bounties are not shown here unless the Bountiful board itself exposes them; they remain Bounty Board tasks.

### Trade

Eventually show one active town-to-town trade at a time, nearby/discovered towns, travel progress/cooldown, route risk/success estimate, Trade Capacity and a Trade button. Do not expose hidden formulas.

## Bounties

Approved direction:

- Bounties come only from the Bountiful Bounty Board.
- They remain optional small-scale tasks.
- Ignoring a bounty has no town penalty.
- Town-themed bounties should later be enabled/generated through a town Bounty Decree or similar Architect progression item.
- Exact Bountiful decree hooks, bounty categories and completion-to-town rewards remain TODO.

The old MCA Towns `bountyKills` counter should only remain as legacy save data until it can be safely removed from the save schema.

## Town Requests

Town Requests represent community/town needs rather than personal errands.

Existing material-delivery requests are a valid simple foundation. Future request types may include civic upgrades, food/storage capacity, security infrastructure, shortages and recreation/community services.

Rewards should generally be larger than ordinary bounties and can include Prosperity and Town Tokens. Normal ignored requests should have little or no penalty. Urgent problems belong to Threat/Disaster events instead.

## Prosperity

Prosperity is current town wellbeing/momentum.

Prosperity Base is permanent civic resilience. Passive Prosperity decay cannot reduce Prosperity below the Base.

Future threat events may temporarily suppress the effective Base without permanently destroying the stored civic Base. Example: Base 40 with Bandit Activity -10 gives effective Base 30 while the event lasts.

Exact thresholds and any small happiness/trade/service/Output modifiers remain TODO.

## Town Threat / Disaster Events

Threat events are temporary town problems such as Bandit Activity, Monster Pressure, Food Shortage or Trade Disruption.

The Requests / Events tab is the player-facing home for these events plus festivals and normal requests.

Rules:

- only one normal disaster/problem event at a time;
- Bandit Activity can coexist independently;
- pressure should be gradual rather than large instant Prosperity losses;
- ignored threats may escalate into world gameplay rather than repeatedly applying numeric punishment.

Existing flavour/random events should be migrated toward this presentation model. Positive events/festivals can remain lightweight and do not need to become threats.

## Bandit Pressure and Persistent Sites

Planned, not yet fully implemented.

Each town may later have hidden Bandit Pressure influenced by nearby persistent bandit sites, unresolved activity, town wealth/development where appropriate, and Security infrastructure.

Player-facing risk should use labels such as Safe, Low Risk, Risky, Dangerous or an estimated success percentage rather than exposing the raw hidden value.

Persistent bandit sites should be latent saved locations with no immediate structure generation:

Dormant Candidate -> Active Saved Site -> Player Approaches -> Revalidate -> Generate If Valid -> Progress If Ignored -> Cleared -> Long Cooldown

Potential progression: Camp -> Outpost -> Tower -> Fortified Stronghold.

No force-loading or constant scans. Revalidate before generation to avoid overwriting player builds or towns.

### Scouting

Bandit sites should not be automatically revealed. Scouting can progress from vague direction to approximate area to exact location through military/adventure infrastructure.

A small camp-detection/scouting feature may be added before the full Adventurer's Guild. The full Guild remains a later system.

## Trade Systems

Keep three distinct systems.

### 1. Harbour Shipments

Player-directed export/request contracts. Future system; exact implementation remains TODO.

### 2. Town Caravans

Simulated town-to-town trade. One active trade at a time initially. Future state should be lightweight and saved rather than represented by always-physical NPCs.

### 3. Wandering Caravans

Revise the existing random caravan system into themed Wandering Trader-style encounters.

Approved direction:

- retain normal Wandering Trader-style interaction/functionality;
- caravan theme/type determines custom trade pool;
- physical caravan is temporary and should despawn after a few Minecraft days;
- it is separate from town-to-town trade simulation.

Exact themed trade pools remain TODO until item/economy balance is reviewed.

## Town Trade Resources and Capacity

Future Town Caravan exports should be generated from broad cached/simple town resource pools rather than scanning every building at trade time.

Candidate pools remain TODO pending final selection, e.g. Agriculture, Food/Livestock, Minerals, Manufactured Goods, Luxury Goods and Specialist Goods.

Trade Capacity will determine the number of export/reward rolls. Exact capacity values and formulas remain TODO.

Bandit Pressure should affect route reliability rather than permanently reducing infrastructure.

## Caravan Interruptions and Escorts

Future Town Caravan feature.

An uncommon failed/interrupted route may create a rescue event with chat notification and coordinates, a short rescue window, a temporary generic caravan encounter, escort guards and a small bandit fight. Success allows the trade to continue; ignored/failed interception loses the trade.

Up to two Guard Villagers may eventually be assigned as simple escorts. They cannot be double-assigned and are unavailable while escorting. A simple success bonus per guard is preferred.

Exact formulas, timers and interruption frequency remain TODO.

## Residents and Guards

Residents should eventually mean all permanent town-affiliated NPCs, including MCA villagers and Guard Villagers.

Guard Villagers are the standard guard NPC type, may live in residences, use the Barracks as their workplace, appear in Residents, and later support statuses such as On Patrol, Working, Caravan Escort and Idle.

Avoid complicated per-guard simulation or repeated entity scans. Keep saved assignment/status state lightweight and resolve live entity details only when needed.

## Specialists

Current concepts:

- Architect
- Blacksmith
- Jeweler
- Scholar

Planned additions:

- Carpenter
- Stonemason

Do not add future specialists without an approved role/building.

Duplicate specialist behaviour remains TODO; current duplicate prevention may remain until a replacement design is approved.

## Specialist Research / Services

No central Town Research tab.

Each specialist owns a direct interaction/service UI. The existing Architect research screen is the foundation but should be generalized before additional specialist trees are implemented.

Potential service direction:

- Carpenter: Store
- Stonemason: Store
- Blacksmith: Store + Modify (repair, rarity reforge, gem socketing)

Exact costs/progression remain modular/TODO.

Scholar's final role is unresolved. Artifact/knowledge identification that unlocks further specialist progress is a brainstorm idea only and must not be hard-coded yet.

## Adventurer's Guild

Later building/system for larger hunting/adventure contracts, bandit scouting and discovery/intelligence. It must remain distinct from normal Bounty Board tasks and should not become a second full quest framework.

A small bandit camp detection system may precede the full Guild.

## Barracks

Existing building/integration is a foundation. Future functions may include Guard Villager hiring/management, assignments, caravan escorts, loadouts and military upgrades.

Barracks food/morale buffs remain brainstorm-only TODO.

## Deferred / Explicit TODOs

- exact Prosperity thresholds/modifiers;
- exact effective Prosperity Base suppression values;
- exact Bandit Pressure formula;
- bandit site activation/progression timing;
- exact scouting progression/building requirements;
- exact Trade Capacity values;
- final trade resource pool categories;
- export/import loot tables;
- town caravan travel time;
- caravan success formula;
- escort success bonuses;
- interruption frequency and rescue timing;
- Town Request generation rules/content expansion;
- Bountiful Bounty Decree integration;
- Scholar long-term role;
- duplicate specialist rules;
- final Adventurer's Guild progression;
- final Barracks loadout/upgrade system;
- Barracks food/morale idea;
- Harbour/shipping details;
- Create: Numismatics currency replacement;
- final vanilla/MCA/specialist trade balance;
- exact Prosperity effects on happiness/trades/services/Output;
- military NPC hierarchy beyond standard Guard Villagers;
- exact Wandering Caravan themed trade pools.

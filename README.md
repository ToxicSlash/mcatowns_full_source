MCA Towns (Fabric 1.20.1)

This project is an MCA Reborn addon that binds Mayor Desk governance to real MCA towns.

Implemented:
- Mayor Desk / Treasury / Barracks blocks
- Vanilla Bell marker for player-founded towns
- Blueprint Scrap item
- Per-town persistent data scoped by MCA village id (not world spawn)
- Player-founded ownership/progression persisted separately and linked to a real MCA village
- Mayor rank check via MCA rank data (Mayor+ required)
- Player-founded towns grant mayor authority to the founding player immediately
- MCA building scan, population, rank bonus integration
- Town-center detection and persistent Mayor Desk town binding
- Weekly taxes as emerald-value integers with split contributions:
  - addon simulation contribution
  - MCA tax contribution (including item-tax-equivalent handling)
- Tax rate slider (Low to Very High)
- Happiness, unrest, food, defense, immigration, and festival systems
- Raid unrest hooks and villager death happiness penalties (graveyard mitigation)
- Treasury/Barracks addon building detection near town center
- Barracks upgrades that buff nearby guards (MCA + Guard Villagers)
- Villager Business compatibility with lightweight periodic store visits
- Multiplayer safety checks for Mayor Desk network actions
- Loaded-chunk-only town simulation, cached building stats, and rate-limited action packets
- JSON config + Mod Menu/Cloth Config UI
- Typed town buildings, research, recruitment, specialists, requests, food, and rank progression
- Storehouse block with a real server-owned 27-slot communal inventory

Player-founded towns:
- Use an MCA Blueprint while outside the configured town search range to create a new empty town.
- Creation requires:
  - 1 Create Numismatics Crown (numismatics:crown)
  - 10 Blueprint Scraps (mcatowns:blueprint_scrap)
- Creation places a vanilla Bell near the player as the visible marker.
- The town centre is saved as a coordinate on town data, so it can later be moved from a Civic Office/Architect flow without depending on the Bell block.
- New towns receive a deterministic generated name, which can be changed from the blueprint UI.
- New player-founded towns start at Camp with no villagers, buildings, Town Tokens, food, or tax base.
- Starting Prosperity defaults to 3 and is configurable.
- The founding player is treated as Mayor for that town.
- The vanilla Bell is registered through MCA's building manager as the initial town marker.
- The Town Manager has Overview, Buildings, Residents, Specialists, and Requests pages. Research is specialist-only.
- Mayors can remove their town from the Town Manager. Removal requires a confirmation and is revalidated by the server.
- Confirmed town removal deletes the town record and MCA link. The vanilla Bell remains a normal block.
- Creative players and permission level 2 operators can remove towns from the blueprint UI.
- Wild MCA towns still require normal MCA progression to Mayor rank before MCA Towns governance actions are allowed.

Town Blueprint and ranks:
- Outside a town, using an MCA Blueprint opens a Start Town page instead of MCA's room/building prompt.
- Hovering Start Town shows the Crown and Blueprint Scrap cost.
- Inside an established town, the Blueprint uses MCA's original map, boundary, catalog, villager, rank, and rules pages.
- The MCA Blueprint screen keeps its original left tabs and adds top-right Prosperity, Food, Population, and Specialist counters.
- Catalog contains Residential, Food, Community, and Utility categories above the town building icons.
- Villagers contains Resident and Specialist categories for inspecting recruited villagers.
- The town advancement checklist is integrated into MCA's Rank page.
- Town content uses the gray panel background while MCA's left navigation remains unobstructed.
- Locked buildings are visible but disabled until researched through an employed Architect.
- Registration checks ownership, boundary, rank limits, Prosperity, currency, Town Tokens, duplicate positions, and the physical structure on the server.
- Residence, Blacksmith, Inn, and Scholar structures reuse MCA's building scanner. Other buildings validate their real block or simple nearby infrastructure.
- The founder is the town Mayor. The town itself starts at Camp.

Town progression:
- Camp -> Hamlet -> Village -> Township -> Town
- Camp has a 30 Prosperity cap and one specialist slot.
- Rank advancement is permanent. Prosperity can later decay without lowering rank.
- Current caps and numeric thresholds are centralized in TownRank for early testing.
- The intended Camp infrastructure remains 2 Residences, Civic Office, Bounty Board, Campfire, and Farm.
- Later ranks use registered building, population, Prosperity, and Town Token thresholds.
- Rank advancement never spends Prosperity and ranks never downgrade.

Buildings and research:
- Founding knowledge: Residence, Farm, Campfire, Storehouse, Bounty Board, and Civic Office.
- Advanced research: Granary, Park, Inn, Guard Post, Blacksmith, Jeweler, and Scholar.
- Research permanently unlocks the building for that town.
- Research supports Blueprint Scraps, Town Tokens, configured currency, and an optional configured Great Essence item ID.
- Registration spends Town Tokens/currency where configured; it never spends Prosperity.
- Residence provides +2 population capacity.
- The existing Silo is reused as the Granary and provides +100 Food capacity.
- Campfire/Park/Inn provide +2/+4/+6 Prosperity Base.

Residents and specialists:
- Sneak-interact with an MCA villager to open Invite to Town services. Normal MCA interaction remains unchanged.
- Normal recruitment uses MCA friendship hearts and requires capacity and currency.
- 3% of naturally unemployed villagers become specialist candidates by default (configurable).
- Specialist types: Architect, Blacksmith, Jeweler, Scholar. Duplicate types are rejected per town.
- Specialists remain ordinary MCA villagers; role and assignment metadata are stored separately.
- The installed RPG Quest framework supplies the data-driven Introductions quest. Without that optional mod, the quest gate is skipped safely.
- Specialist recruitment requires Introductions, the matching workplace, capacity, a specialist slot, and currency.
- An employed Architect provides persistent building research.

Requests and bounties:
- Register a Storehouse to receive Routine and Important requests.
- Requirements scale with population and include building-specific pools for Blacksmith, Guard Post, Scholar, and Inn.
- At the deadline, materials are checked and consumed from the Storehouse. Unloaded chunks postpone evaluation instead of failing.
- Successful requests award Prosperity and Town Tokens. Routine failure has no direct penalty.
- A registered Bountiful Bounty Board enables Monster Control (30 hostile mobs) inside the owner's town.
- Monster Control awards +3 Prosperity, +3 Town Tokens, and 1 Blueprint Scrap.

Town Food and health:
- Food items contributed through the Storehouse or Granary convert using their actual vanilla nutrition value.
- Farms do not generate abstract food automatically.
- A recruited vanilla/MCA Farmer with a registered Farm provides a small capped passive amount per day.
- Residents consume configurable Food each day. Empty Food never kills villagers.
- Food status is Stable, Rationing, Food Shortage, or Severe Food Shortage based on capacity percentage.
- Food shortages and Community buildings modify MCA mood rather than replacing MCA's mood/relationship system.
- Prosperity decays by 1 per Minecraft day by default, but never below the Community-created Prosperity Base.

Persistence:
- Rank, Prosperity, Floor, Town Tokens, typed buildings, research, residents, specialists, requests, timers, Food capacity, population capacity, and bounty progress persist per town.
- Legacy rank names and generic registered positions load safely.

Config:
- File: config/mcatowns.json
- In-game UI: Mod Menu -> MCA Towns -> Config

Dependency style:
- MCA, Guard Villagers, and Villager Business integrations are soft at runtime where possible.
- Cloth Config and Mod Menu are included for config UI.

TODO:
- Add ports
- Fix building/catalog icons
- Add mint
- Add port block

Development approach:
- Keep gameplay logic server-side and validate every client action.
- Reuse small helpers for repeated inventory and packet behavior.
- Avoid chunk loading, world scans, or entity updates on render paths and frequent ticks.
- Prefer cached town statistics and only persist values when they change.

# MCA Towns TODO

This file tracks planned systems and design questions that are not yet final implementation decisions.

## Harbour, Ports and Shipping

- [ ] Add Ports / Harbour infrastructure.
- [ ] Add shipping requests / shipment contracts.
- [ ] Let fishing progression or fishing-related town bonuses improve shipping rewards.

## Military, Guards and Raids

- [ ] Use Guard Villagers mod villagers as the town's primary standard guards.
- [ ] Explore MCA villager military roles such as soldiers, champions and generals.
- [ ] Add persistent pop-up raid camps outside towns.
  - Store camp location/state as world or town-related saved data.
  - Select locations roughly 2,000 blocks from the target town.
  - Prevent camps from overlapping or spawning too close to another town.
  - Do not generate the physical structure until players approach the saved location.
  - Allow surviving camps to develop into larger towers / fortified raid sites.
- [ ] Add simple skirmishes / bandit raids against towns with low military infrastructure.
- [ ] Let higher military infrastructure attract or enable larger, more dangerous raids rather than simply removing raids entirely.

## Buildings and Progression

- [ ] Add simple building upgrades.
- [ ] Clarify villager trade balance and how normal villager trading should interact with the town economy.
- [ ] Clarify specialist trade and research progression.
  - Decide whether a Scholar acts as the central research/upgrading specialist and benefits from Libraries and other utility buildings.
  - Alternatively, decide whether specialists such as Blacksmiths should have their own independent research tabs / progression paths.

## Economy

- [ ] Replace the generic `currency` abstraction with actual Create: Numismatics currency integration.
- [ ] Clarify how Bountiful bounties affect towns.
  - Investigate using a town Bounty Decree in the Bounty Board to control the pool/types of town bounties that appear.
  - Decide what completing bounties should contribute to the town beyond the current direct rewards.

## Town Requests

- [ ] Add a dedicated Town Requests page to the town UI.
- [ ] Decide whether ignoring or failing town requests should reduce Prosperity, and by how much.

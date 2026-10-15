# Spec Delta

## MODIFIED Requirements

### Requirement: Shuttle tiers
A shuttle SHALL be one of three items: Shuttle Mk1, Shuttle Mk2 and Shuttle Mk3. Shuttle Mk1 SHALL be crafted; Shuttle Mk2 and Mk3 SHALL have no crafting recipe and SHALL only come out of a tier build in the Shuttle Workshop. Every tier SHALL work in garages and the workshop like any shuttle. A tier SHALL set how many stacks the shuttle carries per trip and the max count of each count upgrade, from `data/<ns>/shuttles/shuttle.json` (examples: Mk1 2 stacks and up to 2 of each upgrade, Mk2 4 and 4, Mk3 8 and 8).

#### Scenario: Stacks per tier
- **WHEN** a warehouse has one shuttle of each tier and the stats file gives 2, 4 and 8 stacks
- **THEN** the Mk1 shuttle carries up to two jobs that share a way per trip, the Mk2 shuttle four and the Mk3 shuttle eight

#### Scenario: No recipe for higher tiers
- **WHEN** a player looks for a crafting recipe of a Shuttle Mk2
- **THEN** there is none and the workshop is the only way to get one

### Requirement: Shuttle stats file
`data/<ns>/shuttles/shuttle.json` SHALL hold the base speed and the step per Speed count, the stop time, the time per stack and the Handling factor, and per tier its stacks, its max Speed and Handling counts and, for Mk2 and Mk3, its build sheet: a list of supply items with counts. Missing values SHALL fall back to built in defaults, and stacks and max counts SHALL be at least 1 and 0.

#### Scenario: A datapack changes the stacks
- **WHEN** a datapack sets Mk2 to 6 stacks and the server reloads
- **THEN** Mk2 shuttles carry up to six jobs per trip

### Requirement: Count upgrades
A shuttle SHALL hold a count of Speed upgrades and a count of Handling upgrades. The Speed upgrade SHALL be the Foundation Speed Upgrade item; the Handling Upgrade SHALL be a craftable Shuttles item. A count SHALL never exceed the max of the shuttle's tier. The counts SHALL be stored on the shuttle item with its tool, so a shuttle keeps them in inventories, garages, the workshop and through tier builds. The tooltip of a shuttle SHALL list its counts and its tool. A shuttle with no counts and no tool SHALL stack with a newly made shuttle of its tier.

#### Scenario: Max of the tier
- **WHEN** a Mk1 shuttle with a max of 2 already has two Speed upgrades
- **THEN** a third Speed upgrade does not go in

#### Scenario: Counts survive a build
- **WHEN** a Mk1 shuttle with two Speed upgrades and a Harvester is built into a Mk2 shuttle
- **THEN** the Mk2 shuttle has two Speed upgrades and the Harvester, and takes up to four Speed upgrades

# shuttles/shuttle-tiers Specification

## Purpose

Shuttles come in tiers and take count upgrades, so a warehouse can grow its throughput shuttle by shuttle: a higher tier carries more stacks per trip and takes more upgrades, Speed makes it travel faster and Handling makes it quicker at stops. Higher tiers are built in the Shuttle Workshop from supplies.

## Requirements

### Requirement: Shuttle tiers
A shuttle SHALL be one of three items: Shuttle Mk1, Shuttle Mk2 and Shuttle Mk3. Shuttle Mk1 SHALL be crafted; Shuttle Mk2 and Mk3 SHALL have no crafting recipe and SHALL only come out of a tier build in the Shuttle Workshop. Every tier SHALL work in garages and the workshop like any shuttle. A tier SHALL set how many stacks the shuttle carries per trip and the max count of each count upgrade, from `data/<ns>/storage/shuttle.json` (examples: Mk1 2 stacks and up to 2 of each upgrade, Mk2 4 and 4, Mk3 8 and 8).

#### Scenario: Stacks per tier
- **WHEN** a warehouse has one shuttle of each tier and the stats file gives 2, 4 and 8 stacks
- **THEN** the Mk1 shuttle carries up to two jobs that share a way per trip, the Mk2 shuttle four and the Mk3 shuttle eight

#### Scenario: No recipe for higher tiers
- **WHEN** a player looks for a crafting recipe of a Shuttle Mk2
- **THEN** there is none and the workshop is the only way to get one

### Requirement: Count upgrades
A shuttle SHALL hold a count of Speed upgrades and a count of Handling upgrades. The Speed upgrade SHALL be the Foundation Speed Upgrade item; the Handling Upgrade SHALL be a craftable Storage item. A count SHALL never exceed the max of the shuttle's tier. The counts SHALL be stored on the shuttle item with its tool, so a shuttle keeps them in inventories, garages, the workshop and through tier builds. The tooltip of a shuttle SHALL list its counts and its tool. A shuttle with no counts and no tool SHALL stack with a newly made shuttle of its tier.

#### Scenario: Max of the tier
- **WHEN** a Mk1 shuttle with a max of 2 already has two Speed upgrades
- **THEN** a third Speed upgrade does not go in

#### Scenario: Counts survive a build
- **WHEN** a Mk1 shuttle with two Speed upgrades and a Harvester is built into a Mk2 shuttle
- **THEN** the Mk2 shuttle has two Speed upgrades and the Harvester, and takes up to four Speed upgrades

### Requirement: Speed between stops
A shuttle SHALL travel at the base speed plus a step per Speed count, from `shuttle.json` (examples: base 0.15 blocks per tick, plus 0.04 per count). The lift car's speed SHALL NOT change with Speed: a shuttle riding a car moves at the car's speed.

#### Scenario: Faster with upgrades
- **WHEN** one shuttle has no Speed upgrade and another has two, with the example numbers
- **THEN** the first drives 0.15 blocks per tick and the second 0.23, and the second reaches a rack twelve rails away in about 52 ticks instead of 80

### Requirement: Stops take time
A shuttle SHALL stand still at each stop where it loads or unloads: at the source of a pick, at the Packing Station or terminal rail where it delivers, at the station of a put-away and at its target. A stop SHALL take a stop time plus a time per stack moved there, from `shuttle.json` (examples 10 ticks and 4 ticks per stack). Each Handling count SHALL shorten that time by a factor (example 0.8 per count), and a stop SHALL take at least one tick. Driving home and returning a load to its station SHALL take no stop time.

#### Scenario: Loading two stacks
- **WHEN** a Mk1 shuttle without Handling arrives at a rack for two pick jobs, with the example numbers
- **THEN** it stands at the rack for 18 ticks before it drives on with both stacks

#### Scenario: Handling shortens the stop
- **WHEN** the same shuttle has two Handling upgrades
- **THEN** the stop takes about 12 ticks

### Requirement: Shuttle stats file
`data/<ns>/storage/shuttle.json` SHALL hold the base speed and the step per Speed count, the stop time, the time per stack and the Handling factor, and per tier its stacks, its max Speed and Handling counts and, for Mk2 and Mk3, its build sheet: a list of supply items with counts. Missing values SHALL fall back to built in defaults, and stacks and max counts SHALL be at least 1 and 0.

#### Scenario: A datapack changes the stacks
- **WHEN** a datapack sets Mk2 to 6 stacks and the server reloads
- **THEN** Mk2 shuttles carry up to six jobs per trip

### Requirement: Tier builds
The Shuttle Workshop SHALL build the next tier of the shuttle in its shuttle slot: Mk1 into Mk2 and Mk2 into Mk3. The GUI SHALL show the build sheet of the next tier with how many of each supply are in the workshop's supply slots and how many are needed. While every supply is there, a **Build now** button SHALL use exactly the needed supplies and turn the shuttle into the next tier, keeping its counts, tool, name and name plate, and a **Build by hand** button SHALL start the same build as a short minigame. A sheet SHALL also list the steps of its build by hand. A Mk3 shuttle SHALL show no build. Supplies SHALL stay in the workshop until a build uses them or a player takes them out.

#### Scenario: Build a Mk2
- **WHEN** a player puts a Mk1 shuttle and every supply of the Mk2 sheet into the workshop and presses Build now
- **THEN** the shuttle slot holds a Mk2 shuttle with the Mk1 shuttle's counts and tool, and the supplies are used

#### Scenario: Supplies missing
- **WHEN** one supply of the sheet is short
- **THEN** the list shows how many are missing and Build now and Build by hand are off

#### Scenario: Extra supplies stay
- **WHEN** the supply slots hold more than the sheet needs and a build is done
- **THEN** the rest stays in the supply slots

#### Scenario: Top tier
- **WHEN** a Mk3 shuttle is in the workshop
- **THEN** no build sheet and neither build button are shown

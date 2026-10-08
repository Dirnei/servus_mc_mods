# Spec Delta

## Purpose

The Data Cable is the shared control wiring of the Servus family: it links producers and stock without setup, so on-demand autocrafting can find them, and any mod joins by exposing the Servus Lib capabilities.

## ADDED Requirements

### Requirement: Cables link to cables
A Data Cable SHALL connect to every Data Cable on its six sides and show an arm toward each. Touching cables SHALL form one data network; placing a cable between two networks SHALL merge them and breaking a cable SHALL split a network where it no longer connects.

#### Scenario: Straight run
- **WHEN** three Data Cables are placed in a row
- **THEN** each shows arms toward its neighbours and all three report the same data network

#### Scenario: Split
- **WHEN** the middle cable of that row is broken
- **THEN** the two remaining cables report two different data networks

### Requirement: Members by capability
A block SHALL be a member of a data network on a side that touches one of its cables when it exposes, on that side, the production capability (role producer) or the stock provider or stock sink capability (role stock). A block with both kinds SHALL have both roles. The cable SHALL show an arm toward a member. Blocks without these capabilities, such as chests, hoppers, rails and ducts, SHALL NOT be members and the cable SHALL show no arm toward them.

#### Scenario: Crafting Station
- **WHEN** a Data Cable touches a Crafting Station
- **THEN** the cable shows an arm toward it and the station is a producer of that network

#### Scenario: Warehouse Controller
- **WHEN** a Data Cable touches a Warehouse Controller
- **THEN** the controller is a stock member of that network

#### Scenario: Chest
- **WHEN** a Data Cable touches a chest
- **THEN** the cable shows no arm toward it and the network has no member there

### Requirement: Members follow the world
The member list SHALL change when a member is placed next to a cable, broken, or its chunk unloads, and when a cable is placed or broken next to it, without scanning blocks every tick. A block touched by several cables of one network SHALL count as one member. A block touching two separate networks SHALL be a member of both.

#### Scenario: Member placed later
- **WHEN** a Crafting Station is placed next to an existing cable
- **THEN** the network lists it as a producer from then on

#### Scenario: Member broken
- **WHEN** that Crafting Station is broken
- **THEN** the network no longer lists it

#### Scenario: Touched twice
- **WHEN** two cables of one network both touch the same Warehouse Controller
- **THEN** the network lists one stock member

### Requirement: Member query
Servus Lib SHALL let any mod ask, on the server, for the data network at a cable position or at a member position and for that network's members, each with its position and roles, filtered by role. A position that is neither a cable nor a member SHALL have no data network.

#### Scenario: Ask from a member
- **WHEN** a mod asks for the data network at the position of a Warehouse Controller touched by a cable
- **THEN** it gets that cable's network, and its stock members include the controller

#### Scenario: Nothing there
- **WHEN** a mod asks for the data network at a stone block
- **THEN** it gets no network

### Requirement: Cable moves nothing
A Data Cable SHALL hold no items, energy or fluids, SHALL expose no item, energy or fluid capability, and SHALL never move anything between its members on its own.

#### Scenario: Station and controller linked
- **WHEN** a cable links a Warehouse Controller holding planks and a Crafting Station with the sticks recipe and nothing orders sticks
- **THEN** no planks leave the warehouse and the station's inputs stay empty

### Requirement: Network summary
Right clicking a Data Cable with an empty hand SHALL show the player a one line summary in the action bar: how many cables the network has, how many producers and how many stock members.

#### Scenario: Summary
- **WHEN** a player right clicks a cable of a network of 5 cables with one Crafting Station and one Warehouse Controller as members
- **THEN** the action bar says 5 cables, 1 producer, 1 stock

### Requirement: Cable is obtainable and placed like a run
The Data Cable SHALL be crafted from copper and redstone (example: two copper ingots around a redstone dust give 4 cables), mined with a pickaxe, drop itself when broken and be dismantled by sneaking with the Configuration Carrot. Its item SHALL use the Lib placement assist: a sneaking player clicking a cable extends the straight cable run at its far end.

#### Scenario: Dismantle
- **WHEN** a sneaking player uses the Configuration Carrot on a Data Cable
- **THEN** the cable drops as an item and the network updates

#### Scenario: Extend a run
- **WHEN** a sneaking player holding Data Cables clicks the top of a cable in a straight west to east run, east of its centre
- **THEN** the new cable is placed past the east end of the run

### Requirement: Lib alone
The Data Cable SHALL be part of `servus_lib`, so it can be placed, crafted and summarised when no content mod is installed; its network then simply has no members.

#### Scenario: Only Lib installed
- **WHEN** a game runs with only `servus_lib` and a player places two cables and right clicks one
- **THEN** the action bar says 2 cables, 0 producers, 0 stock

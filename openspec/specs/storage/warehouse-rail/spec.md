# storage/warehouse-rail Specification

## Purpose

The Warehouse Rail is the wiring of the mid game warehouse: a floor rail that links storage blocks into one network and later carries shuttles.

## Requirements

### Requirement: Floor rail block
The Warehouse Rail SHALL be a flat block that sits on the floor of its block space, one block wide, with a selection shape a few pixels high so players walk over it. It SHALL be placeable on any block with a solid top.

#### Scenario: Walk over
- **WHEN** a player walks along an aisle paved with rails
- **THEN** the player is not blocked and does not climb

#### Scenario: Placement
- **WHEN** a player places a rail on a stone floor
- **THEN** the rail is placed with its flat side on the floor

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail, every Rail Elevator segment, every stock holding storage block (crate, shelving rack, warehouse controller, counter terminal) and every Dispatch Station on the same level and SHALL render the matching piece: straight, curve, T or cross. Towards a storage block or a station the piece SHALL be a dock, visibly different from the arm towards a rail or segment. A Heavy Duty Rack SHALL take a dock only on its front side. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail, segment, storage block or station is placed, rotated or removed.

#### Scenario: Straight run
- **WHEN** three rails are placed in a line
- **THEN** the middle rail shows a straight piece and the end rails each show one arm

#### Scenario: Crossing
- **WHEN** a rail has rails on all four horizontal sides
- **THEN** it renders a cross

#### Scenario: Different level
- **WHEN** a rail is placed directly above another rail
- **THEN** the two rails are not connected to each other

#### Scenario: Rail meets a shaft
- **WHEN** a rail is placed beside an elevator segment
- **THEN** the rail shows an arm towards the segment

#### Scenario: Rail docks onto a rack
- **WHEN** a rack is placed beside a rail
- **THEN** the rail shows a dock piece towards the rack that looks different from an arm towards a rail, and the dock disappears when the rack is broken

#### Scenario: Rail behind a heavy duty rack
- **WHEN** a rail runs behind a Heavy Duty Rack
- **THEN** the rail shows no dock towards it, and placing the rack again so that it faces the rail makes the dock appear

#### Scenario: Rail docks onto a station
- **WHEN** a Dispatch Station is placed beside a rail, on any of its sides
- **THEN** the rail shows a dock piece towards the station

### Requirement: Storage blocks join through rails
Every rail SHALL be a node of the warehouse network. Crates, shelving racks, warehouse controllers, counter terminals and dispatch stations touching a rail on any side SHALL belong to the same network as that rail, and a chain of connected rails SHALL carry membership along its whole length. Rails SHALL link to each other sideways only; a rail directly above another rail does not link to it.

#### Scenario: Two aisles one controller
- **WHEN** a controller touches a rail, the rail runs ten blocks, and racks stand beside the rail at both ends
- **THEN** all racks and the controller form one online warehouse and the counter terminal lists their contents

#### Scenario: Rail removed
- **WHEN** a rail in the middle of that run is broken
- **THEN** the racks past the gap leave the controller's network and their items leave the index

#### Scenario: Stacked rails
- **WHEN** a rail lies on a block directly above another rail and nothing else joins them
- **THEN** the two rails are in different networks

#### Scenario: Station at the end of a rail
- **WHEN** a controller touches a rail and a Dispatch Station stands beside the far end of the rail
- **THEN** the station is in the controller's warehouse and shows the online lamp

### Requirement: Rails hold nothing
A rail SHALL NOT be indexed as a source and SHALL NOT make the inventories beside it part of the warehouse. Only storage blocks pull neighbouring inventories in; a Dispatch Station does not.

#### Scenario: Chest beside a rail
- **WHEN** a chest with bread stands beside a rail of an online warehouse and touches no storage block
- **THEN** the bread is not in the index

#### Scenario: Chest beside a rack
- **WHEN** that chest also touches a rack of the same warehouse
- **THEN** the bread is in the index

#### Scenario: Chest beside a station
- **WHEN** a chest with bread touches a Dispatch Station and nothing else of the warehouse
- **THEN** the bread is not in the index

### Requirement: Rail is obtainable and dismantled
The rail SHALL be craftable from vanilla materials in a batch, SHALL drop itself when broken, SHALL drop itself when the block under it is removed, and SHALL be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and a stick in the rail pattern (for example two rows of three ingots around a stick)
- **THEN** the result is several rails (for example eight)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed
- **WHEN** the block under a rail is broken
- **THEN** the rail drops as an item

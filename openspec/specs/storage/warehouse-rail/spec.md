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
A rail SHALL connect to every neighbouring rail and every Rail Elevator segment on the same level and SHALL render the matching piece: straight, curve, T or cross. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail or segment is placed or removed.

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

### Requirement: Storage blocks join through rails
Every rail SHALL be a node of the warehouse network. Crates, shelving racks, warehouse controllers and counter terminals touching a rail on any side SHALL belong to the same network as that rail, and a chain of connected rails SHALL carry membership along its whole length.

#### Scenario: Two aisles one controller
- **WHEN** a controller touches a rail, the rail runs ten blocks, and racks stand beside the rail at both ends
- **THEN** all racks and the controller form one online warehouse and the counter terminal lists their contents

#### Scenario: Rail removed
- **WHEN** a rail in the middle of that run is broken
- **THEN** the racks past the gap leave the controller's network and their items leave the index

### Requirement: Rails hold nothing
A rail SHALL NOT be indexed as a source and SHALL NOT make the inventories beside it part of the warehouse. Only storage blocks pull neighbouring inventories in.

#### Scenario: Chest beside a rail
- **WHEN** a chest with bread stands beside a rail of an online warehouse and touches no storage block
- **THEN** the bread is not in the index

#### Scenario: Chest beside a rack
- **WHEN** that chest also touches a rack of the same warehouse
- **THEN** the bread is in the index

### Requirement: Rail is obtainable and dismantled
The rail SHALL be craftable from vanilla materials in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and a stick in the rail pattern (for example two rows of three ingots around a stick)
- **THEN** the result is several rails (for example eight)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

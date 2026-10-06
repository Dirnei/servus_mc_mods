# storage/rail-elevator Specification

## Purpose

The Rail Elevator is a shaft of stacked segments that links Warehouse Rails on different floors into one network and later carries shuttles between them.

## Requirements

### Requirement: Shaft segment block
The Rail Elevator SHALL be a full height block that renders as a shaft segment. Segments SHALL stack on top of each other to form a shaft of any height, and SHALL render as one continuous shaft.

#### Scenario: Three segments
- **WHEN** three elevator segments are placed on top of each other
- **THEN** they look like one shaft from floor to top

### Requirement: Segments connect vertically and to rails
An elevator segment SHALL connect to elevator segments directly above and below it, and to Warehouse Rails on its four horizontal sides, rendering a rail arm towards each connected rail. Segments SHALL NOT connect sideways to other segments.

#### Scenario: Rail into shaft
- **WHEN** a rail lies beside a segment
- **THEN** the segment shows a rail arm towards that rail and the rail shows an arm towards the segment

#### Scenario: Side by side shafts
- **WHEN** two segments stand beside each other on the same level
- **THEN** they show no connection to each other

### Requirement: Rail levels join through a shaft
Every elevator segment SHALL be a node of the warehouse network. Rails and storage blocks touching any segment SHALL belong to the same network, so rails on different floors linked by a shaft form one warehouse.

#### Scenario: Two floors one controller
- **WHEN** a crate stands beside a rail on the ground floor, the rail meets a shaft, and three segments up another rail leads to a controller and a terminal
- **THEN** the warehouse is online and the terminal lists the crate's contents

#### Scenario: Broken segment
- **WHEN** the middle segment of that shaft is broken
- **THEN** the ground floor leaves the controller's network and the crate's items leave the index

### Requirement: Segments hold nothing
An elevator segment SHALL NOT be indexed as a source and SHALL NOT make the inventories beside it part of the warehouse.

#### Scenario: Chest beside a shaft
- **WHEN** a chest with bread touches only elevator segments of an online warehouse
- **THEN** the bread is not in the index

### Requirement: Elevator is obtainable and dismantled
The elevator segment SHALL be craftable from vanilla materials and a Warehouse Rail in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player places iron ingots around a rail in the elevator pattern (for example a rail in the middle of a ring of six ingots)
- **THEN** the result is several segments (for example four)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a segment
- **THEN** the segment drops as an item and the shaft above it stays in place

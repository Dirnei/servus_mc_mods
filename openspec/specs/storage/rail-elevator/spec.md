# storage/rail-elevator Specification

## Purpose

The Rail Elevator is a shaft of stacked segments that links Warehouse Rails on different floors into one network and later carries shuttles between them.

## Requirements

### Requirement: Shaft segment block
The Rail Elevator SHALL be a full height block that renders as a frame of four corner posts with an open middle. The warehouse rail SHALL run through the middle of the segment at floor level, with an arm towards every connected rail. Segments SHALL stack on top of each other to form a shaft of any height, SHALL render as one continuous frame, and players SHALL be able to walk through a segment between its posts.

#### Scenario: Three segments
- **WHEN** three elevator segments are placed on top of each other
- **THEN** they look like one frame from floor to top with the posts lining up

#### Scenario: Walk through
- **WHEN** a player walks along a rail into a ground floor segment
- **THEN** the player passes between the posts and stands in the middle of the segment on the rail

#### Scenario: Rail through the middle
- **WHEN** rails lie on the west and east sides of a segment
- **THEN** the segment shows a rail from its west edge through its centre to its east edge

### Requirement: Segments connect vertically and to rails
An elevator segment SHALL connect to elevator segments directly above and below it, and to Warehouse Rails on its four horizontal sides, rendering a rail arm towards each connected rail. Segments SHALL NOT connect sideways to other segments.

#### Scenario: Rail into shaft
- **WHEN** a rail lies beside a segment
- **THEN** the segment shows a rail arm towards that rail and the rail shows an arm towards the segment

#### Scenario: Side by side shafts
- **WHEN** two segments stand beside each other on the same level
- **THEN** they show no connection to each other

### Requirement: Rail levels join through a shaft
Every elevator segment SHALL be a node of the warehouse network. Segments SHALL link to the segments directly above and below them and to rails beside them, so rails on different floors linked by a shaft form one warehouse. A storage block touching a segment SHALL NOT join the warehouse through that segment; only rails and direct contact between storage blocks carry membership.

#### Scenario: Two floors one controller
- **WHEN** a crate stands beside a rail on the ground floor, the rail meets a shaft, and three segments up another rail leads to a controller and a terminal
- **THEN** the warehouse is online and the terminal lists the crate's contents

#### Scenario: Broken segment
- **WHEN** the middle segment of that shaft is broken
- **THEN** the ground floor leaves the controller's network and the crate's items leave the index

#### Scenario: Crate against the shaft
- **WHEN** a crate touches a segment of an online warehouse and no rail or storage block of it
- **THEN** the crate is not part of the warehouse, its items are not listed and its lamp stays dark

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
- **THEN** the segment drops as an item, and the segments above it lose their support and drop too

### Requirement: Segments need support
An elevator segment SHALL only be placed on a block with a solid top or on another segment. A segment whose support is removed SHALL drop as an item.

#### Scenario: Placed in the air
- **WHEN** the player tries to place a segment against a wall with nothing below it
- **THEN** the segment is not placed

#### Scenario: Floor removed
- **WHEN** the block under the lowest segment of a three segment shaft is broken
- **THEN** all three segments drop as items

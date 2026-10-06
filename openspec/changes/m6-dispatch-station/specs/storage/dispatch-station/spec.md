# Spec Delta

## Purpose

The Dispatch Station is the warehouse's inbound dock: items arrive in its buffer and the warehouse puts them away by its zones, carried by shuttles over the rails when the warehouse has them.

## ADDED Requirements

### Requirement: Station block and buffer
The Dispatch Station SHALL be a block with a horizontal facing and an intake buffer of `dispatch_buffer_slots` slots (example 9, from `data/<ns>/storage/warehouse.json`). The buffer SHALL be exposed as an item handler on every side that accepts inserts and refuses extraction, so hoppers and ducts feed it but never take from it. Breaking the station SHALL drop the buffer contents.

#### Scenario: Hopper feeds the station
- **WHEN** a hopper above the station holds 20 cobblestone
- **THEN** the cobblestone moves into the station's buffer

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper under the station points into a chest and the buffer holds bread
- **THEN** the bread stays in the buffer

#### Scenario: Broken with contents
- **WHEN** the station is broken while the buffer holds 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Station is a warehouse node without stock
The station SHALL be a member of the warehouse network like a storage block: it SHALL link to touching storage blocks and stations, rails SHALL dock onto it on any side, and it SHALL NOT link to elevator segments. Its buffer SHALL NOT be indexed and inventories touching it SHALL NOT become warehouse sources. The station SHALL show the connection lamp like storage blocks.

#### Scenario: Chest feeding the station is not stock
- **WHEN** a chest with 30 bread touches only the station of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Buffer is not stock
- **WHEN** the buffer holds 10 bread that has not been put away yet
- **THEN** the terminal does not list those 10 bread

#### Scenario: Station links an aisle
- **WHEN** a rail runs from a controller to a station and on to a crate
- **THEN** the crate, the station and the controller form one online warehouse

### Requirement: Put-away without shuttles
When the warehouse is online and has no shuttle or no home rail, or the station or the chosen target touches no rail the home rail reaches, the warehouse SHALL move a buffer stack to its target at once, at most `put_away_per_tick` stacks per station per tick (example 1). Targets SHALL follow the put-away routing of the zones. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Instant put-away
- **WHEN** a station touching the controller receives 64 cobblestone and the warehouse has no shuttles
- **THEN** within a few ticks the cobblestone sits in a storage block and the buffer is empty

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a crate is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** the raw iron lands in that crate

### Requirement: Put-away by shuttle
When the warehouse has shuttles and a home rail, and both the station and the chosen target touch rails the home rail reaches, a buffer stack SHALL become a put-away job. A free shuttle SHALL drive to the station's rail, take the stack, drive to the target's rail, insert it, and return home. The stack SHALL stay in the buffer until the shuttle loads it and SHALL NOT be offered to a second shuttle meanwhile. What the target no longer accepts on arrival SHALL ride back and return to the buffer.

#### Scenario: Shuttle put-away
- **WHEN** a station beside a rail receives 40 cobblestone and the only crate with room stands beside a rail eight blocks further
- **THEN** the cobblestone leaves the buffer only when a shuttle arrives, is carried along the rail, and lands in the crate when the shuttle gets there

#### Scenario: Buffer shows the wait
- **WHEN** three stacks arrive and one shuttle exists
- **THEN** the stacks leave the buffer one at a time as the shuttle comes back for each

#### Scenario: Taken out before loading
- **WHEN** the player removes the stack from the buffer GUI before the shuttle arrives
- **THEN** the shuttle returns home empty and nothing is stored

#### Scenario: Target filled meanwhile
- **WHEN** the target crate is filled by hand while the shuttle is on its way
- **THEN** the shuttle brings the stack back and it is in the buffer again

### Requirement: Backed up station
A stack that the routing cannot place anywhere SHALL wait in the buffer and the station SHALL show a red lamp while any stack waits. The station GUI SHALL name the first waiting stack and the reason: no room in its zone and in general storage, or no storage at all.

#### Scenario: Zone and general storage full
- **WHEN** raw copper waits because every "Aisle 3" block and every unzoned block is full
- **THEN** the station lamp is red and the GUI reads that raw copper has no room in "Aisle 3" or in general storage

#### Scenario: Room appears
- **WHEN** a new crate is stamped "Aisle 3" while raw copper waits
- **THEN** the copper is put away within a few ticks and the lamp goes back to the online colour

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the buffer slots, the player inventory and a status line: idle, putting away, or the first waiting stack with its reason. Shift clicking an inventory stack SHALL move it into the buffer. While offline the status SHALL show the reason. Zones are defined on Zone Labels and seen on the blocks; the station does not list them.

#### Scenario: Shift click into the buffer
- **WHEN** the player shift clicks 20 cobblestone in the inventory
- **THEN** the cobblestone is in the buffer and is put away as if a hopper had fed it

#### Scenario: Offline
- **WHEN** the station touches crates but no controller
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Waiting stack
- **WHEN** raw copper waits because its zone and general storage are full
- **THEN** the status line names raw copper and the reason

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents. The Configuration Carrot without sneaking SHALL turn its facing.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest and a hopper in the station pattern (for example a chest over a hopper framed by ingots)
- **THEN** a Dispatch Station is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items

# storage/inbound-station Specification

## Purpose

The Inbound Station is the warehouse's inbound dock: items arrive in its buffer and the warehouse puts them away by its zones, carried by shuttles over the rails when the warehouse has them.

## Requirements

### Requirement: Station block and buffer
The Inbound Station SHALL be a block with a horizontal facing and an intake buffer of `inbound_buffer_slots` slots (example 9, from `data/<ns>/storage/warehouse.json`). Every relative side but the front SHALL be in one of three modes of the Lib side configuration, Input, Pull or Off, all Input by default; the front is fixed to Off. On Input sides the buffer SHALL be exposed as an item handler that accepts inserts and refuses extraction, so hoppers, ducts and machines feed it but never take from it. On Pull sides the station SHALL additionally take stacks out of the neighbour's item handler every `push_interval` ticks while the buffer has room. Off sides SHALL expose nothing. The Configuration Carrot used on a face SHALL cycle that face's mode and the GUI SHALL show the Lib side panel. Breaking the station SHALL drop the buffer contents.

#### Scenario: Hopper feeds the station
- **WHEN** a hopper above the station holds 20 cobblestone
- **THEN** the cobblestone moves into the station's buffer

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper under the station points into a chest and the buffer holds bread
- **THEN** the bread stays in the buffer

#### Scenario: Side switched off
- **WHEN** the player uses the carrot on the top face twice and a hopper above offers cobblestone
- **THEN** the cobblestone stays in the hopper until the top is set to Input again

#### Scenario: Pull from a chest
- **WHEN** the player sets the back side to Pull and a chest behind the station holds 30 bread
- **THEN** the bread moves into the buffer and on into the warehouse without a hopper

#### Scenario: Front closed
- **WHEN** a machine pushes against the station's front
- **THEN** nothing goes in and the carrot on the front only reports Off

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
When the warehouse is online and the chosen target is a crate or foreign inventory that no shuttle can carry to, the warehouse SHALL move a buffer stack there at once, at most `put_away_per_tick` stacks per station per tick (example 1). Targets SHALL follow the put-away routing of the zones. A Heavy Duty Rack SHALL never be filled instantly: a stack whose only room is in racks SHALL wait for a shuttle, and when no shuttle can reach them the station SHALL report that the stack needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Instant put-away
- **WHEN** a station touching the controller receives 64 cobblestone, a crate touches the controller and the warehouse has no garage
- **THEN** within a few ticks the cobblestone sits in the crate and the buffer is empty

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a crate is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** the raw iron lands in that crate

#### Scenario: Only a rack has room
- **WHEN** a station receives bread, the only block with room is a rack and the warehouse has no garage
- **THEN** the bread waits in the buffer, the lamp turns red and the GUI says the bread needs a shuttle

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
A stack that the routing cannot place anywhere SHALL wait in the buffer. The station SHALL carry a second lamp beside the connection lamp that shows what it is doing: green while the buffer is empty, orange while stacks are being put away or wait for a shuttle on its way, red while any stack has no room, and dark while the warehouse is offline. The station GUI SHALL name the first waiting stack and the reason while the lamp is red: no room in its zone and in general storage, no storage at all, or only racks and no shuttle.

#### Scenario: Zone and general storage full
- **WHEN** raw copper waits because every "Aisle 3" block and every unzoned block is full
- **THEN** the activity lamp is red and the GUI reads that raw copper has no room in "Aisle 3" or in general storage

#### Scenario: Room appears
- **WHEN** a new crate is stamped "Aisle 3" while raw copper waits
- **THEN** the copper is put away within a few ticks and the activity lamp goes back to green

#### Scenario: Shuttle on its way
- **WHEN** a stack waits in the buffer for the shuttle that was sent for it
- **THEN** the activity lamp is orange and turns green once the buffer is empty

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the buffer slots and the player inventory. Idle and working are shown by the lamps on the block, not in the GUI; the GUI SHALL show a line only while the warehouse is offline, naming the reason, or while a stack has no room, naming the stack and the reason. Shift clicking an inventory stack SHALL move it into the buffer. Zones are defined on Zone Labels and seen on the blocks; the station does not list them.

#### Scenario: Shift click into the buffer
- **WHEN** the player shift clicks 20 cobblestone in the inventory
- **THEN** the cobblestone is in the buffer and is put away as if a hopper had fed it

#### Scenario: Offline
- **WHEN** the station touches crates but no controller
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Waiting stack
- **WHEN** raw copper waits because its zone and general storage are full
- **THEN** the GUI names raw copper and the reason

#### Scenario: Nothing to say
- **WHEN** the station is online and nothing waits
- **THEN** the GUI shows only the buffer and the inventory

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents and keeping its side modes in the dropped item. The facing is set at placement; the carrot without sneaking configures sides and does not turn the station.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest and a hopper in the station pattern (for example a chest over a hopper framed by ingots)
- **THEN** a Inbound Station is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items

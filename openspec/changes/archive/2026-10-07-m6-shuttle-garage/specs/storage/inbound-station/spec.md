# Spec Delta

## MODIFIED Requirements

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

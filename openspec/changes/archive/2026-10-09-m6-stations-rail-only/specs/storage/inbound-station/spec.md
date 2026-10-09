# Spec Delta

## MODIFIED Requirements

### Requirement: Station is a warehouse node without stock
The station SHALL be a member of the warehouse network only through rails: rails SHALL dock onto it on any side and link it, and it SHALL NOT link to touching controllers, storage blocks, racks, garages, other stations or elevator segments, so it never passes the warehouse connection on to a neighbour. Its buffer SHALL NOT be indexed and inventories touching it SHALL NOT become warehouse sources. The station SHALL show the connection lamp like storage blocks, lit only while a rail links it to an online warehouse.

#### Scenario: Chest feeding the station is not stock
- **WHEN** a chest with 30 bread touches only the station of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Buffer is not stock
- **WHEN** the buffer holds 10 bread that has not been put away yet
- **THEN** the terminal does not list those 10 bread

#### Scenario: Station links an aisle
- **WHEN** a rail runs from a controller to a station and on to a crate
- **THEN** the crate, the station and the controller form one online warehouse

#### Scenario: Touching the controller is not enough
- **WHEN** a station touches the controller and no rail touches the station
- **THEN** the station is not part of that warehouse, its lamp is dark and items in its buffer stay there

#### Scenario: Rail placed later
- **WHEN** a rail is then placed between the station and the controller
- **THEN** the station joins the warehouse and its lamp lights; breaking that rail takes it out again

#### Scenario: Crate touching only the station
- **WHEN** a crate touches only a station that is linked by rail
- **THEN** the crate is not part of the warehouse

### Requirement: Put-away without shuttles
When the warehouse is online and the chosen target is a crate or foreign inventory that no shuttle can carry to, the warehouse SHALL move a buffer stack there at once, at most `put_away_per_tick` stacks per station per tick (example 1). Targets SHALL follow the put-away routing of the zones. A Heavy Duty Rack SHALL never be filled instantly: a stack whose only room is in racks SHALL wait for a shuttle, and when no shuttle can reach them the station SHALL report that the stack needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Instant put-away
- **WHEN** a station linked by a rail to the controller receives 64 cobblestone, a crate touches the controller and the warehouse has no garage
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

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the buffer slots and the player inventory. Idle and working are shown by the lamps on the block, not in the GUI; the GUI SHALL show a line only while the warehouse is offline, naming the reason, or while a stack has no room, naming the stack and the reason. Shift clicking an inventory stack SHALL move it into the buffer. Zones are defined on Zone Labels and seen on the blocks; the station does not list them.

#### Scenario: Shift click into the buffer
- **WHEN** the player shift clicks 20 cobblestone in the inventory
- **THEN** the cobblestone is in the buffer and is put away as if a hopper had fed it

#### Scenario: Offline
- **WHEN** the station touches crates and a controller but no rail
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Waiting stack
- **WHEN** raw copper waits because its zone and general storage are full
- **THEN** the GUI names raw copper and the reason

#### Scenario: Nothing to say
- **WHEN** the station is online and nothing waits
- **THEN** the GUI shows only the buffer and the inventory

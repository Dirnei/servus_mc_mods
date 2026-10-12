# storage/inbound-station Specification

## Purpose

The Inbound Station is the warehouse's inbound dock: items arrive in its buffer and the warehouse puts them away by its zones, carried by shuttles over the rails when the warehouse has them.

## Requirements

### Requirement: Station block and buffer
The Inbound Station SHALL be a block with a horizontal facing and an intake buffer of `inbound_buffer_slots` slots (example 9, from `data/<ns>/storage/warehouse.json`). Every relative side but the front SHALL be in one of three modes of the Lib side configuration, Input, Pull or Off, all Input by default; the front is fixed to Off. On Input and Pull sides the buffer SHALL be exposed as an item handler that accepts inserts and refuses extraction, so hoppers, item pipes and machines feed it but never take from it, and item pipes show an item arm toward those sides. On Pull sides the station SHALL additionally take stacks out of the neighbour's item handler every `push_interval` ticks while the buffer has room. Off sides SHALL expose nothing. The Configuration Carrot used on a face SHALL cycle that face's mode and the GUI SHALL show the Lib side panel. Breaking the station SHALL drop the buffer contents.

#### Scenario: Hopper feeds the station
- **WHEN** a hopper above the station holds 20 cobblestone
- **THEN** the cobblestone moves into the station's buffer

#### Scenario: Item pipe feeds the station
- **WHEN** a duct with the Item Pipe beside the station carries cobblestone toward it
- **THEN** the duct shows an item arm toward the station and the cobblestone moves into the buffer

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper under the station points into a chest and the buffer holds bread
- **THEN** the bread stays in the buffer

#### Scenario: Side switched off
- **WHEN** the player uses the carrot on the top face twice and a hopper above offers cobblestone
- **THEN** the cobblestone stays in the hopper until the top is set to Input again, and a duct on that face shows no item arm

#### Scenario: Pull from a chest
- **WHEN** the player sets the back side to Pull and a chest behind the station holds 30 bread
- **THEN** the bread moves into the buffer and on into the warehouse without a hopper

#### Scenario: Front closed
- **WHEN** a machine pushes against the station's front
- **THEN** nothing goes in and the carrot on the front only reports Off

#### Scenario: Broken with contents
- **WHEN** the station is broken while the buffer holds 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Put-away by shuttle
A buffer stack SHALL be put away only by shuttle. When the warehouse is online, has shuttles and a home rail, and both the station and the chosen rack touch rails the home rail reaches, a buffer stack SHALL become a put-away job. A free shuttle SHALL drive to the station's rail, take the stack, drive to the rack's rail, insert it, and return home. The stack SHALL stay in the buffer until the shuttle loads it and SHALL NOT be offered to a second shuttle meanwhile. What the rack no longer accepts on arrival SHALL ride back and return to the buffer. Without a shuttle that can do this, the stack SHALL wait and the station SHALL report that it needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Shuttle put-away
- **WHEN** a station beside a rail receives 40 cobblestone and the only rack with room stands beside a rail eight blocks further
- **THEN** the cobblestone leaves the buffer only when a shuttle arrives, is carried along the rail, and lands in the rack when the shuttle gets there

#### Scenario: Buffer shows the wait
- **WHEN** three stacks arrive and one shuttle exists
- **THEN** the stacks leave the buffer one at a time as the shuttle comes back for each

#### Scenario: Taken out before loading
- **WHEN** the player removes the stack from the buffer GUI before the shuttle arrives
- **THEN** the shuttle returns home empty and nothing is stored

#### Scenario: Target filled meanwhile
- **WHEN** the target rack is filled by hand while the shuttle is on its way
- **THEN** the shuttle brings the stack back and it is in the buffer again

#### Scenario: No garage
- **WHEN** a station receives bread, a rack has room and the warehouse has no garage
- **THEN** the bread waits in the buffer, the lamp turns red and the GUI says the bread needs a shuttle

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a rack is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** a shuttle puts the raw iron into that rack

### Requirement: Backed up station
A stack that the routing cannot place anywhere SHALL wait in the buffer. The station SHALL carry a second lamp beside the connection lamp that shows what it is doing: green while the buffer is empty, orange while stacks are being put away or wait for a shuttle on its way, red while any stack has no room, and dark while the warehouse is offline. The station GUI SHALL name the first waiting stack and the reason while the lamp is red: no room in its zone and in general storage, no storage at all, or no shuttle.

#### Scenario: Zone and general storage full
- **WHEN** raw copper waits because every "Aisle 3" rack and every unzoned rack is full
- **THEN** the activity lamp is red and the GUI reads that raw copper has no room in "Aisle 3" or in general storage

#### Scenario: Room appears
- **WHEN** a new rack is stamped "Aisle 3" while raw copper waits
- **THEN** a shuttle puts the copper away and the activity lamp goes back to green

#### Scenario: Shuttle on its way
- **WHEN** a stack waits in the buffer for the shuttle that was sent for it
- **THEN** the activity lamp is orange and turns green once the buffer is empty

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the buffer slots and the player inventory. Idle and working are shown by the lamps on the block, not in the GUI; the GUI SHALL show a line only while the warehouse is offline, naming the reason, or while a stack has no room, naming the stack and the reason. Shift clicking an inventory stack SHALL move it into the buffer. Zones are defined on Zone Labels and seen on the blocks; the station does not list them.

#### Scenario: Shift click into the buffer
- **WHEN** the player shift clicks 20 cobblestone in the inventory
- **THEN** the cobblestone is in the buffer and is put away as if the station had pulled it

#### Scenario: Offline
- **WHEN** the station touches a controller but no rail
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Waiting stack
- **WHEN** raw copper waits because its zone and general storage are full
- **THEN** the GUI names raw copper and the reason

#### Scenario: Nothing to say
- **WHEN** the station is online and nothing waits
- **THEN** the GUI shows only the buffer and the inventory

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials including planks, recording the wood of the planks, SHALL drop itself with its wood when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents and keeping its side modes and wood in the dropped item. The facing is set at placement; the carrot without sneaking configures sides and does not turn the station.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest, a hopper and planks in the station pattern (for example a chest framed by ingots over a hopper between planks)
- **THEN** a Inbound Station carrying the wood of the planks is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items

### Requirement: Inbound Station joins by rail and holds no stock
The station SHALL be a member of the warehouse network only through rails: rails SHALL dock onto it on any side and link it, and it SHALL NOT link to touching controllers, storage blocks, racks, garages, other stations or elevator segments, so it never passes the warehouse connection on to a neighbour. Its buffer SHALL NOT be indexed and inventories touching it SHALL NOT become warehouse sources. The station SHALL show the connection lamp like storage blocks, lit only while a rail links it to an online warehouse.

#### Scenario: Chest feeding the station is not stock
- **WHEN** a chest with 30 bread touches only the station of an online warehouse on an Off side
- **THEN** the terminal does not list the bread

#### Scenario: Buffer is not stock
- **WHEN** the buffer holds 10 bread that has not been put away yet
- **THEN** the terminal does not list those 10 bread

#### Scenario: Station links an aisle
- **WHEN** a rail runs from a controller to a station and on to a rack front
- **THEN** the rack, the station and the controller form one online warehouse

#### Scenario: Touching the controller is not enough
- **WHEN** a station touches the controller and no rail touches the station
- **THEN** the station is not part of that warehouse, its lamp is dark and items in its buffer stay there

#### Scenario: Rail placed later
- **WHEN** a rail is then placed between the station and the controller
- **THEN** the station joins the warehouse and its lamp lights; breaking that rail takes it out again

#### Scenario: Rack touching only the station
- **WHEN** a rack touches only a station that is linked by rail and no rail stands in front of the rack
- **THEN** the rack is not part of the warehouse

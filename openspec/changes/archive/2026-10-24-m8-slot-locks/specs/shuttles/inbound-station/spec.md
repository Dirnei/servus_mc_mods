## MODIFIED Requirements

### Requirement: Station block and buffer
The Inbound Station SHALL be a block with a horizontal facing and an intake buffer of `inbound_buffer_slots` slots (example 9, from `data/<ns>/shuttles/warehouse.json`). Every relative side but the front SHALL be in one of three modes of the Lib side configuration, Input, Pull or Off, all Input by default; the front is fixed to Off. On Input and Pull sides the buffer SHALL be exposed as an item handler that accepts inserts and refuses extraction, so hoppers, item pipes and machines feed it but never take from it, and item pipes show an item arm toward those sides. On Pull sides the station SHALL additionally take stacks out of the neighbour's item handler every `push_interval` ticks while the buffer has room, taking only what its unlocked slots accept and never losing an item between the neighbour and the buffer. Off sides SHALL expose nothing. Slots locked by a job SHALL take nothing from any side. The Configuration Carrot used on a face SHALL cycle that face's mode and the GUI SHALL show the Lib side panel. Breaking the station SHALL drop the buffer contents.

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

#### Scenario: Pull into a nearly full buffer
- **WHEN** a Pull side offers 64 cobblestone while the unlocked slots take only 10
- **THEN** 10 cobblestone move into the buffer and 54 stay in the neighbour

#### Scenario: Front closed
- **WHEN** a machine pushes against the station's front
- **THEN** nothing goes in and the carrot on the front only reports Off

#### Scenario: Broken with contents
- **WHEN** the station is broken while the buffer holds 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Put-away by shuttle
A buffer stack SHALL be put away only by shuttle. When the warehouse is online, has shuttles and a home rail, and both the station and the chosen rack touch rails the home rail reaches, a buffer stack SHALL become a put-away job: its slot is OUT locked and the room for it in the target rack is IN locked (see `shuttles/slot-locks`). A free shuttle SHALL drive to the station's rail, take the locked amount, drive to the rack's rail, unload into the locked slots, and return home. The stack SHALL stay in the buffer until the shuttle loads it; nobody else SHALL take it out or add to it meanwhile, and it SHALL NOT be offered to a second shuttle. Cargo SHALL never return to the station and never drop: what a rack does not take on arrival SHALL be routed again by the put-away routing, and with no room anywhere the shuttle SHALL park it in its garage (see `shuttles/shuttle-garage`). Without a shuttle that can do this, the stack SHALL wait and the station SHALL report that it needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Shuttle put-away
- **WHEN** a station beside a rail receives 40 cobblestone and the only rack with room stands beside a rail eight blocks further
- **THEN** the cobblestone leaves the buffer only when a shuttle arrives, is carried along the rail, and lands in the rack when the shuttle gets there

#### Scenario: Buffer shows the wait
- **WHEN** three stacks arrive and one shuttle exists
- **THEN** the stacks leave the buffer one at a time as the shuttle comes back for each

#### Scenario: Taken out before loading
- **WHEN** the player tries to take the stack out of the buffer GUI before the shuttle arrives
- **THEN** the stack stays in its slot and the shuttle takes it

#### Scenario: Target filled meanwhile
- **WHEN** a player tries to fill the target rack by hand while the shuttle is on its way
- **THEN** the slots locked for the shuttle stay free and the cobblestone lands there

#### Scenario: Station pulling fast
- **WHEN** a chest and a backpack on Pull sides keep the buffer full while shuttles put away into a nearly full rack
- **THEN** every item ends in a rack or waits in the buffer, and nothing drops at the station or the rack

#### Scenario: No garage
- **WHEN** a station receives bread, a rack has room and the warehouse has no garage
- **THEN** the bread waits in the buffer, the lamp turns red and the GUI says the bread needs a shuttle

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a rack is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** a shuttle puts the raw iron into that rack

# shuttles/outbound-station Specification

## Purpose

The Outbound Station is where items leave the warehouse: it keeps a configured set of items stocked in its buffer and hands them to machines, hoppers and ducts.

## Requirements

### Requirement: Station block, buffer and keep slots
The Outbound Station SHALL be a block with a horizontal facing, a buffer of `inbound_buffer_slots` slots (example 9, from `warehouse.json`) and the same number of keep slots. A keep slot SHALL hold a ghost item and a target amount. Breaking or dismantling the station SHALL drop the buffer contents; keep slots are configuration and SHALL persist with the block.

#### Scenario: Keep slot set
- **WHEN** the player clicks a keep slot while carrying 32 iron ingots
- **THEN** the slot shows iron ingots with the amount 32 and the carried stack is unchanged

#### Scenario: Amount adjusted
- **WHEN** the player scrolls over a keep slot showing 32 iron ingots, once up and once down with shift
- **THEN** the amount reads 33, then 33 minus one stack, never below 1

#### Scenario: Keep slot cleared
- **WHEN** the player right clicks a keep slot with an empty hand
- **THEN** the slot is empty

### Requirement: Side modes
Every relative side of the station but the front SHALL be in one of three modes of the Lib side configuration: Push, Output or Off; the front is fixed to Off. On a Push side the station SHALL push its buffer into the neighbour's item handler every `push_interval` ticks (example 8), stacks the neighbour refuses waiting in the buffer. On Push and Output sides the buffer SHALL be exposed as an item handler that allows extraction and refuses inserts, so a neighbour MAY extract from it and item pipes show an item arm toward those sides. Off sides SHALL expose nothing. Inserting through any side SHALL be refused. The back SHALL default to Push and the other sides to Off. The Configuration Carrot used on a face SHALL cycle that face's mode Push, Output, Off, and the GUI SHALL show the Lib side panel.

#### Scenario: Pushed into an Assembler
- **WHEN** the back faces an Assembler with the iron gear recipe and 4 iron ingots sit in the buffer
- **THEN** the ingots are in the Assembler's inputs within the push interval

#### Scenario: Refused by the machine
- **WHEN** cobblestone sits in the buffer and the Assembler's recipes do not use it
- **THEN** the cobblestone stays in the buffer

#### Scenario: Pushed into a pipe
- **WHEN** the back touches a duct whose Item Pipe side toward the station takes items in, and iron ingots sit in the buffer
- **THEN** the duct shows an item arm toward the station and the station pushes the ingots into the duct, which carries them on

#### Scenario: Hopper on an extract side
- **WHEN** the bottom side is set to Output and a hopper hangs under the station
- **THEN** the hopper drains the buffer

#### Scenario: Hopper on an off side
- **WHEN** the bottom side is Off
- **THEN** the hopper takes nothing

#### Scenario: Nothing goes in
- **WHEN** a hopper above the station offers cobblestone
- **THEN** it is refused on every side

#### Scenario: Carrot cycles a side
- **WHEN** the player uses the carrot on the top face twice
- **THEN** the top is Output after the first use and Off after the second

#### Scenario: Front closed
- **WHEN** the player uses the carrot on the front
- **THEN** the front stays Off

### Requirement: Station is a warehouse node without stock
The station SHALL be a member of the warehouse network only through rails, like an Inbound Station: rails SHALL dock onto it on every side and link it, and it SHALL NOT link to touching controllers, storage blocks, racks, garages, other stations or elevator segments. Its buffer SHALL NOT be indexed and the blocks it pushes into SHALL NOT become warehouse sources through it. It SHALL show the connection lamp, lit only while a rail links it to an online warehouse, and an activity lamp: green while every keep slot is satisfied, orange while deliveries are on their way, red while a keep slot cannot be filled, dark while offline.

#### Scenario: Machine is not stock
- **WHEN** an Assembler holds 20 iron ingots in front of a station of an online warehouse
- **THEN** the terminal does not list the ingots

#### Scenario: Lamp while short
- **WHEN** a keep slot asks for sticks that are not in stock
- **THEN** the activity lamp is red and turns green once the sticks arrived

#### Scenario: Touching the controller is not enough
- **WHEN** an Outbound Station touches the controller and no rail touches the station
- **THEN** the station is offline and its keep slots are not filled

### Requirement: Deliveries on request
The warehouse SHALL deliver a requested amount of an item into the station's buffer when asked by `/servus_warehouse feed <station> <item> <count>`, planned like a terminal request and answering with the refusal reason when it cannot. Such deliveries SHALL come on top of the keep amounts. Work orders SHALL NOT deliver to Outbound Stations.

#### Scenario: Op feeds a station
- **WHEN** an op feeds 4 iron ingots to a station on the rails while a rack holds 10 and a garage holds a shuttle
- **THEN** a shuttle brings 4 ingots into the buffer

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the keep row, the buffer row, six side buttons, the player inventory and a state line that names the first keep slot that cannot be filled and why, or the offline reason. Shift clicking an inventory stack SHALL move it into the buffer.

#### Scenario: Hand stocked
- **WHEN** the player shift clicks 8 iron ingots in the inventory
- **THEN** the ingots are in the buffer and are pushed out like delivered ones

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials including planks, recording the wood of the planks, SHALL drop itself with its wood when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer and keeping its configuration and wood in the dropped item.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest, a hopper, a dropper and planks in the station pattern (for example chest over dropper framed by ingots over a hopper between planks)
- **THEN** an Outbound Station carrying the wood of the planks is produced

### Requirement: Buffer is restocked by shuttle
Every `outbound_check_interval` ticks (example 40) an online warehouse SHALL compare each keep slot's amount with the buffer count of that item plus what is already on its way to the station, and SHALL request the difference through the normal delivery path: by shuttle from racks a shuttle can reach. What is not in stock, or needs a shuttle the warehouse does not have, SHALL be requested again at the next check. The station SHALL NOT send anything back to storage.

#### Scenario: Restocked by shuttle
- **WHEN** a keep slot says 16 sticks, the buffer is empty, a rack on the rails holds 50 sticks and a garage holds a shuttle
- **THEN** a shuttle brings 16 sticks into the buffer and 34 remain in the rack

#### Scenario: Topped up after use
- **WHEN** the station pushes 5 of the 16 sticks into the block behind it
- **THEN** a shuttle brings 5 more sticks after the next check

#### Scenario: Out of stock
- **WHEN** no sticks are in stock
- **THEN** the buffer stays as it is and the station's GUI says sticks are out of stock

#### Scenario: No shuttle
- **WHEN** the sticks sit in a rack and the warehouse has no garage
- **THEN** the buffer stays as it is and the station's GUI says sticks need a shuttle

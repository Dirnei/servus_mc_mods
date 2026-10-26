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
Every `outbound_check_interval` ticks (example 40) an online warehouse SHALL compare each keep slot's amount with the buffer count of that item plus what is already on its way to the station, and SHALL request the difference through the normal delivery path: by shuttle from racks a shuttle can reach. A check SHALL ask for no more than the buffer's free unlocked slots take, one stack per slot, because every pick locks the slot it fills. What is not in stock, has no free slot yet, or needs a shuttle the warehouse does not have, SHALL be requested again at the next check. The station SHALL NOT send anything back to storage.

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

#### Scenario: Only free slots are asked for
- **WHEN** keep slots ask for 4 stacks of stone and only 2 buffer slots are free
- **THEN** a check plans 2 stacks and the rest at later checks as slots free up

### Requirement: Claim an export picker
Left clicking an empty keep slot of an online station with an empty hand SHALL open a Claim an export picker over the station GUI. Clicking an empty keep slot while carrying an item SHALL set a keep amount as before and SHALL NOT open the picker.

#### Scenario: Open the picker
- **WHEN** the player clicks an empty keep slot of Outbound #3 with an empty hand
- **THEN** the Claim an export picker opens over the station GUI

#### Scenario: Carrying an item
- **WHEN** the player clicks an empty keep slot carrying 32 iron ingots
- **THEN** the slot keeps 32 iron ingots and no picker opens

### Requirement: Picker lists
The picker SHALL list the exports of the station's warehouse that no station holds, each with its item and threshold, and below them the exports held by other stations of that warehouse, each with the holding station's label. Exports held by this station SHALL NOT be listed. With no export to list the picker SHALL say so.

#### Scenario: Unassigned and held elsewhere
- **WHEN** the wheat export has no station and Smoker Line (#1) holds the kelp export, and the player opens the picker at Outbound #3
- **THEN** wheat is listed with its threshold under the exports without a station and kelp is listed below with Smoker Line (#1)

#### Scenario: Nothing to claim
- **WHEN** every export of the warehouse is held by this station
- **THEN** the picker says there is no export to claim

### Requirement: Picker search
A search box SHALL filter both lists of the picker by item name or station label as the player types.

#### Scenario: Search
- **WHEN** the picker lists rotten flesh, wheat seeds and kelp at Smoker Line (#1) and the player types "smoker"
- **THEN** only kelp is listed

### Requirement: Claiming an export
Picking an entry SHALL assign that export to this station and that slot and close the picker. For an export held elsewhere it SHALL also free the slot at the other station.

#### Scenario: Claim an unassigned export
- **WHEN** the wheat rule exports above 2000 with no station and the player clicks the third empty keep slot of Outbound #3 with an empty hand and picks wheat
- **THEN** the third keep slot shows wheat with the export marker and the wheat rule names Outbound #3

#### Scenario: Move from another station
- **WHEN** Smoker Line (#1) holds the kelp export and the player claims kelp at Outbound #3
- **THEN** Outbound #3 holds the kelp export and the keep slot at Smoker Line (#1) is empty

### Requirement: Export slots
A keep slot holding an export SHALL show the item with an export marker and the threshold, and its tooltip SHALL name the rule's threshold and export state. The export slot SHALL NOT keep anything stocked in the buffer.

#### Scenario: Marker and tooltip
- **WHEN** Outbound #3 holds the wheat export above 2000 in its third keep slot and the player hovers it
- **THEN** the slot shows wheat with the export marker and the tooltip names the threshold 2000 and the export state

### Requirement: Export threshold is read only at the station
The threshold of an export slot SHALL be edited only on the rule. Scrolling over an export slot SHALL change nothing and clicking it while carrying an item SHALL change nothing.

#### Scenario: Read only threshold
- **WHEN** the player scrolls over the wheat export slot
- **THEN** the threshold still reads the rule's 2000

#### Scenario: Click with an item
- **WHEN** the player clicks the wheat export slot carrying 32 iron ingots
- **THEN** the slot still holds the wheat export and the player still carries the iron ingots

### Requirement: Releasing an export at the station
Right clicking an export slot with an empty hand SHALL release the export, leaving the rule without a station and the slot empty.

#### Scenario: Release at the station
- **WHEN** the player right clicks the wheat export slot with an empty hand
- **THEN** the slot is empty and the wheat rule shows no station

### Requirement: Exports are delivered by shuttle
Every `outbound_check_interval` ticks an online warehouse SHALL, for each export a station holds, compare the indexed count of the item with the threshold and request the surplus not yet on its way into that station's buffer through the normal delivery path, by shuttle from racks a shuttle can reach, limited by the buffer's free unlocked slots like a keep check. Surplus that cannot be planned at a check SHALL be requested again at the next one.

#### Scenario: Surplus shipped
- **WHEN** Outbound #3 holds the wheat export above 2000, the racks hold 2300 wheat and a garage holds a shuttle
- **THEN** shuttles bring 300 wheat into the station's buffer and 2000 remain in the racks

#### Scenario: Below the threshold
- **WHEN** the racks hold 1800 wheat
- **THEN** no wheat is shipped to the station

#### Scenario: Not asked twice
- **WHEN** 300 wheat surplus is on its way to the station and the next check runs
- **THEN** no further wheat is requested for those 300

### Requirement: Incoming surplus skips the racks
When items are put away and an Outbound Station holds the item's export, the part that would lift the stock above the threshold once stored SHALL go by shuttle straight from the source to that station. Only as much SHALL go as the station's free unlocked slots take, and only when a shuttle can drive there; the rest SHALL be stored, and the export ships it later.

#### Scenario: Terminal input above the threshold
- **WHEN** Outbound #3 holds the wheat export above 40, the racks hold 30 wheat and the player puts 50 wheat into the Storage Terminal's input
- **THEN** 10 wheat go into a rack, 40 wheat ride straight to Outbound #3 and the racks never hold more than 40

#### Scenario: Station has no room
- **WHEN** every buffer slot of Outbound #3 is full, the racks hold 40 wheat and the player puts 20 wheat into the Storage Terminal's input
- **THEN** the 20 wheat are stored in a rack, and once the station has room the export ships them from the rack

#### Scenario: Below the threshold
- **WHEN** the racks hold 1000 wheat, the export is above 2000 and 64 wheat arrive at an Inbound Station
- **THEN** all 64 wheat are stored in a rack

### Requirement: Surplus on its way straight to the station is counted once
Items on their way straight from a source to the export's station SHALL count as on their way to that station, so the export check does not request them again, and SHALL NOT count as held for the rule's cap.

#### Scenario: Not requested again
- **WHEN** 40 wheat surplus ride from the Packing Station straight to Outbound #3 and the export check runs
- **THEN** no wheat is requested from the racks for them

#### Scenario: Not held for the cap
- **WHEN** the wheat rule caps at 100 and exports above 40, and 40 wheat wait at an Inbound Station to go straight to Outbound #3
- **THEN** the cap does not count those 40 wheat as held

### Requirement: Delivered surplus leaves the station
The station SHALL push delivered surplus out of its Push sides and offer it on its Output sides like any buffer stack.

#### Scenario: Pushed into a Sink
- **WHEN** the station's back faces a Sink and wheat surplus arrives
- **THEN** the wheat moves on into the Sink and the next check ships more surplus if the racks hold any

### Requirement: Export lamp and state line
While surplus waits and the buffer has no free unlocked slot, the activity lamp SHALL be red and the GUI state line SHALL name the item and say the station is full. While surplus is on its way the lamp SHALL be orange.

#### Scenario: Nothing takes it
- **WHEN** the station has no Push side and every buffer slot holds wheat while the racks still hold 3000
- **THEN** no more wheat is planned, the lamp is red and the GUI says the station is full of wheat

#### Scenario: On its way
- **WHEN** a shuttle is bringing wheat surplus to the station
- **THEN** the lamp is orange

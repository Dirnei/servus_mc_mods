## MODIFIED Requirements

### Requirement: Deliveries on request
The warehouse SHALL deliver a requested amount of an item into the station's buffer when asked by `/servus_warehouse feed <station> <item> <count>`, planned like a terminal request and answering with the refusal reason when it cannot. Such deliveries SHALL come on top of the keep amounts. Work orders SHALL NOT deliver to Outbound Stations.

#### Scenario: Op feeds a station
- **WHEN** an op feeds 4 iron ingots to a station on the rails while a rack holds 10 and a garage holds a shuttle
- **THEN** a shuttle brings 4 ingots into the buffer

## ADDED Requirements

### Requirement: Push sides
Every relative side of the station but the front SHALL be Push or Off; the front is fixed to Off. On a Push side the station SHALL push its buffer into the neighbour's item handler every `push_interval` ticks (example 8), stacks the neighbour refuses waiting in the buffer. The station SHALL register no item capability, so hoppers, item pipes and machines can neither insert into nor extract from it and pipes show no item arm toward it. The back SHALL default to Push and the other sides to Off. The Configuration Carrot used on a face SHALL cycle that face's mode Push, Off, and the GUI SHALL show the Lib side panel.

#### Scenario: Pushed into an Assembler
- **WHEN** the back faces an Assembler with the iron gear recipe and 4 iron ingots sit in the buffer
- **THEN** the ingots are in the Assembler's inputs within the push interval

#### Scenario: Refused by the machine
- **WHEN** cobblestone sits in the buffer and the Assembler's recipes do not use it
- **THEN** the cobblestone stays in the buffer

#### Scenario: Pushed into a pipe
- **WHEN** the back touches a duct whose Item Pipe side toward the station takes items in, and iron ingots sit in the buffer
- **THEN** the station pushes the ingots into the duct, which carries them on

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper hangs under the station and the buffer holds iron ingots
- **THEN** the hopper takes nothing

#### Scenario: Nothing goes in
- **WHEN** a hopper above the station offers cobblestone
- **THEN** it is refused on every side

#### Scenario: Carrot cycles a side
- **WHEN** the player uses the carrot on the top face twice
- **THEN** the top is Push after the first use and Off after the second

#### Scenario: Front closed
- **WHEN** the player uses the carrot on the front
- **THEN** the front stays Off

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

## REMOVED Requirements

### Requirement: Side modes
**Reason**: The Output mode let hoppers and pipes extract through the item capability, which the station no longer registers.
**Migration**: See "Push sides": the station pushes into the block behind it; set a side to Push to feed a pipe or a hopper's container.

### Requirement: Buffer is kept stocked
**Reason**: The keep amounts were also filled instantly from crates and foreign inventories, which are no longer part of a warehouse.
**Migration**: See "Buffer is restocked by shuttle"; the warehouse needs a garage with a shuttle that reaches the station's rail.

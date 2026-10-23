## MODIFIED Requirements

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

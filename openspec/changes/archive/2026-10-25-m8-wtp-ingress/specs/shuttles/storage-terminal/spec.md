## MODIFIED Requirements

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid, filled by the player and by packets that no request of the terminal waits for. Stacks in the Input row SHALL be moved over the data line to the Packing Station the warehouse chooses for them, which puts them away like any intake (racks by shuttle, zones). Each input slot SHALL send only what a station has granted it, at most the station's conversion rate into storage, so the row shrinks over time and keeps what no station can take yet; the message line SHALL say why a slot waits. An input slot whose stack is on its way SHALL take no more items until its move ends, so a stack handed in meanwhile, by click, shift click or packet, goes into another slot and gets a grant of its own. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock, also when the terminal touches a storage node or the controller, SHALL never be a put-away target or a source for picks, and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while a Packing Station of the warehouse is on the line
- **THEN** the slot empties over a few seconds and the cobblestone ends in the warehouse after the put-away

#### Scenario: More than the station can hold
- **WHEN** the player fills all 9 input slots with cobblestone while the station's To storage buffer has one free slot and no shuttle is free
- **THEN** one stack leaves the Input row and the other eight stay until the buffer has a free slot again

#### Scenario: Different items do not share slots
- **WHEN** the player puts a stack of cobblestone, logs and sand into the Input row while the station has three free To storage slots
- **THEN** each stack gets a slot of its own and each arrives whole in its slot

#### Scenario: Second stack while the first leaves
- **WHEN** the player shift clicks a stack of apples into the Input row, and a second stack of apples while the first is still leaving
- **THEN** the first slot is not topped up, the second stack takes a slot of its own, and the apples arrive as two whole stacks in two To storage slots

#### Scenario: No room
- **WHEN** the player puts bread into an input slot and no Packing Station is on the data line
- **THEN** the bread stays in the input slot and the message line says "No Packing Station on this data line"

#### Scenario: Racks only
- **WHEN** the warehouse has only Heavy Duty Racks with room and the player puts planks into an input slot
- **THEN** the planks end in a rack

#### Scenario: Terminal touching the controller
- **WHEN** the terminal stands directly beside the Warehouse Controller, its Output row holds 5 diamonds and the player puts 64 logs into the Input row of a warehouse whose only room is a rack
- **THEN** the stock shows no diamonds, all 64 logs end in the rack and the stock counts them once

#### Scenario: Nowhere to put it
- **WHEN** the player puts logs into the Input row and no storage of the warehouse has room for them
- **THEN** the logs stay in the Input row and the message line says that no storage takes them, instead of the logs waiting silently

#### Scenario: Hopper above the terminal
- **WHEN** a hopper above the terminal holds cobblestone
- **THEN** the cobblestone stays in the hopper and the Input row is unchanged

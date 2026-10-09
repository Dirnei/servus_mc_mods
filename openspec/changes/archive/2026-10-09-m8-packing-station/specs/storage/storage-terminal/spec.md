# Spec Delta

## MODIFIED Requirements

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid. Stacks in the Input row SHALL be moved over the data line to the Packing Station on the terminal's data line nearest to the terminal, which puts them away like any intake (racks by shuttle, zones, crates and foreign inventories). Items SHALL leave the Input row only as fast as the Packing Station takes them: at most its conversion rate into storage and only while its To storage buffer has room, so the row shrinks over time and keeps what the station cannot take yet. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while a Packing Station of the warehouse is on the line
- **THEN** the slot empties over a few seconds and the cobblestone ends in the warehouse after the put-away

#### Scenario: More than the station can hold
- **WHEN** the player fills all 9 input slots with cobblestone while the station's To storage buffer has room for one stack and no shuttle is free
- **THEN** one stack leaves the Input row and the other eight stay until the buffer has room

#### Scenario: No room
- **WHEN** the player puts bread into an input slot and no Packing Station is on the data line
- **THEN** the bread stays in the input slot and the message line says "No Packing Station on this data line"

#### Scenario: Racks only
- **WHEN** the warehouse has only Heavy Duty Racks with room and the player puts planks into an input slot
- **THEN** the planks end in a rack

# storage/data-carriers Specification

## Purpose
Lets Storage devices that sit side by side share one data network without a duct between them, like devices in Refined Storage.

## Requirements

### Requirement: Storage devices carry the data network
The Counter Terminal, the Production Terminal, the Crafting Station, the Pattern Storage and every block of a formed Crafting CPU SHALL be data carriers. An unformed Crafting CPU block SHALL carry nothing and SHALL join when its cube forms and leave when it unforms. The Access Point, the Machine Adapter and the Warehouse Controller SHALL NOT be carriers.

#### Scenario: Stack without a duct
- **WHEN** a Counter Terminal, a Pattern Storage and a Crafting Station stand in a row touching each other with no duct between them
- **THEN** they share one data network, and a duct touching any of them connects all three to the rest of the line

#### Scenario: Craft through the stack
- **WHEN** that row touches a formed Crafting CPU, one duct links the row to an Access Point of a warehouse holding planks, and the Pattern Storage holds a sticks pattern
- **THEN** requesting sticks at the terminal crafts them on the station and delivers them to the terminal

#### Scenario: Split
- **WHEN** the middle block of the row is broken
- **THEN** the two remaining blocks report two different data networks

#### Scenario: Chest in between
- **WHEN** a chest stands between a terminal and a Pattern Storage
- **THEN** they do not share a network

#### Scenario: Not carriers
- **WHEN** an Access Point or a Machine Adapter touches a terminal and no duct is near
- **THEN** it is not a member of the terminal's network

#### Scenario: CPU forms
- **WHEN** a Crafting CPU cube touching a terminal is formed with the carrot
- **THEN** the CPU is on the terminal's network, and when one CPU block is broken the rest leave it

# Spec Delta

## MODIFIED Requirements

### Requirement: Side modes
Every relative side of the station but the front SHALL be in one of three modes of the Lib side configuration: Push, Output or Off; the front is fixed to Off. On a Push side the station SHALL push its buffer into the neighbour's item handler every `push_interval` ticks (example 8), stacks the neighbour refuses waiting in the buffer. On an Output side the neighbour MAY extract from the buffer through the item handler capability. Inserting through any side SHALL be refused. The back SHALL default to Push and the other sides to Off. The Configuration Carrot used on a face SHALL cycle that face's mode Push, Output, Off, and the GUI SHALL show the Lib side panel.

#### Scenario: Pushed into an Assembler
- **WHEN** the back faces an Assembler with the iron gear recipe and 4 iron ingots sit in the buffer
- **THEN** the ingots are in the Assembler's inputs within the push interval

#### Scenario: Refused by the machine
- **WHEN** cobblestone sits in the buffer and the Assembler's recipes do not use it
- **THEN** the cobblestone stays in the buffer

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

### Requirement: Station reports its producer
A station whose back faces a block with the Lib production capability SHALL report that block's recipes and active recipe to the warehouse. `/servus_warehouse producers <pos>` SHALL list every Outbound Station of the network with the block behind it and its recipes, or say that it faces no producer.

#### Scenario: Assembler in front
- **WHEN** an op runs the producers command on a warehouse whose station stands with its back to an Assembler with the iron gear recipe
- **THEN** chat lists the station, the Assembler and "iron gear from 2 iron ingot, 20 ticks"

#### Scenario: Chest in front
- **WHEN** a station stands with its back to a chest
- **THEN** the producers list says the station faces no producer

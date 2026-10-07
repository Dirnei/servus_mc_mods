# Spec Delta

## MODIFIED Requirements

### Requirement: Side modes
Every relative side of the station SHALL be in one of three modes of the Lib side configuration: Push, Output or Off. On a Push side the station SHALL push its buffer into the neighbour's item handler every `push_interval` ticks (example 8), stacks the neighbour refuses waiting in the buffer. On an Output side the neighbour MAY extract from the buffer through the item handler capability. Inserting through any side SHALL be refused. The front SHALL default to Push and the other sides to Off. The Configuration Carrot used on a face SHALL cycle that face's mode Push, Output, Off, and the GUI SHALL show the Lib side panel.

#### Scenario: Pushed into an Assembler
- **WHEN** the front faces an Assembler with the iron gear recipe and 4 iron ingots sit in the buffer
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

# foundation/data-packets Specification

## Purpose
Packets move items over the data line for a delivery that a mod or an op starts, at a size and speed set by the datapack, so production on demand reaches distant machines without a pipe.

## Requirements

### Requirement: Deliveries split into packets
A delivery SHALL move a stack to a target member of a data network in packets of at most `packet_size` items (example 8, from `data/<ns>/data/packets.json`). One packet of a delivery SHALL be in flight at a time; the next leaves when the previous one has arrived.

#### Scenario: Twenty items
- **WHEN** a delivery of 20 planks starts with a packet size of 8
- **THEN** packets of 8, 8 and 4 planks travel one after another and the delivery is done when all 20 arrived

### Requirement: Packets follow the shortest route
A packet SHALL travel the shortest path of data ducts between the duct touching the source and the duct touching the target, at `packet_speed` blocks per second (example 4). The distance SHALL count every duct on the path plus the step from the source and the step into the target.

#### Scenario: Travel time
- **WHEN** a packet travels from a member through 6 data ducts to another member at 4 blocks per second
- **THEN** it arrives after 7 blocks, 35 ticks after it left

#### Scenario: Shorter branch
- **WHEN** two routes of 10 and 4 ducts connect source and target
- **THEN** the packet takes the route of 4 ducts

### Requirement: Delivery into a member
An arriving packet SHALL insert its items into the target's item handler on the face that touches the data duct, or into its stock sink when it has no item handler there. Items that do not fit SHALL wait at the target and be offered again every `retry_interval` ticks (example 20) until they fit. The delivery SHALL count only items that were accepted.

#### Scenario: Station accepts planks
- **WHEN** a packet of 8 planks arrives at a Crafting Station with the sticks recipe
- **THEN** the planks are in the station's inputs

#### Scenario: Target full
- **WHEN** a packet arrives at a target with room for 3 of its 8 items
- **THEN** 3 are inserted, 5 wait at the target, and they are inserted once room frees up

### Requirement: Taking items from a member
A move SHALL take up to the requested amount of an item from a source member, packet by packet when each packet leaves, through the source's stock provider or else its item handler on the face that touches the data duct. When the source runs out, the move SHALL end as done with fewer items and report how many it delivered.

#### Scenario: From the warehouse
- **WHEN** a move of 16 planks starts from a Warehouse Controller whose warehouse holds 32 planks to a Crafting Station
- **THEN** the warehouse holds 16 planks and the station 16 once the move is done

#### Scenario: Source short
- **WHEN** a move of 16 sticks starts from a station holding 4 sticks
- **THEN** the move ends as done after 4 sticks arrived and reports 4

### Requirement: Delivery state
Every delivery SHALL have an id and a state: running, done or failed with a reason. Starting a delivery between positions that no route connects SHALL fail at once with the reason no route and SHALL take nothing from the source. Callers SHALL be able to read the state by id and be told when it ends.

#### Scenario: No route
- **WHEN** a mod asks to move items to a member of a different data network
- **THEN** the delivery fails with no route and the source keeps its items

### Requirement: Broken routes keep the items
When a packet's route breaks, the packet SHALL take a new route from its current duct if one exists. Otherwise its items SHALL be inserted into the target, else returned to the source, else dropped as an item entity at the first loaded position among target, source and its last duct; items that cannot be placed anywhere yet SHALL be kept and dropped once such a position loads. Items SHALL never be voided. Packets in flight when the server stops SHALL be settled the same way before the world saves.

#### Scenario: Duct broken mid route
- **WHEN** the only duct between source and target is broken while a packet of 8 planks is on the way
- **THEN** the 8 planks end up in the target, the source or on the ground, and the delivery fails with the reason route broken

#### Scenario: Detour
- **WHEN** a duct on the packet's route is broken and a longer route still connects it to the target
- **THEN** the packet arrives by the longer route

### Requirement: Packet limits from data
`packet_size`, `packet_speed` and `retry_interval` SHALL be read from datapack JSON on load and reload, with defaults when the file is missing. Changing them SHALL affect packets that leave after the reload.

#### Scenario: Bigger packets
- **WHEN** a datapack sets `packet_size` to 32 and the server reloads
- **THEN** a delivery of 64 items travels as two packets

### Requirement: Test command
An op SHALL be able to start a move with `/servus_foundation data move <from> <to> <item> <count>`; the command SHALL report the delivery id, or why it failed.

#### Scenario: Command
- **WHEN** an op runs the command from a Warehouse Controller to a Crafting Station on one data network with 8 planks
- **THEN** chat shows the delivery id and the planks arrive at the station

# foundation/data-packets Specification

## Purpose
Packets move items over the data line for a delivery that a mod or an op starts, at a size and speed set by the datapack, so production on demand reaches distant machines without a pipe.

## Requirements

### Requirement: Deliveries split into packets
A delivery SHALL move items to a target member of a data network without travel time: each tick it moves as many items as the source can give, the target can take and the gates of source and target allow. A delivery SHALL keep going until its amount has arrived, its source runs out, or it fails.

#### Scenario: Twenty items
- **WHEN** a delivery of 20 planks starts between two members that are no gates and the target has room
- **THEN** all 20 planks arrive in the same tick and the delivery is done

### Requirement: Delivery into a member
Items SHALL be inserted into the target's item handler on the face that touches the data line, or into its stock sink when it has no item handler there. A move SHALL take items from its source only as far as the target accepts them, so items that do not fit stay at the source; a delivery of a stack the caller handed over SHALL keep what does not fit and offer it again every `retry_interval` ticks (example 20). The delivery SHALL count only items that were accepted.

#### Scenario: Station accepts planks
- **WHEN** a move of 8 planks reaches a Crafting Station with a job that uses planks
- **THEN** the planks are in the station's inputs

#### Scenario: Target full
- **WHEN** a move of 8 planks goes from a crate to a target with room for 3
- **THEN** 3 are inserted, 5 stay in the crate, and they follow once room frees up

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

### Requirement: Test command
An op SHALL be able to start a move with `/servus_foundation data move <from> <to> <item> <count>`; the command SHALL report the delivery id, or why it failed.

#### Scenario: Command
- **WHEN** an op runs the command from a Warehouse Controller to a Crafting Station on one data network with 8 planks
- **THEN** chat shows the delivery id and the planks arrive at the station

### Requirement: Transfer gates
A data member SHALL be able to act as a transfer gate with a rate in items per second for items entering it and a separate rate for items leaving it. A delivery whose source or target is a gate SHALL move no more items per tick than the gate allows in that direction; deliveries through the same gate in the same direction SHALL share its rate in creation order. Members that are no gate SHALL not limit a delivery.

#### Scenario: Gate limits a move
- **WHEN** a move of 64 planks leaves a gate member whose outgoing rate is 8 per second
- **THEN** about 8 planks reach the target each second

#### Scenario: No gate
- **WHEN** a move of 64 sticks goes from a Crafting Station to a Storage Terminal
- **THEN** all 64 arrive in the same tick, as far as the terminal has room

### Requirement: Broken routes leave items at the source
When no route connects source and target any more, a running delivery SHALL fail with the reason route broken. A move SHALL leave the items it has not moved at its source. A delivery of a stack the caller handed over SHALL return what is left to the source, else insert it into the target, else drop it as an item entity at the first loaded position among source and target; items that cannot be placed anywhere yet SHALL be kept and dropped once such a position loads. Items SHALL never be voided. Nothing SHALL be in flight when the server stops.

#### Scenario: Duct broken during a move
- **WHEN** the only duct between a crate and a station is broken while a move of 64 planks has moved 24
- **THEN** 24 planks are in the station, 40 stay in the crate and the delivery fails with the reason route broken

### Requirement: Retry interval from data
`retry_interval` SHALL be read from datapack JSON (`data/<ns>/data/packets.json`) on load and reload, with a default when the file is missing. The file SHALL no longer have a packet size or a travel speed.

#### Scenario: Retry
- **WHEN** a datapack sets `retry_interval` to 40 and the server reloads
- **THEN** a handed over stack that did not fit is offered again every 40 ticks

### Requirement: Bandwidth along the path
A delivery SHALL move per tick no more than its gates allow and no more than the remaining budget of every duct on its path in its direction. Deliveries that cross the same duct in the same tick SHALL share that duct's budget in creation order. A delivery SHALL take the path with the most bandwidth among the shortest paths when several exist.

#### Scenario: Slowest duct decides
- **WHEN** a delivery leaves a Packing Station of rate 64 over two Fast Data Cable ducts of 128 and one Data Cable duct of 32
- **THEN** it moves about 32 items per second

#### Scenario: Shared duct
- **WHEN** two deliveries in the same direction cross the same duplex duct of bandwidth 32 and nothing else limits them
- **THEN** together they move about 32 items per second through it

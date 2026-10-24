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
Items SHALL be inserted on the face of the target that touches the data line: into its packet inlet when it has one there, else into its item handler (foreign machines, the Machine Adapter, Alchemy machines), else into its stock sink. A move SHALL take items from its source only as far as the target accepts them, so items that do not fit stay at the source; a delivery of a stack the caller handed over SHALL keep what does not fit and offer it again every `retry_interval` ticks (example 20). The delivery SHALL count only items that were accepted.

#### Scenario: Station accepts planks
- **WHEN** a move of 8 planks reaches a Crafting Station with a job that uses planks
- **THEN** the planks are in the station's inputs

#### Scenario: Target full
- **WHEN** a move of 8 planks goes from a Crafting Station's outputs to a target with room for 3
- **THEN** 3 are inserted, 5 stay in the Crafting Station, and they follow once room frees up

#### Scenario: Foreign machine through the adapter
- **WHEN** a move of 8 cobblestone reaches a Machine Adapter on a vanilla furnace
- **THEN** the cobblestone goes into the furnace through the adapter's item handler

### Requirement: Taking items from a member
A move SHALL take up to the requested amount of an item from a source member, packet by packet when each packet leaves, on the face that touches the data duct: through the source's stock provider, else its packet inlet, else its item handler. When the source runs out, the move SHALL end as done with fewer items and report how many it delivered.

#### Scenario: From a producer
- **WHEN** a move of 16 sticks starts from a Crafting Station holding 16 sticks in its outputs to a Storage Terminal with an open request for sticks
- **THEN** the station holds no sticks and the terminal's output slots hold 16 once the move is done

#### Scenario: Source short
- **WHEN** a move of 16 sticks starts from a station holding 4 sticks
- **THEN** the move ends as done after 4 sticks arrived and reports 4

#### Scenario: From the warehouse
- **WHEN** a move of 16 planks starts from a Warehouse Controller whose racks hold 32 planks
- **THEN** the move ends as done with 0 planks, because a warehouse ships only by shuttle through a Packing Station

### Requirement: Delivery state
Every delivery SHALL have an id and a state: running, done or failed with a reason. Starting a delivery between positions that no route connects SHALL fail at once with the reason no route and SHALL take nothing from the source. Starting a delivery to a target whose endpoint refuses SHALL fail at once with the reason refused and the endpoint's reason, and SHALL take nothing from the source. Callers SHALL be able to read the state by id and be told when it ends.

#### Scenario: No route
- **WHEN** a mod asks to move items to a member of a different data network
- **THEN** the delivery fails with no route and the source keeps its items

#### Scenario: Refused with a reason
- **WHEN** a mod asks to move logs to a Packing Station whose warehouse has no rack that takes logs
- **THEN** the delivery fails as refused, the reason says that no storage takes them, and the source keeps its logs

### Requirement: Test command
An op SHALL be able to start a move with `/servus_foundation data move <from> <to> <item> <count>`; the command SHALL report the delivery id, or why it failed.

#### Scenario: Command
- **WHEN** an op runs the command from a Crafting Station holding 8 sticks in its outputs to a Storage Terminal on one data line with 8 sticks
- **THEN** chat shows the delivery id and the sticks arrive at the terminal

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
- **WHEN** the only duct between a Machine Adapter on a furnace holding 64 stone in its output and a Packing Station is broken while a move of 64 stone has moved 24
- **THEN** 24 stone are in the Packing Station, 40 stay in the furnace and the delivery fails with the reason route broken

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

### Requirement: Packet inlet
Foundation SHALL offer a packet inlet: a block capability, separate from NeoForge's item capability, through which a block lets data line packets in and, where it wants, out. Item pipes, hoppers and every other user of the item capability SHALL NOT see a packet inlet, so a block with only a packet inlet shows no item arm on a duct and takes nothing from a hopper. Having a packet inlet SHALL NOT make a block a member of a data network; membership stays with the member capabilities.

#### Scenario: Packets reach a block without pipe access
- **WHEN** a move of 8 planks goes to a Crafting Station with a sticks job that has a packet inlet and no item capability
- **THEN** the planks are in the station's inputs

#### Scenario: Pipe sees nothing
- **WHEN** a duct with the Item Pipe is placed beside a block that has only a packet inlet
- **THEN** the duct shows no item arm toward it and routes no items into it

#### Scenario: Inlet alone is no member
- **WHEN** a block that has a packet inlet but none of the member capabilities touches a data duct
- **THEN** the duct shows no data arm toward it and the network has no member there

### Requirement: Packet endpoint handshake
Foundation SHALL offer a packet endpoint, a block capability through which a target answers an offer of an item and a total before a delivery starts: with a grant of an amount and a ticket, room reserved for it, or with a refusal and a reason. A delivery to a target with an endpoint SHALL offer first, send at most the granted amount, and insert only as its grant. A refused delivery SHALL fail at once and take nothing from its source.

#### Scenario: Granted in part
- **WHEN** a move of 200 cobblestone goes to a target whose endpoint grants 128
- **THEN** the delivery moves 128 cobblestone, ends as done with 128 and the other 72 stay at the source

#### Scenario: Refused
- **WHEN** a move of 64 logs goes to a target whose endpoint refuses because it has no room
- **THEN** the delivery fails as refused with that reason and the source keeps all 64 logs

#### Scenario: No endpoint
- **WHEN** a move of 8 planks goes to a Crafting Station, which has no endpoint
- **THEN** the planks are delivered as before, as far as the station accepts them

#### Scenario: Reserved room is the grant's
- **WHEN** a grant reserved one slot of a target for 64 cobblestone and a second delivery of cobblestone reaches the same target
- **THEN** the second delivery cannot put anything into that slot

### Requirement: Grants end with their delivery
A grant SHALL end when its delivery ends, done, failed or settled, and its endpoint SHALL be told how many items arrived for it. An endpoint SHALL also end grants whose delivery no longer runs, checked at least once per second. A sender that already holds a grant from an endpoint SHALL be able to start a delivery with it instead of offering again.

#### Scenario: Delivery ends short
- **WHEN** a delivery granted 64 logs ends after 40 because its source was broken
- **THEN** the endpoint learns that 40 arrived and frees the room left for the other 24

#### Scenario: Lost delivery
- **WHEN** a grant's delivery disappears without ending, for example when its network is gone
- **THEN** the endpoint ends the grant within a second

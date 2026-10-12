## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Delivery into a member
Items SHALL be inserted on the face of the target that touches the data line: into its packet inlet when it has one there, else into its item handler (foreign machines, the Machine Adapter, Manufacturing machines), else into its stock sink. A move SHALL take items from its source only as far as the target accepts them, so items that do not fit stay at the source; a delivery of a stack the caller handed over SHALL keep what does not fit and offer it again every `retry_interval` ticks (example 20). The delivery SHALL count only items that were accepted.

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

### Requirement: Test command
An op SHALL be able to start a move with `/servus_foundation data move <from> <to> <item> <count>`; the command SHALL report the delivery id, or why it failed.

#### Scenario: Command
- **WHEN** an op runs the command from a Crafting Station holding 8 sticks in its outputs to a Storage Terminal on one data line with 8 sticks
- **THEN** chat shows the delivery id and the sticks arrive at the terminal

### Requirement: Broken routes leave items at the source
When no route connects source and target any more, a running delivery SHALL fail with the reason route broken. A move SHALL leave the items it has not moved at its source. A delivery of a stack the caller handed over SHALL return what is left to the source, else insert it into the target, else drop it as an item entity at the first loaded position among source and target; items that cannot be placed anywhere yet SHALL be kept and dropped once such a position loads. Items SHALL never be voided. Nothing SHALL be in flight when the server stops.

#### Scenario: Duct broken during a move
- **WHEN** the only duct between a Machine Adapter on a furnace holding 64 stone in its output and a Packing Station is broken while a move of 64 stone has moved 24
- **THEN** 24 stone are in the Packing Station, 40 stay in the furnace and the delivery fails with the reason route broken

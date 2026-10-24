## MODIFIED Requirements

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Each open request SHALL own as many output slots as its amount needs at the item's stack size, as far as they are free and at least one, from the moment it is made until it ends; deliveries for a request SHALL land only in its slots as packets, and the terminal SHALL grant a delivery only the room left in them. The player SHALL take them out by hand or shift click at any time. The output slots SHALL NOT be indexed as warehouse stock. Hoppers, item pipes and other automation SHALL neither insert into nor extract from them. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** packets have delivered 64 cobblestone into a request's output slot
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

#### Scenario: No extraction
- **WHEN** a hopper sits below a terminal holding cobblestone in its output slots
- **THEN** the hopper takes nothing

#### Scenario: More than a stack refills the slot
- **WHEN** a request for 94 iron ingots owns one slot because only one was free, and that slot holds 64 the player has not taken
- **THEN** the other 30 wait in the warehouse, the slot shows waiting for room, and they land in the same slot once the player takes the 64

#### Scenario: Two stacks in two slots
- **WHEN** the player requests 128 copper and the Output row has free slots
- **THEN** the request owns two slots and all 128 copper arrive without the player taking anything out

#### Scenario: Slot taken until emptied
- **WHEN** a request for 16 cobblestone is done and the 16 still lie in its slot
- **THEN** a new request for bread gets another slot, and a new request for cobblestone may use that slot

### Requirement: Output row full
A new request, transfer, craft request or order grid start SHALL get output slots that are free: no open request owns them and they hold nothing, or they hold the same item with room left. It SHALL get as many as its amount needs, as far as they are free, and at least one. A slot that still holds items of a request that ended SHALL stay taken until it is emptied. When no slot is free the terminal SHALL refuse with "Output row full, take your items"; adding to an open request does not need a new slot.

#### Scenario: Ten requests
- **WHEN** nine requests for different items are open and the player clicks a tenth item
- **THEN** no request is made and the message line says "Output row full, take your items"

#### Scenario: Room after taking
- **WHEN** every slot is taken, one holds the 16 cobblestone of a finished request and the player takes them out
- **THEN** the next click on an item makes a request in that slot

#### Scenario: Adding needs no slot
- **WHEN** every slot is taken, one by an open request for cobblestone, and the player clicks cobblestone
- **THEN** that request grows by one and nothing is refused

#### Scenario: Fewer free slots than stacks
- **WHEN** two slots are free and the player requests 256 cobblestone
- **THEN** the request owns those two slots and the rest arrives as the player empties them

### Requirement: Request slots survive a reload
The output slots a request owns SHALL be saved with the request and SHALL be the same after the world is reloaded or the terminal's chunk loads again.

#### Scenario: Restart with two requests
- **WHEN** a request for bread owns slot 1 and a request for iron owns slots 4 and 5 and the server restarts
- **THEN** after the restart bread still shows on slot 1 and iron on slots 4 and 5, and their deliveries land there

### Requirement: Slot choice is pure
Choosing the output slots for a new request (same item with room first, then the leftmost empty slots that no request owns, as many as the amount needs at the stack size, at least one, else none) SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Same item first
- **WHEN** slot 0 is empty and free and slot 3 holds 16 cobblestone of an ended request
- **THEN** a new cobblestone request gets slot 3

#### Scenario: Owned slots are skipped
- **WHEN** slot 0 is owned by an open request and holds nothing, and slot 1 is empty and free
- **THEN** a new request gets slot 1

#### Scenario: Several slots
- **WHEN** slots 0 to 8 are empty and free and a request is for 130 cobblestone
- **THEN** it gets slots 0, 1 and 2

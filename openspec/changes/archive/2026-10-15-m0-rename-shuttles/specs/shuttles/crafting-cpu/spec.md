# Spec Delta

## MODIFIED Requirements

### Requirement: Order slots
A formed CPU SHALL have `orders_per_cpu_block` order slots (example 1, from `data/<ns>/shuttles/cpu.json`) for each of its CPU blocks; Buffer blocks add none. It SHALL run at most that many open orders at a time, the oldest first. Further orders on that CPU SHALL wait and show "waiting for CPU" until a slot is free. Right clicking a formed CPU or Buffer block with an empty hand or with any item except the Configuration Carrot, a Crafting CPU block and a CPU Buffer block SHALL show in the action bar how many slots are in use, how many orders wait and how many buffer stacks are in use of how many; a sneaking player holding an item uses that item instead. Right clicking any CPU or Buffer block, formed or not, while holding a Crafting CPU block or a CPU Buffer block SHALL place that block against it without sneaking.

#### Scenario: Slots from CPU blocks
- **WHEN** a 2x2x2 CPU is formed from 8 CPU blocks and `orders_per_cpu_block` is 1
- **THEN** the CPU has 8 order slots

#### Scenario: Third order waits
- **WHEN** one CPU with 2 slots holds three open orders
- **THEN** the two oldest run or are blocked and the third waits for the CPU

#### Scenario: Summary
- **WHEN** a player right clicks a CPU of 2 CPU blocks and 6 Buffer blocks with an empty hand while it holds three open orders and 3 stacks in its buffer
- **THEN** the action bar says 2 of 2 slots in use, 1 waiting and 3 of 6 buffer stacks

#### Scenario: Summary while holding a block
- **WHEN** a player holding cobblestone right clicks a formed CPU block without sneaking
- **THEN** the action bar shows the slot summary and no cobblestone is placed

#### Scenario: CPU blocks build on each other
- **WHEN** a player holding a Crafting CPU block right clicks an unformed CPU block without sneaking
- **THEN** the new CPU block is placed against the clicked face and no message about forming is shown

#### Scenario: Buffer blocks build on CPU blocks
- **WHEN** a player holding a CPU Buffer block right clicks an unformed CPU block without sneaking
- **THEN** the Buffer block is placed against the clicked face

### Requirement: Intermediates go straight to the next step
When the CPU collects outputs of a step that a later step of the same order still needs, and that later step is delivering on another producer, it SHALL send them as packets straight from the step's producer to the later step's producer, up to what the later step still needs, counted as fed inputs of the later step and as collected outputs of the step. Outputs beyond that SHALL go to the CPU's buffer while a later step still needs them, else to a Packing Station. Shuttles SHALL ship the later step only the inputs it still needs after that.

#### Scenario: Planks to sticks on two stations
- **WHEN** an order for 8 sticks has a planks step on one Crafting Station and a sticks step on a second station
- **THEN** the planks go from the first station straight to the second, the warehouse never holds planks, and 8 sticks reach the terminal

#### Scenario: Smelt then craft
- **WHEN** an order needs stone smelted in a furnace with a Machine Adapter and then crafted on a Crafting Station
- **THEN** the stone goes from the furnace straight to the station and never enters the warehouse

#### Scenario: Surplus still stored
- **WHEN** a step makes 4 planks and the only later step needs 2
- **THEN** 2 planks go to the later step and 2 to a Packing Station

### Requirement: Waiting for the next step
When a later step of the same order still needs a step's outputs but is not delivering yet, because it runs on the same producer or its producer is busy with another order, the outputs SHALL go as packets from the producer into the CPU's buffer, up to what the later steps still need and as far as the buffer has room, counted as collected outputs of the step. Outputs that do not fit SHALL wait at their producer for up to `handoff_wait_ticks` (example 100, from `data/<ns>/shuttles/cpu.json`) and then go to a Packing Station; on the same producer they SHALL go to a Packing Station without waiting. The order SHALL remember what went to storage this way.

#### Scenario: Same producer
- **WHEN** a chain from oak logs to a crafting table runs both steps on one Crafting Station and the CPU has buffer room
- **THEN** the 4 planks go into the CPU buffer, the station is free for the next step, the warehouse never holds planks and the order completes

#### Scenario: Next producer stays busy
- **WHEN** the only producer for the later step is busy with another order for longer than the wait and the CPU has buffer room
- **THEN** the outputs wait in the CPU buffer, not in storage, and the order completes once the producer is free

#### Scenario: Buffer full, busy producer
- **WHEN** the CPU's buffer has no room and the only producer for the later step is busy with another order for longer than the wait
- **THEN** the outputs go to storage, the order notes it and still completes once the producer is free

#### Scenario: Buffer full, same producer
- **WHEN** the CPU's buffer has no room and a chain runs both steps on one Crafting Station
- **THEN** the intermediate goes to storage right away and the order completes

#### Scenario: Partly full
- **WHEN** the buffer has room for one stack of 64 planks and a step makes 80 planks that a later step on the same producer needs
- **THEN** 64 planks go into the buffer, 16 go to storage and the order completes

### Requirement: Forming says why not
When the carrot does not form a CPU, the action bar SHALL say why, checked in this order: too big when more than `max_blocks` blocks are joined (example 512, from `data/<ns>/shuttles/cpu.json`); too small when a side of the smallest box around the joined blocks is shorter than 2; not a box when a corner of that box is not one of them; a gap with its position, the lowest first, when another place of that box is not one of them; no CPU block when all of them are Buffer blocks. Right clicking an unformed block with an empty hand SHALL say how to form a CPU.

#### Scenario: Gap
- **WHEN** a 3x3x3 box of CPU blocks misses its middle block and the player uses the carrot
- **THEN** nothing forms and the action bar says there is a gap at the middle block's position

#### Scenario: Not a box
- **WHEN** CPU blocks form an L shape and the player uses the carrot on one of them
- **THEN** nothing forms and the action bar says the blocks are not a box

#### Scenario: Too small
- **WHEN** a player uses the carrot on a flat 2x2x1 layer of CPU blocks
- **THEN** nothing forms and the action bar says every side needs at least 2 blocks

#### Scenario: Buffer only
- **WHEN** a player uses the carrot on a 2x2x2 box of Buffer blocks
- **THEN** nothing forms and the action bar says a CPU needs at least one CPU block

#### Scenario: Casing hint
- **WHEN** a player right clicks an unformed CPU block with an empty hand
- **THEN** the action bar says to build a solid box of at least 2x2x2 and use the Configuration Carrot on it

### Requirement: CPU buffer room
A formed CPU SHALL have buffer room of `stacks_per_buffer_block` stacks (example 1, from `data/<ns>/shuttles/cpu.json`) for each of its Buffer blocks; CPU blocks add none. A stack SHALL be one item type of one order up to that item's maximum stack size. All orders of the CPU SHALL share the room, first come first served, and items on their way into the buffer SHALL hold their room until they land.

#### Scenario: One stack of planks
- **WHEN** an order puts 64 Oak Planks into the buffer of a CPU with 1 stack of room
- **THEN** the buffer is full

#### Scenario: Stack size
- **WHEN** an order puts 16 Ender Pearls into a buffer with 2 stacks of room
- **THEN** 1 of 2 stacks is in use

#### Scenario: Orders do not share a stack
- **WHEN** two orders each put 4 Oak Planks into the same buffer
- **THEN** 2 stacks are in use

#### Scenario: No Buffer blocks
- **WHEN** a CPU is formed from CPU blocks only
- **THEN** its buffer has no room and intermediates go to storage as before

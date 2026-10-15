# shuttles/crafting-cpu Specification

## Purpose
The Crafting CPU is the owner of work orders on the data line: a 3x3x3 multiblock that plans orders over the producers on its line, runs their steps with packets and adds order slots, so more CPUs run more orders at once.

## Requirements

### Requirement: CPU blocks form a box
The Crafting CPU block and the CPU Buffer block SHALL be craftable and SHALL drop themselves when broken. Using the Configuration Carrot on an unformed CPU or Buffer block SHALL form a CPU from all unformed CPU and Buffer blocks joined to it face to face when they fill a solid box with every side at least 2 blocks long and at least one CPU block; otherwise nothing SHALL form. Formed blocks SHALL look different from unformed ones. Sneaking with the carrot SHALL dismantle one block.

#### Scenario: Smallest CPU
- **WHEN** a player builds 8 CPU blocks into a 2x2x2 box and uses the carrot on any of them
- **THEN** all 8 blocks show as formed

#### Scenario: Box with Buffer blocks
- **WHEN** a player builds a 3x2x2 box of 4 CPU blocks and 8 Buffer blocks in any arrangement and uses the carrot on a Buffer block
- **THEN** all 12 blocks show as formed and the CPU has the slots of 4 CPU blocks and the room of 8 Buffer blocks

#### Scenario: Form
- **WHEN** a player builds 27 CPU blocks into a 3x3x3 cube and uses the carrot on any of them
- **THEN** all 27 blocks show as formed

#### Scenario: Incomplete
- **WHEN** one block of a 3x3x3 cube is missing in the middle of a face and the player uses the carrot
- **THEN** nothing forms and the action bar says why

### Requirement: Breaking unforms and cancels
Removing any block of a formed CPU SHALL turn its other blocks back into unformed blocks and SHALL cancel the CPU's open orders: their reservations are released and items already delivered stay where they are. Items waiting in the CPU's buffer SHALL be sent as packets to a Packing Station of the CPU's data line in the same tick, before the blocks unform, and that station's warehouse SHALL put them away by shuttle like any packets it takes in; items the Packing Station cannot take SHALL drop as items beside it. When no Packing Station reaches the CPU's line, the buffered items SHALL drop as items at the removed block. When the CPU's chunk unloads its orders and its buffer SHALL pause instead and go on when it loads again. A master that loads unformed SHALL cancel the orders it saved and drop the buffered items it saved at its position.

#### Scenario: Break one block
- **WHEN** a formed CPU runs an order and a player breaks one of its blocks
- **THEN** the other blocks show as unformed and the order is cancelled with its reservations released

#### Scenario: Buffer goes to storage
- **WHEN** a formed CPU holds 4 Oak Planks in its buffer, a Packing Station of a warehouse with a rack that takes planks and a garage with a shuttle is on its data line and a player breaks a Buffer block
- **THEN** the 4 Oak Planks land in the Packing Station, a shuttle puts them into the rack and none are lost

#### Scenario: Packing Station full
- **WHEN** a formed CPU holds 4 Oak Planks in its buffer, the only Packing Station on its data line has a full To storage buffer and a player breaks one of its blocks
- **THEN** the 4 Oak Planks drop as items beside the Packing Station and none are lost

#### Scenario: No Packing Station
- **WHEN** a formed CPU holds 4 Oak Planks in its buffer, no Packing Station is on its data line and a player breaks one of its blocks
- **THEN** the 4 Oak Planks drop as items at the broken block

#### Scenario: Chunk unloads
- **WHEN** a formed CPU runs an order and its chunk unloads and loads again
- **THEN** the order is still open and finishes

### Requirement: CPU joins the data line
A formed CPU block or Buffer block touching a data line SHALL be a member of that data line with the link role. Unformed blocks SHALL NOT be members. A CPU is on a data line when any of its blocks is.

#### Scenario: Formed CPU on the line
- **WHEN** a data duct touches one block of a formed CPU
- **THEN** the duct shows an arm toward it and the data network lists it as a link member

#### Scenario: Buffer block on the line
- **WHEN** a data duct touches only a Buffer block of a formed CPU
- **THEN** the duct shows an arm toward it and the CPU runs orders of that line

#### Scenario: Unformed block
- **WHEN** a data duct touches an unformed CPU or Buffer block
- **THEN** the duct shows no arm toward it

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

### Requirement: Choice of CPU
A new order SHALL go to the CPU of the data line that has a free slot, the one with the lowest position first. When no CPU has a free slot, it SHALL go to the CPU with the fewest open orders, the lowest position on a tie. Orders SHALL NOT move between CPUs afterwards.

#### Scenario: Two CPUs share the work
- **WHEN** two CPUs with 2 slots each are on one data line and four orders are made
- **THEN** each CPU holds two orders and no order waits

### Requirement: Steps run over the data line
The producers of a CPU SHALL be the members of its data line with the producer role. A step SHALL run on the producer whose reported recipe it uses, or, for a step planned from a pattern, on a producer of the line that can execute that pattern; when it plans, a CPU SHALL prefer producers no step occupies. Before the inputs of a pattern step on a Crafting Station are shipped, the CPU SHALL hand the station the step's pattern and batches as its job; a station that refuses keeps the step waiting for the station. Its inputs SHALL be shipped by the warehouses of the line through a Packing Station as packets into the producer, the warehouse holding the most of an input first. While the step delivers or produces, outputs of its recipe in the producer SHALL be moved as packets to a Packing Station of the line, where they are put away; what arrives there SHALL count for the step. A step whose shipment ends short SHALL ship the rest again. When a step is done or its order is cancelled, the CPU SHALL end the station's job. Each producer SHALL run one step at a time over all CPUs of the line, orders in creation order.

#### Scenario: Sticks over the line
- **WHEN** a CPU runs a sticks step for 2 batches on a Crafting Station on the line, and the warehouse has planks and a Packing Station on the line
- **THEN** the station gets a sticks job for 2 crafts, 4 planks arrive in the station as packets, it crafts twice, the 8 sticks travel back to the Packing Station and into storage, the step is done and the station is idle again

#### Scenario: Producer busy
- **WHEN** two CPUs each have an order that needs the same Crafting Station
- **THEN** the older order's step runs first and the other waits for the station

#### Scenario: Two stations in parallel
- **WHEN** a line has two idle Crafting Stations and a sticks pattern, and two orders for sticks are made one after the other
- **THEN** each order's step runs on a different station at the same time

#### Scenario: Step from a pattern
- **WHEN** a CPU plans sticks from a sticks pattern of the line and a Crafting Station can execute it
- **THEN** the step runs on that station and counts the sticks that come back like any step

### Requirement: Diagnosis commands
`/servus_warehouse orders <pos>` SHALL list the orders of every CPU on the data line found from the position (a data member, a CPU block, or a warehouse block whose Packing Station is on a line) with item, amount, state, reason and steps, and for every CPU its size, its slots and what its buffer holds per order. `/servus_warehouse order <pos> <item> <count>` SHALL make an order on that line without a request. `/servus_warehouse producers <pos>` SHALL list the producers of that line with their recipes.

#### Scenario: Listing
- **WHEN** an op runs the orders command on a controller whose Packing Station is on a line with a CPU running a sticks order
- **THEN** chat lists the CPU, the order with its state and the sticks step on the Crafting Station

#### Scenario: Buffer listing
- **WHEN** an op runs the orders command while a CPU holds 4 Oak Planks for a crafting table order
- **THEN** chat lists the CPU's buffer with 4 Oak Planks for that order

### Requirement: Orders survive reloads
A CPU SHALL save its work orders with its master block: item, amount, the request or rule that made it, state and reason, reservations, and every step with its producer, recipe, batches, state, the inputs fed and the outputs collected. It SHALL save its buffer with the master too: the items waiting for each order and the items on their way into or out of the buffer. Order ids SHALL not repeat on that CPU after a reload. Shipments of inputs and outputs travelling back that were under way, and transfers into or out of the buffer, SHALL be booked by their saved ids once they end; an input shipment that ended short SHALL be shipped again as before.

#### Scenario: Restart mid step
- **WHEN** an order for 8 sticks has delivered the planks to a station and the server restarts before the sticks are collected
- **THEN** after the restart the order is still open, the sticks are collected and the order finishes

#### Scenario: Restart with a full buffer
- **WHEN** a crafting table order on one Crafting Station holds 4 Oak Planks in the CPU buffer and the server restarts
- **THEN** after the restart the buffer still holds the 4 Oak Planks for that order, they go to the station and the crafting table is made

### Requirement: Formed box look
A formed CPU SHALL show each side of its box as one face: a continuous iron frame along the outer edges and corners of the box and lights on the inner tiles, teal data lights on CPU blocks and amber storage lights on Buffer blocks. Every CPU and Buffer block SHALL know its CPU and its place in the box while formed and forget them when unformed; unformed blocks SHALL show a riveted casing, the Buffer block its own.

#### Scenario: Corner block
- **WHEN** a box forms
- **THEN** the block at a corner of the box shows frame on its outer edges and a block in the middle of a side shows only lights

#### Scenario: Thin side
- **WHEN** a 2x2x2 box forms
- **THEN** every block shows frame on its outer edges and no face shows a tile without frame

#### Scenario: Unform
- **WHEN** a block of a formed box is broken
- **THEN** the remaining blocks show their casing again

### Requirement: Active lights
The lights of a formed CPU SHALL run while the CPU has orders and SHALL glow slowly while it has none. The change SHALL follow order changes, not a per tick check.

#### Scenario: Order starts
- **WHEN** a formed CPU without orders takes an order
- **THEN** its blocks switch to running lights, and back to the slow glow when the order is done

### Requirement: Lights keep off the frame
On every face of a formed CPU the lights SHALL sit inside the dark glass with at least one pixel of glass between a light and the edge of the glass, so no light touches the wooden or iron frame on edge and corner tiles.

#### Scenario: Corner tile
- **WHEN** a formed CPU is looked at on a corner of one of its faces
- **THEN** the lights nearest the corner are surrounded by glass and do not touch the frame

### Requirement: Results go straight to the requesting terminal
A work order made for a Storage Terminal request SHALL remember that terminal. When the CPU collects the outputs of a step that makes the order's item, it SHALL send up to the amount the request still needs (and at most the order's amount) directly from the producer to the terminal as packets; the rest SHALL go to a Packing Station and be put away. Every output of a step that makes another item SHALL go to a later step or the CPU's buffer as the requirements on intermediates say, and otherwise to a Packing Station. Outputs SHALL count for the step wherever they land. When the terminal cannot be reached from the producer's data line the outputs SHALL go to the Packing Station. The link and the amount already sent SHALL be saved with the order.

#### Scenario: Sticks for a request
- **WHEN** a request for 8 sticks makes an order and the station crafts 8 sticks
- **THEN** the sticks travel from the station straight into the terminal's output slots and never enter the warehouse

#### Scenario: Batch surplus
- **WHEN** a request for 2 sticks makes an order whose one craft gives 4 sticks
- **THEN** 2 sticks go to the terminal and 2 are put away

#### Scenario: Chain
- **WHEN** a request for sticks makes an order that first crafts planks from logs on the only Crafting Station of the line
- **THEN** the planks wait in the CPU buffer, never enter the warehouse, and only the sticks go to the terminal

#### Scenario: Stock rule
- **WHEN** a stock rule makes an order
- **THEN** its outputs are put away as before

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

### Requirement: Waiting steps move to a free producer
A step waiting for its producer because another step occupies it SHALL move to another producer of the CPU's data line that offers the same recipe with the same inputs and is free, and start there.

#### Scenario: Second station takes over
- **WHEN** two Crafting Stations are on the line and an order's second step was planned on the station its first step runs on
- **THEN** the second step starts on the other station

### Requirement: No missing report for inputs still being made
A delivering step SHALL NOT report an input as missing while an earlier step of the same order that makes that input is not done.

#### Scenario: Waiting for planks
- **WHEN** the sticks step delivers while the planks step still crafts
- **THEN** the order is not blocked with planks missing

### Requirement: Handoffs survive reloads
A handoff SHALL be booked in the production ledger like other transfers: the step counts what landed as collected, the later step counts it as fed, and a handoff the ledger forgot counts as landed for the step and as nothing for the later step, which is then fed again from storage. Nothing SHALL be lost or counted twice across a reload.

#### Scenario: Reload mid handoff
- **WHEN** the CPU and both producers are reloaded while planks travel from one station to the other
- **THEN** the order completes with 8 sticks at the terminal and no planks lost

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

### Requirement: Touching boxes stay apart
Only unformed blocks SHALL join a box when the carrot is used; blocks of a formed CPU SHALL never join another box. Two formed CPUs that touch SHALL stay two CPUs with their own slots, buffer and orders.

#### Scenario: Second box beside a formed CPU
- **WHEN** a 2x2x2 CPU is formed and a second 2x2x2 box of unformed CPU blocks is built against it and the player uses the carrot on the second box
- **THEN** the second box forms as its own CPU and both CPUs show their own slots

#### Scenario: Two unformed boxes side by side
- **WHEN** two unformed 2x2x2 boxes touch along a full face and the player uses the carrot on one of them
- **THEN** both form together as one 4x2x2 CPU

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

### Requirement: Buffered intermediates feed the later step
When a step delivers, inputs of its order that wait in the CPU's buffer SHALL be sent as packets from the CPU to the step's producer first, up to what the step still needs, and count as fed; the warehouses SHALL ship only the rest. Buffered items SHALL belong to their order: they are not stock of the line, are not shown in the Storage Terminal and cannot be reserved or used by another order.

#### Scenario: Planks back to the station
- **WHEN** 4 Oak Planks of a crafting table order wait in the buffer and the crafting table step starts on the Crafting Station
- **THEN** the 4 planks go from the CPU into the station, no planks are shipped from storage, and the crafting table is made

#### Scenario: Not for others
- **WHEN** 4 Oak Planks of one order wait in the buffer and a second order needs Oak Planks
- **THEN** the second order plans with the warehouse stock only and the 4 buffered planks stay with the first order

### Requirement: Leftovers leave the buffer
Items in the buffer that belong to an order that is done or cancelled SHALL go as packets to a Packing Station of the line and be put away; until they leave they SHALL keep their room. A cancelled order SHALL leave nothing behind in the buffer.

#### Scenario: Cancel with buffered planks
- **WHEN** an order with 4 Oak Planks in the buffer is cancelled
- **THEN** the 4 planks go as packets to the Packing Station, a shuttle puts them into a rack and the buffer room is free again

### Requirement: Steps wait for warehouse power
A step that runs on a Crafting Station SHALL wait while that station cannot pay its work: the order stays on its CPU, keeps its reservations, buffered intermediates and progress, is not cancelled or replanned, and the Production Terminal SHALL show the step as waiting for power. Steps on other producers (Machine Adapters, Alchemy machines) SHALL run on their own fuel and power and SHALL NOT wait for the warehouse's power, but their inputs and outputs still move only as far as shuttles can run. The CPU itself SHALL draw no FE.

#### Scenario: Station without power
- **WHEN** an order for a crafting table runs its planks step on a Crafting Station and the warehouse's controller runs dry
- **THEN** the order stays, the Production Terminal shows the step waiting for power, and once FE arrives the step finishes and the order goes on

#### Scenario: Furnace keeps smelting
- **WHEN** an order smelts iron through a Machine Adapter on a vanilla furnace and the warehouse's controller runs dry after the ore arrived
- **THEN** the furnace keeps smelting the ore it holds

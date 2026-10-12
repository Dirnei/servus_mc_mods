# storage/crafting-cpu Specification

## Purpose
The Crafting CPU is the owner of work orders on the data line: a 3x3x3 multiblock that plans orders over the producers on its line, runs their steps with packets and adds order slots, so more CPUs run more orders at once.

## Requirements

### Requirement: CPU blocks form a cube
The Crafting CPU block SHALL be craftable and SHALL drop itself when broken. Using the Configuration Carrot on an unformed CPU block SHALL form a CPU when that block is part of a full 3x3x3 cube of 27 unformed CPU blocks; otherwise the action bar SHALL say that a full cube is needed. Formed blocks SHALL look different from unformed ones. Sneaking with the carrot SHALL dismantle one block.

#### Scenario: Form
- **WHEN** a player builds 27 CPU blocks into a 3x3x3 cube and uses the carrot on any of them
- **THEN** all 27 blocks show as formed

#### Scenario: Incomplete
- **WHEN** one block of the cube is missing and the player uses the carrot
- **THEN** nothing forms and the action bar says a full 3x3x3 cube is needed

### Requirement: Breaking unforms and cancels
Removing any block of a formed CPU SHALL turn the other 26 back into unformed blocks and SHALL cancel the CPU's open orders: their reservations are released and items already delivered stay where they are. When the CPU's chunk unloads its orders SHALL pause instead and go on when it loads again. A master that loads unformed SHALL cancel the orders it saved.

#### Scenario: Break one block
- **WHEN** a formed CPU runs an order and a player breaks one of its blocks
- **THEN** the other blocks show as unformed and the order is cancelled with its reservations released

#### Scenario: Chunk unloads
- **WHEN** a formed CPU runs an order and its chunk unloads and loads again
- **THEN** the order is still open and finishes

### Requirement: CPU joins the data line
A formed CPU block touching a data line SHALL be a member of that data line with the link role. Unformed blocks SHALL NOT be members. A CPU is on a data line when any of its blocks is.

#### Scenario: Formed CPU on the line
- **WHEN** a data duct touches one block of a formed CPU
- **THEN** the duct shows an arm toward it and the data network lists it as a link member

#### Scenario: Unformed block
- **WHEN** a data duct touches an unformed CPU block
- **THEN** the duct shows no arm toward it

### Requirement: Order slots
A formed CPU SHALL run at most `order_slots` open orders at a time (example 2, from `data/<ns>/storage/cpu.json`), the oldest first. Further orders on that CPU SHALL wait and show "waiting for CPU" until a slot is free. Right clicking a formed CPU block with an empty hand or with any item except the Configuration Carrot and a Crafting CPU block SHALL show in the action bar how many slots are in use and how many orders wait; a sneaking player holding an item uses that item instead. Right clicking any CPU block, formed or not, while holding a Crafting CPU block SHALL place that block against it without sneaking.

#### Scenario: Third order waits
- **WHEN** one CPU with 2 slots holds three open orders
- **THEN** the two oldest run or are blocked and the third waits for the CPU

#### Scenario: Summary
- **WHEN** a player right clicks that CPU with an empty hand
- **THEN** the action bar says 2 of 2 slots in use and 1 waiting

#### Scenario: Summary while holding a block
- **WHEN** a player holding cobblestone right clicks a formed CPU block without sneaking
- **THEN** the action bar shows the slot summary and no cobblestone is placed

#### Scenario: CPU blocks build on each other
- **WHEN** a player holding a Crafting CPU block right clicks an unformed CPU block without sneaking
- **THEN** the new CPU block is placed against the clicked face and no message about forming is shown

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
`/servus_warehouse orders <pos>` SHALL list the orders of every CPU on the data line found from the position (a data member, a CPU block, or a warehouse block whose Packing Station is on a line) with item, amount, state, reason and steps. `/servus_warehouse order <pos> <item> <count>` SHALL make an order on that line without a request. `/servus_warehouse producers <pos>` SHALL list the producers of that line with their recipes.

#### Scenario: Listing
- **WHEN** an op runs the orders command on a controller whose Packing Station is on a line with a CPU running a sticks order
- **THEN** chat lists the CPU, the order with its state and the sticks step on the Crafting Station

### Requirement: Pushed outputs count
Items of a step's recipe output that the step's producer pushes into a Packing Station through the face touching the producer SHALL count for that step while it delivers or produces, up to what the step still needs; when several steps of the Packing Station's data line match, the oldest order's step SHALL be served first. Items entering a Packing Station through a face that does not touch the producer SHALL count for no step.

#### Scenario: Producer beside the Packing Station
- **WHEN** a sticks step runs on a producer directly beside a Packing Station and the producer pushes 4 sticks into it
- **THEN** the 4 sticks are put away, the step counts them and the order is done

#### Scenario: Other face
- **WHEN** 4 sticks enter that Packing Station through a face that touches a hopper instead of the producer
- **THEN** the sticks are put away and the step counts none of them

#### Scenario: No double count
- **WHEN** the CPU moves the step's outputs as packets into a Packing Station through its data duct
- **THEN** they count once, when the packets land

### Requirement: Orders survive reloads
A CPU SHALL save its work orders with its master block: item, amount, the request or rule that made it, state and reason, reservations, and every step with its producer, recipe, batches, state, the inputs fed and the outputs collected. Order ids SHALL not repeat on that CPU after a reload. Shipments of inputs and outputs travelling back that were under way SHALL be booked by their saved ids once they end; an input shipment that ended short SHALL be shipped again as before.

#### Scenario: Restart mid step
- **WHEN** an order for 8 sticks has delivered the planks to a station and the server restarts before the sticks are collected
- **THEN** after the restart the order is still open, the sticks are collected and the order finishes

### Requirement: Formed cube look
A formed CPU SHALL show each side of the cube as one 3x3 face: a continuous iron frame along the outer edges and corners of the cube and teal data lights on the inner tiles. Every CPU block SHALL know its position in the cube while formed and forget it when unformed; unformed blocks SHALL show a riveted casing.

#### Scenario: Corner block
- **WHEN** a cube forms
- **THEN** the block at a corner of the cube shows frame on its outer edges and the block in the middle of a side shows only lights

#### Scenario: Unform
- **WHEN** a block of a formed cube is broken
- **THEN** the remaining blocks show the casing again

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
A work order made for a Storage Terminal request SHALL remember that terminal. When the CPU collects the outputs of a step that makes the order's item, it SHALL send up to the amount the request still needs (and at most the order's amount) directly from the producer to the terminal as packets; the rest, and every output of a step that makes another item, SHALL go to a Packing Station and be put away. Outputs SHALL count for the step wherever they land. When the terminal cannot be reached from the producer's data line the outputs SHALL go to the Packing Station. The link and the amount already sent SHALL be saved with the order.

#### Scenario: Sticks for a request
- **WHEN** a request for 8 sticks makes an order and the station crafts 8 sticks
- **THEN** the sticks travel from the station straight into the terminal's output slots and never enter the warehouse

#### Scenario: Batch surplus
- **WHEN** a request for 2 sticks makes an order whose one craft gives 4 sticks
- **THEN** 2 sticks go to the terminal and 2 are put away

#### Scenario: Chain
- **WHEN** a request for sticks makes an order that first crafts planks from logs
- **THEN** the planks go to storage and only the sticks go to the terminal

#### Scenario: Stock rule
- **WHEN** a stock rule makes an order
- **THEN** its outputs are put away as before

### Requirement: Intermediates go straight to the next step
When the CPU collects outputs of a step that a later step of the same order still needs, and that later step is delivering on another producer, it SHALL send them as packets straight from the step's producer to the later step's producer, up to what the later step still needs, counted as fed inputs of the later step and as collected outputs of the step. Only outputs beyond that go to a Packing Station. Storage SHALL ship the later step only the inputs it still needs after that.

#### Scenario: Planks to sticks on two stations
- **WHEN** an order for 8 sticks has a planks step on one Crafting Station and a sticks step on a second station
- **THEN** the planks go from the first station straight to the second, the warehouse never holds planks, and 8 sticks reach the terminal

#### Scenario: Smelt then craft
- **WHEN** an order needs stone smelted in a furnace with a Machine Adapter and then crafted on a Crafting Station
- **THEN** the stone goes from the furnace straight to the station and never enters the warehouse

#### Scenario: Surplus still stored
- **WHEN** a step makes 4 planks and the later step needs 2
- **THEN** 2 planks go to the later step and 2 to a Packing Station

### Requirement: Waiting for the next step
When a later step of the same order still needs a step's outputs but is not delivering yet because its producer is busy with another order, the outputs SHALL wait at their producer for up to `handoff_wait_ticks` (example 100, from `data/<ns>/storage/cpu.json`) and then go to a Packing Station as before. When the later step runs on the same producer as the step, the outputs SHALL go to a Packing Station without waiting.

#### Scenario: Next producer stays busy
- **WHEN** the only producer for the later step is busy with another order for longer than the wait
- **THEN** the outputs go to storage and the order still completes once the producer is free

#### Scenario: Same producer
- **WHEN** a chain runs both steps on one Crafting Station
- **THEN** the intermediate goes to storage right away and the order completes

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

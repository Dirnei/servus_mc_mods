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
Removing any block of a formed CPU SHALL turn the other 26 back into unformed blocks and SHALL cancel the CPU's open orders: their reservations are released and items already delivered stay where they are. The same SHALL happen when the CPU's chunk unloads.

#### Scenario: Break one block
- **WHEN** a formed CPU runs an order and a player breaks one of its blocks
- **THEN** the other blocks show as unformed and the order is cancelled with its reservations released

### Requirement: CPU joins the data line
A formed CPU block touching a data line SHALL be a member of that data line with the link role. Unformed blocks SHALL NOT be members. A CPU is on a data line when any of its blocks is.

#### Scenario: Formed CPU on the line
- **WHEN** a data duct touches one block of a formed CPU
- **THEN** the duct shows an arm toward it and the data network lists it as a link member

#### Scenario: Unformed block
- **WHEN** a data duct touches an unformed CPU block
- **THEN** the duct shows no arm toward it

### Requirement: Order slots
A formed CPU SHALL run at most `order_slots` open orders at a time (example 2, from `data/<ns>/storage/cpu.json`), the oldest first. Further orders on that CPU SHALL wait and show "waiting for CPU" until a slot is free. Right clicking a formed CPU block with an empty hand SHALL show in the action bar how many slots are in use and how many orders wait.

#### Scenario: Third order waits
- **WHEN** one CPU with 2 slots holds three open orders
- **THEN** the two oldest run or are blocked and the third waits for the CPU

#### Scenario: Summary
- **WHEN** a player right clicks that CPU with an empty hand
- **THEN** the action bar says 2 of 2 slots in use and 1 waiting

### Requirement: Choice of CPU
A new order SHALL go to the CPU of the data line that has a free slot, the one with the lowest position first. When no CPU has a free slot, it SHALL go to the CPU with the fewest open orders, the lowest position on a tie. Orders SHALL NOT move between CPUs afterwards.

#### Scenario: Two CPUs share the work
- **WHEN** two CPUs with 2 slots each are on one data line and four orders are made
- **THEN** each CPU holds two orders and no order waits

### Requirement: Steps run over the data line
The producers of a CPU SHALL be the members of its data line with the producer role. A step SHALL run on the producer whose reported recipe it uses, or, for a step planned from a pattern, on a producer of the line that can execute that pattern; when it plans, a CPU SHALL prefer producers no step occupies. Before the inputs of a pattern step on a Crafting Station are shipped, the CPU SHALL hand the station the step's pattern and batches as its job; a station that refuses keeps the step waiting for the station. Its inputs SHALL be shipped by the warehouses of the line through an Access Point as packets into the producer, the warehouse holding the most of an input first. While the step delivers or produces, outputs of its recipe in the producer SHALL be moved as packets to an Access Point of the line, where they are put away; what arrives there SHALL count for the step. A step whose shipment ends short SHALL ship the rest again. When a step is done or its order is cancelled, the CPU SHALL end the station's job. Each producer SHALL run one step at a time over all CPUs of the line, orders in creation order.

#### Scenario: Sticks over the line
- **WHEN** a CPU runs a sticks step for 2 batches on a Crafting Station on the line, and the warehouse has planks and an Access Point on the line
- **THEN** the station gets a sticks job for 2 crafts, 4 planks arrive in the station as packets, it crafts twice, the 8 sticks travel back to the Access Point and into storage, the step is done and the station is idle again

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
`/servus_warehouse orders <pos>` SHALL list the orders of every CPU on the data line found from the position (a data member, a CPU block, or a warehouse block whose Access Point is on a line) with item, amount, state, reason and steps. `/servus_warehouse order <pos> <item> <count>` SHALL make an order on that line without a request. `/servus_warehouse producers <pos>` SHALL list the producers of that line with their recipes.

#### Scenario: Listing
- **WHEN** an op runs the orders command on a controller whose Access Point is on a line with a CPU running a sticks order
- **THEN** chat lists the CPU, the order with its state and the sticks step on the Crafting Station

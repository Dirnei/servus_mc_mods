# Spec Delta

## ADDED Requirements

### Requirement: Steps run on outbound stations
Each step SHALL run on the Outbound Station whose faced producer offers its recipe. The warehouse SHALL deliver the step's inputs into that station's buffer, one step at a time per station, orders in creation order; the station pushes them into the machine. A step SHALL be producing once its inputs are delivered and done once Inbound Stations have collected the step's outputs: outputs count from an Inbound Station touching the step's machine, or from any Inbound Station when none touches it. The order SHALL be done when every step is done.

#### Scenario: Deliver, produce, collect
- **WHEN** a sticks step for 2 batches starts on a station facing a Crafting Station with an Inbound Station beside it
- **THEN** 4 planks are delivered to the outbound buffer, the Crafting Station crafts twice, and the step is done when 8 sticks were collected by the Inbound Station

#### Scenario: Station busy
- **WHEN** two orders need the same outbound station
- **THEN** the older order's step runs first and the other waits for the station

#### Scenario: Request served
- **WHEN** the order's sticks have been collected into storage
- **THEN** the request that created it receives them within its retry interval

## MODIFIED Requirements

### Requirement: Shortage creates an order
When a terminal request cannot be filled from stock and some Outbound Station of the warehouse faces a producer whose recipes can make the item, directly or through intermediate recipes, the warehouse SHALL create a work order for the missing amount, linked to the request. A request whose item no producer can make SHALL stay a plain request with missing stock. An order SHALL also be created by `/servus_warehouse order <pos> <item> <count>`.

#### Scenario: Sticks on order
- **WHEN** a player requests 8 sticks, none are in stock, planks are, and an Outbound Station faces a Crafting Station with the sticks recipe
- **THEN** a work order for 8 sticks is created and the request shows producing

#### Scenario: Nothing can make it
- **WHEN** a player requests bread and no station's producer has a recipe for bread
- **THEN** no order is created and the request shows the bread as missing

#### Scenario: Manual order
- **WHEN** an op runs the order command for 16 sticks
- **THEN** an order for 16 sticks exists without a request

### Requirement: Planning with reservations
An order SHALL be planned with the Lib order planner over the recipes of every Outbound Station's producer, trying stations that are idle first. Stock the plan uses SHALL be reserved for that order, SHALL NOT count as available to later plans, and SHALL shrink as the stock is delivered to stations. Surplus from a step SHALL serve later steps of the same order. Raw materials the plan cannot get SHALL be reported as the order's blocked reason until they arrive.

#### Scenario: Two levels
- **WHEN** 8 sticks are ordered, only logs are in stock, and a station knows planks from logs and sticks from planks
- **THEN** the order has a planks step and a sticks step, the planks step first

#### Scenario: Reserved stock
- **WHEN** an order reserves 4 planks and a second order for sticks is planned while 4 planks are in stock
- **THEN** the second order finds no planks available and reports them missing

#### Scenario: Missing raw material
- **WHEN** sticks are ordered and no planks and no logs are in stock
- **THEN** the order is blocked with planks missing and starts when planks arrive

### Requirement: States and reasons
An order SHALL be running, blocked or done. Blocked reasons SHALL be: no producer for an item, a missing raw material by name, the station's delivery needing a shuttle no garage provides, or the station out of reach. Each step SHALL show waiting for the station, delivering, producing or done. `/servus_warehouse orders <pos>` SHALL list every order with its item, amount, state, reason and steps.

#### Scenario: Listing
- **WHEN** an op runs the orders command while a sticks order is producing
- **THEN** chat lists the order with its state and the sticks step as producing on its station

### Requirement: Cancellation
Cancelling the request of an order SHALL cancel the order: steps not yet delivered are dropped and their reservations released; items already delivered stay in the station and machine and what they produce is put away as usual. A done order SHALL leave the list after a short while.

#### Scenario: Cancel while waiting
- **WHEN** the player cancels a request whose order waits for planks
- **THEN** the order is gone and the planks reserved for it are available again

## REMOVED Requirements

### Requirement: Steps run on docks
**Reason**: Replaced by "Steps run on outbound stations".
**Migration**: None.

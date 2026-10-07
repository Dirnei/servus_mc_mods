# storage/work-orders Specification

## Purpose

Work orders turn a shortage into production: the warehouse plans what to make from the recipes its docks offer, reserves what it has, feeds the machines and counts the results.

## Requirements

### Requirement: Shortage creates an order
When a terminal request cannot be filled from stock and some dock of the warehouse faces a producer whose recipes can make the item, directly or through intermediate recipes, the warehouse SHALL create a work order for the missing amount, linked to the request. A request whose item no producer can make SHALL stay a plain request with missing stock. An order SHALL also be created by `/servus_warehouse order <pos> <item> <count>`.

#### Scenario: Sticks on order
- **WHEN** a player requests 8 sticks, none are in stock, planks are, and a dock faces a Crafting Station with the sticks recipe
- **THEN** a work order for 8 sticks is created and the request shows producing

#### Scenario: Nothing can make it
- **WHEN** a player requests bread and no dock's producer has a recipe for bread
- **THEN** no order is created and the request shows the bread as missing

#### Scenario: Manual order
- **WHEN** an op runs the order command for 16 sticks
- **THEN** an order for 16 sticks exists without a request

### Requirement: Planning with reservations
An order SHALL be planned with the Lib order planner over the recipes of every dock, trying docks that are idle first. Stock the plan uses SHALL be reserved for that order, SHALL NOT count as available to later plans, and SHALL shrink as the stock is fed to docks. Surplus from a step SHALL serve later steps of the same order. Raw materials the plan cannot get SHALL be reported as the order's blocked reason until they arrive.

#### Scenario: Two levels
- **WHEN** 8 sticks are ordered, only logs are in stock, and a station knows planks from logs and sticks from planks
- **THEN** the order has a planks step and a sticks step, the planks step first

#### Scenario: Reserved stock
- **WHEN** an order reserves 4 planks and a second order for sticks is planned while 4 planks are in stock
- **THEN** the second order finds no planks available and reports them missing

#### Scenario: Missing raw material
- **WHEN** sticks are ordered and no planks and no logs are in stock
- **THEN** the order is blocked with planks missing and starts when planks arrive

### Requirement: Steps run on docks
Each step SHALL run on the dock whose producer offers its recipe. The warehouse SHALL feed the step's inputs to that dock through the feed path, one step at a time per dock, orders in creation order. A step SHALL be producing once its inputs are fed and done once the dock has collected the step's outputs. The order SHALL be done when every step is done.

#### Scenario: Feed, produce, collect
- **WHEN** a sticks step for 2 batches starts on a dock facing a Crafting Station
- **THEN** 4 planks are delivered to the dock's feed buffer, the station crafts twice, and the step is done when 8 sticks were collected from the dock

#### Scenario: Dock busy
- **WHEN** two orders need the same dock
- **THEN** the older order's step runs first and the other waits for the dock

#### Scenario: Request served
- **WHEN** the order's sticks have been collected into storage
- **THEN** the request that created it receives them within its retry interval

### Requirement: States and reasons
An order SHALL be running, blocked or done. Blocked reasons SHALL be: no producer for an item, a missing raw material by name, the dock's feed needing a shuttle no garage provides, or the dock out of reach. Each step SHALL show waiting for the dock, feeding, producing or done. `/servus_warehouse orders <pos>` SHALL list every order with its item, amount, state, reason and steps.

#### Scenario: Listing
- **WHEN** an op runs the orders command while a sticks order is producing
- **THEN** chat lists the order with its state and the sticks step as producing on its dock

### Requirement: Cancellation
Cancelling the request of an order SHALL cancel the order: steps not yet fed are dropped and their reservations released; items already fed stay in the machines and what they produce is put away as usual. A done order SHALL leave the list after a short while.

#### Scenario: Cancel while waiting
- **WHEN** the player cancels a request whose order waits for planks
- **THEN** the order is gone and the planks reserved for it are available again

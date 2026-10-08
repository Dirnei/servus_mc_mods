# storage/work-orders Specification

## Purpose

Work orders turn a shortage into production: the warehouse plans what to make from the recipes its docks offer, reserves what it has, feeds the machines and counts the results.

## Requirements

### Requirement: Shortage creates an order
When a terminal request cannot be filled from stock and some producer on the terminal's data line can make the item, directly or through intermediate recipes, a Crafting CPU of that line SHALL create a work order for the missing amount, linked to the request. A request whose item no producer can make, or whose line has no CPU, SHALL stay a plain request with missing stock. An order SHALL also be created by `/servus_warehouse order <pos> <item> <count>`.

#### Scenario: Sticks on order
- **WHEN** a player requests 8 sticks, none are in stock, planks are, and a Crafting Station with the sticks recipe and a CPU are on the terminal's data line
- **THEN** a work order for 8 sticks is created on the CPU and the request shows producing

#### Scenario: Nothing can make it
- **WHEN** a player requests bread and no producer on the line has a recipe for bread
- **THEN** no order is created and the request shows the bread as missing

#### Scenario: Manual order
- **WHEN** an op runs the order command for 16 sticks
- **THEN** an order for 16 sticks exists on a CPU of the line without a request

### Requirement: Planning with reservations
An order SHALL be planned with the Foundation order planner over the recipes of every producer on the CPU's data line, trying producers that are idle first, against the stock of the warehouses on that line. Stock the plan uses SHALL be reserved for that order, SHALL NOT count as available to later plans of any CPU of the line, and SHALL shrink as the stock is shipped to producers. Surplus from a step SHALL serve later steps of the same order. Raw materials the plan cannot get SHALL be reported as the order's blocked reason until they arrive.

#### Scenario: Two levels
- **WHEN** 8 sticks are ordered, only logs are in stock, and a producer knows planks from logs and sticks from planks
- **THEN** the order has a planks step and a sticks step, the planks step first

#### Scenario: Reserved stock
- **WHEN** an order reserves 4 planks and a second order for sticks is planned while 4 planks are in stock
- **THEN** the second order finds no planks available and reports them missing

#### Scenario: Missing raw material
- **WHEN** sticks are ordered and no planks and no logs are in stock
- **THEN** the order is blocked with planks missing and starts when planks arrive

### Requirement: States and reasons
An order SHALL be waiting for its CPU, running, blocked, done or cancelled. Blocked reasons SHALL be: a missing raw material by name, the delivery needing a shuttle no garage provides, no Access Point on the line, or the producer out of reach. Each step SHALL show waiting for the producer, delivering, producing or done. `/servus_warehouse orders <pos>` SHALL list every order with its item, amount, state, reason and steps.

#### Scenario: Listing
- **WHEN** an op runs the orders command while a sticks order is producing
- **THEN** chat lists the order with its state and the sticks step as producing on its producer

### Requirement: Cancellation
Cancelling the request of an order SHALL cancel the order: steps not yet delivered are dropped and their reservations released; items already delivered stay in the producer, and its outputs are no longer collected for the order. A done or cancelled order SHALL leave the list after a short while.

#### Scenario: Cancel while waiting
- **WHEN** the player cancels a request whose order waits for planks
- **THEN** the order is gone and the planks reserved for it are available again

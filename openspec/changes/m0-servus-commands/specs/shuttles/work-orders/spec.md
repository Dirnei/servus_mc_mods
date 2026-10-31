## MODIFIED Requirements

### Requirement: Shortage creates an order
When a terminal request cannot be filled from stock and some producer on the terminal's data line can make the item, directly or through intermediate recipes, a Crafting CPU of that line SHALL create a work order for the missing amount, linked to the request. A request whose item no producer can make, or whose line has no CPU, SHALL stay a plain request with missing stock. An order SHALL also be created by `/servus shuttles order <warehouse> <item> <count>`.

#### Scenario: Sticks on order
- **WHEN** a player requests 8 sticks, none are in stock, planks are, and a Crafting Station with the sticks recipe and a CPU are on the terminal's data line
- **THEN** a work order for 8 sticks is created on the CPU and the request shows producing

#### Scenario: Nothing can make it
- **WHEN** a player requests bread and no producer on the line has a recipe for bread
- **THEN** no order is created and the request shows the bread as missing

#### Scenario: Manual order
- **WHEN** an op runs the order command for 16 sticks
- **THEN** an order for 16 sticks exists on a CPU of the line without a request

### Requirement: States and reasons
An order SHALL be waiting for its CPU, running, blocked, done or cancelled. Blocked reasons SHALL be: a missing raw material by name, the delivery needing a shuttle no garage provides, no Packing Station on the line, or the producer out of reach. Each step SHALL show waiting for the producer, delivering, producing or done. `/servus shuttles orders <warehouse>` SHALL list every order with its item, amount, state, reason and steps.

#### Scenario: Listing
- **WHEN** an op runs the orders command while a sticks order is producing
- **THEN** chat lists the order with its state and the sticks step as producing on its producer

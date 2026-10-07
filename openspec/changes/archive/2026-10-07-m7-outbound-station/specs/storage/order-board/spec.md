# Spec Delta

## MODIFIED Requirements

### Requirement: Machines section
The screen SHALL list every Outbound Station of the warehouse with the name of the block it faces and its state: working on an item when the machine's producer reports an active recipe, idle when it has recipes but none runs, no recipes when it has a producer without recipes, no machine when the station faces no producer.

#### Scenario: Crafting Station at work
- **WHEN** an Outbound Station faces a Crafting Station that is crafting sticks
- **THEN** the board lists "Crafting Station: working on Stick"

#### Scenario: Empty dock
- **WHEN** an Outbound Station faces a chest
- **THEN** the board lists "Chest: no machine"

### Requirement: Orders section
The screen SHALL list every work order with its item, amount, state (running, blocked, done, cancelled), the blocked reason, and whether it comes from a request or a stock rule. Under each order it SHALL list every step with the machine on its Outbound Station, the output and batches, and the step state (waiting for station, delivering, producing, done) with its reason. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Blocked order
- **WHEN** an order for sticks waits for planks that are not in stock
- **THEN** the board shows the order as blocked with "missing Oak Planks" and its step as delivering

#### Scenario: Two steps
- **WHEN** an order makes planks from logs and then sticks
- **THEN** the board shows both steps under the order, the planks step first

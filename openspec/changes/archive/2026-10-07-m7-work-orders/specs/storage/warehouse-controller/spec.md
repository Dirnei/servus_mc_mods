# Spec Delta

## ADDED Requirements

### Requirement: Order commands
`/servus_warehouse orders <pos>` SHALL list every work order of the network at the position with item, amount, state, reason and steps. `/servus_warehouse order <pos> <item> <count>` SHALL create a work order and SHALL answer with the reason when none can be made.

#### Scenario: Op orders sticks
- **WHEN** an op runs the order command for 8 sticks while a dock faces a Crafting Station with the sticks recipe
- **THEN** chat confirms the order and the orders command lists it

#### Scenario: Op orders the impossible
- **WHEN** an op orders bread and nothing can make it
- **THEN** chat says there is no producer for bread

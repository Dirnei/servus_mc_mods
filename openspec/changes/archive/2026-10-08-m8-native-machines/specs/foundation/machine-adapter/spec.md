# Spec Delta

## MODIFIED Requirements

### Requirement: Upkeep stays stocked
The adapter SHALL expose the upkeep capability for the machine's upkeep side (its upkeep item, its level, the count in the machine), so the data network's shared upkeep keeper keeps that side stocked at the level, independent of orders. Recipes SHALL never contain upkeep. Upkeep the adapter was told is coming SHALL go through the upkeep side until it has arrived or the request timed out. The GUI SHALL show the upkeep item as a ghost slot, the level with buttons to change it, and the count in the machine; an empty ghost slot means no upkeep.

#### Scenario: Refill
- **WHEN** an adapter with upkeep coal at level 16 faces a furnace holding 4 coal and a warehouse with coal is on the line
- **THEN** the adapter asks for 12 coal and the furnace ends up with 16

#### Scenario: Order without fuel
- **WHEN** a CPU plans 8 stone through an adapter
- **THEN** the order's steps contain cobblestone only, no coal

#### Scenario: Change the upkeep
- **WHEN** the player puts charcoal into the ghost slot and lowers the level to 8
- **THEN** the adapter keeps 8 charcoal in the machine from then on

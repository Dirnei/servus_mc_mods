# Spec Delta

## MODIFIED Requirements

### Requirement: Several warehouses on one line
When more than one warehouse is reachable, the stock view SHALL show the sum of their stock. A request SHALL be served first by the warehouse holding the most of the item, then by the next, until the amount is covered or the stock runs out. Work orders for a shortfall SHALL go to a Crafting CPU on the terminal's data line; stock rules set in the terminal and pushes SHALL use the first reachable warehouse, the one whose controller has the lowest position.

#### Scenario: Merged stock
- **WHEN** one reachable warehouse holds 30 cobblestone and another 20
- **THEN** the terminal lists 50 cobblestone

#### Scenario: Split request
- **WHEN** the player requests 40 of that cobblestone
- **THEN** 30 come from the first warehouse and 10 from the second, and 40 land in the output slots

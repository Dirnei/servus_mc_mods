## MODIFIED Requirements

### Requirement: Recipe viewer transfer into the terminal
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the open Production Terminal prefilled with that recipe's layout. For each cell it SHALL choose, among the stacks the warehouse of the terminal's data line can give out that the cell's ingredient accepts, the one the warehouse has the most of, with that stack's components; without such a stack or without a warehouse on the line the cell SHALL take the ingredient's first item. The editor's preview SHALL show what the recipe makes from the grid, with the components the recipe adds (for example the wood of the planks), the same result a saved pattern has. Without JEI the terminal SHALL work unchanged.

#### Scenario: Plus button in JEI
- **WHEN** the terminal screen is open and the player presses the transfer button on the stick recipe in JEI
- **THEN** the editor opens with a plank above a plank and the preview shows four sticks

#### Scenario: Stocked planks
- **WHEN** the warehouse of the line holds spruce planks and no other planks, and the player presses the transfer button on the Heavy Duty Rack recipe
- **THEN** the middle row shows spruce planks and the preview shows Heavy Duty Racks whose tooltip names spruce wood

#### Scenario: Stocked Rail
- **WHEN** the warehouse of the line holds oak Rails and the player presses the transfer button on the Elevator recipe
- **THEN** the centre cell shows an oak Rail with its wood, and the saved pattern takes that Rail from stock

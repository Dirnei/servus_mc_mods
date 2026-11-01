# Spec Delta

## MODIFIED Requirements

### Requirement: Recipe viewer transfer into the terminal
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the open Production Terminal prefilled with the stacks JEI shows for the recipe, the same way it fills the Storage Terminal's order grid; this SHALL also work for crafting recipes that exist only in JEI. For each cell it SHALL choose, among the stacks the warehouse of the terminal's data line can give out that match a stack JEI shows in that cell, the one the warehouse has the most of, with that stack's components; a shown stack with components of its own SHALL match only stacks with those components. Without such a stack or without a warehouse on the line the cell SHALL take the first stack JEI shows. A transfer that opens the editor SHALL start a new pattern as Any when Any is possible; a transfer into the open editor SHALL keep its mode. The editor's preview SHALL show what the recipe makes from the grid, with the components the recipe adds (for example the wood of the planks), the same result a saved pattern has. Without JEI the terminal SHALL work unchanged.

#### Scenario: Plus button in JEI
- **WHEN** the terminal screen is open and the player presses the transfer button on the stick recipe in JEI
- **THEN** the editor opens with a plank above a plank, the preview shows four sticks and the mode shows Any

#### Scenario: Stocked planks
- **WHEN** the warehouse of the line holds spruce planks and no other planks, and the player presses the transfer button on the Heavy Duty Rack recipe
- **THEN** the middle row shows spruce planks and the preview shows Heavy Duty Racks whose tooltip names spruce wood

#### Scenario: Stocked Rail
- **WHEN** the warehouse of the line holds oak Rails and the player presses the transfer button on the Elevator recipe
- **THEN** the centre cell shows an oak Rail with its wood, and the saved pattern takes that Rail from stock

#### Scenario: Transfer keeps the editor's mode
- **WHEN** the editor is open on a new pattern set to Exact and the player presses the transfer button on the stick recipe
- **THEN** the grid shows the stick recipe and the mode still shows Exact

#### Scenario: Fluid Tank pattern
- **WHEN** the warehouse of the line holds stone bricks and the player presses the transfer button on the Fluid Tank recipe and saves
- **THEN** the editor shows stone bricks in the bottom middle and the saved pattern makes a Fluid Tank with a stone brick floor

#### Scenario: Facade example pattern
- **WHEN** the player presses the transfer button on the "Facade (Oak Planks)" recipe
- **THEN** the editor shows oak planks and a nugget and the preview shows six "Facade (Oak Planks)"

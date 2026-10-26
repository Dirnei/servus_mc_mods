# Spec Delta

## ADDED Requirements

### Requirement: Recipe editor on the shared grid view
The recipe editor SHALL be the shared recipe grid view, the same as the Storage Terminal's order grid: ghost cells filled by click or by dragging the cursor's item over them, the result as a bare icon after an arrow with its normal tooltip, and the Exact / Any button. Its action row SHALL have Cancel on the left, Clear and, for an existing pattern, Delete in the middle and Save on the right.

#### Scenario: Drag in the editor
- **WHEN** the editor is open and the player drags oak planks with the button down over the bottom three cells
- **THEN** each of the three cells shows one plank and the player keeps every plank

#### Scenario: Buttons of a new pattern
- **WHEN** the player opens the editor with plus
- **THEN** the row shows Cancel, Clear and Save, and no Delete

#### Scenario: Buttons of an existing pattern
- **WHEN** the player clicks the sticks pattern in the Patterns tab
- **THEN** the row shows Cancel, Clear, Delete and Save

#### Scenario: Clear in the editor
- **WHEN** the editor shows the sticks pattern and the player presses Clear
- **THEN** every cell is empty, the pattern is unchanged until Save, and Save is greyed out

### Requirement: Save needs a match
Save in the recipe editor SHALL be greyed out, with the tooltip that the grid matches no recipe, while the cells are empty or match no vanilla crafting recipe.

#### Scenario: Single stick
- **WHEN** the editor's cells hold a single stick
- **THEN** no result shows and Save is greyed out with the tooltip that the grid matches no recipe

## MODIFIED Requirements

### Requirement: Exact or Any in the recipe editor
The recipe editor SHALL have the shared grid view's mode button, which shows and switches the pattern's mode, Exact or Any. For a grid whose result changes with the items the recipe accepts the button SHALL show Exact, be greyed and say in its tooltip why Any is not possible. A new pattern SHALL start as Any when Any is possible; an existing pattern SHALL open with its own mode. The Patterns tab SHALL mark Any patterns, and their tooltip SHALL name what each Any cell accepts.

#### Scenario: New sticks pattern
- **WHEN** the player opens the editor with plus and puts a plank above a plank
- **THEN** the button shows Any, and pressing it switches to Exact

#### Scenario: Rack
- **WHEN** the grid holds a Heavy Duty Rack recipe
- **THEN** the button shows Exact, is greyed and its tooltip says the result changes with the planks

#### Scenario: Marked in the list
- **WHEN** the line has an Any sticks pattern and an Exact rack pattern
- **THEN** the Patterns tab marks the sticks pattern as Any and its tooltip says "Any planks"

### Requirement: Recipe viewer transfer into the terminal
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the open Production Terminal prefilled with that recipe's layout, the same way it fills the Storage Terminal's order grid. For each cell it SHALL choose, among the stacks the warehouse of the terminal's data line can give out that the cell's ingredient accepts, the one the warehouse has the most of, with that stack's components; without such a stack or without a warehouse on the line the cell SHALL take the ingredient's first item. A transfer that opens the editor SHALL start a new pattern as Any when Any is possible; a transfer into the open editor SHALL keep its mode. The editor's preview SHALL show what the recipe makes from the grid, with the components the recipe adds (for example the wood of the planks), the same result a saved pattern has. Without JEI the terminal SHALL work unchanged.

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

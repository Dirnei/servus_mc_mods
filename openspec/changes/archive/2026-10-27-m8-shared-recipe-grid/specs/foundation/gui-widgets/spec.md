# Spec Delta

## ADDED Requirements

### Requirement: Recipe grid view
Foundation SHALL offer one recipe grid view for every screen that edits or orders a 3x3 crafting grid: a popup with an optional title, the 3x3 ghost cells, an arrow, the result as a bare item icon with the count of one craft and its normal tooltip, a mode button, and an action row whose buttons the screen supplies. The screen SHALL say what the result is and what the mode button shows.

#### Scenario: Same view in two screens
- **WHEN** the player opens the Production Terminal's recipe editor and the Storage Terminal's order grid
- **THEN** both show the grid, the arrow, the result and the mode button at the same places in a popup of the same size

#### Scenario: No match
- **WHEN** the cells hold a single stick
- **THEN** no result icon shows and hovering the result place shows no tooltip

### Requirement: Recipe grid ghost cells
The cells of the recipe grid view SHALL hold ghost copies only. A click with an item on the cursor SHALL set a single copy of it, dragging the cursor's item over cells SHALL set a copy in each cell it passes, and a click with an empty cursor SHALL clear the cell; the cursor and the player inventory SHALL keep their items. Clear SHALL empty every cell.

#### Scenario: Drag fills cells
- **WHEN** the player holds 5 oak planks and drags with the button down over the top three cells
- **THEN** each of the three cells shows one plank and the player still holds 5 planks

#### Scenario: Clear
- **WHEN** four cells hold planks and the player presses Clear
- **THEN** every cell is empty and no result shows

### Requirement: Recipe grid mode button
The mode button SHALL show the label the screen gives, switch the mode when pressed, and SHALL be greyed out, take no presses and show the screen's reason as its tooltip when the screen says the mode cannot change.

#### Scenario: Greyed with reason
- **WHEN** the screen says the mode cannot change and gives the reason "The result changes with these items"
- **THEN** the button is greyed out, a press does nothing and its tooltip reads that reason

### Requirement: Recipe grid actions from the screen
The action row SHALL hold the buttons the screen supplies, the first on the left, the last on the right and the others centred between them. A button the screen marks as not pressable SHALL be greyed out with the screen's reason as its tooltip.

#### Scenario: Three actions
- **WHEN** a screen supplies Cancel, Clear and Save
- **THEN** Cancel is on the left, Clear in the middle and Save on the right of the row

#### Scenario: Greyed action
- **WHEN** a screen marks Save as not pressable with the reason "Matches no recipe"
- **THEN** Save is greyed out and its tooltip reads "Matches no recipe"

### Requirement: Recipe grid layout is pure
The places of the recipe grid view's popup, cells, arrow, result, mode button and action row SHALL be computed in plain Java without Minecraft classes and covered by JUnit tests that check that no two of them overlap and that all lie inside the popup.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** the layout is checked for overlaps and for fitting inside the popup without starting Minecraft

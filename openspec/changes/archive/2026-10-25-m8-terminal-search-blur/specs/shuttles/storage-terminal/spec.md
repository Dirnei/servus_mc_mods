# Spec Delta

## ADDED Requirements

### Requirement: Clicking away from the search box
The search box SHALL have focus when the Storage Terminal opens. A press of any mouse button anywhere in the screen that is not on the search box SHALL take the focus from it, keep its text, and still do what the press does there. Without focus the box SHALL take no keys, so the inventory key closes the terminal and the number keys move items to the hotbar. A click on the box SHALL give it focus again.

#### Scenario: Click on the grid
- **WHEN** the player has typed "plank" in the search box and clicks an item in the grid
- **THEN** the item is requested, the search box loses focus and still holds "plank"

#### Scenario: Close with the inventory key
- **WHEN** the player clicks on an empty part of the panel after typing in the search box and then presses the inventory key
- **THEN** the terminal closes

#### Scenario: Hotbar key on a slot
- **WHEN** the player clicks a slot of the player inventory while the search box has focus, then hovers a slot and presses 1
- **THEN** that slot's stack swaps with the first hotbar slot and no "1" is typed in the box

#### Scenario: Back into the box
- **WHEN** the search box has no focus and the player clicks it
- **THEN** it has focus and takes typed text again

### Requirement: Recipe viewer keys on grid items
With JEI installed, the item under the mouse in the Storage Terminal's grid SHALL count as hovered for JEI, so its show recipes and show uses keys open that item's recipes and uses while the search box has no focus.

#### Scenario: Recipes of a stocked item
- **WHEN** the player hovers Oak Planks in the grid with the search box unfocused and presses the show recipes key (R by default)
- **THEN** JEI shows the recipes that make Oak Planks

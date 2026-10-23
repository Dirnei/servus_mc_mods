## MODIFIED Requirements

### Requirement: Encoding from the terminal
Saving a new pattern in the Production Terminal's editor, or choosing "Save as pattern" in the Storage Terminal's order grid, SHALL match the 3x3 ghost grid to a vanilla crafting recipe and store a pattern with the recipe id, the grid items and the result in the first Pattern Storage of the terminal's line with a free place. It SHALL refuse with a message and keep the grid when the grid is empty or matches nothing, when the line already has a pattern for the same recipe with the same items, when the line has no Pattern Storage, or when no Pattern Storage has room; the Production Terminal says so in the action bar and the Storage Terminal on its message line. Saving an existing pattern SHALL rewrite it in place. Deleting a pattern SHALL remove it.

#### Scenario: New pattern
- **WHEN** the player opens the editor with plus, puts a plank above a plank and presses Save
- **THEN** a sticks pattern is in the first Pattern Storage with room and the Patterns tab lists it, and the player's inventory is unchanged

#### Scenario: No blank
- **WHEN** the player has an empty inventory and saves a matching grid
- **THEN** the pattern is stored; no item is needed

#### Scenario: No Pattern Storage
- **WHEN** the terminal's line has no Pattern Storage and the player saves a matching grid
- **THEN** nothing is stored and the action bar says there is no Pattern Storage on this data line

#### Scenario: Full
- **WHEN** every Pattern Storage of the line is full and the player saves a new pattern
- **THEN** nothing is stored and the action bar says the Pattern Storage is full

#### Scenario: Delete
- **WHEN** the player clicks the sticks pattern in the Patterns tab and presses Delete
- **THEN** the pattern is gone from its Pattern Storage and the player gets nothing

#### Scenario: Saved from the Storage Terminal
- **WHEN** the Storage Terminal's order grid holds four oak planks in a square and the player chooses "Save as pattern"
- **THEN** a crafting table pattern is in the first Pattern Storage of the line with room and the Production Terminal's Patterns tab lists it

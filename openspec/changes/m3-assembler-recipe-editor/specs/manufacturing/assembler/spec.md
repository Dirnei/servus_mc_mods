# Spec Delta

## MODIFIED Requirements

### Requirement: Recipe selection
The assembler GUI SHALL show the configured recipes (up to `recipe_slots` from the machine stats, default 2 for the Mk1) as cells with a plus button. Plus SHALL open a recipe editor popup with an empty ghost pattern grid; clicking a configured recipe SHALL open the editor prefilled with that recipe's layout. The editor SHALL show a live preview of the recipe the grid matches, and offer Save, Delete (for an existing recipe) and Cancel. Save SHALL add or replace the recipe under the cap, duplicate and research rules and close the editor on success; on failure the editor stays open and the action bar says why. The configuration SHALL persist with the block and SHALL be visible to every player who opens the GUI.

#### Scenario: Add from the pattern grid
- **WHEN** the player presses plus, lays out two iron ingots in the editor grid and presses Save
- **THEN** the iron gear assembling recipe is added to the configured list, the editor closes and the machine crafts it when ingots are present

#### Scenario: Cap reached
- **WHEN** two recipes are configured on an Assembler Mk1 and the player saves a third from the editor
- **THEN** the recipe is refused, the editor stays open and the action bar says the recipe slots are in use

#### Scenario: Remove
- **WHEN** the player clicks a configured recipe and presses Delete in the editor
- **THEN** it is removed and the machine stops crafting it

#### Scenario: Select a recipe
- **WHEN** the player saves the iron gear recipe from the editor or opens it through the recipe viewer and saves
- **THEN** it shows in the configured list and the machine starts crafting it when inputs are present

#### Scenario: No recipe selected
- **WHEN** no recipe is configured
- **THEN** the assembler accepts no inputs from automation and does not craft

#### Scenario: Edit a recipe
- **WHEN** the player clicks a configured stick recipe, changes the grid to planks and presses Save
- **THEN** the cell now holds the planks recipe and the stick recipe is gone

#### Scenario: Preview
- **WHEN** the editor grid holds two iron ingots
- **THEN** the preview shows an iron gear; with an unmatched layout it shows nothing

### Requirement: Recipe viewer transfer
With JEI installed, the transfer button on a crafting or assembling recipe SHALL open the recipe editor of the assembler whose GUI is open, prefilled with that recipe's layout, so the player can save it under the same cap and research rules.

#### Scenario: Plus button
- **WHEN** the player opens an assembler, looks up sticks in JEI and presses plus, then presses Save
- **THEN** the stick recipe appears in the configured list

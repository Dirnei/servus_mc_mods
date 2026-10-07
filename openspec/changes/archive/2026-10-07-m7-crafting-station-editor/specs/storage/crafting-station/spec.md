## MODIFIED Requirements

### Requirement: Configured recipes
The Crafting Station SHALL hold up to `crafting_station_recipes` configured recipes (example 4, from `data/<ns>/storage/crafting_station.json`). The GUI SHALL show the configured recipes as a column of cells with a plus button, like the Assembler. Plus SHALL open a recipe editor popup with an empty ghost pattern grid; clicking a configured cell SHALL open the editor prefilled with that recipe's template. The editor SHALL show a live preview of the vanilla crafting recipe the grid matches and offer Save, Delete (for an existing recipe) and Cancel. Save SHALL add or replace the recipe, using the grid's items as its template, and close the editor; it SHALL refuse with an action bar message and keep the editor open when the grid matches nothing, the recipe is already configured or the slots are full. The configuration SHALL persist with the block and SHALL be kept by the dropped item when dismantled with the carrot.

#### Scenario: Save sticks
- **WHEN** the player presses plus, puts a plank above a plank in the editor grid and presses Save
- **THEN** the editor closes, a cell shows four sticks and the station crafts sticks from then on

#### Scenario: Preview
- **WHEN** the editor grid holds a plank above a plank
- **THEN** the preview shows four sticks; with an unmatched layout it shows nothing

#### Scenario: No match
- **WHEN** the editor grid holds a single cobblestone and the player presses Save
- **THEN** nothing is added, the editor stays open and the action bar says the grid matches no recipe

#### Scenario: Slots full
- **WHEN** all recipe slots are used and the player saves a new recipe from the editor
- **THEN** the recipe is refused, the editor stays open and the action bar says the recipe slots are in use

#### Scenario: Edit a recipe
- **WHEN** the player clicks the sticks cell, changes the grid to a torch layout and presses Save
- **THEN** the cell now holds the torch recipe and the sticks recipe is gone

#### Scenario: Remove
- **WHEN** the player clicks the sticks cell and presses Delete in the editor
- **THEN** the cell is empty and the station stops crafting sticks

#### Scenario: Click does not delete
- **WHEN** the player clicks the sticks cell and presses Cancel
- **THEN** the sticks recipe is still configured

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from a crafting table, iron ingots and a chest, SHALL drop itself and its items when broken, and SHALL be dismantled by sneaking with the Configuration Carrot keeping its configuration. The carrot without sneaking SHALL cycle the mode of the clicked side, like on machines.

#### Scenario: Crafting
- **WHEN** the player places a crafting table over a chest framed by iron ingots
- **THEN** a Crafting Station is produced

## ADDED Requirements

### Requirement: Station GUI layout
The station GUI SHALL show the nine inputs on the left, the recipe cells with the plus button in the middle with the recipe being crafted marked, and the four outputs with a progress arrow on the right, above the player inventory and beside the side panel. While the editor is open the rest of the GUI SHALL be dimmed and inert.

#### Scenario: Working
- **WHEN** the station crafts sticks
- **THEN** the sticks cell is marked and the progress arrow fills toward the outputs

#### Scenario: Editor open
- **WHEN** the editor popup is open
- **THEN** clicks on the inputs, outputs and recipe cells do nothing until the editor closes

### Requirement: Recipe viewer transfer
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the Crafting Station whose GUI is open, prefilled with that recipe's layout, so the player can save it under the same rules. Without JEI the station SHALL work unchanged.

#### Scenario: Plus button in JEI
- **WHEN** the station GUI is open and the player presses the transfer button on the stick recipe in JEI
- **THEN** the editor opens with a plank above a plank and the preview shows four sticks

#### Scenario: No JEI
- **WHEN** JEI is not installed
- **THEN** Servus Storage loads and the station GUI works with plus and the editor

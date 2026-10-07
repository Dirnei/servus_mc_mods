# storage/crafting-station Specification

## Purpose

The Crafting Station is Storage's own producer: a powerless block that crafts the vanilla recipes a player configured, so a warehouse without Manufacturing can still autocraft.

## Requirements

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

### Requirement: Crafting
The station SHALL have nine input slots and four output slots. It SHALL craft any configured recipe whose template items are all present in the inputs and whose result and remainders fit the outputs, taking turns between craftable recipes, each craft taking `work_ticks` (example 40). Finished items and remainders SHALL go to the outputs. No power is needed.

#### Scenario: Sticks from planks
- **WHEN** the sticks recipe is configured and four planks are in the inputs
- **THEN** after the work time four sticks are in the outputs and two planks remain, and after the work time again eight sticks

#### Scenario: Outputs full
- **WHEN** the result would not fit the outputs
- **THEN** the craft waits and the inputs are untouched

### Requirement: Machine like sides
The station SHALL have a Lib side configuration like a machine: Auto, Input, Output, Both, Pull, Push and Off, all sides Auto by default except the front, which is fixed to Off, shown in the Lib side panel and cycled by the carrot. Through sides that allow insertion the inputs accept only items used by a configured recipe; through sides that allow extraction the outputs can be taken. Items in the outputs SHALL be pushed into adjacent inventories through Auto and Push sides, checking regularly, so an Inbound Station beside the station collects them, and items the recipes use SHALL be pulled from neighbours through Pull sides.

#### Scenario: Dock feeds and collects
- **WHEN** an Outbound Station faces the station with planks in its buffer, an Inbound Station touches the station and the sticks recipe is configured
- **THEN** the planks move into the station and the sticks end up in the Inbound Station's buffer

#### Scenario: Unused item refused
- **WHEN** a hopper offers cobblestone and no configured recipe uses it
- **THEN** the cobblestone is refused

#### Scenario: Side off
- **WHEN** the top side is Off and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Front closed
- **WHEN** an Outbound Station pushes planks against the station's front
- **THEN** the planks stay in the outbound buffer

### Requirement: Station reports its production
The station SHALL expose the Lib production capability: one recipe per configured recipe with the template items as inputs, the result as output and the work time as ticks, and the recipe it is working on as active.

#### Scenario: Configured sticks
- **WHEN** the sticks recipe is configured and the production capability is queried
- **THEN** one recipe is reported with two planks in, four sticks out and the work time

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from a crafting table, iron ingots and a chest, SHALL drop itself and its items when broken, and SHALL be dismantled by sneaking with the Configuration Carrot keeping its configuration. The carrot without sneaking SHALL cycle the mode of the clicked side, like on machines.

#### Scenario: Crafting
- **WHEN** the player places a crafting table over a chest framed by iron ingots
- **THEN** a Crafting Station is produced

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

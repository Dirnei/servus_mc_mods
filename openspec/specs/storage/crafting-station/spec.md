# storage/crafting-station Specification

## Purpose

The Crafting Station is Storage's own producer: a powerless block that crafts the vanilla recipes a player configured, so a warehouse without Manufacturing can still autocraft.

## Requirements

### Requirement: Configured recipes
The Crafting Station SHALL hold up to `crafting_station_recipes` configured recipes (example 4, from `data/<ns>/storage/crafting_station.json`). The GUI SHALL show a three by three ghost pattern grid, a Save button and one cell per recipe slot. Save SHALL add the vanilla crafting recipe the grid matches as a configured recipe, using the grid's items as its template, and SHALL refuse with a message when the grid matches nothing, the recipe is already configured or the slots are full. Clicking a configured cell SHALL remove that recipe. The configuration SHALL persist with the block and SHALL be kept by the dropped item when dismantled with the carrot.

#### Scenario: Save sticks
- **WHEN** the player puts a plank above a plank in the grid and presses Save
- **THEN** a cell shows four sticks and the station crafts sticks from then on

#### Scenario: No match
- **WHEN** the grid holds a single cobblestone and the player presses Save
- **THEN** nothing is added and the action bar says the grid matches no recipe

#### Scenario: Remove
- **WHEN** the player clicks the sticks cell
- **THEN** the cell is empty and the station stops crafting sticks

### Requirement: Crafting
The station SHALL have nine input slots and four output slots. It SHALL craft any configured recipe whose template items are all present in the inputs and whose result and remainders fit the outputs, taking turns between craftable recipes, each craft taking `work_ticks` (example 40). Finished items and remainders SHALL go to the outputs. No power is needed.

#### Scenario: Sticks from planks
- **WHEN** the sticks recipe is configured and four planks are in the inputs
- **THEN** after the work time four sticks are in the outputs and two planks remain, and after the work time again eight sticks

#### Scenario: Outputs full
- **WHEN** the result would not fit the outputs
- **THEN** the craft waits and the inputs are untouched

### Requirement: Machine like sides
The station SHALL expose its inventory through the item handler capability on every side: inputs accept insertion only, and only of items used by a configured recipe; outputs allow extraction only. Items in the outputs SHALL be pushed into adjacent inventories that accept them, checking regularly, so a Machine Dock facing the station collects them.

#### Scenario: Dock feeds and collects
- **WHEN** a Machine Dock faces the station, planks are fed to the dock and the sticks recipe is configured
- **THEN** the planks move into the station and the sticks end up in the dock's collect buffer

#### Scenario: Unused item refused
- **WHEN** a hopper offers cobblestone and no configured recipe uses it
- **THEN** the cobblestone is refused

### Requirement: Station reports its production
The station SHALL expose the Lib production capability: one recipe per configured recipe with the template items as inputs, the result as output and the work time as ticks, and the recipe it is working on as active.

#### Scenario: Configured sticks
- **WHEN** the sticks recipe is configured and the production capability is queried
- **THEN** one recipe is reported with two planks in, four sticks out and the work time

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from a crafting table, iron ingots and a chest, SHALL drop itself and its items when broken, and SHALL be dismantled by sneaking with the Configuration Carrot keeping its configuration. The carrot without sneaking SHALL turn it.

#### Scenario: Crafting
- **WHEN** the player places a crafting table over a chest framed by iron ingots
- **THEN** a Crafting Station is produced

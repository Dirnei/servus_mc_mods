# manufacturing/assembler Specification

## Purpose

The Assembler crafts intermediate products from a recipe the player selects, at fixed times and power cost, so factories can be planned around clean ratios.

## Requirements

### Requirement: Assembling recipes are data
An assembling recipe SHALL be a JSON file of type `servus_manufacturing:assembling` under a datapack's `recipe` folder with a list of ingredients with counts, a result stack and a time in ticks. Modpacks SHALL be able to add, change and remove recipes.

#### Scenario: Built in recipes
- **WHEN** the server loads with Servus Manufacturing
- **THEN** assembling recipes exist for one iron gear from two iron ingots in 20 ticks, two copper wire from one copper ingot in 10 ticks, and one red pack from one iron gear and two copper wire in 100 ticks (example values, retunable in data)

#### Scenario: Datapack recipe
- **WHEN** a datapack adds an assembling recipe for a vanilla item
- **THEN** after `/reload` it appears in the assembler's recipe list and can be crafted

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

### Requirement: Crafting the selected recipe
The assembler SHALL craft any of its configured recipes whose required items are all present in the input slots and whose result and remainders fit the output slots, taking turns between craftable recipes. A crafting recipe takes the machine's work time; an assembling recipe takes its own time. Crafting remainders such as empty buckets go to the output slots together with the result.

#### Scenario: Vanilla recipe
- **WHEN** the stick recipe is configured and two planks are in the inputs with energy available
- **THEN** after the machine's work time four sticks are in the output and the planks are gone

#### Scenario: Round robin
- **WHEN** the gear and wire recipes are configured and both iron and copper ingots are present
- **THEN** crafts alternate between gears and wire

#### Scenario: Missing ingredient
- **WHEN** a configured recipe lacks an ingredient
- **THEN** it is skipped and no energy is spent on it

#### Scenario: Iron gear
- **WHEN** iron gear is configured, four iron ingots are in the inputs, the machine has energy
- **THEN** after 20 ticks one iron gear is in the output and two ingots remain, and after 20 more ticks a second gear is produced

#### Scenario: No power
- **WHEN** a configured recipe could be crafted but the energy buffer is empty
- **THEN** progress does not advance and the inputs are untouched

#### Scenario: Remainders
- **WHEN** the cake recipe is configured and three milk buckets, two sugar, an egg and three wheat are in the inputs
- **THEN** after one craft the outputs hold one cake and three empty buckets

#### Scenario: Outputs full
- **WHEN** the result or a remainder would not fit the output slots
- **THEN** the craft waits and the inputs are untouched

### Requirement: Input slots accept only useful items
The assembler SHALL accept into its input slots only items used by at least one configured recipe.

#### Scenario: Wrong item by automation
- **WHEN** only the copper wire recipe is configured and a hopper offers cobblestone
- **THEN** the cobblestone is rejected

### Requirement: Tier 1 items and crafting
Iron Gear, Copper Wire and Red Pack SHALL exist as items, and the Assembler SHALL be craftable from vanilla materials so the loop can start.

#### Scenario: Assembler recipe
- **WHEN** the player crafts with iron ingots, copper ingots, redstone and a crafting table in the assembler pattern
- **THEN** the result is one Assembler Mk1

### Requirement: Locked recipes cannot be selected
An assembling recipe with a `tech` field SHALL be refused when added until the player's team has completed that tech. Recipes without a `tech` field and crafting recipes are always allowed.

#### Scenario: Locked recipe
- **WHEN** a recipe requires a tech the team has not completed and the player adds it
- **THEN** it is refused and the action bar says research is needed

#### Scenario: Unlocked after research
- **WHEN** the team completes the tech
- **THEN** the recipe can be added from then on

### Requirement: Any crafting recipe
The assembler SHALL accept every recipe of the vanilla crafting type, from any mod, as a configured recipe, using one concrete item per ingredient as its template.

#### Scenario: Modded crafting recipe
- **WHEN** a mod adds a shaped crafting recipe and the player adds it through the recipe viewer
- **THEN** the assembler crafts it from the template items

### Requirement: Recipe viewer transfer
With JEI installed, the transfer button on a crafting or assembling recipe SHALL open the recipe editor of the assembler whose GUI is open, prefilled with that recipe's layout, so the player can save it under the same cap and research rules.

#### Scenario: Plus button
- **WHEN** the player opens an assembler, looks up sticks in JEI and presses plus, then presses Save
- **THEN** the stick recipe appears in the configured list

### Requirement: Buffers
The Assembler Mk1 SHALL have 18 input slots and 4 output slots, so two configured recipes can be buffered and results and remainders can accumulate.

#### Scenario: Slot counts
- **WHEN** the player opens the assembler GUI
- **THEN** three rows of six input slots and a two by two block of output slots are shown

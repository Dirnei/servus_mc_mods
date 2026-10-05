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
The assembler GUI SHALL let the player configure up to its recipe slot count of recipes (`recipe_slots` in the machine stats, default 2 for the Mk1). Recipes are added by laying them out in a ghost pattern grid and pressing plus, or through a recipe viewer's transfer button, and removed by clicking them in the configured list. The configuration SHALL persist with the block and SHALL be visible to every player who opens the GUI.

#### Scenario: Add from the pattern grid
- **WHEN** the player lays out two iron ingots in the pattern grid and presses plus
- **THEN** the iron gear assembling recipe is added to the configured list and the machine crafts it when ingots are present

#### Scenario: Cap reached
- **WHEN** two recipes are configured on an Assembler Mk1 and the player adds a third
- **THEN** the recipe is refused and the action bar says the recipe slots are in use

#### Scenario: Remove
- **WHEN** the player clicks a configured recipe
- **THEN** it is removed and the machine stops crafting it

#### Scenario: Select a recipe
- **WHEN** the player adds the iron gear recipe through the pattern grid or the recipe viewer
- **THEN** it shows in the configured list and the machine starts crafting it when inputs are present

#### Scenario: No recipe selected
- **WHEN** no recipe is configured
- **THEN** the assembler accepts no inputs from automation and does not craft

### Requirement: Crafting the selected recipe
The assembler SHALL craft any of its configured recipes whose required items are all present in the input slots and whose result fits the output slot, taking turns between craftable recipes. A crafting recipe takes the machine's work time; an assembling recipe takes its own time. Crafting remainders such as empty buckets return to the input slots.

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
With JEI installed, the transfer button on a crafting or assembling recipe SHALL add it to the assembler whose GUI is open, subject to the same cap and research rules.

#### Scenario: Plus button
- **WHEN** the player opens an assembler, looks up sticks in JEI and presses plus
- **THEN** the stick recipe appears in the configured list

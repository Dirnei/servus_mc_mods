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
The assembler GUI SHALL list every assembling recipe and SHALL let the player select exactly one. The selection SHALL persist with the block and SHALL be visible to every player who opens the GUI.

#### Scenario: Select a recipe
- **WHEN** the player clicks the iron gear entry in the recipe panel
- **THEN** the iron gear recipe is selected, shown as highlighted, and the machine starts crafting it when inputs are present

#### Scenario: No recipe selected
- **WHEN** no recipe is selected
- **THEN** the assembler accepts no inputs from automation except items used by some assembling recipe, and does not craft

### Requirement: Crafting the selected recipe
While a recipe is selected, the assembler SHALL craft it whenever the input slots together hold every ingredient in the required count and the output slot can take the result. One craft takes the recipe's time in ticks and costs the machine's energy per tick while running.

#### Scenario: Iron gear
- **WHEN** iron gear is selected, four iron ingots are in the inputs, the machine has energy
- **THEN** after 20 ticks one iron gear is in the output and two ingots remain, and after 20 more ticks a second gear is produced

#### Scenario: Missing ingredient
- **WHEN** red pack is selected and the inputs hold a gear but no wire
- **THEN** nothing is crafted and no energy is spent

#### Scenario: No power
- **WHEN** the selected recipe could be crafted but the energy buffer is empty
- **THEN** progress does not advance and the inputs are untouched

### Requirement: Input slots accept only useful items
The assembler SHALL accept into its input slots only items that are an ingredient of the selected recipe, or of any assembling recipe when none is selected.

#### Scenario: Wrong item by automation
- **WHEN** copper wire is selected and a hopper offers cobblestone
- **THEN** the cobblestone is rejected

### Requirement: Tier 1 items and crafting
Iron Gear, Copper Wire and Red Pack SHALL exist as items, and the Assembler SHALL be craftable from vanilla materials so the loop can start.

#### Scenario: Assembler recipe
- **WHEN** the player crafts with iron ingots, copper ingots, redstone and a crafting table in the assembler pattern
- **THEN** the result is one Assembler Mk1

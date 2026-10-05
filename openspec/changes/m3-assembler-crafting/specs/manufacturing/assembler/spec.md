# Spec Delta

## MODIFIED Requirements

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

### Requirement: Input slots accept only useful items
The assembler SHALL accept into its input slots only items used by at least one configured recipe.

#### Scenario: Wrong item by automation
- **WHEN** only the copper wire recipe is configured and a hopper offers cobblestone
- **THEN** the cobblestone is rejected

### Requirement: Locked recipes cannot be selected
An assembling recipe with a `tech` field SHALL be refused when added until the player's team has completed that tech. Recipes without a `tech` field and crafting recipes are always allowed.

#### Scenario: Locked recipe
- **WHEN** a recipe requires a tech the team has not completed and the player adds it
- **THEN** it is refused and the action bar says research is needed

## ADDED Requirements

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

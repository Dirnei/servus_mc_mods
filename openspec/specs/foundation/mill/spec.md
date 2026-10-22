# foundation/mill Specification

## Purpose

The Mill is a Foundation machine that grinds items only while a driver drives it. It covers the block, its slots, sides, screen and look, the data driven `servus_foundation:milling` recipe type with the shipped gravel recipe, and the JEI category.

## Requirements

### Requirement: Milling recipes
Foundation SHALL provide the recipe type `servus_foundation:milling`: an input ingredient with a count, one result, and the work one milling takes. Recipes SHALL be datapack JSON, SHALL be sent to clients, and any mod or datapack MAY add more. Foundation SHALL ship one milling recipe: 1 Gravel into 1 Sand (example work 200).

#### Scenario: Shipped recipe
- **WHEN** a world loads with only the Servus mods
- **THEN** a milling recipe turns 1 Gravel into 1 Sand

#### Scenario: Datapack recipe
- **WHEN** a datapack adds a milling recipe from 2 Cobblestone to 1 Gravel with work 400
- **THEN** the Mill accepts Cobblestone and makes 1 Gravel from every 2 after 400 work

### Requirement: Mill block
Foundation SHALL provide the Mill, a machine crafted from vanilla and Foundation materials. It SHALL face the player when placed, open a screen with an input slot, a progress arrow, an output slot and the side panel on right click, take into its input only items some milling recipe accepts, and drop itself and its contents when broken. The Configuration Carrot SHALL cycle its sides and dismantle it like other machines.

#### Scenario: Place and open
- **WHEN** a player places a Mill and right clicks it with an empty hand
- **THEN** the Mill's screen opens with input, arrow, output and the side panel

#### Scenario: Wrong item
- **WHEN** a player tries to put Cobblestone into the input slot and no milling recipe takes Cobblestone
- **THEN** the slot refuses it

### Requirement: The Mill grinds only while driven
The Mill SHALL have no power or fuel of its own. It SHALL advance a milling only by the work a driver gives it through the drive link, SHALL finish one milling when the work given reaches the recipe's work, then take the recipe's count from the input and put the result into the output. Without a driver its progress SHALL stay where it is. It SHALL want work only while its input holds enough for a recipe and its output has room for the result.

#### Scenario: Driven
- **WHEN** a Mill holds Gravel and a driver gives it 200 work
- **THEN** one Gravel is gone and one Sand is in the output

#### Scenario: Not driven
- **WHEN** a Mill holds Gravel and nothing drives it for ten seconds
- **THEN** the Gravel is still in the input and the output is empty

#### Scenario: Output full
- **WHEN** the Mill's output holds a full stack of Sand
- **THEN** the Mill wants no work and its drivers spend nothing

### Requirement: Mill sides
The Mill SHALL take drive on its back, left and right sides and not on its top, bottom or front. Its item sides SHALL default to Input on the top, back, left and right and Auto on the bottom; its front, where the chute is, SHALL be fixed to Push, so the result leaves through the chute into the block in front and through the bottom, and nothing enters through the chute. Automation SHALL never take the input out or put anything into the output.

#### Scenario: Hopper above, chest in front
- **WHEN** a hopper above a driven Mill holds Gravel and a chest stands in front of the chute
- **THEN** the Gravel goes into the Mill and the Sand ends up in the chest

#### Scenario: Chute refuses input
- **WHEN** a hopper offers Gravel to the Mill's front
- **THEN** nothing goes in

### Requirement: Mill look
The Mill SHALL look like a family steel machine: a heavy iron base, a steel housing, an iron grinding wheel with a brass hub on the housing, a steel hopper with a brass rim on top and a steel chute out of the front. While a driver turns it, the grinding wheel SHALL turn as described in the drive's flywheel motion, also when the Mill has nothing to grind; standing, it SHALL keep its pose. No two overlapping faces of its parts SHALL share a plane.

#### Scenario: Wheel turns
- **WHEN** a Mill is driven and grinding
- **THEN** its grinding wheel turns smoothly, and when the drive stops it slows down to a stop within about a second

#### Scenario: Cranked while empty
- **WHEN** a player holds right click on a Hand Crank on an empty Mill
- **THEN** the grinding wheel turns with the crank, and the Mill grinds nothing

### Requirement: Milling in JEI
When JEI is installed, a Milling category SHALL list every milling recipe with its counted input, its result and its work, with the Mill as catalyst.

#### Scenario: Look up Sand
- **WHEN** the player looks up how to make Sand in JEI
- **THEN** the Milling category shows 1 Gravel into 1 Sand and the Mill

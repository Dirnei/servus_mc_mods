# Spec Delta

## MODIFIED Requirements

### Requirement: Milling recipes
Foundation SHALL provide the recipe type `servus_foundation:milling`: an input ingredient with a count, one result, and the work one milling takes. Recipes SHALL be datapack JSON, SHALL be sent to clients, and any mod or datapack MAY add more. Foundation SHALL ship milling recipes for vanilla items: Gravel into Sand, Stone and Deepslate into their cobble, Cobblestone and Cobbled Deepslate into Gravel, Sandstone and Red Sandstone into their sand, Sugar Cane into Sugar, Glowstone into Glowstone Dust, Bone into Bone Meal, Blaze Rod into Blaze Powder, any Wool into String, Clay into Clay Balls, Block of Quartz into Nether Quartz, Block of Amethyst into Amethyst Shards, and every flower vanilla crafts into a dye into twice that dye. No two shipped milling recipes SHALL take the same item. Foundation SHALL ship no milling recipe for ores, raw ores or gems.

#### Scenario: Shipped recipe
- **WHEN** a world loads with only the Servus mods
- **THEN** a milling recipe turns 1 Gravel into 1 Sand

#### Scenario: Stone to sand chain
- **WHEN** a player mills 1 Stone, then the Cobblestone it gives, then the Gravel that gives
- **THEN** the Mill makes 1 Cobblestone (example work 100), then 1 Gravel (example work 200), then 1 Sand

#### Scenario: Blocks back to their pieces
- **WHEN** a driven Mill holds 1 Glowstone
- **THEN** it makes 4 Glowstone Dust (example work 100)

#### Scenario: More than by hand
- **WHEN** a driven Mill holds 1 Sugar Cane, 1 Bone or 1 Blaze Rod
- **THEN** it makes 2 Sugar, 4 Bone Meal or 3 Blaze Powder, more than the vanilla crafting recipe gives

#### Scenario: Any wool
- **WHEN** a driven Mill holds 1 Red Wool
- **THEN** it makes 4 String

#### Scenario: Flowers give twice the dye
- **WHEN** a driven Mill holds 1 Poppy, and later 1 Rose Bush
- **THEN** it makes 2 Red Dye from the Poppy and 4 Red Dye from the Rose Bush

#### Scenario: No ore recipes
- **WHEN** a player tries to put Iron Ore or a Diamond into the Mill's input
- **THEN** the slot refuses it

#### Scenario: Datapack recipe
- **WHEN** a datapack adds a milling recipe from 2 Netherrack to 1 Gravel with work 400
- **THEN** the Mill accepts Netherrack and makes 1 Gravel from every 2 after 400 work

### Requirement: Mill block
Foundation SHALL provide the Mill, a machine crafted from vanilla and Foundation materials. It SHALL face the player when placed, open a screen with an input slot, a progress arrow, an output slot and the side panel on right click, take into its input only items some milling recipe accepts, and drop itself and its contents when broken. The Configuration Carrot SHALL cycle its sides and dismantle it like other machines.

#### Scenario: Place and open
- **WHEN** a player places a Mill and right clicks it with an empty hand
- **THEN** the Mill's screen opens with input, arrow, output and the side panel

#### Scenario: Wrong item
- **WHEN** a player tries to put Dirt into the input slot and no milling recipe takes Dirt
- **THEN** the slot refuses it

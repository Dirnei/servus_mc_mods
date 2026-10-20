## Purpose

Defines the bakery of Servus: Delicacies: flour from the Foundation Mill, dough, salt, wood ash and lye, the Baking Oven that bakes on the Tiled Stove, and the baked goods with their food values.

## ADDED Requirements

### Requirement: Flour from the Mill
Servus: Delicacies SHALL add Flour and SHALL ship a `servus_foundation:milling` recipe that grinds Wheat into Flour on the Foundation Mill (example: one Wheat into one Flour). The amount of work per milling SHALL come from the recipe data. Flour SHALL be in the tag `c:flours/wheat`.

#### Scenario: Milling wheat
- **WHEN** a driven Mill holds one Wheat
- **THEN** after the recipe's work the Mill holds one Flour and no Wheat

### Requirement: Dough
Servus: Delicacies SHALL add Dough, crafted shapeless from Flour (any item in `c:flours/wheat`) and a Water Bucket, with the empty Bucket left in the grid (example: three Flour and a Water Bucket into three Dough). Dough SHALL be in the tags `c:foods/dough/wheat` and `c:foods/dough` and SHALL NOT be edible.

#### Scenario: Kneading dough
- **WHEN** a player crafts three Flour with a Water Bucket
- **THEN** the player gets three Dough and the empty Bucket stays in the crafting grid

### Requirement: Salt from the Pot
Servus: Delicacies SHALL add Salt in the tag `c:dusts/salt`, and its recipes SHALL ask for that tag so salt from other mods is accepted. The Pot SHALL boil water with no items in it down to Salt (example: 1000 mB of water into one Salt).

#### Scenario: Boiling salt
- **WHEN** a Pot on a lit stove holds 1000 mB of water and no items
- **THEN** after the recipe's time the Pot holds one Salt and no water

#### Scenario: Water waits with an ingredient in it
- **WHEN** a Pot on a lit stove holds 1000 mB of water and two Potatoes
- **THEN** it makes Boiled Potatoes, not Salt

### Requirement: Wood ash and lye
Servus: Delicacies SHALL add Wood Ash in the tag `c:dusts/ash`, which SHALL also list the Alchemy Ash when Servus: Alchemy is installed, without needing it. Logs that burn SHALL cook into Wood Ash on a vanilla Campfire, and Charcoal SHALL smelt into Wood Ash in a Furnace. The Pot SHALL make Lye from an item in `c:dusts/ash` and water (example: one ash and 250 mB of water into one Lye). Lye SHALL NOT be edible.

#### Scenario: Ash from a campfire
- **WHEN** a player puts an Oak Log on a lit Campfire
- **THEN** the Campfire drops one Wood Ash once the log is done

#### Scenario: Lye in the Pot
- **WHEN** a Pot on a lit stove holds one Wood Ash and at least 250 mB of water
- **THEN** after the recipe's time the Pot holds one Lye and 250 mB less water

### Requirement: Baking Oven
Servus: Delicacies SHALL add a Baking Oven, placed like the Pot and the Pan and crafted from iron and a glass pane. It SHALL hold a few input stacks and one output stack and SHALL bake only while the block below it is a hot firebox, by the same matching, timing, hand and automation rules as the Pan, with its own data driven recipe type `servus_delicacies:baking` (counted items, a result and a time in ticks). Breaking it SHALL drop the oven and everything in it.

#### Scenario: Bread rolls on a lit stove
- **WHEN** a Baking Oven on a lit Tiled Stove holds one Dough, with the shipped recipe (example: 200 ticks)
- **THEN** after the recipe's time it holds one Bread Roll and no Dough

#### Scenario: No baking without fire
- **WHEN** a Baking Oven holding Dough stands on a formed stove without fuel, or on any other block
- **THEN** nothing bakes, however long it waits

#### Scenario: Dough by hand and by hopper
- **WHEN** a player right clicks a Baking Oven with Dough, or a hopper above it holds Dough
- **THEN** the Dough goes into the oven; Dirt stays in the hand

#### Scenario: Only the bakes come out
- **WHEN** a hopper below a Baking Oven pulls items while the oven holds Dough and a Bread Roll
- **THEN** only the Bread Roll comes out

### Requirement: Baking Oven look
The Baking Oven SHALL look like a small steel box with an iron door, a window and a brass handle. While it bakes, its window SHALL glow and it SHALL give off a little light.

#### Scenario: Window glows while baking
- **WHEN** a Baking Oven bakes on a lit stove
- **THEN** its window glows and it lights the area a little, and both stop when it stops baking

### Requirement: Baked goods
Servus: Delicacies SHALL ship baking recipes: Dough into a Bread Roll, and Dough with Lye and Salt (any item in `c:dusts/salt`) into Pretzels (example: two Dough, one Lye and one Salt into two Pretzels). The Cutting Board SHALL cut a Bread Roll with a knife into Bread Crumbs (example: two). Bread Roll and Pretzel SHALL be edible with their hunger and saturation from the `servus_delicacies:food` data map and SHALL be in `c:foods/bread`; Bread Crumbs SHALL NOT be edible.

#### Scenario: Pretzels only on a hot stove
- **WHEN** a Baking Oven holds two Dough, one Lye and one Salt on a lit stove
- **THEN** after the recipe's time it holds two Pretzels; on a cold stove it holds nothing new

#### Scenario: Eating a Pretzel
- **WHEN** a hungry player eats a Pretzel with the shipped data (example: 6 hunger, 7.2 saturation)
- **THEN** the player's hunger rises by 6 and the Pretzel is used

#### Scenario: Bread Crumbs
- **WHEN** a player cuts a Bread Roll on the Cutting Board with the Kitchen Knife
- **THEN** the board holds two Bread Crumbs

### Requirement: Bakery in JEI
When JEI is installed, a Baking category SHALL list every baking recipe with its items, result and time, and SHALL show the Baking Oven and the Stove Tile as catalysts. The Pot's salt and lye recipes and the Mill's flour recipe SHALL show in the existing Pot and milling categories.

#### Scenario: Looking up the Pretzel
- **WHEN** a player looks up how to make a Pretzel in JEI
- **THEN** the Baking category shows two Dough, Lye and Salt, the Pretzels and the time

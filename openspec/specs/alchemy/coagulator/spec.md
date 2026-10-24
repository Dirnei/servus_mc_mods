# alchemy/coagulator Specification

## Purpose
The Coagulator joins the three principles again: from a fixed ratio of Salt, Sulfur and Mercury, sometimes with an item, it makes the materials of alchemy and everyday goods.

## Requirements

### Requirement: Coagulation recipes are data
A coagulation recipe SHALL be a datapack recipe of type `servus_alchemy:coagulation` with the mB of Salt, Sulfur and Mercury it needs (each may be 0), at most one item ingredient with a count, an optional `seed` ingredient, one result item with a count and a time in ticks. A recipe without a `seed` SHALL take its own result item as its seed.

#### Scenario: Salt Crystal
- **WHEN** the shipped data holds a recipe of 250 mB Salt for one Salt Crystal in 40 ticks
- **THEN** a Coagulator with a Salt Crystal as its seed and 250 mB Salt uses up the seed and makes two Salt Crystals in 2 seconds

#### Scenario: Like begets like
- **WHEN** a datapack adds a coagulation recipe for Flint without a `seed`
- **THEN** a piece of Flint is the seed that grows it

### Requirement: Coagulator works on time alone
The Coagulator SHALL need no power: no FE buffer, no fuel and no energy capability. The joined principles simply take the recipe's time to cool and set. It SHALL work only while its structure is formed. A cycle SHALL start only when its seed grows a recipe, its tanks hold the recipe's principles, its ingredient slot holds the recipe's item, and its output slot has room for what the seed grows; the principles, the item and one seed SHALL be taken when the cycle completes.

#### Scenario: Missing Mercury
- **WHEN** the seed's recipe needs 50 mB Mercury and the Mercury tank holds 20 mB
- **THEN** the Coagulator waits and keeps no progress

#### Scenario: No power needed
- **WHEN** a formed Coagulator holds 250 mB Salt with a Salt Crystal as its seed and nothing touches it
- **THEN** it makes two Salt Crystals after the recipe's time

#### Scenario: Unformed does nothing
- **WHEN** an unformed Coagulator holds 250 mB Salt with a Salt Crystal as its seed
- **THEN** it makes nothing

### Requirement: Coagulator crafting
The Coagulator SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Coagulator pattern of cobblestone, glass, a cauldron and redstone
- **THEN** one Coagulator is produced

#### Scenario: Iron crafts nothing
- **WHEN** the player lays iron ingots in the corners of the Coagulator pattern
- **THEN** nothing is produced

### Requirement: First alchemical materials
Alchemy SHALL add the items Salt Crystal, Sulfur and Quicksilver, made only in the Coagulator, as the first materials later alchemy builds on. Their shipped recipes SHALL take a first seed from the Purifier as well as their own product: Salt Crystal grows from Kandis or a Salt Crystal, Sulfur from Brimstone or Sulfur, Quicksilver from Cinnabar or Quicksilver.

#### Scenario: Sulfur
- **WHEN** a Coagulator runs the shipped Sulfur recipe
- **THEN** it makes the Sulfur item from Sulfur principle

#### Scenario: First Sulfur from Brimstone
- **WHEN** a formed Coagulator with Brimstone as its seed holds 250 mB Sulfur
- **THEN** it makes one Sulfur and the Brimstone is used up

### Requirement: Buckets pour in
A filled principle bucket or container SHALL pour into the matching tank when the player uses it on the Vat of that principle, and when the player clicks any principle gauge in the GUI with it on the cursor. The player SHALL get the empty container back.

#### Scenario: Salt bucket on the block
- **WHEN** a player uses a Salt bucket on the Salt Vat of a formed Coagulator with room in its Salt tank
- **THEN** the Salt tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Coagulator reports to the journal
The Coagulator SHALL remember the player who formed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `coagulate` event for its result with the result's count.

#### Scenario: Salt Crystal counts
- **WHEN** a Coagulator a player formed makes a Salt Crystal
- **THEN** the player's team gets a `coagulate` event for one Salt Crystal

### Requirement: Nigredo goods
Alchemy SHALL ship coagulation recipes that make the plain goods of Nigredo from the principles alone, without an item and without a chapter: Clay Ball (example 100 mB Salt and 50 mB Mercury) and Bone Meal (example 60 mB Salt and 20 mB Sulfur). The Coagulator's Workshop entry SHALL list them.

#### Scenario: Clay
- **WHEN** a formed Coagulator with a Clay Ball as its seed holds the principles of its recipe
- **THEN** it makes two Clay Balls, and the seed and the principles are used up

#### Scenario: Bone Meal
- **WHEN** a formed Coagulator with Bone Meal as its seed holds the principles of its recipe
- **THEN** it makes two Bone Meal

### Requirement: The seed picks the recipe
The Coagulator SHALL hold seeds of one item in a seed slot and SHALL run the coagulation recipe that the seed belongs to. Without a seed, or with an item that is no recipe's seed, the Coagulator SHALL do nothing. The seeds SHALL stay in the slot across reloads and while the structure is unformed and formed again.

#### Scenario: No seed
- **WHEN** a formed Coagulator without a seed holds 250 mB Salt
- **THEN** it makes nothing and the Salt stays in the tank

### Requirement: Seeds are used up
Each finished cycle SHALL use up one seed. A transmutation seed, one that is not the recipe's result item, SHALL turn into the recipe's result; a seed that is the result item itself SHALL come back with the result, so the cycle gives the result count plus one. When the last seed is used up the Coagulator SHALL stop.

#### Scenario: Kandis turns into Salt Crystals
- **WHEN** a formed Coagulator with 3 Kandis as its seeds holds 1,000 mB Salt
- **THEN** it makes three Salt Crystals, the seed slot is empty and it stops with 250 mB Salt left

#### Scenario: Redstone doubles
- **WHEN** a formed Coagulator with 2 Redstone as its seeds holds the principles of three Redstone cycles
- **THEN** it makes four Redstone, the seed slot is empty and it stops with one cycle of principles left

### Requirement: Taking the seed out stops it
Taking the seed out SHALL stop the Coagulator at once, losing the cycle's progress and keeping the principles, the ingredient and the products.

#### Scenario: Seed taken out
- **WHEN** the player takes the Salt Crystal seed out of a Coagulator that is making Salt Crystals
- **THEN** it stops, and its principles stay in the tanks

### Requirement: One seed, one recipe
Every seed item SHALL belong to at most one coagulation recipe. The shipped data generation SHALL fail when two recipes share a seed item. When loaded recipes share a seed item, the recipe whose id sorts first SHALL keep it, the other recipe SHALL lose that seed item, and an error naming the item and both recipes SHALL be logged.

#### Scenario: Duplicate from a datapack
- **WHEN** a datapack adds `tweaks:coagulation/sugar_cube` with Kandis as its seed
- **THEN** Kandis still grows Salt Crystals (`servus_alchemy:coagulation/salt_crystal` sorts first) and the log names Kandis and both recipes

#### Scenario: Datagen refuses
- **WHEN** the shipped recipe table gives two recipes the seed Salt Crystal
- **THEN** data generation fails and names the item

### Requirement: Locked seeds grow nothing
A coagulation recipe MAY name a journal chapter. The seed of such a recipe SHALL be accepted into the seed slot but SHALL grow nothing until the team of the Coagulator's owner has finished that chapter.

#### Scenario: Locked seed
- **WHEN** a Coagulator whose owner's team has not finished Villach holds the seed of a recipe that names Villach, and the principles for it
- **THEN** it makes nothing and the seed tooltip names Villach

#### Scenario: Chapter finished
- **WHEN** the team finishes Villach
- **THEN** the same Coagulator starts growing

### Requirement: The seed grows in the mold
A formed Coagulator SHALL show its seed lying in the mold under the middle Vat's spout. While a cycle runs, the crystal SHALL grow with the cycle's progress, from its resting size to about twice that, and SHALL be back at its resting size when the product is done. Without a seed the mold SHALL be empty.

#### Scenario: Growing
- **WHEN** a formed Coagulator is halfway through a Salt Crystal cycle
- **THEN** the seed crystal in the mold is about one and a half times its resting size

#### Scenario: Waiting
- **WHEN** a formed Coagulator has a seed but no principles
- **THEN** the seed lies in the mold at its resting size

### Requirement: Coagulator entry explains seeds
The journal's Coagulator entry SHALL explain that the seed decides what grows and how seeds are used up, and SHALL list, from the same data that generates the recipes, every shipped seed that grows something other than itself (such as "Kandis grows Salt Crystal"), saying that every other product grows from a crystal of itself. Formatting the lines SHALL be pure Java covered by JUnit.

#### Scenario: Transmutations listed
- **WHEN** the player reads the Coagulator entry
- **THEN** a page lists "Kandis grows Salt Crystal", "Brimstone grows Sulfur" and "Cinnabar grows Quicksilver"

### Requirement: Coagulator seed GUI
The Coagulator GUI SHALL show the seed slot, the ingredient and output slots, progress and the three principle gauges; it has no side panel, no recipe list and no select button. It SHALL open with an empty hand on a formed Coagulator. Hovering the seed slot SHALL show what the seed grows, that one is used up per cycle and what it gives, the principles and item that needs and, for a locked recipe, the journal chapter it waits for; hovering the empty seed slot SHALL say that seeds belong there.

#### Scenario: Seed tooltip
- **WHEN** the player hovers a Kandis in the seed slot
- **THEN** the tooltip says one is used up per cycle and gives one Salt Crystal from 250 mB Salt

#### Scenario: Self seed tooltip
- **WHEN** the player hovers a Salt Crystal in the seed slot
- **THEN** the tooltip says one is used up per cycle and gives two Salt Crystals

#### Scenario: Empty seed slot
- **WHEN** the player hovers the empty seed slot
- **THEN** the tooltip says to put seeds of what should grow there

### Requirement: The seed slot holds one kind
The seed slot SHALL take a stack of one seed item, up to its stack size, and SHALL refuse another item while it holds one.

#### Scenario: A stack of seeds
- **WHEN** the player shift clicks 20 Salt Crystals into a Coagulator without a seed
- **THEN** all 20 go into the seed slot

#### Scenario: Another seed refused
- **WHEN** the player shift clicks Kandis into a Coagulator whose seed slot holds Salt Crystals
- **THEN** the Kandis stay in the inventory

# alchemy/purifier Specification

## Purpose
Salt leaves the Calciner as Ash, the way alchemists found it in the fire, and the Purifier, a small build of a leaching vat over a catch basin and a residue crock, washes the Ash with water into the liquid Salt that the Coagulator and the Athanor use, leaving Caput Mortuum behind.

## Requirements

### Requirement: Ash
Alchemy SHALL add the item Ash, which the Calciner's Ash Pan gives. It SHALL have no crafting recipe of its own.

#### Scenario: Ash from the Calciner
- **WHEN** a Calciner has made Salt and a player empties its Ash Pan
- **THEN** the player holds Ash

### Requirement: Caput Mortuum
Alchemy SHALL add the item Caput Mortuum, the residue the Purifier leaves after washing Ash. It SHALL have no use in this change; a later stage turns it into carbon and diamond.

#### Scenario: Residue
- **WHEN** a Purifier finishes washing 4 Ash
- **THEN** one Caput Mortuum lies in its Residue Crock

### Requirement: Purification recipes are data
A purification recipe SHALL be a datapack recipe of type `servus_alchemy:purification` with one counted item ingredient, the mB of water it needs, the mB of Salt it makes, an optional residue item with count and a time in ticks. The Salt MAY be 0: such a recipe is a wash that only turns its items into the residue. The Salt one item makes in the first recipe that takes Ash SHALL also be the Salt the Ash Pan collects per Ash.

#### Scenario: Shipped recipe
- **WHEN** the shipped data holds a recipe of 4 Ash and 400 mB water for 400 mB Salt and one Caput Mortuum in 160 ticks
- **THEN** a formed Purifier with 4 Ash and 400 mB water makes 400 mB Salt and one Caput Mortuum in 8 seconds

#### Scenario: A wash makes no Salt
- **WHEN** a formed Purifier with an empty Salt tank washes Red Concrete Powder
- **THEN** the Salt tank stays empty and Red Concrete lies in the Residue Crock

#### Scenario: Ash Pan unchanged
- **WHEN** a Calciner fills its Ash Pan with the shipped recipes
- **THEN** one Ash still stands for 100 mB Salt

### Requirement: Purifier layout
The Purifier structure SHALL be one block deep, 3 wide and 2 tall, seen from its front: the Leaching Vat (the Purifier block) in the middle of the top row with nothing required beside it, a log of any wood below the Vat, and beside the log the Catch Basin on one side and the Residue Crock on the other. The Basin and the Crock MAY stand on either side.

#### Scenario: Layout from the front
- **WHEN** a player looks at a complete Purifier structure
- **THEN** they see, top to bottom: `_ V _`, `B L R` (V Leaching Vat, B Catch Basin, L log, R Residue Crock, `_` anything)

#### Scenario: Mirrored build
- **WHEN** a player builds `_ V _` over `R L B` and right clicks the Vat with the Journal
- **THEN** the Purifier forms

### Requirement: Forming the Purifier
Right clicking the Leaching Vat with Paracelsus' Journal SHALL check the structure along both horizontal axes and form it along the first that matches, making the player its owner, and SHALL NOT open the book; when nothing matches it SHALL name the first missing block. Breaking the Vat, the Basin, the Crock or the log of a formed Purifier SHALL unform it. Only a formed Purifier SHALL work.

#### Scenario: Formed
- **WHEN** a player right clicks the Vat of a complete Purifier with the Journal
- **THEN** the Purifier forms and the player sees "The Purifier is ready."

#### Scenario: Log broken
- **WHEN** a player breaks the log of a formed Purifier
- **THEN** it unforms and stops

### Requirement: The Purifier leaches
The Leaching Vat SHALL hold one item slot for the ingredient, a water tank, a Salt tank and a slot for the residue (tank capacities from machine stats, example 4,000 mB). It SHALL start a cycle only when formed, when it holds the recipe's items and water, the Salt tank has room for the result and the residue slot has room, and SHALL take the items and the water and add the Salt and the residue when the cycle completes. It SHALL need no power and no fuel.

#### Scenario: Leaching
- **WHEN** a formed Purifier holds 8 Ash and 1,000 mB water
- **THEN** after two cycles it holds 800 mB Salt, 200 mB water, two Caput Mortuum and no Ash

#### Scenario: No water
- **WHEN** a formed Purifier holds Ash but no water
- **THEN** it makes nothing

### Requirement: Purifier ports
A formed Purifier SHALL take only its ingredient through the item capability and accept only water through the fluid capability on every face of the Vat, and SHALL give nothing there. It SHALL give only Salt through the fluid capability on every face of the Catch Basin and push its Salt into touching fluid handlers outside the build. It SHALL give only the residue through the item capability on every face of the Residue Crock. The log SHALL expose nothing.

#### Scenario: Pump and tank
- **WHEN** a Fluid Pipe brings water from a pump to the Vat and another pipe leads from the Basin to a Fluid Tank
- **THEN** the water flows into the Vat and the Salt flows out of the Basin to the tank

#### Scenario: Residue by hopper
- **WHEN** a hopper stands under the Crock of a formed Purifier that has made two Caput Mortuum
- **THEN** the hopper holds two Caput Mortuum

#### Scenario: Salt refused
- **WHEN** a pipe offers Salt to the Vat
- **THEN** the Vat accepts none

### Requirement: The Purifier by hand
On a formed Purifier, using an item that a purification recipe takes on the Vat SHALL put it into the item slot; a water bucket on the Vat SHALL pour its water in; an empty bucket on the Basin SHALL fill with Salt when the Salt tank holds enough; an empty hand on the Crock SHALL give the residue; sneaking with an empty hand on the Vat SHALL give the ingredient back. The Purifier SHALL have no GUI.

#### Scenario: Water by bucket
- **WHEN** a player uses a water bucket on the Vat of a formed Purifier with an empty water tank
- **THEN** the water tank holds 1,000 mB and the player holds an empty bucket

#### Scenario: Salt by bucket
- **WHEN** a player uses an empty bucket on the Basin of a formed Purifier holding 1,000 mB Salt
- **THEN** the player holds a Salt bucket and the Salt tank is empty

#### Scenario: Residue by hand
- **WHEN** a player uses the Crock with an empty hand and the Purifier holds two Caput Mortuum
- **THEN** the player receives the two Caput Mortuum

#### Scenario: Wool by hand
- **WHEN** a player uses Blue Wool on the Vat of a formed Purifier
- **THEN** the Blue Wool goes into the Vat

### Requirement: The Purifier shows its work
The Vat SHALL show the items still to wash as a layer on its floor and the water above it; the Basin SHALL show the Salt at the height of what the Salt tank holds; the Crock SHALL show a heap while it holds any residue. The layer and the heap SHALL show the texture of the item's block when the item is a block, and grey ash and dark residue otherwise. With Jade installed, looking at the Vat, the Basin or the Crock SHALL show the items by name and count, the water, the Salt and the progress.

#### Scenario: Salt level
- **WHEN** a formed Purifier holds 2,000 mB Salt
- **THEN** the Basin shows Salt at half its height

#### Scenario: Concrete heap
- **WHEN** a formed Purifier has washed Red Concrete Powder into Red Concrete
- **THEN** the Crock shows a heap with the Red Concrete texture

#### Scenario: Jade names the residue
- **WHEN** a player with Jade looks at the Crock of a Purifier holding two Leather
- **THEN** the tooltip says Leather: 2

### Requirement: Formed Purifier looks like one build
A formed Vat SHALL show spouts running out of its sides down into the Basin and the Crock, and the formed Basin and Crock SHALL show formed models facing the Vat, so the build reads as one object. The pieces SHALL follow the build's axis and side, and the brine, Salt and residue SHALL stay visible. Unformed, the Vat, the Basin and the Crock SHALL show their plain models.

#### Scenario: Formed look
- **WHEN** a player forms a Purifier
- **THEN** the Vat shows spouts down to the Basin and the Crock, and the Basin and the Crock show their formed models facing the Vat

#### Scenario: Unformed look
- **WHEN** a player breaks the log of a formed Purifier
- **THEN** the Vat, the Basin and the Crock show their plain models

### Requirement: Purifier schematic
The journal's Purifier entry SHALL show the structure as a multiblock page with Visualize, standing on the clicked ground, and the crafting recipes of the Vat, the Basin and the Crock.

#### Scenario: Visualize
- **WHEN** a player presses Visualize on the Purifier page and clicks the ground
- **THEN** the ghost of the Purifier stands on that ground

### Requirement: Purifier reports to the journal
The Purifier SHALL remember the player who formed it as its owner and SHALL report every finished cycle that made Salt to the journal of the owner's team as a `purify` event with the Salt it made. A wash that makes no Salt SHALL report nothing.

#### Scenario: First purification
- **WHEN** a player's Purifier finishes its first cycle while Villach is open
- **THEN** the Villach task "purify Ash" is done

#### Scenario: A wash counts nothing
- **WHEN** a player's Purifier washes White Concrete Powder while Villach is open
- **THEN** the Villach task "purify Ash" stays open

### Requirement: Purifier crafting
The Leaching Vat, the Catch Basin and the Residue Crock SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Vat pattern of planks, a cauldron and a hopper
- **THEN** one Purifier (Leaching Vat) is produced

### Requirement: Seeds from the Purifier
Alchemy SHALL add the items Kandis (rock sugar crystal), Brimstone and Cinnabar. They SHALL have no crafting recipe; only the Purifier makes them, as the residue of shipped purification recipes: Sugar gives a little Salt and Kandis, Gunpowder gives Salt and Brimstone, Redstone Dust gives a little Salt and Cinnabar (amounts are datapack examples, such as 8 Sugar and 500 mB water for 100 mB Salt and one Kandis).

#### Scenario: Kandis from Sugar
- **WHEN** a formed Purifier holds 8 Sugar and 500 mB water
- **THEN** after the recipe's time it holds 100 mB Salt and one Kandis lies in the Crock

#### Scenario: No crafting recipe
- **WHEN** a player looks up Brimstone in JEI
- **THEN** the only way shown to make it is the Purification category

### Requirement: Salt from everyday matter
Alchemy SHALL ship purification recipes that wash Salt out of common matter: Dried Kelp gives Salt and no residue, as the good early Salt source; Rotten Flesh gives Salt and Leather; Soul Sand gives a little Salt and Sand; Magma Cream gives a Slimeball and a little Salt. The amounts are datapack examples. No shipped purification recipe SHALL take Ash except the Ash recipe, and none SHALL wash ore.

#### Scenario: Kelp
- **WHEN** a formed Purifier holds 4 Dried Kelp and 250 mB water
- **THEN** it makes Salt and leaves no residue

#### Scenario: Leather
- **WHEN** a formed Purifier washes Rotten Flesh
- **THEN** it makes Salt and Leather lies in the Crock

### Requirement: Washing out colour
Alchemy SHALL ship one wash per colour, without Salt: each of the 16 Concrete Powders gives its Concrete, each dyed Wool gives White Wool, each dyed Carpet gives White Carpet and each dyed Terracotta gives plain Terracotta. They SHALL be generated per colour, so a datapack can remove or change each one alone.

#### Scenario: Concrete
- **WHEN** a formed Purifier holds 8 Red Concrete Powder and 250 mB water
- **THEN** after the recipe's time 8 Red Concrete lie in the Crock and the Salt tank is unchanged

#### Scenario: Wool
- **WHEN** a formed Purifier washes 8 Blue Wool
- **THEN** 8 White Wool lie in the Crock

#### Scenario: Plain already
- **WHEN** a player offers White Wool or plain Terracotta to the Vat
- **THEN** the Vat refuses it

### Requirement: Purifier entry lists its recipes
The journal's Purifier entry SHALL list every shipped purification recipe from the same data that generates the recipes: one line per recipe with its items, water, Salt and residue, and the colour washes as one line per kind (Concrete Powder, Wool, Carpet, Terracotta). Formatting the lines SHALL be pure Java covered by JUnit.

#### Scenario: Seed line
- **WHEN** the player reads the Purifier entry
- **THEN** a page lists "8 Sugar, 500 mB water: 100 Salt, 1 Kandis"

#### Scenario: Colour line
- **WHEN** the player reads the Purifier entry
- **THEN** one line says that Concrete Powder of any colour washes into its Concrete, instead of 16 lines

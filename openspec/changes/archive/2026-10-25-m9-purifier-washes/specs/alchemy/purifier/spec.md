## MODIFIED Requirements

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

### Requirement: Purifier reports to the journal
The Purifier SHALL remember the player who formed it as its owner and SHALL report every finished cycle that made Salt to the journal of the owner's team as a `purify` event with the Salt it made. A wash that makes no Salt SHALL report nothing.

#### Scenario: First purification
- **WHEN** a player's Purifier finishes its first cycle while Villach is open
- **THEN** the Villach task "purify Ash" is done

#### Scenario: A wash counts nothing
- **WHEN** a player's Purifier washes White Concrete Powder while Villach is open
- **THEN** the Villach task "purify Ash" stays open

## ADDED Requirements

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

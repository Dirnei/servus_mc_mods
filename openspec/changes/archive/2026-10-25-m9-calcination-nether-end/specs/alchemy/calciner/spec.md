## MODIFIED Requirements

### Requirement: Calcination recipes are data
A calcination recipe SHALL be a datapack recipe of type `servus_alchemy:calcination` with one ingredient (an item or a tag), the mB of Salt, Sulfur and Mercury it gives (each may be 0), a time in ticks and an optional `dimension` naming where its matter comes from: `overworld`, `nether` or `end`, `overworld` when left out. The dimension SHALL NOT change what the Calciner does. Any matching item SHALL be accepted; an item without a recipe SHALL be refused.

#### Scenario: Cobblestone
- **WHEN** the shipped data holds a recipe for `#c:cobblestones` giving 100 mB Salt and 10 mB Mercury in 80 ticks
- **THEN** a Calciner turns one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds

#### Scenario: Unknown item
- **WHEN** a hopper offers a diamond and no calcination recipe matches diamonds
- **THEN** the Calciner takes nothing

#### Scenario: Dimension left out
- **WHEN** a datapack recipe has no `dimension` field
- **THEN** it loads as an `overworld` recipe

#### Scenario: Unknown dimension
- **WHEN** a datapack recipe names the dimension `aether`
- **THEN** that recipe fails to load with an error in the log and the other recipes load

## ADDED Requirements

### Requirement: The most specific recipe wins
When more than one calcination recipe matches an item, the Calciner SHALL use the most specific one: a recipe whose ingredient lists items directly beats one that uses a tag; among recipes of the same kind the one whose ingredient matches fewer items wins, so a tag that is a subset of another matching tag beats the bigger tag; a remaining tie goes to the lower recipe id. The choice SHALL NOT depend on the order recipes load in.

#### Scenario: Nether Wart
- **WHEN** the shipped data holds a recipe for `#c:crops` and one for `#c:crops/nether_wart`
- **THEN** a Calciner burns Nether Wart with the Nether Wart recipe and wheat with the Crops recipe

#### Scenario: Item beats tag
- **WHEN** a datapack adds a recipe for the item `minecraft:oak_log` next to the shipped `#minecraft:logs` recipe
- **THEN** an Oak Log burns with the new recipe and a Birch Log with the Logs recipe

#### Scenario: Load order
- **WHEN** the same two overlapping recipes are loaded in the opposite order
- **THEN** the Calciner picks the same recipe for each item

#### Scenario: Count of the winner
- **WHEN** the most specific recipe for an item needs 4 of it and the Calciner holds 2
- **THEN** the Calciner waits for more and does not fall back to a less specific recipe

### Requirement: Nether and End matter burns
Alchemy SHALL ship calcination recipes for the common matter of the Nether, marked `nether` and leaning to Sulfur, and of the End, marked `end` and leaning to Mercury. Every shipped recipe without a mark is `overworld`. Dragon's Breath SHALL NOT be calcinable.

#### Scenario: Nether matter
- **WHEN** the shipped data loads
- **THEN** Netherrack, Soul Sand, Soul Soil, Nether Wart, Nether Wart Blocks, Crimson and Warped Stems, Blaze Rods, Magma Cream, Glowstone Dust and Nether Quartz each have a `nether` calcination recipe, for example Blaze Rod giving 400 mB Sulfur

#### Scenario: Nether Wart Block
- **WHEN** a Nether Wart Block goes into the Calciner
- **THEN** it burns into what the nine Nether Wart it is crafted from give, for example 540 mB Sulfur and 180 mB Mercury

#### Scenario: End matter
- **WHEN** the shipped data loads
- **THEN** End Stone, Purpur Blocks, Chorus Fruit, Popped Chorus Fruit, Ender Pearls and Shulker Shells each have an `end` calcination recipe, for example Ender Pearl giving 300 mB Mercury

#### Scenario: Stems over logs
- **WHEN** a Crimson Stem goes into the Calciner
- **THEN** it burns with the Crimson Stems recipe, not with the Logs recipe

#### Scenario: Dragon's Breath
- **WHEN** a hopper offers Dragon's Breath to a Calciner
- **THEN** the Calciner takes nothing

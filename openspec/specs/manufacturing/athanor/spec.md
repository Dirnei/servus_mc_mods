# manufacturing/athanor Specification

## Purpose
The Athanor is the alchemist's furnace and the heart of the lab: the important crafts of Manufacturing happen on it, fed with principles, and the structure built around it marks how far a team is in the Great Work.

## Requirements

### Requirement: Athanor crafts are data
An Athanor craft SHALL be a datapack recipe of type `servus_manufacturing:athanor` with one counted item input, the principles it takes, a minimum tier (default 0), one result and a time in ticks. The item in the Athanor's input slot SHALL pick the craft.

#### Scenario: Blaze Powder
- **WHEN** the shipped data holds a craft of one gunpowder and 200 mB Sulfur for one Blaze Powder in 100 ticks at tier 0
- **THEN** an Athanor with gunpowder on it and 200 mB Sulfur in its tank makes one Blaze Powder in 5 seconds

### Requirement: The Athanor burns principles
A craft SHALL run only while the Athanor's tier is at least the craft's tier, its tanks hold the craft's principles, the input holds the item and the output has room. The principles and the item SHALL be taken when the craft completes. The Athanor SHALL need no energy and SHALL show a burning flame while it crafts.

#### Scenario: Too low a tier
- **WHEN** a glass bottle lies on an Athanor without a Salt Crystal ring and the Mercury is there
- **THEN** no Bottle o' Enchanting is made

### Requirement: Principles by pipe or bucket
The Athanor SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB), SHALL accept each principle only into its own tank through the fluid capability on every side except the top, and SHALL take a principle bucket on right click, giving an empty bucket back. It SHALL never give fluid away.

#### Scenario: Bucket of Sulfur
- **WHEN** a player right clicks the Athanor with a Sulfur bucket
- **THEN** the Sulfur tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_manufacturing/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0.

#### Scenario: Albedo ring
- **WHEN** eight Salt Crystal Blocks surround an Athanor on its own level
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

### Requirement: Tier is checked on interaction and slowly
The Athanor SHALL check its tier when it is placed, when a player uses it, when a craft starts, and otherwise at a slow interval (example every 5 seconds). It SHALL never check every tick. The tier SHALL show as the flame colour and in the GUI.

#### Scenario: Ring placed
- **WHEN** a player completes the Salt Crystal ring and right clicks the Athanor
- **THEN** the GUI shows Albedo and the flame turns white

### Requirement: The item rests on top
The item in the input slot SHALL be drawn lying on top of the Athanor, and a player SHALL be able to put an item there by using it on the Athanor and take it back with an empty hand while sneaking.

#### Scenario: Place gunpowder
- **WHEN** a player uses gunpowder on the Athanor
- **THEN** the gunpowder goes into the input slot and shows on top of the block

### Requirement: Athanor production
The Athanor SHALL expose the production capability with one recipe per craft its current tier allows: the item in, the result out, the time; principles are expected by pipe or bucket.

#### Scenario: Tier filters crafts
- **WHEN** the production capability of a tier 0 Athanor is queried
- **THEN** it reports Blaze Powder but not the Bottle o' Enchanting

### Requirement: Salt Crystal Block
Manufacturing SHALL add a Salt Crystal Block, crafted from nine Salt Crystals and back into nine.

#### Scenario: Block recipe
- **WHEN** the player crafts nine Salt Crystals
- **THEN** one Salt Crystal Block is produced

### Requirement: Athanor crafting
The Athanor SHALL be crafted at a crafting table from bricks, a blast furnace and a cauldron without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Athanor pattern
- **THEN** one Athanor is produced

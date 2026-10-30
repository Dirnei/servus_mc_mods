# alchemy/recipe-viewer Specification

## Purpose

Recipe viewers show how Alchemy items are made so players never have to guess at assembler inputs, times or machine recipes.

## Requirements

### Requirement: Works without JEI
Servus Alchemy SHALL load and work with JEI absent.

#### Scenario: No JEI
- **WHEN** the game runs without JEI
- **THEN** no error is logged and all machines work as before

### Requirement: Calcination recipes appear in JEI
When JEI is installed, every calcination recipe SHALL appear in a "Calcination" category showing the ingredient, the mB of each principle it gives and the time in seconds. The Calciner SHALL be listed as the category's machine.

#### Scenario: Look up Salt
- **WHEN** the player looks up the uses of cobblestone in JEI
- **THEN** the Calcination category shows cobblestone giving Salt and Mercury with their amounts and the time

### Requirement: Coagulation recipes appear in JEI
When JEI is installed, every coagulation recipe SHALL appear in a "Coagulation" category showing the principles with their mB, the item ingredient if any, the seed items that grow it, each saying that it is used up per cycle and what it gives, the result and the time in seconds. A recipe that grows only from its own result SHALL show the result with one more, the seed that comes back. The Coagulator SHALL be listed as the category's machine.

#### Scenario: Look up Gunpowder
- **WHEN** the player looks up how gunpowder is made in JEI
- **THEN** the Coagulation category shows the gunpowder recipe with its principles, gunpowder as its seed, two gunpowder as its result and the time

#### Scenario: Uses of Kandis
- **WHEN** the player looks up the uses of Kandis in JEI
- **THEN** the Coagulation category shows the Salt Crystal recipe with Kandis among its seeds

### Requirement: Athanor crafts appear in JEI
When JEI is installed, every Athanor craft SHALL appear in an "Athanor" category showing the item, the principles with their mB, the result, the time and the minimum tier. The Athanor SHALL be listed as the category's machine.

#### Scenario: Look up Blaze Powder
- **WHEN** the player looks up how Blaze Powder is made in JEI
- **THEN** the Athanor category shows gunpowder, 200 mB Sulfur, the result, the time and tier Nigredo

### Requirement: Calcination shows what a recipe really burns
The input slot of a calcination recipe in JEI SHALL show only the items the Calciner burns with that recipe; items that a more specific recipe takes SHALL appear only in that recipe. A recipe that takes no item at all, because more specific recipes take every one of its items, SHALL NOT be listed.

#### Scenario: Nether Wart
- **WHEN** the player looks up the uses of Nether Wart in JEI
- **THEN** the Calcination category shows the Nether Wart recipe and not the Crops recipe

#### Scenario: Crops recipe
- **WHEN** the player looks at the Crops calcination recipe in JEI
- **THEN** its input slot cycles through wheat, carrots and the other crops but never shows Nether Wart

### Requirement: Purification recipes appear in JEI
When JEI is installed, every purification recipe SHALL appear in a "Purification" category showing the item with its count, the water with its mB, the Salt with its mB when the recipe makes any, the residue with its count when the recipe leaves one, and the time in seconds. The Purifier SHALL be listed as the category's machine.

#### Scenario: Look up Kandis
- **WHEN** the player looks up how Kandis is made in JEI
- **THEN** the Purification category shows Sugar and water giving Salt and Kandis, with the time

#### Scenario: Wash without Salt
- **WHEN** the player looks up the uses of Red Concrete Powder in JEI
- **THEN** the Purification category shows it and water giving Red Concrete, with no Salt slot

### Requirement: Putrefaction recipes appear in JEI
When JEI is installed, every putrefaction recipe SHALL appear in a "Putrefaction" category showing the rot with its count, the result with its count, the time in seconds in a full bed of fresh straw and in an empty bed, and the shipped bedding items. The Cucurbit SHALL be listed as the category's machine, with the Dung Bed and the Bed Hatch beside it.

#### Scenario: Look up Humus
- **WHEN** the player looks up how Humus is made in JEI
- **THEN** the Putrefaction category shows the putrefiable items cycling with count 8, 1 Humus, 60 s warm and 240 s cold

#### Scenario: Uses of Rotten Flesh
- **WHEN** the player looks up the uses of Rotten Flesh in JEI
- **THEN** the Putrefaction category is among them next to the Purification category

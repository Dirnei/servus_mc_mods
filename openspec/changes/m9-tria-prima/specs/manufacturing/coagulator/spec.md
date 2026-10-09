# Spec Delta

## Purpose

The Coagulator joins the three principles again: from a fixed ratio of Salt, Sulfur and Mercury, sometimes with an item, it makes the materials of alchemy and everyday goods.

## ADDED Requirements

### Requirement: Coagulation recipes are data
A coagulation recipe SHALL be a datapack recipe of type `servus_manufacturing:coagulation` with the mB of Salt, Sulfur and Mercury it needs (each may be 0), at most one item ingredient with a count, one result item with a count and a time in ticks.

#### Scenario: Salt Crystal
- **WHEN** the shipped data holds a recipe of 250 mB Salt for one Salt Crystal in 40 ticks
- **THEN** a Coagulator with that recipe selected and 250 mB Salt makes one Salt Crystal in 2 seconds

### Requirement: The player picks the recipe
The Coagulator SHALL run exactly one selected coagulation recipe. Its GUI SHALL list every coagulation recipe with its result and needs, and clicking one SHALL select it. The selection SHALL persist and survive dismantling with the carrot. Without a selection the Coagulator SHALL do nothing.

#### Scenario: Switch recipe
- **WHEN** the player clicks the Gunpowder recipe in a Coagulator that was making Salt Crystals
- **THEN** the Coagulator makes Gunpowder from then on and the selection survives a world reload

### Requirement: Coagulator works on energy
The Coagulator SHALL be a powered machine following the shared machine rules: an FE buffer, energy spent only while it works, progress kept without energy. A cycle SHALL start only when its tanks hold the recipe's principles, its ingredient slot holds the recipe's item, and its output slot has room; the principles and the item SHALL be taken when the cycle completes.

#### Scenario: Missing Mercury
- **WHEN** the selected recipe needs 50 mB Mercury and the Mercury tank holds 20 mB
- **THEN** the Coagulator waits without spending energy

### Requirement: Each principle into its own tank
The Coagulator SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB) and SHALL accept through the fluid capability only Salt into the Salt tank, Sulfur into the Sulfur tank and Mercury into the Mercury tank, on every side that allows insertion. It SHALL never give fluid away.

#### Scenario: Mixed pipe
- **WHEN** a Fluid Pipe offers Salt and then water to a Coagulator
- **THEN** the Salt goes into the Salt tank and the water is refused

### Requirement: Coagulator GUI
The Coagulator GUI SHALL show the ingredient and output slots, progress, energy bar, the three principle gauges, the recipe list with the selected recipe marked, and the shared side panel.

#### Scenario: Selected recipe marked
- **WHEN** the player opens a Coagulator making Salt Crystals
- **THEN** the Salt Crystal entry in the list is highlighted

### Requirement: Coagulator crafting
The Coagulator SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Coagulator pattern of iron ingots, glass, a cauldron and redstone
- **THEN** one Coagulator is produced

### Requirement: First alchemical materials
Manufacturing SHALL add the items Salt Crystal, Sulfur and Quicksilver, made only in the Coagulator, as the first materials later alchemy builds on.

#### Scenario: Sulfur
- **WHEN** a Coagulator runs the shipped Sulfur recipe
- **THEN** it makes the Sulfur item from Sulfur principle

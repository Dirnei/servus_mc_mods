# Spec Delta

## Purpose

The Calciner is the first alchemical machine: it burns ordinary matter into its Salt, Sulfur and Mercury, the principles every later product is made from.

## ADDED Requirements

### Requirement: Calcination recipes are data
A calcination recipe SHALL be a datapack recipe of type `servus_manufacturing:calcination` with one ingredient (an item or a tag), the mB of Salt, Sulfur and Mercury it gives (each may be 0) and a time in ticks. Any matching item SHALL be accepted; an item without a recipe SHALL be refused.

#### Scenario: Cobblestone
- **WHEN** the shipped data holds a recipe for `#c:cobblestones` giving 100 mB Salt and 10 mB Mercury in 80 ticks
- **THEN** a Calciner turns one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds

#### Scenario: Unknown item
- **WHEN** a hopper offers a diamond and no calcination recipe matches diamonds
- **THEN** the Calciner takes nothing

### Requirement: Calciner works on energy
The Calciner SHALL be a powered machine following the shared machine rules: one input slot, an FE buffer, `energy_per_tick` spent only while it works, progress kept without energy. It SHALL start a cycle only when each of its three principle tanks has room for the whole output of the recipe.

#### Scenario: Tanks full
- **WHEN** the Salt tank of a Calciner cannot take the 100 mB the next cobblestone would give
- **THEN** the Calciner waits without spending energy until the Salt is drained

### Requirement: Principles leave through the sides
The Calciner SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB) and SHALL offer all three through the fluid capability on every side that allows extraction, push them into touching fluid handlers through Auto and Push sides, and never accept fluid.

#### Scenario: Tank beside the Calciner
- **WHEN** a Fluid Tank touches an Auto side of a working Calciner
- **THEN** the principles the Calciner makes flow into the tank (a single tank keeps the first principle it receives)

#### Scenario: Pipe drains one principle
- **WHEN** a Fluid Pipe takes fluid from the Calciner and only Sulfur is wanted downstream
- **THEN** the Calciner hands out Sulfur and keeps its Salt and Mercury

### Requirement: Calciner GUI
The Calciner GUI SHALL show its input slot, progress, energy bar, the three principle gauges with amount and capacity on hover, and the shared side panel.

#### Scenario: Gauges
- **WHEN** the player hovers the Salt gauge of a Calciner holding 300 mB of Salt
- **THEN** the tooltip reads 300 / 4000 mB Salt

### Requirement: Calciner crafting
The Calciner SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Calciner pattern of bricks, iron ingots, a furnace and a cauldron
- **THEN** one Calciner is produced

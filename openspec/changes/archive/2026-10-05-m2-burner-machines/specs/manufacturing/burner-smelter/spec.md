# Spec Delta

## Purpose

The Burner Smelter turns raw ore into ingots at a fixed rate so production can be planned.

## ADDED Requirements

### Requirement: Smelts vanilla smelting recipes at a fixed rate
A Burner Smelter SHALL accept any item that has a vanilla smelting recipe in its input slot and SHALL produce that recipe's result once per work period while fueled, independent of the recipe's own cooking time.

#### Scenario: Raw iron
- **WHEN** raw iron is in the input slot and coal in the fuel slot
- **THEN** one iron ingot appears in the output slot every 4 seconds (80 ticks by default)

#### Scenario: Item without smelting recipe
- **WHEN** a stick is offered to the input slot by automation
- **THEN** it is rejected

#### Scenario: Output mismatch
- **WHEN** the output slot holds copper ingots and the input is raw iron
- **THEN** the smelter waits until the output slot is emptied

### Requirement: Fuel handling matches the extractor
The smelter SHALL burn furnace fuel exactly like the Burner Extractor: one item per burn, only when work is available, progress paused without fuel.

#### Scenario: No input
- **WHEN** the input slot is empty
- **THEN** no fuel is consumed

### Requirement: Crafting
The Burner Smelter SHALL be craftable from vanilla materials early in the game.

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots, bricks and a furnace in the smelter pattern
- **THEN** the result is one Burner Smelter

# Spec Delta

## Purpose

The Burner Extractor is the first machine: it sits on a deposit and turns fuel into the deposit's yield forever.

## ADDED Requirements

### Requirement: Extracts from the deposit below
A Burner Extractor SHALL produce the yield of the deposit block directly below it once per work period while it has fuel and the output slot can take the yield. Without a deposit below it SHALL do nothing and burn no fuel.

#### Scenario: On iron deposit
- **WHEN** an extractor stands on an iron deposit with coal in its fuel slot
- **THEN** one raw iron appears in its output slot every 4 seconds (80 ticks by default, retunable in data)

#### Scenario: No deposit
- **WHEN** an extractor stands on dirt with coal in its fuel slot
- **THEN** no fuel is consumed and nothing is produced

#### Scenario: Output full
- **WHEN** the output slot holds a full stack of raw iron
- **THEN** the extractor pauses and does not burn new fuel until space is available

### Requirement: Fuel handling
The extractor SHALL burn any item vanilla furnaces accept as fuel, consuming one item when the previous burn ends and work is available. Burn time follows the item's furnace burn time.

#### Scenario: Coal
- **WHEN** one coal is in the fuel slot and work is available
- **THEN** the extractor runs for 80 seconds on it (1600 ticks), the vanilla coal burn time

#### Scenario: Fuel runs out
- **WHEN** the burn ends and the fuel slot is empty
- **THEN** progress stops and resumes from the same point when fuel is added

### Requirement: Crafting
The Burner Extractor SHALL be craftable from vanilla materials early in the game.

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots, cobblestone and a furnace in the extractor pattern
- **THEN** the result is one Burner Extractor

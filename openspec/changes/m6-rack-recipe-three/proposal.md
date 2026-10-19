# Proposal

## Why

A warehouse needs many rack bays, and one rack per four iron ingots and three planks makes the first warehouse feel expensive next to rails (a batch per craft). The user asked on 2026-10-20 that the rack recipe yields three racks.

## What Changes

This change is a follow-up to PLAN.md milestone **M6 Warehouse automation** (Heavy Duty Rack), as a new item `m6-rack-recipe-three`.

- The Heavy Duty Rack recipe yields 3 racks instead of 1. Pattern and ingredients stay: iron ingots in the corners, three planks across the middle.
- All three racks remember the wood of the planks, as the single rack did.
- The docs game data (Balance lab) follows from `runData`.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `shuttles/heavy-duty-rack`: the crafting scenario yields three racks.

## Non-goals

- Changing the pattern, the ingredients or any other recipe.
- Rebalancing rack slots or section size.

## Impact

- Data, Shuttles: `datagen/ServusShuttlesRecipeProvider.java` (result count), regenerated `data/servus_shuttles/recipe/heavy_rack.json`.
- Tests: `gametest/HeavyRackGameTests.java` (result count); autocrafting GameTests that craft racks through patterns (`PatternResultGameTests` and similar) where they count results.
- Docs: `docs/src/data/gamedata.json` regenerated.

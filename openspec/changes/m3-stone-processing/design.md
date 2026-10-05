# Design

## Context

Assembling recipes with `tech` gating, the recipe provider and the tech registry exist. See proposal.md.

## Decisions

### D1: Data only
Three assembling recipes in `ServusManufacturingRecipeProvider` with `.tech("stone_processing")`: `stone` (2 cobblestone to 2 stone, 20 ticks), `stone_bricks` (2 stone to 2 stone bricks, 20 ticks), `glass` (1 sand to 1 glass, 40 ticks). Tech `stone_processing` gets the stone bricks icon and the three unlock ids.

## Pure Java vs Minecraft

Data only. Verified by datagen output with the `tech` field and a server run where an assembler with the stone recipe merged in produces stone.

## Risks / Trade-offs

- [Bypasses furnace balance for stone and glass] -> Intended reward for the research.

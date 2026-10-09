# Proposal: m5-tank-copper-bottom

PLAN.md milestone M5, Fluid Tank, play check follow-up.

## Why

The Fluid Tank is glass on all six sides, so a tank shows the ground through its bottom. The play check asked for a solid copper floor, and for the recipe to say so: the ingredient in the bottom middle of the recipe is what the bottom of the tank shows.

## What Changes

- The bottom middle of the Fluid Tank recipe becomes a copper ingot instead of glass.
- The bottom of a tank is a solid copper plate in the copper block texture, seen from below and from inside. In a formed tank only the lowest layer shows it, like the glass faces today.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/fluid-tank`: the Connected look gets a copper floor; Crafting uses a copper ingot in the bottom middle.

## Non-goals

- No choice of floor block per tank (no facade style recipe).
- Sides and top stay glass, the frame stays as it is.

## Impact

- `foundation/.../logistics/datagen/FluidTankModels` (floor model), `LogisticsDataParts` (recipe), generated models, recipe and the docs page that names the recipe; `gamedata.json`.

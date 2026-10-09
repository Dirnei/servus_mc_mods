# Proposal: m5-tank-floor

PLAN.md milestone M5, Fluid Tank, play check follow-up.

## Why

The Fluid Tank is glass on all six sides, so a tank shows the ground through its bottom, and the middle of a bigger tank looks shadowed. The play check asked for a solid floor chosen in the recipe, copper instead of iron, and even lighting.

## What Changes

- New tank recipe: copper ingots in the corners, glass on the top, left and right edges, an empty centre, and any solid full block in the bottom middle. That block becomes the tank's floor, like a facade. Glass and other see through blocks are not accepted.
- The tank's floor shows that block's texture as a thin plate, seen from below and from inside, only on blocks with nothing of their tank below. The block travels with the item when the tank is broken and placed again.
- The frame looks like copper.
- Tank blocks let light through like air and cast no ambient occlusion shadow, so a big tank is lit evenly.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/fluid-tank`: Connected look (floor, copper frame, even light), Crafting (copper corners and a floor block), Broken block keeps its floor.

## Non-goals

- The item icon always shows a copper floor; the tooltip names the real floor.
- JEI does not list the special recipe, like the facade recipe.
- Sides and top stay glass.

## Impact

- `foundation/.../logistics/tank` (`FluidTankBlock` light, `FluidTankBlockEntity` floor, `FluidTankItem` floor, new `FluidTankRecipe`), `registry` (component `tank_floor`, recipe serializer), `client/FluidTankRenderer` (floor plate), datagen models, loot, recipe and lang; docs and `gamedata.json`.

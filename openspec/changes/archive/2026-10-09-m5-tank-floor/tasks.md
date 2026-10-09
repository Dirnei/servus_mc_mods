# Tasks

## 1. Floor, recipe and light

- [x] 1.1 `FluidTankBlock`: skylight passes, light block 0, shade brightness 1. `FluidTankBlockEntity`: floor block (default copper block) saved, synced in the update tag and carried by the `servus_foundation:tank_floor` component; loot copies it. `FluidTankItem.withFloor`, `floor` and a tooltip line. Verify: build.
- [x] 1.2 `FluidTankRecipe` (special crafting recipe: copper corners, glass on three edges, empty centre, a facadeable block in the bottom middle) with its serializer and datagen; the shaped recipe is removed. `FluidTankModels`: no glass bottom, copper frame from `minecraft:block/copper_block`, the item model keeps a copper floor. `FluidTankRenderer` draws the floor block as a 1 pixel plate on blocks not joined below and starts the fluid on top of it. GameTests: `recipeTakesTheFloorBlock`, `floorTravelsWithTheItem`. Verify: both data runs, build, Foundation GameTests green.
- [x] 1.4 Play check follow-up: the floor is inset on sides that are not joined and the fluid starts a little above the floor, so neither the floor edges nor the fluid bottom flicker. Verify: build.
- [x] 1.5 Play check follow-up: Foundation JEI plugin (`integration/jei/ServusFoundationJeiPlugin`, JEI API compile only) shows the tank recipe with example floor blocks and the facade recipe. Verify: build, recipe visible in JEI.
- [x] 1.6 Play check follow-up: creative players scooping from a tank keep the empty bucket and get the filled one, like vanilla (`FluidTankBlock.fillInCreative`). GameTest `creativeBucketGetsFilled`. Verify: Foundation GameTests green.
- [x] 1.7 Play check follow-up: the fluid is drawn with `Sheets.translucentCullBlockSheet()` instead of the chunk translucent type, so it is drawn after the floor and never hides it depending on the view angle. Verify: build, checked in the client.
- [x] 1.3 Docs: recipe and look in `docs/src/content/docs/mods/foundation.md`; regenerate `gamedata.json`. Verify: docs tests pass.

## 2. Integration

- [x] 2.1 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches from main: tanks with different floors, a two layer tank shows its floor only at the bottom, a 3 by 3 tank is lit evenly, the copper frame. Play checked by the user from main on 2026-10-09.

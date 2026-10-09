# Tasks

## 1. Floor, recipe and light

- [x] 1.1 `FluidTankBlock`: skylight passes, light block 0, shade brightness 1. `FluidTankBlockEntity`: floor block (default copper block) saved, synced in the update tag and carried by the `servus_foundation:tank_floor` component; loot copies it. `FluidTankItem.withFloor`, `floor` and a tooltip line. Verify: build.
- [x] 1.2 `FluidTankRecipe` (special crafting recipe: copper corners, glass on three edges, empty centre, a facadeable block in the bottom middle) with its serializer and datagen; the shaped recipe is removed. `FluidTankModels`: no glass bottom, copper frame from `minecraft:block/copper_block`, the item model keeps a copper floor. `FluidTankRenderer` draws the floor block as a 1 pixel plate on blocks not joined below and starts the fluid on top of it. GameTests: `recipeTakesTheFloorBlock`, `floorTravelsWithTheItem`. Verify: both data runs, build, Foundation GameTests green.
- [x] 1.4 Play check follow-up: the floor is inset on sides that are not joined and the fluid starts a little above the floor, so neither the floor edges nor the fluid bottom flicker. Verify: build.
- [x] 1.3 Docs: recipe and look in `docs/src/content/docs/mods/foundation.md`; regenerate `gamedata.json`. Verify: docs tests pass.

## 2. Integration

- [ ] 2.1 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches from main: tanks with different floors, a two layer tank shows its floor only at the bottom, a 3 by 3 tank is lit evenly, the copper frame.

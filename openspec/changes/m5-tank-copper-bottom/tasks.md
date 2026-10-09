# Tasks

## 1. Floor and recipe

- [x] 1.1 `FluidTankModels`: the down part becomes a 1 pixel copper plate (texture `minecraft:block/copper_block`, faces down and up, down culled), shown while the down side is not joined; the item model gets the same plate. `LogisticsDataParts`: copper ingot in the bottom middle of the tank recipe. Verify: `:foundation:runData`, build, Foundation GameTests green.
- [x] 1.2 Docs: recipe and look in `docs/src/content/docs/mods/foundation.md`; regenerate `gamedata.json`. Verify: docs tests pass.

## 2. Integration

- [ ] 2.1 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches from main: a single tank and a two layer tank show the copper floor only at the bottom, the new recipe crafts a tank.

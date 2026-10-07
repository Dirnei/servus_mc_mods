# Tasks

## 1. Pure logic

- [x] 1.1 Add `logistics/tank/TankShape.java` and `TankLayers.java` (pure). JUnit `TankShapeTest` (box, L, box plus touching block, single) and `TankLayersTest` (tower half full, 2 by 2 by 2 with six blocks' worth, empty, clamp to capacity, remainder sums exactly). Verify `gradlew :logistics:test`.

## 2. Tank block

- [x] 2.1 Add `tank/FluidTankBlock.java` (six joined properties), `FluidTankBlockEntity.java` (share in NBT, handler), `TankNetwork.java` (D1, sets joined properties on form and unform), `tank/TankStatsManager.java` with `data/servus_logistics/tank/fluid_tank.json`, registrations in `registry/ServusLogistics*`, `Capabilities.FluidHandler.BLOCK` in `ServusLogisticsEvents.java`. Verify `gradlew build`.
- [x] 2.2 Add `tank/FluidTankItem.java` (one fluid placement check with message, tooltip), the `fluid` data component in `registry/ServusLogisticsComponents.java`, bucket and empty hand use in `FluidTankBlock`. Verify `gradlew build`.
- [x] 2.3 Client and data: `client/FluidTankRenderer.java`, multipart blockstate with faces and edge bars, item model, loot table with `copy_components`, recipe and lang in `datagen/`. Run `gradlew :logistics:runData` and `gradlew build`.

## 3. Tests and docs

- [x] 3.1 GameTests in `gametest/FluidTankGameTests.java` (own class, registered next to `ServusLogisticsGameTests` in `ServusLogisticsEvents.java`) per the design list. Verify `gradlew :logistics:runGameTestServer`.
- [ ] 3.2 Update `PLAN.md` section 1, 2.2 and M5. Run `openspec validate m5-fluid-tank --strict`.
- [ ] 3.3 Full `gradlew build` and `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 3.4 Play check: build a 2 by 2 by 3 tank block by block (singles until complete, then one frame around the box), fill with buckets, fill level rises bottom up, break a corner and see the frames return.

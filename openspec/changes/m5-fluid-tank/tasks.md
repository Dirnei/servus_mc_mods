# Tasks

## 1. Pure logic

- [x] 1.1 Add `logistics/tank/TankShape.java` and `TankLayers.java` (pure). JUnit `TankShapeTest` (box, L, box plus touching block, single) and `TankLayersTest` (tower half full, 2 by 2 by 2 with six blocks' worth, empty, clamp to capacity, remainder sums exactly). Verify `gradlew :logistics:test`.

## 2. Tank block

- [x] 2.1 Add `tank/FluidTankBlock.java` (six joined properties), `FluidTankBlockEntity.java` (share in NBT, handler), `TankNetwork.java` (D1, sets joined properties on form and unform), `tank/TankStatsManager.java` with `data/servus_logistics/tank/fluid_tank.json`, registrations in `registry/ServusLogistics*`, `Capabilities.FluidHandler.BLOCK` in `ServusLogisticsEvents.java`. Verify `gradlew build`.
- [x] 2.2 Add `tank/FluidTankItem.java` (one fluid placement check with message, tooltip), the `fluid` data component in `registry/ServusLogisticsComponents.java`, bucket and empty hand use in `FluidTankBlock`. Verify `gradlew build`.
- [x] 2.3 Client and data: `client/FluidTankRenderer.java`, multipart blockstate with faces and edge bars, item model, loot table with `copy_components`, recipe and lang in `datagen/`. Run `gradlew :logistics:runData` and `gradlew build`.

## 3. Tests and docs

- [x] 3.1 GameTests in `gametest/FluidTankGameTests.java` (own class, registered next to `ServusLogisticsGameTests` in `ServusLogisticsEvents.java`) per the design list. Verify `gradlew :logistics:runGameTestServer`.
- [x] 3.2 Update `PLAN.md` section 1, 2.2 and M5. Run `openspec validate m5-fluid-tank --strict`.

## 4. Play test changes

- [x] 4.1 Add pure `tank/TankBoxes.java` (D6) with JUnit `TankBoxesTest`. Verify `gradlew :logistics:test`.
- [x] 4.2 Rework `tank/TankNetwork.java` to keep one total and layout per box, store box bounds in `FluidTankBlockEntity` NBT and restore them on load, joined flags per box. Update `gametest/FluidTankGameTests.java`. Verify `gradlew build` and `gradlew :logistics:runGameTestServer`.
- [x] 4.3 Layer placement in `tank/FluidTankItem.java` (D7) with GameTests. Verify `gradlew build` and `gradlew :logistics:runGameTestServer`.

## 5. Verification

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 5.2 Play check: build a 2 by 2 by 3 tank block by block (pairs and singles until a box completes, then one frame around it), a block on top of a 2 by 2 tank leaves it formed, a stack of tank items on top adds a whole layer, fill with buckets, fill level rises bottom up, break a corner and see it regroup, reload keeps the tanks.

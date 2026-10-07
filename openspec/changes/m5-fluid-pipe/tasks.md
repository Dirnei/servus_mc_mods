# Tasks

## 1. Preconditions and pure logic

- [x] 1.1 Verify `gradlew build` is green. `m5-fluid-tank` is built in parallel, so GameTests here use vanilla water and lava cauldrons (NeoForge `CauldronWrapper`, 1,000 mB each) as sources and sinks.
- [x] 1.2 Move the water filling from `lib/energy/EnergyDistributor.java` into a shared helper and add `lib/fluid/FluidRouting.java`. JUnit `FluidRoutingTest`: one source to two sinks shares evenly, short supply, mixed keys reach only matching sinks, empty sink takes the first offered key, no supply moves nothing. `EnergyDistributorTest` stays green. Verify `gradlew :lib:test`.

## 2. Line and look

- [x] 2.1 Add `Line.FLUID`; generalize `duct/DuctParts.java` (seven masks, joined suffixes), `duct/DuctShapes.java` (three radii), line counting in `duct/UtilityDuctBlock.java`, `UtilityDuctBlockEntity.networkType`, `ServusLogisticsItems.item(Line)` and the `FLUID_PIPE` item. Update `DuctShapesTest`, add `DuctPartsTest`. Verify `gradlew :logistics:test`.
- [x] 2.2 Generate parts for every line set in `datagen/DuctPartModels.java` (band textures, axis and junction centres, rings per line), fluid skin and band textures, mask lookup in `client/UtilityDuctModel.java`. Run `gradlew :logistics:runData` and `gradlew build`.

## 3. Behaviour and GUI

- [ ] 3.1 Add `network/FluidChannelNetwork.java` (D3) and register it as the fluid line network. Verify `gradlew build`.
- [ ] 3.2 Add `duct/DuctFluidHandler.java` and register `Capabilities.FluidHandler.BLOCK` in `ServusLogisticsEvents.java`. Verify `gradlew build`.
- [ ] 3.3 Fluid Pipe page in `menu/UtilityDuctMenu.java` and `client/UtilityDuctScreen.java`; recipe, item model and lang in datagen. Run `gradlew :logistics:runData` and `gradlew build`.
- [ ] 3.4 GameTests in `gametest/ServusLogisticsGameTests.java` per the design list. Verify `gradlew :logistics:runGameTestServer`.

## 4. Docs and verification

- [ ] 4.1 Update `PLAN.md` 2.2 and M5. Run `openspec validate m5-fluid-pipe --strict`.
- [ ] 4.2 Full `gradlew build` and `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 4.3 Play check: three line duct size, bands and rings, Fluid Pipe GUI tab, a full tank empties into another through a duct run with default sides.

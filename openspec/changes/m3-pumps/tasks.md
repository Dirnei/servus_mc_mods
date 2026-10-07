# Tasks

## 1. Preconditions and pure logic

- [x] 1.1 Verify `gradlew build` is green. `m5-fluid-tank` and `m5-fluid-pipe` are built in parallel, so GameTests here fill a vanilla empty cauldron (NeoForge `CauldronWrapper`) and read the pump buffer through its capability.
- [x] 1.2 Add `machine/pump/PumpSource.java` (pure). JUnit `PumpSourceTest`: 0 to 4 source neighbours against a minimum of 2, no source below. Verify `gradlew :manufacturing:test`.

## 2. Machines

- [x] 2.1 Extend `machine/MachineStats.java` with `fluid_per_cycle`, `fluid_capacity`, `min_source_neighbours`. Add `machine/pump/PumpLogic.java`, `BurnerPumpBlock(Entity).java`, `ElectricPumpBlock(Entity).java`, registrations in `registry/ServusManufacturing*`, item, energy and fluid capabilities in `ServusManufacturingEvents.java`, `data/servus_manufacturing/machine/burner_pump.json` and `electric_pump.json`, front and bottom locked Off. Verify `gradlew build`.
- [x] 2.2 Add Lib `client/FluidGauge.java`; `menu/BurnerPumpMenu.java`, `menu/ElectricPumpMenu.java`, gauge and source status in `client/MachineScreen.java`, screen registration in `client/ServusManufacturingClient.java`. Verify `gradlew build`.
- [x] 2.3 Datagen: models, blockstates, loot, lang, Burner Pump crafting recipe, Electric Pump assembling recipe with `tech: electric_extractor`, add it to `tech/electric_extractor.json` unlocks. Run `gradlew :manufacturing:runData` and `gradlew build`.

## 3. Tests and docs

- [x] 3.1 GameTests in `gametest/ServusManufacturingGameTests.java` per the design list. Verify `gradlew :manufacturing:runGameTestServer`.
- [ ] 3.2 Cross mod check on `runServerAll` over RCON after all three fluid changes are merged: Burner Pump on a pond, Fluid Pipe run, Fluid Tank fills.
- [x] 3.3 Update `PLAN.md` 2.1 and M3. Run `openspec validate m3-pumps --strict`.
- [ ] 3.4 Full `gradlew build` and `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 3.5 Play check: both pumps on a pond, GUI gauge and status line, pump on dirt says it needs a source, Electric Pump recipe locked before research.

## 4. Play test changes

- [x] 4.1 Compact pump body (D7): shared shape in `machine/pump/PumpBlocks.java`, `getShape` in both pump blocks, `noOcclusion` in `registry/ServusManufacturingBlocks.java`, element models in `datagen/ServusManufacturingBlockStateProvider.java`. Run `gradlew :manufacturing:runData`, `gradlew build` and `gradlew :manufacturing:runGameTestServer`.

# Tasks

## 1. Machine base in Foundation

- [x] 1.1 Move `MachineBlock`, `MachineBlockEntity`, `BurnerMachineBlockEntity`, `PoweredMachineBlockEntity`, `MachineEnergyStorage`, `EnergyNeighbors`, `OwnedMachine`, `MachineStats` and `MachineStatsManager` from `manufacturing/.../machine` to `foundation/.../machine`; register `MachineStatsManager` in Foundation's reload listener event and remove it from `ServusManufacturingEvents`; update every Manufacturing import. Verify: `gradlew build` green.
- [x] 1.2 Turn `MachineDefaults` into a registry in `foundation/.../machine` with `register(id, kind, recipeTypes, stats)` and `all()` in registration order; move `EXTRACTION` and `RESEARCH` to a Manufacturing constants class; Manufacturing registers its machines from its mod constructor. Split `MachineDefaultsTest` into a Foundation test (registration order, defaults lookup) and a Manufacturing test (its machines are registered). Verify: `gradlew test` green.
- [x] 1.3 Move `MachineMenu`, `MachineSlot`, `OutputSlot` to `foundation/.../machine/menu` and `MachineScreen` to `foundation/.../client`; move `machine_side.png`, `machine_top.png` and the `burner_machine.png` GUI to `assets/servus_foundation/textures/`; point Manufacturing's block state datagen and screens at the Foundation textures. Verify: `gradlew build`, `:manufacturing:runData` with no missing texture warnings, Manufacturing GameTests green.

## 2. Coal Generator

- [x] 2.1 Move `machine/generator` to `foundation/.../machine/generator` with its menu; register block, block entity, item, menu, capabilities (energy, item handler, upkeep), screen and side marker renderer in Foundation; move `coal_generator.json` stats to `data/servus_foundation/machine/`; Foundation datagen for block state, models, loot, pickaxe tag, lang and the existing shaped recipe; move the front textures and GUI. Remove all of it from Manufacturing. Verify: `gradlew build`, both data runs.
- [x] 2.2 Update `NativeUpkeepGameTests` to place `servus_foundation:coal_generator`, and add a Foundation GameTest that a generator with coal fills an adjacent FE buffer (spec foundation/coal-generator, "Consumer beside the generator"). Verify: Foundation and Manufacturing GameTests green.

## 3. Pumps

- [x] 3.1 Move `machine/pump` (`PumpBlock`, `PumpBlocks`, `PumpLogic`, `PumpMachine`, `PumpSource`, Burner and Electric Pump blocks and block entities) and both menus to Foundation; move the pump neighbour hook and the energy capability lambda from `ServusManufacturingEvents` to Foundation's events; register blocks, items, menus, capabilities (item, energy, fluid), screens and renderers; move stats JSON, textures and GUIs. Remove all of it from Manufacturing. Verify: `gradlew build`.
- [x] 3.2 Foundation datagen: block states and models for the compact body and stubs, loot, tags, lang, the existing Burner Pump recipe and the new shaped Electric Pump recipe (Burner Pump, iron ingots, copper ingots, redstone); delete `assembling/electric_pump` and its unlock in `tech/electric_extractor.json`. Verify: both data runs, the generated recipe JSON exists under `data/servus_foundation/recipe/`.
- [x] 3.3 Move `PumpSourceTest` to `foundation/src/test/java`, move the nine pump GameTests from `ServusManufacturingGameTests` to a new Foundation machine GameTest class, and add a GameTest that crafts the Electric Pump recipe through the recipe manager with no team research (spec foundation/pumps, "Electric recipe"). Verify: `gradlew test` and Foundation GameTests green.

## 4. Sink and Chimney

- [x] 4.1 Move `machine/sink` (`SinkBlock`, `SinkBlockEntity`, `SinkHandlers`, `SinkLayout`, `SinkRates`, `SinkStats`, `SinkStatsManager`, `VoidBudget`, `ChimneyBlock`), `SinkMenu`, `SinkScreen` and `SinkRenderer` to Foundation; register `SinkStatsManager` in Foundation; register blocks, items, menu, capabilities, screen and renderer; move `sink/sink.json` to `data/servus_foundation/sink/`, textures and GUI. Remove all of it from Manufacturing. Verify: `gradlew build`.
- [x] 4.2 Foundation datagen for the Sink parts, fan, Chimney top, loot, tags, lang and the existing shaped recipes. Verify: both data runs.
- [x] 4.3 Move `SinkLayoutTest`, `SinkRatesTest` and `VoidBudgetTest` to Foundation and the Sink GameTests from `ServusManufacturingGameTests` to the Foundation machine GameTest class. Verify: `gradlew test` and Foundation GameTests green.

## 5. Chunk Loader

- [x] 5.1 Move `machine/loader` (`ChunkLoaderBlock`, `ChunkLoaderBlockEntity`, `ChunkLoaderData`, the ticket controller now `servus_foundation:chunk_loader`), `ChunkLoaderMenu` and `ChunkLoaderScreen` to Foundation; move `maxChunkLoadersPerTeam` and `chunkLoaderRespectsFtbChunks` to `ServusFoundationConfig` with the same defaults and comments and drop them from `ServusManufacturingConfig`; register block, item, menu, capabilities, screen and renderer; move stats JSON, textures and GUI. Verify: `gradlew build`.
- [x] 5.2 Foundation datagen: block state, models, loot, tags, lang and the new shaped recipe (ender pearl, iron ingots, copper ingots, redstone); delete `assembling/chunk_loader` and any tech unlock of it. Verify: both data runs, recipe JSON under `data/servus_foundation/recipe/`.
- [x] 5.3 Add a Foundation GameTest that a placed, powered Chunk Loader reports active and that breaking it releases its ticket (spec foundation/chunk-loader, "Removed"). Verify: Foundation GameTests green.

## 6. Docs and export

- [x] 6.1 Run the Manufacturing data run to regenerate `docs/src/data/gamedata.json`, regenerate the icons with the icon dev run, and update `docs/src/content/docs/mods/foundation.md`, `mods/manufacturing.md`, `reference/datapack/machines.md` and `reference/datapack/recipes.md` so the moved blocks are described under Foundation with `servus_foundation` ids. Verify: the docs tests and build pass, `git diff` of `gamedata.json` shows only the namespace and recipe changes. (Done: the export now reads machine stats from every mod's resources; the six icons were moved to `icons/servus_foundation/` instead of a fresh icon run, since their models are unchanged.)

## 7. Integration

- [x] 7.1 Search the repo for leftover `servus_manufacturing:` ids of the moved blocks and for `manufacturing.machine.(pump|sink|loader|generator)` imports. Verify: no matches outside the archived changes.
- [ ] 7.2 Full `gradlew build`, `gradlew test`, all GameTests of the three mods green, and the client launches with all three mods: craft each of the six blocks at a crafting table, form a Sink, run a Burner Pump into a Fluid Tank, power a Chunk Loader from a Coal Generator. (Build, unit tests and GameTests green in the worktree: Foundation 65, Manufacturing 16, Storage 233. The client part is owed to the play check from main.)

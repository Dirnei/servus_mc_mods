# Tasks

## 1. Station jobs

- [x] 1.1 Move `match` and `templateOf` to `storage/pattern/CraftingGrid`, rename `OpenStationEditorPayload` to `pattern/OpenRecipeEditorPayload` handled only by the Production Terminal, delete the station's JEI transfer handler and its registration. Verify with `gradlew build` and a grep that every payload TYPE in `ServusStoragePayloads` is registered.
- [ ] 1.2 Replace the configured recipes of `CraftingStationBlockEntity` with the transient job (`assign`, `release`, `job`, `jobLeft`), crafting and insertion from the job, production capability with no recipes and the job as active, update tag with job result and crafts left; drop `recipes` from `crafting_station.json` and `StorageStatsManager`. Rework `CraftingStationMenu` and `CraftingStationScreen` (no grid, no editor, job area) and the station lang keys. Verify with `gradlew build`.

## 2. CPU and terminal

- [ ] 2.1 `OrderBook.Host.start/release`, called on step start, step done, cancel and cancelAll; producer ordering by occupation; `CpuBlockEntity` finds the step's pattern and assigns or releases the station job; `PatternRunners.canRun` true for every Crafting Station. Verify with `gradlew build`.
- [ ] 2.2 `ProductionRows.MachineRow.left`, station rows in `ProductionView`, Machines tab lines in `ProductionTerminalScreen`, lang. Verify with `gradlew build`.

## 3. Tests and docs

- [ ] 3.1 Rework the Storage GameTests that configured station recipes (`ServusStorageGameTests`, `CraftingCpuGameTests`, `PatternGameTests`, `ProductionTerminalGameTests`, `AccessPointGameTests`, `DataCableGameTests`, `DataPacketGameTests`), delete tests of the removed editor, and add `StationHandsGameTests` (pattern order on an unconfigured station, two stations in parallel, no job refuses a hopper, cancel returns to idle and keeps inputs, second job refused). Verify with `gradlew test` and `:storage:runGameTestServer`, `:foundation:runGameTestServer`, `:manufacturing:runGameTestServer` all green.
- [ ] 3.2 Update the Storage docs page (Crafting Station, patterns), run `:storage:runData` and `:manufacturing:runData`, docs `npx vitest run` and `npm run build`. Verify the tests pass and the build succeeds.

## 4. Integration

- [ ] 4.1 Full `gradlew build` and `gradlew test` green, all GameTest suites green, `runClientAll` reaches the title screen without asset warnings.

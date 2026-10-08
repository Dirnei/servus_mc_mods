# Tasks

## 1. Foundation upkeep

- [x] 1.1 Add pure `foundation/data/UpkeepBook.java` and `foundation/src/test/.../data/UpkeepBookTest.java` with tests `firstCheckAsksTheMissingAmount`, `levelMetAsksNothing`, `openRequestBlocksAsking`, `arrivalCompletesTheRequest`, `timeoutReopens`, `retainForgetsMembersThatLeft`. Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `production/UpkeepHolder.java` and `ServusFoundationCapabilities.UPKEEP`; `DataRole.UPKEEP`; the fifth probe in `DataNetwork` and the upkeep check in `DataMembership`; the keeper in `DataNetwork.onTick` (interval 40, timeout 1200, suppliers lowest position first, the member itself skipped). Verify with `gradlew build` and `:foundation:runGameTestServer`.
- [x] 1.3 Move the single ingredient recipe listing to `production/SingleInputRecipes.java`; refactor `MachineAdapterBlockEntity` to expose `UPKEEP` through a holder, drop its own loop and ticker (`MachineAdapterBlock`), keep `expect(int)` for tests; register the capability in `AdapterEvents`. Verify with `gradlew build`, `:foundation:runGameTestServer` and `:storage:runGameTestServer` (adapter tests green).

## 2. Manufacturing

- [ ] 2.1 `MachineStats` with `upkeepItem` and `upkeepLevel` (codec fields `upkeep_item`, `upkeep_level`, factory defaults), `MachineDefaults`, `GameDataExportProvider`, `MachineDefaultsTest` extended with the upkeep defaults. Verify with `gradlew :manufacturing:test`.
- [ ] 2.2 `BurnerMachineBlockEntity.upkeep()` over the fuel slot; register `UPKEEP` for Burner Extractor, Burner Smelter, Burner Pump and Coal Generator; `production()` for Burner Smelter (smelting through `SingleInputRecipes`, its work time) and Crusher (crushing recipes with count and time), registered in `ServusManufacturingEvents`. Manufacturing GameTests `burnerMachinesExposeUpkeep`, `electricExtractorHasNoUpkeep`, `smelterReportsSmeltingProduction`, `crusherReportsCrushingProduction`. Verify with `gradlew build` and `:manufacturing:runGameTestServer`.

## 3. Storage

- [ ] 3.1 `ProductionView.machines` over producer and upkeep members, upkeep from the capability, state `upkeep_only` with its lang entry in `ServusStorageLanguageProvider` and the screen. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [ ] 3.2 Storage GameTests in a new `NativeMachineGameTests`: `smelterFuelRefilledFromWarehouse`, `orderRunsOnBurnerSmelter`, `coalGeneratorIsUpkeepOnly` (member role and terminal view). Verify with `:storage:runGameTestServer` all green.

## 4. Docs and data

- [ ] 4.1 Run `:foundation:runData`, `:storage:runData` and `:manufacturing:runData` (lang, gamedata export); docs `gamedata.ts` schema reads `upkeep_item` and `upkeep_level`; `reference/datapack/machines.md` documents both; Manufacturing and Foundation pages describe upkeep and the new producers. Verify with `npx vitest run` and `npm run build` in `docs/`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test`, all three GameTest suites green, `runClientAll` reaches the title screen. PLAN.md M8 marked complete if the plan records finished milestones.

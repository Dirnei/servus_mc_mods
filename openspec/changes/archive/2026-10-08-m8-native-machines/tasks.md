# Tasks

## 1. Foundation upkeep

- [x] 1.1 Add pure `foundation/data/UpkeepBook.java` and `foundation/src/test/.../data/UpkeepBookTest.java` with tests `firstCheckAsksTheMissingAmount`, `levelMetAsksNothing`, `openRequestBlocksAsking`, `arrivalCompletesTheRequest`, `timeoutReopens`, `retainForgetsMembersThatLeft`. Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `production/UpkeepHolder.java` and `ServusFoundationCapabilities.UPKEEP`; `DataRole.UPKEEP`; the fifth probe in `DataNetwork` and the upkeep check in `DataMembership`; the keeper in `DataNetwork.onTick` (interval 40, timeout 1200, suppliers lowest position first, the member itself skipped). Verify with `gradlew build` and `:foundation:runGameTestServer`.
- [x] 1.3 Move the single ingredient recipe listing to `production/SingleInputRecipes.java`; refactor `MachineAdapterBlockEntity` to expose `UPKEEP` through a holder, drop its own loop and ticker (`MachineAdapterBlock`), keep `expect(int)` for tests; register the capability in `AdapterEvents`. Verify with `gradlew build`, `:foundation:runGameTestServer` and `:storage:runGameTestServer` (adapter tests green).

## 2. Manufacturing

- [x] 2.1 `MachineStats` with `upkeepItem` and `upkeepLevel` (codec fields `upkeep_item`, `upkeep_level`, factory defaults), `MachineDefaults`, `GameDataExportProvider`, `MachineDefaultsTest` extended with the upkeep defaults. Verify with `gradlew :manufacturing:test`.
- [x] 2.2 `BurnerMachineBlockEntity.upkeep()` over the fuel slot; register `UPKEEP` for Burner Extractor, Burner Smelter, Burner Pump and Coal Generator; `production()` for Burner Smelter (smelting through `SingleInputRecipes`, its work time) and Crusher (crushing recipes with count and time), registered in `ServusManufacturingEvents`. Manufacturing GameTests `burnerMachinesExposeUpkeep`, `electricExtractorHasNoUpkeep`, `smelterReportsSmeltingProduction`, `crusherReportsCrushingProduction`. Verify with `gradlew build` and `:manufacturing:runGameTestServer`.

## 3. Storage

- [x] 3.1 `ProductionView.machines` over producer and upkeep members, upkeep from the capability, state `upkeep_only` with its lang entry in `ServusStorageLanguageProvider` and the screen. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [x] 3.2 Storage may not reference Manufacturing blocks, so the smelter tests live in Manufacturing (`NativeUpkeepGameTests`): `burnersJoinTheDataLine` (smelter producer and upkeep, Coal Generator upkeep only, through a Foundation data duct) and `smelterTakesOrderItemsThroughAFace` (coal and raw iron in through a side, the ingot out through it, as packets do); the keeper's refill from a warehouse stays covered by `AdapterOrderGameTests.adapterUpkeepFromWarehouse`. Verify with `:manufacturing:runGameTestServer` and `:storage:runGameTestServer` all green.

## 4. Docs and data

- [x] 4.1 Run `:foundation:runData`, `:storage:runData` and `:manufacturing:runData` (lang, gamedata export); docs `gamedata.ts` schema reads `upkeep_item` and `upkeep_level`; `reference/datapack/machines.md` documents both; Manufacturing and Foundation pages describe upkeep and the new producers. Verify with `npx vitest run` and `npm run build` in `docs/`.

## 5. Integration

- [x] 5.1 Full `gradlew build` and `gradlew test`, all three GameTest suites green, `runClientAll` reaches the title screen. PLAN.md records no finished milestones, so its M8 list stays.

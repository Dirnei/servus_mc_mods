# Tasks

Apply after `m10-field-head-buffer` is archived.

## 1. Pure logic and data

- [x] 1.1 Add pure `Produce.pick(tally, crop)` and `StockLevels` (`harvestPaused`, `fertilizeAllowed`, `step`, `keepOrder`) in `storage/.../field/`. JUnit `ProduceTest`: wheat with more seeds than wheat, carrots only, ties by id, harvest only. JUnit `StockLevelsTest`: 0 means off, at and below the limit, the fertilize level with a paused harvest, an unknown produce, clamping, keeping the order. Add `harvest_limit` and `fertilize_below` to `storage/field.json` and `StorageStatsManager`. Verify: `gradlew :storage:test` green.

## 2. Head and runs

- [x] 2.1 `FieldHeadBlockEntity`: produce, `harvestLimit`, `fertilizeBelow`, the produce stock read at field checks, save and load, clearing the produce on a crop change. `fertilizeDue` and `fertilizeTargets` respect `StockLevels`. Verify: `gradlew build` green.
- [x] 2.2 `FieldRuns`, `HarvestRun`, `PlotWork`: a tally of harvested drops handed to the head at the end of a run, and a `harvest` flag that leaves ripe plots while paused. `WarehouseNetwork.tickFields` passes the paused harvest to `FieldDue`. Verify: `gradlew build` green.

## 3. GUI and statuses

- [x] 3.1 `FieldHeadMenu`, `FieldHeadScreen`, `SetFieldLevelPayload` (registered in `ServusStoragePayloads`): the rows on the Field and Fertilizer tabs, the work status "Harvest paused", and `FertilizeStatus.NOT_NEEDED`, with no red markers. Lang keys; the field command prints the produce and both levels. Verify: `gradlew :storage:runData` and `gradlew build` green, every payload type registered.

## 4. GameTests and docs

- [x] 4.1 GameTests in `storage/.../gametest/`:
  - `produceIsLearned` (wheat field harvest gives wheat)
  - `harvestStopsAtTheLimit` (ripe field, stock above the limit, no run)
  - `harvestResumesBelowTheLimit`
  - `cappedFieldStillPlants` (empty farmland is planted while ripe plots stay)
  - `fertilizeOnlyBelowTheLevel`
  - `levelsStayInOrder`
  - `levelsSaved`

  Verify: `gradlew :storage:runGameTestServer` green.
- [x] 4.2 Describe the produce and the two levels in `docs/src/content/docs/mods/storage.md`. Verify: docs tests green.

## 5. Integration

- [x] 5.1 Full `gradlew build`, unit tests and storage GameTests green. The game launches with all three mods.
- [ ] 5.2 Play check in game by the user from main:
  - A wheat field learns wheat at its first harvest.
  - With a limit below the stock it stops harvesting, and resumes when wheat is taken out.
  - With a fertilize level below the stock no Spreader comes.
  - The Stock tab reads as a table: warehouse counts, then the head's buffer, then Keep at least, Harvest up to and Fertilize below, explanations only in tooltips; typed levels and both reserves stick, Keep anti-corrosion in stock keeps honeycomb in the warehouse, the Water tab picks the anti-corrosion item and Off lets the copper age, Enter and clicking away both commit, the buffer counts show; the Field and Fertilizer tabs are no longer crowded and long item names fit.
  - The statuses read well.

  Verify: the user confirms in game.

## 6. Play test rework: Stock tab

The user's play test found the Field Head GUI too crowded and asked for typed amounts on a page of their own.

- [x] 6.1 Pure `FieldAmount` (`HARVEST_LIMIT`, `FERTILIZE_BELOW`, `RESERVE`) with `parse(text)` and `clamp(value)` in `storage/.../field/`; drop `StockLevels.step` and `FertilizePlan.stepReserve`. JUnit `FieldAmountTest`: empty is 0, digits, overflow saturates, clamping per kind; adjust `StockLevelsTest` and `FertilizePlanTest`. Verify: `gradlew :storage:test` green.
- [x] 6.2 `SetFieldAmountPayload` replaces `SetFieldLevelPayload` (registered in `ServusStoragePayloads`); `FieldHeadMenu.setAmount` (usable menu of that head, clamp, keep order) replaces `stepLevel` and the reserve menu buttons; `FieldHeadBlockEntity.setLevel` and `setReserve` replace `stepLevel` and `stepReserve`. Adjust the GameTests that stepped (`FieldLevelGameTests`, `SpreaderGameTests`, `FieldSupplyGameTests`). Verify: `gradlew build` green.
- [x] 6.3 `FieldHeadMenu`, `FieldHeadScreen`, `PanelPainter`, new client `AmountField`: the 264 x 236 panel with the centred inventory and the size check at `init`, the Stock tab with produce, three typed fields and the buffer, the Field, Water and Fertilizer tabs without level rows, reserve and buffer counts, full width pickers. Lang keys in `ServusStorageLanguageProvider`. Verify: `gradlew :storage:runData` and `gradlew build` green.
- [x] 6.4 GameTests in `FieldLevelGameTests`: `typedAmountsClamped` (levels to 99,999, reserve to `MAX_RESERVE`, negative to 0), `levelsStayInOrder` rewritten for typed levels, `typedAmountRefusedFarAway` (a player out of reach changes nothing); `SpreaderGameTests.menuChoosesFertilizerAndSettings` types the reserve. Verify: `gradlew runGameTestServerAll` green.
- [x] 6.5 Describe the Stock tab and typed amounts in `docs/src/content/docs/mods/storage.md`. Verify: docs tests green, `openspec validate m10-field-stock-levels --strict` passes.

## 7. Play test rework: anti-corrosion item and reserve

The user asked for a minimum stock level for honeycomb too.

- [x] 7.1 `anti_corrosion_reserve` in `storage/field.json` and `StorageStatsManager`; `FieldHeadBlockEntity.antiCorrosionReserve`, `setAntiCorrosionReserve`, `supplyReserve(part)`, save and load; `WarehouseNetwork.supplyDue` and `FieldRuns.planSupply` with `SupplyRun.reserves` use it. `FieldAmount.ANTI_CORROSION_RESERVE`, `FieldHeadMenu` (data slot, `setAmount`), `FieldHeadScreen` (fourth row, item name hint, rows and buffer moved up), lang, the field command. JUnit `FieldAmountTest` for the new kind. GameTests `FieldSupplyGameTests.supplyKeepsTheAntiCorrosionReserve`, `FieldLevelGameTests.typedAmountsClamped` and `levelsSaved` cover it. Docs in `storage.md`. Verify: `gradlew :storage:runData`, `gradlew build test runGameTestServerAll` green, `openspec validate m10-field-stock-levels --strict` passes.
- [x] 7.2 The anti-corrosion item per head: `FieldHeadBlockEntity` (`antiCorrosion`, `setAntiCorrosion`, save, load and sync, `supplyItem` and `supplyLevel`, `supplyStock`), `ChooseAntiCorrosionPayload` (registered in `ServusStoragePayloads`), `FieldHeadMenu.chooseAntiCorrosion`, `FieldSupplyStockPayload` replacing `FieldFertilizerStockPayload`, client `TagPickerEntries` replacing `FertilizerPickerEntries`, the Water tab picker in `FieldHeadScreen`, lang keys; "Keep anti-corrosion in stock", never "wax", with the box at x 158. GameTests `RailCorrosionGameTests.antiCorrosionOffAges` and `antiCorrosionChoiceSaved`. Docs in `storage.md`. Verify: `gradlew :storage:runData`, `gradlew build test runGameTestServerAll` green, `openspec validate m10-field-stock-levels --strict` passes.

## 8. Play test rework: Stock table

- [x] 8.1 The Stock tab as a table (design.md "Stock table"): `FieldHeadScreen` (columns produce, seeds, fertilizer, anti-corrosion; rows Warehouse, Field Head, Keep at least, Harvest up to, Fertilize below; tooltips for headers, counts, buffer cells, row labels and fields), pure client `CompactCount` with JUnit `CompactCountTest`, lang keys in `ServusStorageLanguageProvider`, docs in `storage.md`. Verify: `gradlew :storage:runData`, `gradlew build test runGameTestServerAll` green, `openspec validate m10-field-stock-levels --strict` passes, docs tests green.

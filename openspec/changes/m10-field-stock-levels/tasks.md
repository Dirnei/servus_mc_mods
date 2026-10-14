# Tasks

Apply after `m10-field-head-buffer` is archived.

## 1. Pure logic and data

- [x] 1.1 Add pure `Produce.pick(tally, crop)` and `StockLevels` (`harvestPaused`, `fertilizeAllowed`, `step`, `keepOrder`) in `storage/.../field/`. JUnit `ProduceTest`: wheat with more seeds than wheat, carrots only, ties by id, harvest only. JUnit `StockLevelsTest`: 0 means off, at and below the limit, the fertilize level with a paused harvest, an unknown produce, clamping, keeping the order. Add `harvest_limit` and `fertilize_below` to `storage/field.json` and `StorageStatsManager`. Verify: `gradlew :storage:test` green.

## 2. Head and runs

- [x] 2.1 `FieldHeadBlockEntity`: produce, `harvestLimit`, `fertilizeBelow`, the produce stock read at field checks, save and load, clearing the produce on a crop change. `fertilizeDue` and `fertilizeTargets` respect `StockLevels`. Verify: `gradlew build` green.
- [ ] 2.2 `FieldRuns`, `HarvestRun`, `PlotWork`: a tally of harvested drops handed to the head at the end of a run, and a `harvest` flag that leaves ripe plots while paused. `WarehouseNetwork.tickFields` passes the paused harvest to `FieldDue`. Verify: `gradlew build` green.

## 3. GUI and statuses

- [ ] 3.1 `FieldHeadMenu`, `FieldHeadScreen`, `SetFieldLevelPayload` (registered in `ServusStoragePayloads`): the rows on the Field and Fertilizer tabs, the work status "Harvest paused", and `FertilizeStatus.NOT_NEEDED`, with no red markers. Lang keys; the field command prints the produce and both levels. Verify: `gradlew :storage:runData` and `gradlew build` green, every payload type registered.

## 4. GameTests and docs

- [ ] 4.1 GameTests in `storage/.../gametest/`:
  - `produceIsLearned` (wheat field harvest gives wheat)
  - `harvestStopsAtTheLimit` (ripe field, stock above the limit, no run)
  - `harvestResumesBelowTheLimit`
  - `cappedFieldStillPlants` (empty farmland is planted while ripe plots stay)
  - `fertilizeOnlyBelowTheLevel`
  - `levelsStayInOrder`
  - `levelsSaved`

  Verify: `gradlew :storage:runGameTestServer` green.
- [ ] 4.2 Describe the produce and the two levels in `docs/src/content/docs/mods/storage.md`. Verify: docs tests green.

## 5. Integration

- [ ] 5.1 Full `gradlew build`, unit tests and storage GameTests green. The game launches with all three mods.
- [ ] 5.2 Play check in game by the user from main:
  - A wheat field learns wheat at its first harvest.
  - With a limit below the stock it stops harvesting, and resumes when wheat is taken out.
  - With a fertilize level below the stock no Spreader comes.
  - The rows and statuses read well.

  Verify: the user confirms in game.

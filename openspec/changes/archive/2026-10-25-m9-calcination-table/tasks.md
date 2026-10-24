# Tasks

## 1. Pure logic

- [x] 1.1 Add pure `alchemy/.../alchemy/journal/CalcinationTable.java` (`Heights`, `PAGE`, `Line`, `Heading`, `Entry`, `Table`, `layout`, `hintKey`, `nameKey`) with JUnit `CalcinationTableTest`: group and id order, no heading last on a page, a continued group repeats its heading, the shipped page count, footer and left out count at the page limit, no heading for an empty group, key derivation with and without the `calcination/` prefix and with a nested path. Verify: `gradlew :alchemy:test` green.
- [x] 1.2 Add `markTried` and `tried` to `JournalLedger.TeamJournal` with tests in `JournalLedgerTest` (new id gives true, a repeat gives false, teams stay apart). Verify: `gradlew :alchemy:test` green.

## 2. Server state and sync

- [x] 2.1 `JournalSavedData` saves and loads `Tried` per team, `load` public. `Journal.recordCalcination` marks and queues the team. `CalcinerBlockEntity.finishWork` reports the recipe id it used. `JournalProgressPayload` carries the tried ids, `JournalSync.payload(TeamJournal)` builds it, `ClientJournalProgress.tried()` keeps them. Verify: `gradlew build` green.
- [x] 2.2 GameTests in `alchemy/.../gametest/AlchemyGameTests.java`: `calcineRevealsTheRowForTheTeam`, `triedRecipesSurviveASave`. Verify: `gradlew :alchemy:runGameTestServer` green.

## 3. Book page

- [x] 3.1 Add `alchemy/.../alchemy/journal/CalcinationTablePage.java` (fields `part`, `last`, registered in `ServusAlchemy` like the task page) and `alchemy/.../alchemy/client/journal/CalcinationTablePageRenderer.java` (rows from the client recipes through `CalcinationRecipes.burns`, cached per recipe map, tried state from `ClientJournalProgress`), registered in `ServusAlchemyClient`. Verify: `gradlew build` green.
- [x] 3.2 Datagen: `CalcinationTablePageModel`, `JournalEntries.TextEntry.calcinationTable()` with the part count from `CalcinationTable.layout` of the shipped rows, the Calciner entry in `WorkshopCategory` uses it instead of the "What Burns" list; table texts, names and hints in `ServusAlchemyLanguageProvider`. Verify: `gradlew :alchemy:runData` writes the `burns*` pages as `servus_alchemy:calcination_table` with `part` and `last`, a name and hint for every shipped recipe, and `gradlew build` green.

## 4. Integration

- [x] 4.1 Full `gradlew build`, unit tests and the alchemy GameTests green.
- [x] 4.2 Play check in game by the user from main:
  - A new world shows every row of the Calciner table hidden, with a "?" and a hint, under Overworld, Nether and End.
  - Burning one Nether Wart reveals its row with the item, the name and 60 Sulfur, 20 Mercury, while the book is open.
  - The table runs over its pages without text running off a page; continued groups repeat their heading.
  - A teammate sees the same rows; another team does not.
  - JEI still lists every calcination recipe.

  Verify: the user confirms in game.

## Workflow follow-up

- At archive: add a line under `## Unreleased` in `alchemy/CHANGELOG.md` (Changed: the journal's Calciner table fills in as your team burns things, with hints for the rest).

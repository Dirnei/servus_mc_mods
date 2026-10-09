# Tasks

## 1. Layout

- [ ] 1.1 Extend `terminal/TerminalLayout.Counter` (height 260, `CRAFT_LABEL`, `CLEAR`, `CRAFT_GRID`, `CRAFT_RESULT` in `areas()`); `TerminalLayoutTest` stays green. Verify with `gradlew :storage:test`.

## 2. Menu and payloads

- [ ] 2.1 `menu/CounterTerminalMenu`: crafting container, result slot with refill, result recomputation, shift click on result and grid, grid emptied on close, `pullToCursor`, `clearGrid`, `fillGrid`. New payloads `terminal/GridPullPayload`, `GridClearPayload`, `GridFillPayload` registered in `ServusStoragePayloads`; grep that every payload TYPE is registered. Verify with `gradlew build`.
- [ ] 2.2 GameTests in `gametest/CraftingGridGameTests.java`: `gridCraftsSticks`, `gridShiftCraftRefills`, `gridSkipsRackStock`, `gridClearStores`, `gridCloseStores`, `gridFillFromCrateFirst`, `gridPullToCursor`. Verify with `:storage:runGameTestServer`.

## 3. Screen and JEI

- [ ] 3.1 `client/CounterTerminalScreen`: panel height, grid, arrow, result and clear button drawing, Crafting label, Alt click on stock entries; lang entries. Verify with `gradlew build`.
- [ ] 3.2 `integration/jei/CounterTerminalRecipeTransferHandler` registered in `ServusStorageJeiPlugin`. Verify with `gradlew build`.

## 4. Docs

- [ ] 4.1 Storage docs page: crafting grid, Alt click, clear, JEI. Verify with docs `npx vitest run`.

## 5. Integration

- [ ] 5.1 Full `gradlew build test`, Storage GameTests green.

# Tasks

## 1. Picker in Foundation

- [x] 1.1 Move `storage/.../picker/PickerList.java` and `PickerStock.java` to `foundation/.../picker/`, their tests `PickerListTest` and `PickerStockTest` to `foundation/src/test/java/.../foundation/picker/`, and `storage/.../client/ItemPicker.java` to `foundation/.../client/ItemPicker.java` (design D1). Update the imports in `storage/.../client/FieldHeadScreen.java`, `CropPickerEntries.java`, `TagPickerEntries.java` and `menu/FieldHeadMenu.java`. Verify: `gradlew :foundation:test :storage:test` green (the moved tests run in Foundation), `gradlew build` green.
- [x] 1.2 `foundation/.../client/ItemPicker.java`: allow opening the popup without the button (design D1), so a caller can open it for its own target; the Field Head keeps using the button. Verify: `gradlew build` green; in game the Field Head crop picker still opens, searches and picks.

## 2. Filter by id on the server

- [x] 2.1 Add `foundation/.../logistics/menu/SetDuctFilterPayload.java` and register it with `playToServer` in `foundation/.../data/ServusFoundationPayloads.java`; add `UtilityDuctMenu.setFilter(int, ItemStack)` and a static handler that checks the open menu, the Item Pipe and the slot range (design D4). Verify: `gradlew build` green.
- [x] 2.2 `UtilityDuctMenu.clicked`: right click on a filter slot clears it; left click with an item keeps today's copy (design D3). GameTests in `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`: `filterSetByIdWithoutOwning` (mock player with empty inventory, `setFilter` through the payload handler, the side then passes only that item and the inventory stays empty), `filterRightClickClears`, `filterCursorItemKept`. Verify: `gradlew build` green and the GameTests pass in `gradlew :foundation:runGameTestServer`.

## 3. Search popup in the duct GUI

- [x] 3.1 `foundation/.../logistics/client/UtilityDuctScreen.java`: an `ItemPicker` over the GUI with every registered item but air, built once per session and sorted by name (design D2); a left click on a filter slot with an empty cursor opens it for that slot and sends nothing (design D3); picking sends `SetDuctFilterPayload`, "none" clears; forward mouse and key events to the popup first and hide slot tooltips while it is open. Add the lang keys for the popup title and the none entry in `foundation/.../logistics/datagen/LogisticsDataParts.java` (the picker's search hint in `foundation/.../datagen/ServusFoundationLanguageProvider.java`) and run `gradlew :foundation:runData`. Verify: `gradlew build` green; in game, with an empty inventory, click a filter slot, type "diam", pick Diamond, and the slot shows it; Escape closes without change; right click clears.

## 4. JEI drag

- [x] 4.1 Add `foundation/.../integration/jei/DuctFilterGhostHandler.java` and register it in `ServusFoundationJeiPlugin.registerGuiHandlers` (design D5); add `UtilityDuctScreen.filterSlotArea(int)` and an `isPickerOpen()` the handler checks. Verify: `gradlew build` green; in game with JEI, dragging an item from the JEI list highlights the nine slots on the Item Pipe page only, dropping sets the slot, and nothing highlights on the Power Cable page.

## 5. Plan and integration

- [ ] 5.1 Full `gradlew build`, `gradlew test` green, the GameTests pass, and the client launches with all three mods (play check from main by the user). Build, unit tests (Foundation 372, Alchemy 199, Storage 575) and GameTests (Foundation 143, Storage 622) are green; waits for the user's play check.

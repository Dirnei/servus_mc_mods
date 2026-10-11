# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/picker/PickerList.java` and `PickerStock.java` per design.md; JUnit tests `PickerListTest` (sort by name case insensitive with the none entry first, ties by id, blank query keeps all, search by name and by id, none entry kept by a search) and `PickerStockTest` (signature ignores map order, changes with a count and with the networked flag, short counts) under `storage/src/test/java/io/github/dirnei/mc/servus/storage/picker/`. Run `gradlew :storage:test`.

## 2. Server side

- [x] 2.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/network/WarehouseNetwork.java`: `stockCount(ItemKey)`. `storage/src/main/java/io/github/dirnei/mc/servus/storage/field/FieldHeadBlockEntity.java`: `cropItems()`, `cropStock()`, `setCrop` syncs the mode and the look. Run `gradlew build`.
- [x] 2.2 `storage/src/main/java/io/github/dirnei/mc/servus/storage/field/ChooseFieldCropPayload.java`, `FieldCropStockPayload.java`, registration in `storage/src/main/java/io/github/dirnei/mc/servus/storage/terminal/ServusStoragePayloads.java`, `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/FieldHeadMenu.java` (no crop slot, no harvest only button, `choose`, stock sync, client stock state). Grep that every payload type is registered. Run `gradlew build`.

## 3. Client

- [x] 3.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/ItemPicker.java`: the reusable picker widget per design.md. Run `gradlew build`.
- [x] 3.2 `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/CropPickerEntries.java` and `FieldHeadScreen.java`: the crop picker on the Field tab, sends the choice, shows counts and the zero hint. Run `gradlew build`.
- [x] 3.3 `storage/src/main/java/io/github/dirnei/mc/servus/storage/datagen/ServusStorageLanguageProvider.java`: picker keys, toggle keys removed. Run `gradlew :storage:runData`, then `gradlew build`.

## 4. Tests

- [ ] 4.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/gametest/FieldGameTests.java`: replace `menuSetsCropAndHarvestOnly` with `menuChoosesCropById` (wheat by id sets the crop and clears harvest only, harvest only entry, cobblestone and an unknown id refused, no item needed), adapt `menuTabsRouteShiftClick` to the new slot indices, and `menuCropStockCounts` (a head on a warehouse counts the seeds in a crate, a head without one reports no stock). Run `gradlew :storage:runGameTestServer`.

## 5. Docs and plan

- [ ] 5.1 `docs/src/content/docs/mods/storage.md` (Field Head GUI) and `PLAN.md` M10 list (`m10-crop-picker`). Run `openspec validate m10-crop-picker`.

## 6. Verification

- [ ] 6.1 Play check by the user: the Field tab shows the picker button with the crop or "None, harvest only"; the list opens, scrolls, searches, lists modded crops, shows warehouse counts that update while open, greys zero stock entries with the hint; choosing a crop or harvest only sets it without the item in the inventory; the top plate shows the new crop.

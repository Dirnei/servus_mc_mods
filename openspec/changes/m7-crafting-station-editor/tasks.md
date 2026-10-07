# Tasks

## 1. Shared editor in Lib

- [x] 1.1 Add `lib/client/RecipeEditorPopup.java` and `lib/client/RecipeEditorButtons.java` (D1). Switch `manufacturing/client/AssemblerScreen.java` and `menu/AssemblerMenu.java` to them with unchanged positions and ids. Verify `gradlew build` and `gradlew :manufacturing:runGameTestServer`.

## 2. Station editor

- [x] 2.1 Editor state in `storage/crafting/CraftingStationMenu.java` (D2): synced state, ghost slots active only while editing, open new, open edit, save or replace, delete, cancel, clear on close; active recipe in the station's container data in `CraftingStationBlockEntity.java`. Verify `gradlew build`.
- [x] 2.2 Redo `storage/client/CraftingStationScreen.java` with the D3 layout and the Lib popup, generate the new `textures/gui/crafting_station.png`, lang keys in the language provider. Run `gradlew :storage:runData` and `gradlew build`.
- [x] 2.3 GameTests in `gametest/ServusStorageGameTests.java`: save sticks through the editor, no match keeps the editor open, edit replaces, delete removes, cancel keeps, slots full refused. Fix the carrot wording in the spec if code differs. Verify `gradlew :storage:runGameTestServer`.

## 3. JEI transfer

- [x] 3.1 JEI lines in `storage/build.gradle`, `storage/integration/jei/ServusStorageJeiPlugin.java`, `CraftingStationRecipeTransferHandler.java`, `crafting/OpenStationEditorPayload.java` registered in `terminal/ServusStoragePayloads`, `CraftingStationMenu.openEditorWith`. Grep that every payload type is registered. Verify `gradlew build`.

## 4. Verification

- [x] 4.1 Run `openspec validate m7-crafting-station-editor --strict`. Full `gradlew build` and `gradlew test` green, all GameTests green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 4.2 Play check: station GUI layout, plus opens the editor with a live preview, edit and delete through the editor, JEI transfer opens the editor, the Assembler editor still looks and works as before.

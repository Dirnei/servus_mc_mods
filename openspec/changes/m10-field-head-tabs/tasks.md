# Tasks

## 1. Menu

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/FieldHeadMenu.java`: `Page` enum, client page state with `page()` and `setPage`, `pageOf(int)`, slot subclasses inactive off their page on the client, new slot positions (crop and range 8,18, anti-corrosion 8,58), inventory 96 and hotbar 154. Run `gradlew build`.

## 2. Screen and lang

- [x] 2.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/FieldHeadScreen.java`: tabs in the duct look with icons and tooltips, red markers, Field, Water and Upgrades pages per design.md, 176 by 178 panel in one blit. Run `gradlew build`.
- [x] 2.2 `storage/src/main/java/io/github/dirnei/mc/servus/storage/datagen/ServusStorageLanguageProvider.java`: tab names, `radius`, `size` without the radius. Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Tests

- [ ] 3.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/gametest/FieldGameTests.java`: GameTest `menuTabsRouteShiftClick` (page of each head slot, honeycomb and a Range Upgrade shift clicked from the inventory land in their slots with the Field page set, and shift click from the slots moves them back). Run `gradlew :storage:runGameTestServer`.

## 4. Docs and plan

- [ ] 4.1 `docs/src/content/docs/mods/storage.md` (Field Head GUI) and `PLAN.md` M10 list (`m10-field-head-tabs`). Run `openspec validate m10-field-head-tabs`.

## 5. Verification

- [ ] 5.1 Play check by the user: the GUI opens on the Field tab, each tab shows its page with the inventory below, honeycomb and Range Upgrades shift click into their slots from any tab, a dry or corroded field marks the Water tab red, a warning work status marks the Field tab red.

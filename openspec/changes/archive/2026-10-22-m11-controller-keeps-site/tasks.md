# Tasks

## 1. Components and block entity

- [x] 1.1 Add `SITE_NAME` and `CONTROLLER_ENERGY` to `shuttles/.../registry/ServusShuttlesComponents.java`; verify with `gradlew :shuttles:compileJava`.
- [x] 1.2 In `shuttles/.../controller/WarehouseControllerBlockEntity.java` collect and apply the two components, discard `SiteName` and `Energy` in `removeComponentsFromTag`, delete `clearEnergyForItem`; in `WarehouseControllerBlock.java` stop clearing FE on dismantle and placement; verify with `gradlew :shuttles:compileJava`.

## 2. Loot, item and lang

- [x] 2.1 Give the controller its own loot table in `shuttles/.../datagen/ServusShuttlesBlockLoot.java` that copies wood, site name and FE; run `gradlew :shuttles:runData` and check `warehouse_controller.json` lists the three components.
- [x] 2.2 Add `shuttles/.../controller/WarehouseControllerItem.java` (tooltip with site name and FE), register it in `ServusShuttlesItems.java`, add the lang keys in `ServusShuttlesLanguageProvider.java`; run `gradlew :shuttles:runData` and `gradlew :alchemy:runData` and commit the generated files and `docs/src/data/gamedata.json` if it changed.

## 3. GameTests

- [x] 3.1 In `shuttles/.../gametest/ControllerSiteGameTests.java` add `minedControllerKeepsNameAndEnergy` (drops from the loot table, tooltip lines, place, name and FE back), extend `nameSavedAndCarriedByCarrot` with FE, add `placedEnergyIsCappedAtCapacity` (fresh item: no tooltip lines, no FE; capped FE), with the shared placement helper `WarehouseFixtures.placeController`; update `ControllerWirelessGameTests.upgradeDropsWhenDismantled` to expect the FE back; run `gradlew :shuttles:runGameTestServer`.

## 4. Verification

- [x] 4.1 Full `gradlew build` and `gradlew test` green, `:shuttles:runGameTestServer` green apart from failures that also fail on main.
- [x] 4.2 User play check: mine and carrot dismantle a named controller with FE, check the tooltip, place it elsewhere, name and FE are back, game launches with all mods.

## Workflow follow-up

- Archive after the play check and groom PLAN.md in the same commit.

# Tasks

Paths are relative to `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/` unless written in full. GameTests run with `gradlew :foundation:runGameTestServer`.

## 1. Item and slots

- [x] 1.1 `upgrade/CapacityUpgradeItem.java` (new, design.md D1), registration in `registry/ServusFoundationItems.java` and the creative tab; datagen item model, lang "Capacity Upgrade" with tooltip, recipe (design.md D3); texture `foundation/src/main/resources/assets/servus_foundation/textures/item/capacity_upgrade.png` by a job script. Run `gradlew :foundation:runData`, `gradlew build`.
- [x] 1.2 `upgrade/CapacityUpgradeSlots.java` (new, design.md D2; shared base with `AnchorUpgradeSlots` only if it removes real duplication). Run `gradlew build`.
- [x] 1.3 Foundation GameTests in the upgrades or machine GameTest class: `capacitySlotTakesOnlyCapacityUpgrades`, `speedSlotRefusesCapacityUpgrade`, `capacityUpgradeRecipe`. Run the Foundation GameTests.

## 2. Plan

- [x] 2.1 `PLAN.md`: add the M13 Fluid logistics milestone with its open points (design.md D4). Run `gradlew build`.

## 3. Verify

- [x] 3.1 Full `gradlew build`, `gradlew test`, the Foundation and Shuttles GameTests green; the game launches with all three mods.
- [x] 3.2 Play check by the user: the Capacity Upgrade shows in the creative tab and JEI with its recipe and tooltip.

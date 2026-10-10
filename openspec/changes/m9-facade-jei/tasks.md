# Tasks

## 1. Example facades

- [x] 1.1 Add `FacadeItem.EXAMPLES` and `FacadeItem.examples()` in `foundation/.../logistics/facade/FacadeItem.java`, and add the example stacks to the Servus tab in `foundation/.../logistics/ServusLogisticsEvents.java` after `CREATIVE_TAB_ORDER`. Add the GameTest `facadeExamplesAreCraftable` in `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`. Verify: `gradlew build`, `gradlew test` and `gradlew runGameTestServer` green.

## 2. JEI

- [ ] 2.1 In `foundation/.../integration/jei/ServusFoundationJeiPlugin.java`: register the facade subtype interpreter in `registerItemSubtypes`, replace the single facade example with one shapeless example recipe per `FacadeItem.EXAMPLES` block, and add the information page with `addItemStackInfo`. Add the `jei.servus_foundation.facade.info` text to `foundation/.../logistics/datagen/LogisticsDataParts.java`. Verify: `gradlew runData`, then `gradlew build`.
- [ ] 2.2 Play check by the user from main with JEI: searching "facade" shows five separate entries, each recipe shows its own block, the information page shows, the iron ingot's uses list the facade examples, and the Servus creative tab shows the facades. Verify: the user confirms in game.

## 3. Plan

- [ ] 3.1 `PLAN.md`: add M9 item 1c `m9-facade-jei`. Verify: `openspec validate m9-facade-jei` passes.

## 4. Integration

- [ ] 4.1 Full gradle build, tests green, game launches with all three mods.

# Tasks

## 1. Layout

- [x] 1.1 Pure `alchemy/athanor/AthanorLayout` (hearth cells, `schematic(extra)` with layers and mapping) with JUnit `AthanorLayoutTest`; `gradlew :manufacturing:test` green

## 2. Forming

- [x] 2.1 `FORMED` on `AthanorBlock`; Journal forms or names the missing brick (lang); `neighborChanged` unforms on the brick below; `checkTier` checks the hearth and unforms; `canCraft` needs `formed()`; the forming player owns it; blockstate datagen keeps the models for every `FORMED`; GameTests (forming, missing brick, not formed no craft, corner brick unforms) and existing Athanor GameTests build and form first; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green

## 3. Schematics and journal

- [x] 3.1 `AthanorTierData.SHIPPED`; `ServusManufacturingMultiblockProvider` writes `athanor_nigredo` and `athanor_albedo` from `AthanorLayout` and the tier JSON on the classpath; Athanor and Salt Crystal Block pages get their multiblock pages and text; GameTest that both schematics accept their builds; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 3.2 PLAN.md M9 item 6d; no em dashes

## 4. Integration

- [ ] 4.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches, and the user play checks the Athanor hearth and the schematics

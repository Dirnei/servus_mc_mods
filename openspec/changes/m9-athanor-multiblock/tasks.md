# Tasks

## 1. Layout

- [x] 1.1 Pure `alchemy/athanor/AthanorLayout` (hearth cells, `schematic(extra)` with layers and mapping) with JUnit `AthanorLayoutTest`; `gradlew :manufacturing:test` green

## 2. Forming

- [x] 2.1 `FORMED` on `AthanorBlock`; Journal forms or names the missing brick (lang); `neighborChanged` unforms on the brick below; `checkTier` checks the hearth and unforms; `canCraft` needs `formed()`; the forming player owns it; blockstate datagen keeps the models for every `FORMED`; GameTests (forming, missing brick, not formed no craft, corner brick unforms) and existing Athanor GameTests build and form first; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green

## 3. Schematics and journal

- [x] 3.1 `AthanorTierData.SHIPPED`; `ServusManufacturingMultiblockProvider` writes `athanor_nigredo` and `athanor_albedo` from `AthanorLayout` and the tier JSON on the classpath; Athanor and Salt Crystal Block pages get their multiblock pages and text; GameTest that both schematics accept their builds; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 3.2 PLAN.md M9 item 6d; no em dashes

## 4. Tanks

- [x] 4.1 `AthanorLayout.tanks(Facing)` and the tanks in `schematic`, with `AthanorLayoutTest` (three offsets per facing, none touching, Nigredo pattern with tanks); `gradlew :manufacturing:test` green
- [x] 4.2 `AthanorBlockEntity` reads and drains the three tanks, `PrincipleTanks` removed (save, bucket use, `fluidHandler` registration), forming and the slow check include the tanks with a named missing tank (lang), menu data from the tanks; GameTests: craft from a piped tank, any principle in any tank, missing tank named, removed tank unforms, existing Athanor tests fill tanks; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [ ] 4.3 Schematic pages with the tanks, Athanor journal text, PLAN.md item 6d; no em dashes; schematic GameTests accept the builds; `gradlew build` and `:manufacturing:runGameTestServer` green

## 5. Integration

- [ ] 5.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches, and the user play checks the Athanor hearth and the schematics

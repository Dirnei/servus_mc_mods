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
- [x] 4.3 Schematic pages with the tanks, Athanor journal text, PLAN.md item 6d; no em dashes; schematic GameTests accept the builds; `gradlew build` and `:manufacturing:runGameTestServer` green

## 5. Look and no GUI

- [x] 5.1 No GUI: `AthanorBlock` hand use (result by empty hand, input back by sneaking), `AthanorMenu`, `AthanorScreen`, the menu type and tier data removed, renderer shows input or result with sync on both, Jade Athanor provider (lang); GameTests: result taken by hand, item placed by hand; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 5.2 Formed tower models per tier and lit state with the fire door and sockets, blockstates by `FORMED` and facing, `noOcclusion` on the block; `AthanorRenderer` pipes to the tanks (over the ring, down into the top or into the side of a stacked tank) and the item on the plate; journal text; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors

## 6. Diamond hearth and grown Salt Crystal Block

- [x] 6.1 `AthanorLayout.hearth()` as the 13 cell diamond, `athanor_tier/albedo.json` as the twelve cells around it in the hearth layer, `AthanorLayoutTest` (13 cells, tanks on the tips, Nigredo and Albedo patterns) and `AthanorTiersTest` as needed; `gradlew :manufacturing:test` green
- [x] 6.2 Salt Crystal Block crafting and split recipes removed, Athanor craft `salt_crystal_block` in `AlchemyRecipeTable`, journal text for the hearth and the Salt Crystal Block page, PLAN.md item 6d; GameTests build the diamond and the new ring, a Salt Crystal grows into a block; no em dashes; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches

## 7. Copper Ducts

- [x] 7.1 Rename `SULFUR_DUCT` to `COPPER_DUCT` (id, models, lang, recipe, loot, tags, journal, capability, Jade) and `CalcinerDuctBlock` to `structure/CopperDuctBlock`; Calciner GameTests still green; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 7.2 `AthanorLayout.ducts(Facing)` and the ducts in `schematic` with `AthanorLayoutTest` (two cells per tank, over the tank, patterns with `D`); `gradlew :manufacturing:test` green
- [x] 7.3 Athanor duct pieces 3 and 4 with models, forming and unforming the ducts, missing duct named (lang), stacked tank in the far cell, Calciner code ignores pieces 3 and 4, duct breaks unform the Athanor at once, renderer pipes removed, pipes at the bottom of the duct with their own collision shape, sockets at 16 to 20 px, formed collision shape of the tower, journal text; GameTests: ducts formed with the Athanor, missing duct named, broken duct unforms, stacked tank forms without the far duct, all Athanor tests build ducts; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors

## 8. Integration

- [ ] 8.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches, and the user play checks the Athanor hearth and the schematics

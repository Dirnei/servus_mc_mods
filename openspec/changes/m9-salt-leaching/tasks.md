# Tasks

## 1. Ash

- [x] 1.1 Pure `alchemy/AshCount` (whole Ash from mB and a Salt per Ash, mB taken for n Ash) with JUnit `AshCountTest` (exact, remainder, zero, never negative); `gradlew :manufacturing:test` green
- [x] 1.2 Ash item with a generated grey texture, item model, lang; `PurificationRecipe` with type and serializer in `ServusManufacturingRecipes`, datagen of `purification/ash.json` (1 Ash, 100 mB water, 100 mB Salt, 40 ticks); `gradlew :manufacturing:runData` and `gradlew build` green

## 2. Ash Pan gives Ash

- [x] 2.1 `CalcinerBlockEntity`: Salt per Ash from the purification recipes (cached), `takeAsh(n)`, `ashCount()`; `CalcinerPorts.AshPort` extract only on the Ash Pan, the Salt fluid view and push removed; empty hand on the Ash Pan gives the Ash; bucket on the Ash Pan does nothing; `CalcinerRenderer` draws an ash heap; capability registration updated; GameTests in `CalcinerGameTests`: hopper under the Ash Pan receives Ash, hand takes Ash, no fluid handler at the Ash Pan, existing tests adjusted (Salt through the Ash Pan); `:manufacturing:runGameTestServer` green

## 3. Purifier

- [x] 3.1 `alchemy/purifier/PurifierBlock`, `PurifierBlockEntity` (slot, water and Salt tanks, cycle, push, owner, `purify` event), machine stats `machine/purifier.json`, registrations, capabilities, hand use, crafting recipe, loot, tag, lang, vat model; GameTests `PurifierGameTests`: leaching three Ash, no water no work, Salt refused and water accepted by the handler, water bucket in and Salt bucket out, journal event counted; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 3.2 `PurifierRenderer` with brine level synced by sixteenths; Jade plugin renamed `ServusManufacturingJadePlugin` with a Purifier provider; JEI `PurificationRecipeCategory`; `gradlew build` green and the client launches without model or plugin errors

## 4. Journal and plan

- [x] 4.1 Villach task `purify` in `journal/villach.json` and the Villach book text; Workshop Purifier entry (machine, recipe, how to use, purification list) after the Calciner; Calciner and Principles pages explain Ash; lang; `gradlew :manufacturing:runData` and `gradlew build` green, the journal GameTest still lists every entry
- [x] 4.2 PLAN.md M9: item 6b2, Tria Prima paragraph, Nigredo table; no em dashes; `gradlew build` green

## 5. Purifier build

- [x] 5.1 Pure `PurifierLayout` with JUnit `PurifierLayoutTest`; Caput Mortuum item (texture, lang); `PurificationRecipe` residue and the shipped 4 Ash recipe; `gradlew :manufacturing:test` and `:manufacturing:runData` green
- [x] 5.2 `CatchBasinBlock` (item, recipe, loot, tag, lang, model), `FORMED` and `HORIZONTAL_AXIS` on the Purifier, `PurifierStructure` (check both axes, form, unform, neighbour checks, Journal message), works only formed, residue slot and item port, Basin Salt port and push, hand use (residue, Ash back, buckets on Vat and Basin), Basin Salt drawn, Jade on the Basin; `PurifierGameTests` build and form first and cover the residue, the Basin port, forming and unforming; `:manufacturing:runGameTestServer` green
- [x] 5.3 Schematic and journal page (multiblock, Vat and Basin recipes, text), PROGRESSION.md diamond path, GameTest that the schematic accepts the build; `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors

## 6. Split outputs and forming looks

- [x] 6.1 `PurifierLayout` with `P V P` over `B L R` and its mirror, `PurifierLayoutTest` updated (both mirrors, placements); `gradlew :manufacturing:test` green
- [x] 6.2 `ResidueCrockBlock` (item, recipe, loot, tag, lang, plain model); `HORIZONTAL_FACING` on Basin and Crock; `PurifierStructure` reworked (both axes and mirrors, Crock and log checks, controller from the side blocks); Vat item port insert only, Crock residue port by block, empty hand on the Crock gives the residue; Crock heap in `PurifierRenderer`; Jade on the Crock; `PurifierGameTests` cover both mirrors, residue by hopper under the Crock, breaking the Crock unforms; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 6.3 Formed models and blockstates for the Vat, the Basin and the Crock in `CalcinerModels`; GameTest that forming sets the formed states with axis and facing and unforming clears them; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors
- [x] 6.4 `alchemy/structure/FormingEffect` called after a successful Journal form in `CalcinerStructure`, `PurifierStructure`, `CoagulatorStructure` and the Athanor; `gradlew build` and `:manufacturing:runGameTestServer` green
- [x] 6.5 Schematic (`ServusManufacturingMultiblockProvider`) and Purifier journal page for the new layout and the Crock recipe, PLAN.md item text; schematic GameTest accepts the build; no em dashes; `gradlew build` and `:manufacturing:runGameTestServer` green
- [ ] 6.6 Drop the planks (play check): `PurifierLayout` `_ V _` over `B L R` with `PurifierLayoutTest`, placements and lang, side spouts in `purifier_formed`, schematic, journal page, PLAN.md and PROGRESSION.md text, `PurifierGameTests` build without planks and unform by the log; `gradlew :manufacturing:test`, `:manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors

## 7. Integration

- [ ] 7.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches with all three mods, and the user play checks Ash from the Calciner through the Purifier into Salt in the Basin and Caput Mortuum in the Crock, the formed looks, and the forming effect on all four builds

# Tasks

## 1. Ash

- [x] 1.1 Pure `alchemy/AshCount` (whole Ash from mB and a Salt per Ash, mB taken for n Ash) with JUnit `AshCountTest` (exact, remainder, zero, never negative); `gradlew :manufacturing:test` green
- [x] 1.2 Ash item with a generated grey texture, item model, lang; `PurificationRecipe` with type and serializer in `ServusManufacturingRecipes`, datagen of `purification/ash.json` (1 Ash, 100 mB water, 100 mB Salt, 40 ticks); `gradlew :manufacturing:runData` and `gradlew build` green

## 2. Ash Pan gives Ash

- [x] 2.1 `CalcinerBlockEntity`: Salt per Ash from the purification recipes (cached), `takeAsh(n)`, `ashCount()`; `CalcinerPorts.AshPort` extract only on the Ash Pan, the Salt fluid view and push removed; empty hand on the Ash Pan gives the Ash; bucket on the Ash Pan does nothing; `CalcinerRenderer` draws an ash heap; capability registration updated; GameTests in `CalcinerGameTests`: hopper under the Ash Pan receives Ash, hand takes Ash, no fluid handler at the Ash Pan, existing tests adjusted (Salt through the Ash Pan); `:manufacturing:runGameTestServer` green

## 3. Purifier

- [x] 3.1 `alchemy/purifier/PurifierBlock`, `PurifierBlockEntity` (slot, water and Salt tanks, cycle, push, owner, `purify` event), machine stats `machine/purifier.json`, registrations, capabilities, hand use, crafting recipe, loot, tag, lang, vat model; GameTests `PurifierGameTests`: leaching three Ash, no water no work, Salt refused and water accepted by the handler, water bucket in and Salt bucket out, journal event counted; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green
- [ ] 3.2 `PurifierRenderer` with brine level synced by sixteenths; Jade plugin renamed `ServusManufacturingJadePlugin` with a Purifier provider; JEI `PurificationRecipeCategory`; `gradlew build` green and the client launches without model or plugin errors

## 4. Journal and plan

- [ ] 4.1 Villach task `purify` in `journal/villach.json` and the Villach book text; Workshop Purifier entry (machine, recipe, how to use, purification list) after the Calciner; Calciner and Principles pages explain Ash; lang; `gradlew :manufacturing:runData` and `gradlew build` green, the journal GameTest still lists every entry
- [ ] 4.2 PLAN.md M9: item 6b2, Tria Prima paragraph, Nigredo table; no em dashes; `gradlew build` green

## 5. Integration

- [ ] 5.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches with all three mods, and the user play checks Ash from the Calciner through the Purifier into liquid Salt

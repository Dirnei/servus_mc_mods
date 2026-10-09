# Tasks

## 1. Remove the Factorio content

- [x] 1.1 Move the `energyStaysOnItsChannel` GameTest to Foundation (`foundation/.../machine/MachineGameTests.java`) with Electric Pumps as consumers. Verify: Foundation GameTests green.
- [x] 1.2 Delete `manufacturing/.../deposit`, `machine/{extractor,smelter,crusher,assembler}`, `recipe/*`, `network/ConfigureRecipePayload`, `menu/{AssemblerMenu,BurnerExtractorMenu,BurnerSmelterMenu,CrusherMenu,ElectricExtractorMenu,PatternSlot}`, `client/AssemblerScreen`, the JEI assembling and crushing categories and transfer handler, `datagen/{AssemblingRecipeBuilder,CrushingRecipeBuilder}`, and their registry entries, recipe types, payload registration, capabilities, lang, models, loot, tags, recipes and creative tab entries; delete the deposit, biome modifier, worldgen and tech JSON, the deposit, machine front, GUI and item textures of the removed content, and the Factorio GameTests and native upkeep tests of removed machines; drop removed machines from `ManufacturingMachines` and its test. Verify: `gradlew build`, both data runs, Manufacturing GameTests green.
- [x] 1.3 Move the docs Balance lab tests to a fixture: copy `docs/src/data/gamedata.json` to `docs/src/lib/fixtures/tier1-gamedata.json` (before regenerating) and point every `*.test.ts` at it; regenerate `gamedata.json`. Verify: docs tests and docs build pass.

## 2. Principles

- [x] 2.1 Pure `Principle` enum and `Principles` value type in `manufacturing/.../alchemy/` with JUnit tests `PrinciplesTest` (sum, subtract, covers, fits into capacity, negative amounts refused, missing keys as 0). Verify: `gradlew test`.
- [x] 2.2 Fluid types, source and flowing fluids, liquid blocks and buckets for Salt, Sulfur and Mercury in `manufacturing/.../alchemy/PrincipleFluids.java`; client tint extensions; lang, bucket item models and a `PrincipleTanks` fluid handler (three restricted tanks, give or accept mode). GameTest: a bucket fills from a Fluid Tank holding Mercury. Verify: build, data run, GameTests green.

## 3. Calciner

- [x] 3.1 `CalcinationRecipe`, serializer, recipe type and datagen builder; shipped recipes from design assumption 1 in `ServusManufacturingRecipeProvider`. Verify: data run writes the recipes under `data/servus_manufacturing/recipe/calcination/`.
- [x] 3.2 `CalcinerBlock`, `CalcinerBlockEntity` (powered, one input slot, `PrincipleTanks` in give mode, pushes on Auto and Push sides, waits when a tank is full), `CalcinerMenu`, screen with three gauges, registration, capabilities (item, energy, fluid, production), stats JSON, model, lang, loot, tag, shaped recipe, defaults in `ManufacturingMachines`. GameTests: cobblestone becomes 100 Salt and 10 Mercury; full Salt tank stops the machine without spending energy; a touching Fluid Tank receives principle; diamond refused (the Calciner exposes no production, see design D6). Verify: build, data run, GameTests green.

## 4. Coagulator

- [x] 4.1 `CoagulationRecipe`, serializer, recipe type, datagen builder and the shipped recipes from design assumption 2; items Salt Crystal, Sulfur and Quicksilver with placeholder textures. Verify: data run writes recipes and item models.
- [x] 4.2 `CoagulatorBlock`, `CoagulatorBlockEntity` (powered, ingredient and output slots, `PrincipleTanks` in accept mode, selected recipe id saved and kept on dismantle), `CoagulatorMenu` with recipe selection by menu button, screen with gauges and a scrolled recipe list, registration, capabilities, stats JSON, model, lang, loot, tag, recipe, defaults. GameTests: piped Salt makes a Salt Crystal; water is refused; missing Mercury waits; selection survives save and load of the block entity; production reports only the selected recipe. Verify: build, data run, GameTests green.
- [ ] 4.3 Play check follow-up: replace the side recipe list with a select button and a recipe picker popup (`foundation/client/RecipePickerPopup` for reuse, grid of results, visible scrollbar, selected marked, Escape or a click outside closes) in `CoagulatorScreen`; lang keys in the Manufacturing language provider. Verify: build, data run, Manufacturing GameTests green, popup checked in the client.

## 5. Integration

- [x] 5.1 JEI Calcination and Coagulation categories in `integration/jei` with the machines as catalysts. Verify: build.
- [x] 5.2 Update `docs/src/content/docs/mods/manufacturing.md` (Calciner, Coagulator, principles, no deposits, research note) and the datapack reference pages (machine table, new recipe types, deposits and assembling sections removed); regenerate `gamedata.json` and icons of new items by copying placeholder textures. Verify: docs tests and docs build pass.
- [ ] 5.3 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: calcine cobblestone, pipe Salt into a Coagulator and make a Salt Crystal, check JEI categories. (Build, unit tests, docs tests and build, and GameTests green in the worktree: Foundation 70, Manufacturing 10, Storage 233. The client part is owed to the play check from main.)

## 6. Play check follow-ups: fire, time and stone

- [x] 6.1 Team ownership as a `TeamOwnership` helper in `alchemy` used by Calciner, Coagulator, Athanor and Gnome Hollow; `AlchemyMachineBlockEntity` removed. Verify: build, Manufacturing GameTests green.
- [x] 6.2 Calciner extends `BurnerMachineBlockEntity` (input and fuel slot, upkeep capability, no energy capability), `CalcinerMenu` fuel slot, screen layout with flame and no energy bar, `ManufacturingMachines` kind burner, stats JSON; GameTests fuel the Calciner with coal and check that it exposes no energy storage. Verify: build, data run, GameTests green.
- [x] 6.3 Coagulator extends `MachineBlockEntity` with a timed tick (no energy capability), screen layout without energy bar, kind and stats JSON; GameTests run it without power. Verify: build, data run, GameTests green.
- [x] 6.4 Stone look: Calciner, Coagulator, Athanor, Salt Crystal Block and Gnome Hollow models reference vanilla stone textures, with own front textures for the Calciner (furnace mouth), Coagulator (mould) and Athanor (arched fire per tier); the Gnome Hollow stays plain stone as its spec requires. Verify: data run without missing texture warnings.
- [x] 6.5 Docs pages and `gamedata.json` describe the fuelled Calciner and the unpowered Coagulator. Verify: docs tests, all GameTests and `gradlew build test` green. (Docs tests 50, GameTests Foundation 78, Manufacturing 29, Storage 249. The docs icons of the Calciner, Coagulator and Athanor still show the old textures until the icon dev run renders them from main.)

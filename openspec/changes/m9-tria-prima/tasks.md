# Tasks

## 1. Remove the Factorio content

- [x] 1.1 Move the `energyStaysOnItsChannel` GameTest to Foundation (`foundation/.../machine/MachineGameTests.java`) with Electric Pumps as consumers. Verify: Foundation GameTests green.
- [x] 1.2 Delete `manufacturing/.../deposit`, `machine/{extractor,smelter,crusher,assembler}`, `recipe/*`, `network/ConfigureRecipePayload`, `menu/{AssemblerMenu,BurnerExtractorMenu,BurnerSmelterMenu,CrusherMenu,ElectricExtractorMenu,PatternSlot}`, `client/AssemblerScreen`, the JEI assembling and crushing categories and transfer handler, `datagen/{AssemblingRecipeBuilder,CrushingRecipeBuilder}`, and their registry entries, recipe types, payload registration, capabilities, lang, models, loot, tags, recipes and creative tab entries; delete the deposit, biome modifier, worldgen and tech JSON, the deposit, machine front, GUI and item textures of the removed content, and the Factorio GameTests and native upkeep tests of removed machines; drop removed machines from `ManufacturingMachines` and its test. Verify: `gradlew build`, both data runs, Manufacturing GameTests green.
- [x] 1.3 Move the docs Balance lab tests to a fixture: copy `docs/src/data/gamedata.json` to `docs/src/lib/fixtures/tier1-gamedata.json` (before regenerating) and point every `*.test.ts` at it; regenerate `gamedata.json`. Verify: docs tests and docs build pass.

## 2. Principles

- [ ] 2.1 Pure `Principle` enum and `Principles` value type in `manufacturing/.../alchemy/` with JUnit tests `PrinciplesTest` (sum, subtract, covers, fits into capacity, negative amounts refused, missing keys as 0). Verify: `gradlew test`.
- [ ] 2.2 Fluid types, source and flowing fluids, liquid blocks and buckets for Salt, Sulfur and Mercury in `manufacturing/.../alchemy/PrincipleFluids.java`; client tint extensions; lang, bucket item models and a `PrincipleTanks` fluid handler (three restricted tanks, give or accept mode). GameTest: a bucket fills from a Fluid Tank holding Mercury. Verify: build, data run, GameTests green.

## 3. Calciner

- [ ] 3.1 `CalcinationRecipe`, serializer, recipe type and datagen builder; shipped recipes from design assumption 1 in `ServusManufacturingRecipeProvider`. Verify: data run writes the recipes under `data/servus_manufacturing/recipe/calcination/`.
- [ ] 3.2 `CalcinerBlock`, `CalcinerBlockEntity` (powered, one input slot, `PrincipleTanks` in give mode, pushes on Auto and Push sides, waits when a tank is full), `CalcinerMenu`, screen with three gauges, registration, capabilities (item, energy, fluid, production), stats JSON, model, lang, loot, tag, shaped recipe, defaults in `ManufacturingMachines`. GameTests: cobblestone becomes 100 Salt and 10 Mercury; full Salt tank stops the machine without spending energy; a touching Fluid Tank receives principle; diamond refused; production lists the calcination recipes. Verify: build, data run, GameTests green.

## 4. Coagulator

- [ ] 4.1 `CoagulationRecipe`, serializer, recipe type, datagen builder and the shipped recipes from design assumption 2; items Salt Crystal, Sulfur and Quicksilver with placeholder textures. Verify: data run writes recipes and item models.
- [ ] 4.2 `CoagulatorBlock`, `CoagulatorBlockEntity` (powered, ingredient and output slots, `PrincipleTanks` in accept mode, selected recipe id saved and kept on dismantle), `CoagulatorMenu` with recipe selection by menu button, screen with gauges and a scrolled recipe list, registration, capabilities, stats JSON, model, lang, loot, tag, recipe, defaults. GameTests: piped Salt makes a Salt Crystal; water is refused; missing Mercury waits; selection survives save and load of the block entity; production reports only the selected recipe. Verify: build, data run, GameTests green.

## 5. Integration

- [ ] 5.1 JEI Calcination and Coagulation categories in `integration/jei` with the machines as catalysts. Verify: build.
- [ ] 5.2 Update `docs/src/content/docs/mods/manufacturing.md` (Calciner, Coagulator, principles, no deposits, research note) and the datapack reference pages (machine table, new recipe types, deposits and assembling sections removed); regenerate `gamedata.json` and icons of new items by copying placeholder textures. Verify: docs tests and docs build pass.
- [ ] 5.3 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: calcine cobblestone, pipe Salt into a Coagulator and make a Salt Crystal, check JEI categories.

# Tasks

## 1. Veins and Seeds

- [x] 1.1 Add `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/vein/coal.json`, `quartz.json` and `emerald.json` with the numbers of the design; add `coal`, `quartz` and `emerald` to the shipped list in `alchemy/src/test/java/.../hollow/VeinFilesTest.java`. Verify: `gradlew :alchemy:test` green.
- [x] 1.2 Three `seed(...)` rows in `datagen/AlchemyRecipeTable.java` (coal tier 1 from `Items.COAL`, quartz tier 2 from `Tags.Items.GEMS_QUARTZ`, emerald tier 3 from `Tags.Items.GEMS_EMERALD`) and the vein names in `datagen/ServusAlchemyLanguageProvider.java`; run `:alchemy:runData`. Verify: the three `recipe/athanor/seed_of_*.json` files and lang entries are generated, `gradlew build` green.
- [x] 1.3 GameTests in `gametest/HollowGameTests.java`: `seedOfCoalOnAlbedo`, `seedOfQuartzNeedsCitrinitas`, `seedOfEmeraldNeedsRubedo`. Verify: `:alchemy:runGameTestServer` green apart from the known FTB Chunks Ender Anchor tests.

## 2. Docs

- [x] 2.1 Rewrite `docs/src/content/docs/reference/datapack/veins.md` for the current vein format (no `samples` or `chapter`) with all eight veins and their Seeds, and the Gnomes steps in `docs/src/content/docs/mods/alchemy.md` (Salt Crystal, Seed, swap). Verify: docs `npm run build` green, no em dash in the diff.

## 3. Verification

- [x] 3.1 Full `gradlew build test :alchemy:runGameTestServer` green apart from the known FTB Chunks anchor tests; the game launches with all three mods; user play check: the three Seeds in the creative tab and JEI with their names and colours, made at the right Athanor stages, planted, and their colours readable in the cracks under the sight.

# Tasks

## 1. Athanor tiers

- [ ] 1.1 Pure tests first: `AthanorTiersTest` `citrinitasNeedsAlbedo`, `rubedoOnTopOfCitrinitas`, `shortPillarIsCitrinitas`; `AthanorLayoutTest` `rubedoSchematicHasFourLayers` (design D1, D2). Verify: `gradlew :alchemy:test` green.
- [ ] 1.2 Data `athanor_tier/citrinitas.json` and `rubedo.json` (D1 to D3), `AthanorTierData.SHIPPED`; Jade stage name (D4) in `integration/jade/ServusAlchemyJadePlugin.java`; Athanor Workshop "Its Fire" text names the yellow and red flames; `:alchemy:runData` writes `athanor_citrinitas` and `athanor_rubedo`. GameTests in `AlchemyGameTests`: `citrinitasRingMakesTierTwo` (with salt ring tier 2 and block state 2, without the salt ring tier 0, a missing amethyst tier 1), `rubedoPillarsMakeTierThree` (tier 3, a missing top block tier 2), extend `athanorSchematicsMatchTheBuilds` with the Citrinitas and Rubedo schematics. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.

## 2. Noble veins

- [ ] 2.1 Data `vein/gold.json`, `redstone.json`, `diamond.json` (D5). GameTests in `HollowGameTests`: `goldNeedsSchwaz` (raw gold refused after Villach only, seeds after Schwaz), `goldOreOfTheTagSeeds` (Nether Gold Ore), `redstoneAndDiamondSeed` (redstone dust and a diamond seed their veins after Schwaz), `diamondGnomeYieldsDiamonds` (ripe bound diamond Hollow stores a Diamond after its yield time). Verify: `gradlew build`, `:alchemy:runGameTestServer` green.

## 3. Chapters

- [ ] 3.1 Data `journal/wandering_years.json` and `basel_salzburg.json` (D6). JUnit `JournalLedgerTest` `chainOpensOneAfterAnother` (a three chapter chain counts only in the open chapter). GameTests in `AlchemyGameTests`: `citrinitasCraftCountsForWanderingYears` (a craft on a Citrinitas Athanor counts only once Schwaz is finished), `wanderingYearsFinishesAndOpensBasel` (recorded events finish the chapter and Basel and Salzburg takes events). Verify: `gradlew build`, `:alchemy:test`, `:alchemy:runGameTestServer` green.
- [ ] 3.2 Bridge and book (D6 to D8): `JournalAdvancements.CHAPTERS`, `JournalResearch` nodes for Schwaz, Wandering years and Basel and Salzburg, `OpusCategory` entries with conditions, `JournalEntries.WanderingYears` and `BaselSalzburg` (story pages starting with `[storyteller]`, task and unlock pages), Workshop entries `amethyst_ring` and `crying_obsidian` with their schematics in `WorkshopCategory`, lang keys `journal.servus_alchemy.wandering_years`, `basel_salzburg` and `.finished` in `ServusAlchemyLanguageProvider`; `:alchemy:runData`. GameTest `newChapterAdvancementsExist` (both advancements loaded). Verify: `gradlew build`, `:alchemy:runGameTestServer` green, generated lang holds the `[storyteller]` texts, no em dash in the diff.

## 4. Nigredo goods

- [ ] 4.1 GameTests in `CoagulatorGameTests`: `coagulatorMakesClay` and `coagulatorMakesBoneMeal` (selected recipe, principles poured into the vats, result on the casting table). Verify: `:alchemy:runGameTestServer` green.

## 5. Docs and integration

- [ ] 5.1 Docs: `docs/src/content/docs/mods/alchemy.md` (stages Citrinitas and Rubedo, noble veins, chapters), `reference/datapack/athanor.md` (shipped tiers), `veins.md` (shipped veins), `journal.md` (shipped chapters). Verify: no em dash in the diff, `gradlew build` green.
- [ ] 5.2 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 5.3 User play check in game:
  - Twelve Amethyst Blocks on the salt ring turn the Athanor's flame yellow, Jade shows "Stage: Citrinitas"; four Crying Obsidian pillars two high turn it red, "Stage: Rubedo".
  - The Yellow Ring and The Red Crown Workshop schematics Visualize and match the build.
  - After Schwaz the Wandering years entry and The Yellow Ring unlock; raw gold seeds a vein under the sight, before Schwaz it does not.
  - Wandering years and Basel and Salzburg story pages read as placeholders starting with "[storyteller]"; `/servus_alchemy journal` lists their tasks.
  - The Coagulator makes Clay Ball and Bone Meal.

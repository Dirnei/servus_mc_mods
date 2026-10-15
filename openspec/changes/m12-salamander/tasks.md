# Tasks

## 1. Pure logic

- [x] 1.1 `salamander/SalamanderTemper.java` (record, validation, `befriends(roll)`, `DEFAULT`) and `salamander/Digestion.java` (countdown: `start`, `tick` returns true once done, `busy`, `remaining`). JUnit `SalamanderTemperTest` (`rejectsChanceOutsideZeroToOne`, `rejectsDigestBelowOneTick`, `rollBelowChanceBefriends`, `chanceOneAlwaysBefriends`, `chanceZeroNever`) and `DigestionTest` (`idleIsNotBusy`, `finishesAfterTicks`, `finishesOnlyOnce`, `restoredRemainingCounts`). Verify: `gradlew :alchemy:test` green.

## 2. Entity and items

- [x] 2.1 Entity: `salamander/Salamander.java` (D1, D2, D4, D5), `LieInCampfireGoal`, `SalamanderNavigation`, `SalamanderTemperData` (D6) registered in `AlchemyEvents`, `registry/ServusAlchemyEntities.java` with the entity type, attributes and spawn placement (D7) registered from `ServusAlchemy`; data `salamander_temper/salamander.json` and `neoforge/biome_modifier/salamander.json`. Items `SAL_AMMONIAC`, `IGNIS_PERPETUUS` (`QuoteItem`), `SALAMANDER_SPAWN_EGG` in `ServusAlchemyItems` and the creative tab. Verify: `gradlew build` green.
- [x] 2.2 Client: `client/salamander/SalamanderModel`, `SalamanderRenderer` (D3 hiding, D9 full bright), `SalamanderRenderState`, layer and renderer registration in `ServusAlchemyClient`; entity texture and item textures from a generator script in the job directory; Jade ray trace callback in `ServusAlchemyJadePlugin`; item models and lang (entity name, items, quotes) in datagen; `:alchemy:runData`. List the coplanar check of D8 against the code. Verify: `gradlew build` green, generated models and lang present, no em dash in the diff.
- [x] 2.3 GameTests in new `SalamanderGameTests` (registered in `ServusAlchemy`): `goldBefriendsWithChanceOne` (owner set, one ingot used), `goldWithChanceZeroOnlyUsesTheIngot`, `ownerEmptyHandTogglesSitting` (a stranger's click does nothing), `befriendedDigestsGoldIntoSalAmmoniac` (one Sal Ammoniac drops after the digest time, a second ingot is refused while digesting), `strangerCannotFeed`, `fireImmuneInLava` (no damage after standing in lava and fire), `tamedDoesNotDespawnWildDoes`, `shippedTemperAndSpawnsLoad` (temper file and biome modifier loaded), `spawnsOnlyOnOpenLava`. Verify: `:alchemy:runGameTestServer` green.

## 3. Ignis Perpetuus and journal

- [x] 3.1 `AlchemyRecipeTable.athanor()` gains `ignis_perpetuus` (D11); `:alchemy:runData` writes the recipe. GameTests `ignisNeedsCitrinitas` (made on Citrinitas, nothing on Albedo) and `ignisIsNotAFuel` in `SalamanderGameTests`. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.
- [x] 3.2 Workshop entry `salamander` in `WorkshopCategory` (D12), `:alchemy:runData`. Docs: Salamander section in `docs/src/content/docs/mods/alchemy.md`, new `reference/datapack/salamander.md` (temper file, biome modifier). Verify: `gradlew build` green, generated book entry present, no em dash in the diff.

## 4. Spawning after the play check

- [x] 4.1 Pure `salamander/SalamanderSpawn.java` (`crowdAllows(wildNearby, max)`) and the `spawn` object in `SalamanderTemper` (home radius 0 to 4, crowd radius, crowd max, biome chance, defaults, validation; the codec in `SalamanderTemperData` reads it, every field optional). JUnit `SalamanderSpawnTest` (`crowdAtMaxRefuses`, `crowdBelowMaxAllows`, `crowdMaxZeroRefusesAll`) and new cases in `SalamanderTemperTest` (`spawnDefaultsWithoutObject`, `rejectsNegativeRadius`, `rejectsBiomeChanceAboveOne`). Verify: `gradlew :alchemy:test` green.
- [x] 4.2 Tags in datagen: block tag `servus_alchemy:salamander_home` in `ServusAlchemyBlockTagsProvider` and biome tag `servus_alchemy:salamander_lava` in a new `ServusAlchemyBiomeTagsProvider` registered in `ServusAlchemyDataGen`; `:alchemy:runData`. Verify: generated tag files present, `gradlew build` green.
- [x] 4.3 Spawning: `salamander/SalamanderPlacement.java` (ground or lava), the spawn rule in `Salamander.checkSpawnRules` (D7) and its registration in `registry/ServusAlchemyEntities.java`; drop the wild lava preference in `getWalkTargetValue` (D1); replace `neoforge/biome_modifier/salamander.json` with `salamander_forest.json` and `salamander_lava.json`; add the `spawn` object to `salamander_temper/salamander.json`. Verify: `gradlew build` green.
- [ ] 4.4 GameTests in `SalamanderGameTests`: `spawnsNearCrimsonStems`, `spawnsNearNetherWart`, `refusesBareNylium`, `refusesWhenCrowded`, `petsDoNotCrowd`, `stillSpawnsOnOpenLava`, `refusesLavaOutsideLavaBiomes`; update `shippedTemperAndSpawnsLoad` for the two biome modifiers and the spawn numbers; replace `spawnsOnlyOnOpenLava` if it is covered. Verify: `:alchemy:runGameTestServer` green.
- [ ] 4.5 Journal and docs: first page of the Workshop entry `salamander` in `WorkshopCategory` becomes "Among the Crimson Stems" (D12); `docs/src/content/docs/mods/alchemy.md` Salamander section and `docs/src/content/docs/reference/datapack/salamander.md` (spawn object, both biome modifiers, the two tags); `:alchemy:runData`. Verify: `gradlew build` green, docs `npm run build` green, no em dash in the diff.
- [ ] 4.6 Full `gradlew build test :alchemy:runGameTestServer` green.

## 5. Integration

- [x] 5.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 5.2 User play check in game:
  - In a Crimson Forest, without Elemental Sight: no salamander drawn, only a flame among the stems now and then; with the sight salamanders show near the crimson trees and Nether Wart, not crowded (a few per area), glow, sway their tails, crests flicker; no flicker of faces from any angle. In a Warped Forest a bit fewer; on lava lakes in the Nether Wastes or Basalt Deltas now and then one.
  - Gold Ingots befriend it after a few tries (hearts), then it shows without the sight, follows, teleports, sits on an empty hand click, lies in a lit campfire nearby.
  - Feeding it a Gold Ingot gives one Sal Ammoniac after about 60 s; a second ingot is refused meanwhile.
  - Sal Ammoniac with 500 mB Sulfur on a yellow Athanor makes Ignis Perpetuus; tooltips read in Paracelsus' voice; the Workshop entry "The Salamander" reads well; spawn egg works.

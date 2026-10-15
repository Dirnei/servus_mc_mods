# Tasks

## 1. Pure logic

- [x] 1.1 `salamander/SalamanderTemper.java` (record, validation, `befriends(roll)`, `DEFAULT`) and `salamander/Digestion.java` (countdown: `start`, `tick` returns true once done, `busy`, `remaining`). JUnit `SalamanderTemperTest` (`rejectsChanceOutsideZeroToOne`, `rejectsDigestBelowOneTick`, `rollBelowChanceBefriends`, `chanceOneAlwaysBefriends`, `chanceZeroNever`) and `DigestionTest` (`idleIsNotBusy`, `finishesAfterTicks`, `finishesOnlyOnce`, `restoredRemainingCounts`). Verify: `gradlew :alchemy:test` green.

## 2. Entity and items

- [ ] 2.1 Entity: `salamander/Salamander.java` (D1, D2, D4, D5), `LieInCampfireGoal`, `SalamanderNavigation`, `SalamanderTemperData` (D6) registered in `AlchemyEvents`, `registry/ServusAlchemyEntities.java` with the entity type, attributes and spawn placement (D7) registered from `ServusAlchemy`; data `salamander_temper/salamander.json` and `neoforge/biome_modifier/salamander.json`. Items `SAL_AMMONIAC`, `IGNIS_PERPETUUS` (`QuoteItem`), `SALAMANDER_SPAWN_EGG` in `ServusAlchemyItems` and the creative tab. Verify: `gradlew build` green.
- [ ] 2.2 Client: `client/salamander/SalamanderModel`, `SalamanderRenderer` (D3 hiding, D9 full bright), `SalamanderRenderState`, layer and renderer registration in `ServusAlchemyClient`; entity texture and item textures from a generator script in the job directory; Jade ray trace callback in `ServusAlchemyJadePlugin`; item models and lang (entity name, items, quotes) in datagen; `:alchemy:runData`. List the coplanar check of D8 against the code. Verify: `gradlew build` green, generated models and lang present, no em dash in the diff.
- [ ] 2.3 GameTests in new `SalamanderGameTests` (registered in `ServusAlchemy`): `goldBefriendsWithChanceOne` (owner set, one ingot used), `goldWithChanceZeroOnlyUsesTheIngot`, `ownerEmptyHandTogglesSitting` (a stranger's click does nothing), `befriendedDigestsGoldIntoSalAmmoniac` (one Sal Ammoniac drops after the digest time, a second ingot is refused while digesting), `strangerCannotFeed`, `fireImmuneInLava` (no damage after standing in lava and fire), `tamedDoesNotDespawnWildDoes`, `shippedTemperLoads`. Verify: `:alchemy:runGameTestServer` green.

## 3. Ignis Perpetuus and journal

- [ ] 3.1 `AlchemyRecipeTable.athanor()` gains `ignis_perpetuus` (D11); `:alchemy:runData` writes the recipe. GameTests `ignisNeedsCitrinitas` (made on Citrinitas, nothing on Albedo) and `ignisIsNotAFuel` in `SalamanderGameTests`. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.
- [ ] 3.2 Workshop entry `salamander` in `WorkshopCategory` (D12), `:alchemy:runData`. Docs: Salamander section in `docs/src/content/docs/mods/alchemy.md`, new `reference/datapack/salamander.md` (temper file, biome modifier). Verify: `gradlew build` green, generated book entry present, no em dash in the diff.

## 4. Integration

- [ ] 4.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 4.2 User play check in game:
  - In the Nether (Nether Wastes or Basalt Deltas lava), without Elemental Sight: no salamander drawn, only a flame now and then; with the sight the salamander shows on the lava, glows, sways its tail, its crest flickers; no flicker of faces from any angle.
  - Gold Ingots befriend it after a few tries (hearts), then it shows without the sight, follows, teleports, sits on an empty hand click, lies in a lit campfire nearby.
  - Feeding it a Gold Ingot gives one Sal Ammoniac after about 60 s; a second ingot is refused meanwhile.
  - Sal Ammoniac with 500 mB Sulfur on a yellow Athanor makes Ignis Perpetuus; tooltips read in Paracelsus' voice; the Workshop entry "The Salamander" reads well; spawn egg works.

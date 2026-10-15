# Tasks

## 1. Pure logic

- [x] 1.1 `homunculus/HomunculusReach.java`, `HomunculusLife.java`, `ElixirUpkeep.java`, `HomunculusSettings.java`, `WhisperClock.java` (D2, D3, D6, D8); `hollow/HollowStore.take(int)` (D4). JUnit: `HomunculusReachTest` (24 offsets, no centre, corners in, 3 out), `HomunculusLifeTest` (grows only when fed, grows up once, works every interval when grown and fed, freed works unfed, saved state resumes), `ElixirUpkeepTest` (takes a bottle only when none in use, one bottle lasts its ticks, starves without a bottle), `WhisperClockTest` (delay inside the bounds, fires once per delay), `HollowStoreTest.takePart` (takes up to the count, never below zero). Verify: `gradlew :alchemy:test` green.

## 2. Blocks and behaviour

- [x] 2.1 `homunculus/HomunculusData.java` (D6) registered in `AlchemyEvents`, data `servus_alchemy/homunculus/homunculus.json`; `GnomeHollowBlockEntity.takeYield(int)` (D4); machine defaults `receptaculum` in `AlchemyMachineDefaults`. Verify: `gradlew build` green.
- [x] 2.2 `ReceptaculumBlock`, `ReceptaculumBlockEntity`, `ReceptaculumMenu` (D1, D5, D14); shared fire particles extracted from `SalamanderForgeBlock` into `HearthFire`; `VasBlock`, `VasBlockEntity` with growth, upkeep, harvest, gifts, whispers, freeing and sync (D2 to D9); registrations in `ServusAlchemyBlocks`, `ServusAlchemyItems` (Sealed Flask as the Vas' block item, creative tab), `ServusAlchemyBlockEntities`, `ServusAlchemyMenus`; item and production capabilities in `ServusAlchemyEvents`. Verify: `gradlew build` green.

## 3. Client

- [ ] 3.1 Textures from a script in the job directory (D12); `ReceptaculumScreen` registered in `ServusAlchemyClient` (D14); `VasRenderer` registered (D10). Verify: `gradlew build` green.
- [ ] 3.2 Models and block states in `AlchemyModels` (D11), item model for the Sealed Flask, loot (`dropSelf` for both), pickaxe tag, lang (names, screen labels, whispers and secret placeholders); `:alchemy:runData`, commit generated files. Verify: `gradlew build` green.

## 4. Recipes and journal

- [ ] 4.1 Athanor `sealed_flask` in `AlchemyRecipeTable` and crafting `receptaculum` in `ServusAlchemyRecipeProvider` (D13); Workshop entries `receptaculum` and `vas_hermeticum` (D15); docs section in `docs/src/content/docs/mods/alchemy.md`; `:alchemy:runData`. Verify: `gradlew build` green, no em dash and no "star" in the journal, docs or lang outside the whisper lines.

## 5. Behaviour tests

- [ ] 5.1 GameTests in new `HomunculusGameTests` (registered in `ServusAlchemy`): `vasOnlyOnReceptaculum`, `vasBreaksWithReceptaculum`, `growsWithElixir`, `pausesWithoutElixir`, `harvestsHollowsInReach`, `ignoresOtherLevels`, `stopsWithoutElixir`, `deliversGifts`, `dislikedGiftStays`, `netherStarFreesAndDoubles`, `starDoesNothingWhileGrowing`, `pipeTakesTheYield`, `slotRules`, `exposesProduction`, `receptaculumRecipeShipped`, `sealedFlaskNeedsRubedo`. Verify: `:alchemy:runGameTestServer` green.

## 6. Verification

- [ ] 6.1 Full `gradlew build test :alchemy:runGameTestServer` green; client smoke start to the title screen, killed by its command line, no errors mentioning servus; the FTB "Missing entity icon settings for servus_alchemy:salamander" warning fixed in a separate commit if it is a simple asset.
- [ ] 6.2 User play check in game:
  - The Receptaculum reads as a hearth chest: brick hearth with a grated fire and flames, iron banded deepslate body, tile lid, brass collar; it lights the room; no flicker from any angle.
  - The Sealed Flask places only on a Receptaculum; the Vas looks like a glass flask with murky liquid and a red seal; no flicker in the glass or liquid from any angle.
  - The screen shows 27 yield slots, 3 gift slots and the Elixir slot with labels; the side panel works.
  - With Elixir the homunculus grows (shortened with a datapack or by waiting); under the sight it is a pale little figure in the flask and chains sag from the neck to each Hollow in reach.
  - It empties Hollows in reach into the yield and feeds gnomes cookies; an Item Pipe takes the ore out; on a data line a Crafting CPU orders the ore.
  - Whispers appear in the action bar under the sight; a Nether Star breaks the seal, the secret appears in chat, the homunculus sits on the rim, the yield doubles without Elixir.
  - The Workshop entries "The Receptaculum" and "The Vas Hermeticum" read well and never mention a star.

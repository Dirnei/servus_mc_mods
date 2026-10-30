# Tasks

Apply after `m14-mixer` is archived (group 9 needs Foundation's `MixingRecipe`).

## 1. Pure logic

- [x] 1.1 Add `Facing.ground(right, forward)` in `alchemy/.../alchemy/structure/Facing.java` and `Structures.groundPos` in `structure/Structures.java`. Add the pure `alchemy/.../alchemy/venter/VenterLayout.java` (cells, roles, pieces, turns, `FRAME_BLOCKS`, `placement`, `patternRows`). Add JUnit `VenterLayoutTest`: the table, the pattern `DDD`/`DCD`/`DHD`, the placements, `ground` in all four facings, and every frame piece facing away from the Cucurbit. Verify: `gradlew :alchemy:test` green.
- [x] 1.2 Add the pure `venter/StrawBed.java`, with JUnit `StrawBedTest`: full and refused, burn drops the oldest bale, level with a part-burned bale, empty level 0, mixed warmths, save and load pairs. Verify: `gradlew :alchemy:test` green.
- [x] 1.3 Add the pure `venter/PutrefactionWork.java`, with JUnit `PutrefactionWorkTest`: 1,200 / 4,800 gives 1,200, 4,800 and 1,920 ticks at full, empty and half straw; points per tick are never zero; no overflow at 72,000 / 72,000. Verify: `gradlew :alchemy:test` green.
- [x] 1.4 Add the pure `venter/RotStore.java`, with JUnit `RotStoreTest`: capacity and room, mixed kinds, all-or-nothing take, oldest kinds first, drain. Verify: `gradlew :alchemy:test` green.
- [x] 1.5 Add the pure `venter/PutrefactionText.java` (`line`, `mixLine`), with JUnit `PutrefactionTextTest`. Verify: `gradlew :alchemy:test` green.

## 2. Items, data and recipes

- [x] 2.1 Add `tools/venter-textures/gen.py` and generate `humus.png` and `grass_seed.png` into `alchemy/src/main/resources/assets/servus_alchemy/textures/item/`. Verify: the 8x preview reads as a dark soil heap and as seeds with a sprout, next to Ash and Wheat Seeds.
- [x] 2.2 Register `HUMUS` (`QuoteItem`) and `GRASS_SEED` (`venter/GrassSeedItem`, `useOn` with `grassCouldLive`) in `ServusAlchemyItems`, with item models in `ServusAlchemyItemModelProvider` and names and quotes in `ServusAlchemyLanguageProvider`. Verify: `gradlew build` green.
- [x] 2.3 Add `venter/PutrefactionRecipe.java` (codec with an optional `cold_time`), `PUTREFACTION` type and serializer in `ServusAlchemyRecipes`, and `ServusRecipes.syncToClients(PUTREFACTION)` in `ServusAlchemy`. Add `AlchemyRecipeTable.Putrefy` and `putrefaction()`, and the `grass_seed` Athanor row. `ServusAlchemyRecipeProvider` writes `putrefaction/humus.json`. Add the `servus_alchemy:putrefiable` tag in `ServusAlchemyItemTagsProvider`. Verify: `gradlew :alchemy:runData` writes `putrefaction/humus.json`, `athanor/grass_seed.json` and the tag; `gradlew build` green.
- [x] 2.4 Add `alchemy/.../alchemy/AlchemyDataMaps.java` (`BEDDING`, codec `Bedding(int warmth)`, synced, registered on `RegisterDataMapTypesEvent`). Add a new `datagen/ServusAlchemyDataMapProvider.java` (Hay Bale 6,000), hooked into `ServusAlchemyDataGen`. Verify: `gradlew :alchemy:runData` writes `data/servus_alchemy/data_maps/item/bedding.json`; `gradlew build` green.
- [x] 2.5 Add GameTests in `gametest/AlchemyGameTests.java`: `grassSeedGreensDirt`, `grassSeedKeptUnderStone`, `grassSeedIgnoresCoarseDirt`, `athanorMakesGrassSeedAtNigredo`, `calcinerRefusesGrassSeedAndHumus`. Verify: `gradlew :alchemy:runGameTestServer` green.

## 3. Blocks and forming

- [x] 3.1 Add `venter/CucurbitBlock.java` (`FORMED`, `FACING`, `WORKING`, shape, ticker), `venter/VenterFrameBlock.java`, `venter/DungBedBlock.java` and `venter/BedHatchBlock.java` (`FORMED`, `FACING`, `PIECE`, `cucurbit(pos, state)`). Register them in `ServusAlchemyBlocks` and `ServusAlchemyItems` (creative tab after the Residue Crock), with crafting recipes, loot, the pickaxe and axe tags and lang. Verify: `gradlew :alchemy:runData` and `gradlew build` green.
- [x] 3.2 Add `venter/VenterStructure.java` (`tryForm` over four facings, `form`, `unform`, `broken`, `cucurbitOf`, messages with placements) and the Journal path in `CucurbitBlock.useItemOn` with `FormingEffect.play`. Verify: `gradlew build` green.
- [x] 3.3 Add GameTests in a new `gametest/VenterGameTests.java`: `formsWithHatchOnEachSide` (four facings), `missingCornerIsNamed`, `breakingABedUnforms`, `breakingTheHatchUnforms`, `formedStatesSetAndCleared` (`PIECE` and `FACING` per cell). Verify: `gradlew :alchemy:runGameTestServer` green.

## 4. Putrefaction

- [x] 4.1 Add `venter/VenterEquinusBlockEntity.java`: `RotStore<Item>`, `StrawBed`, hatch slot, `PutrefactionWork` progress, recipe choice by id, the owner, the `putrefy` event, `WORKING` on change, save and load, `preRemoveSideEffects` drops. Register it in `ServusAlchemyBlockEntities`. Add `machine/venter_equinus.json` (`item_capacity` 256) and the `AlchemyMachineDefaults` entry. Verify: `gradlew build` green.
- [x] 4.2 Add the `rotPort`, `beddingPort` and hatch port, with `ServusAlchemyEvents.registerVenter` (`Capabilities.Item.BLOCK` on the Cucurbit block entity, and by block on `DUNG_BED` and `BED_HATCH` through `VenterStructure`). Add the hand uses in `CucurbitBlock`, `DungBedBlock` and `BedHatchBlock`. Verify: `gradlew build` green.
- [x] 4.3 Add the GameTest recipe `servus_alchemy:gametest/quick_rot` in `gametest/GameTestRecipes.java`. Add GameTests in `VenterGameTests`:
  - `fullBedTakesWarmTime` and `emptyBedTakesColdTime`;
  - `strawBurnsOnlyWhileWorking`, `fullHatchStops`, `mixedRotMakesOneBatch`;
  - `rotByPipeHayRefused`, `balesByPipeUpToRoom`, `hatchGivesOnlyHumus`, `nothingInAtTheHatch`;
  - `rotStrawAndHumusByHand`, `sneakTakesRotBack`;
  - `unformedKeepsRotAndStraw`, `brokenCucurbitDropsRotAndHumus`;
  - `putrefyCountsInJournal`.

  Verify: `gradlew :alchemy:runGameTestServer` green.

## 5. Look

- [x] 5.1 Add models and blockstates in `datagen/AlchemyModels.java` (`venter()`): `cucurbit`, `dung_bed`, `dung_bed_corner`, `dung_bed_edge` and `bed_hatch`, turned by `FACING` when formed, with the boxes from design.md. In the review, list every overlapping pair from the design's overlap check and confirm no shared same-facing planes. Verify: `gradlew :alchemy:runData` and `gradlew build` green.
- [x] 5.2 Add `client/VenterRenderer.java` (bedding at `4 + 6 * level` with hay or mud, three mats 0.05 px above it, bounding box over the bed), the straw sixteenths in the update tag with resend on change, registration in `ServusAlchemyClient`, and the `WHITE_SMOKE` steam in `CucurbitBlock.animateTick`. Verify: `gradlew build` green and the client launches without model errors.

## 6. Jade and JEI

- [x] 6.1 Add `VenterProvider` in `integration/jade/ServusAlchemyJadePlugin.java` for `CucurbitBlock` and `VenterFrameBlock` (rot, straw, warmth, progress, Humus), with lang keys. Verify: `gradlew build` green.
- [x] 6.2 Add `integration/jei/PutrefactionRecipeCategory.java` (input with count, result, warm and cold seconds, bedding row) and register it with its recipes and catalysts (Cucurbit, Dung Bed, Bed Hatch) in `ServusAlchemyJeiPlugin`. Verify: `gradlew build` green.

## 7. Journal

- [x] 7.1 Add the schematic `VENTER_EQUINUS` in `datagen/ServusAlchemyMultiblockProvider.java` from `VenterLayout.patternRows()`, and a GameTest `schematicAcceptsTheBed` in `VenterGameTests`. Verify: `gradlew :alchemy:runData` and `:alchemy:runGameTestServer` green.
- [x] 7.2 Add `WorkshopCategory.venterEquinus()` (spotlight, multiblock, three crafting pages, "The Horse's Belly", "Rot and Straw", "What Rots" from `PutrefactionText.line`) on the grid as `n` below the Purifier, with the Purifier as parent. Write the flavor in Paracelsus' voice from the design draft, with no homunculus. Verify: `gradlew :alchemy:runData` writes the entry, the journal GameTest still lists every entry, the generated Athanor entry lists "Grass Seed from Wheat Seeds: 20 Sulfur, 20 Mercury", and `gradlew build` is green.

## 8. Balance check

- [x] 8.1 Check that Humus and Grass Seed are in no calcination tag and not in `servus_alchemy:putrefiable`, and that `c:seeds`, `c:crops` and `minecraft:dirt` do not pick them up through other tags. Record the bulk and quick route numbers from design.md in the review. Verify: `calcinerRefusesGrassSeedAndHumus` green, and `gradlew :alchemy:runData` output has no Humus or Grass Seed in those tags.

## 9. Dirt from the Mixer (after `m14-mixer`)

- [x] 9.1 Read the archived `m14-mixer` design for the `MixingRecipe` fields and work unit. Add `AlchemyRecipeTable.Mix` and `dirtMixing()` (`dirt_from_humus`, `dirt_from_bone_meal`). `ServusAlchemyRecipeProvider` writes them into `recipe/mixing/`. Add the "Dirt for the Mixer" list from `PutrefactionText.mixLine` in `WorkshopCategory.venterEquinus()`. Verify: `gradlew :alchemy:runData` writes both mixing recipes and the book page; `gradlew build` green.
- [x] 9.2 Add a GameTest `mixingRecipesMakeDirt` in `VenterGameTests`: both recipes load, and Foundation's mixing matcher takes their contents (Red Sand for the quick route). Verify: `gradlew :alchemy:runGameTestServer` green.

## 10. Integration

- [x] 10.1 Run the full `gradlew build` and `gradlew test` and the alchemy GameTests, all green; commit the `gradlew :alchemy:runData` output. The game launches with all mods.
- [x] 10.2 Play check in game by the user from main:
  - the bed forms with the hatch on any side;
  - the frame reads as one bed, and nothing flickers from any angle while the straw level drops;
  - rot and Hay Bales go in by hand and pipe, and Humus comes out of the hatch;
  - an empty bed shows litter and is slower, and steam rises while it works;
  - Jade, JEI and the book entry with the flavor text;
  - a Grass Seed greens Dirt;
  - a driven Mixer makes 8 Dirt from Sand, Clay, Humus and Water.

  Verify: the user confirms in game.

## Workflow follow-up

- At archive: add under `## Unreleased` in `alchemy/CHANGELOG.md`: Added: the Venter Equinus, a warm straw bed whose Cucurbit putrefies rot into Humus; Grass Seed from the Athanor; Dirt from Sand, Clay and Humus or Bone Meal in the Mixer.
- At archive: remove the Venter Equinus open point from PLAN.md "Open: Alchemy", and add Humus and Dirt to the Nigredo products in `alchemy_plan.md`.

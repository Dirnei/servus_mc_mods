# Tasks: m9-stone-sink-chimney

Apply after `m0-material-kinds` (applied on the same branch, not archived yet, as the user asked).

## 1. Pure logic

- [x] 1.1 Add `ChimneyRules` in `foundation/.../machine/sink/ChimneyRules.java` (no Minecraft imports): enums `Smoke` (off, thin, cosy, signal) and `Mode` (always, powered, unpowered), `nextSmoke`, `nextMode`, `smokes(smoke, mode, powered, onSink)`. Add `foundation/src/test/.../machine/sink/ChimneyRulesTest.java` with these cases:
  - smoke steps off, thin, cosy, signal and wraps to off;
  - mode steps always, powered, unpowered and wraps to always;
  - off never smokes;
  - always smokes powered and unpowered;
  - while powered smokes only when powered, while unpowered only when not;
  - on a Sink never smokes, whatever the setting.

  Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `SinkStones.distinct(List<String>)` in `foundation/.../machine/sink/SinkStones.java` and `SinkStonesTest` with these cases:
  - eight equal ids give one;
  - six and two give both in first seen order;
  - three different ids keep their order;
  - an empty list gives none.

  Verify with `gradlew :foundation:test`.

## 2. Stone on the blocks

- [x] 2.1 In `foundation/.../machine/MachineContent.java` register the Sink and Chimney items as `MaterialBlockItem`, call `Material.register(..., MaterialKind.STONE)` for both blocks, and add the `chimney` block entity type. Add `ChimneyBlockEntity` (a `MaterialSlot` with the forwards from design.md D3, no ticker) and make `ChimneyBlock` a `BaseEntityBlock` with `getRenderShape` `MODEL`. Verify with `gradlew :foundation:build`.
- [x] 2.2 In `SinkBlockEntity` add the `MaterialSlot` on every part and the forwards from design.md D3 (components, save, load, update tag, model data). In `SinkBlock.onWrench` stamp the stone on the dismantled item. Verify with `gradlew :foundation:build`.
- [x] 2.3 In `datagen/MachineBlockLoot` copy the material component for the Sink and the Chimney. In `datagen/MachineDataParts` write both recipes through `MaterialRecipes.output(..., MaterialKind.STONE)` with `#servus_foundation:stones`: Sink `BBB/BRB/BBB` with iron bars, 8; Chimney `B B/B B/B B`, 6. Run `gradlew :foundation:runData` and check both recipe files (`material_shaped`, `"kind": "stone"`), both loot tables and both item model definitions (`servus_foundation:material`, `"kind": "stone"`). Verify with `gradlew :foundation:build`.

## 3. Models and textures

- [x] 3.1 Extend `tools/sink-textures/gen.py` to write `foundation/src/main/resources/assets/servus_foundation/textures/block/sink_casing_frame.png`: the iron frame of `sink_casing.png` with the brick pixels transparent. In `datagen/SinkModels.sink` build the casing as a stone cube inset 0.05 px in `minecraft:block/bricks` under a full cube of `sink_casing_frame`, render type cutout. Remove `sink_casing.png` if nothing else uses it. Run `gradlew :foundation:runData`. Verify with `gradlew :foundation:build`.
- [x] 3.2 Look at `chimney_top.png`. If it paints a brick rim, split the cap top face in `SinkModels.chimney` into four rim faces in `#bricks` and a centre flue face. The flue gets a new `chimney_flue.png` cut from the old texture's inner part by `gen.py`. If it has no brick rim, leave the model as it is and note that here. Found: a brick rim three texture pixels wide around a dark 10 by 10 flue; the cap top is split, `chimney_top.png` is removed. Run `gradlew :foundation:runData`. Verify with `gradlew :foundation:build`.
- [x] 3.3 List every pair of overlapping boxes in the casing and the cap and state that no faces share a plane where they overlap (design.md D4). Verify with `gradlew :foundation:build`.
  - casing stone cube (0.05 to 15.95) and frame cube (0 to 16): every stone face sits 0.05 px inside its frame face, no shared plane;
  - cap box (2..14, 13..16) and shaft part (3..13, 0..13): the cap's sides lie at x and z 2 and 14 and its down face at y 13, the shaft's sides at x and z 3 and 13 and it has no face at y 13, so no shared plane;
  - the five cap top faces at y 16 (four rim strips, one flue): same plane, but they only touch at their edges and never overlap; the cap box has no up face;
  - formed parts and the shaft: geometry unchanged.

## 4. One stone forming

- [x] 4.1 In `SinkBlock.tryForm` read the stones of the eight parts, form only when `SinkStones.distinct` gives one, and otherwise send `message.servus_foundation.sink.mixed_stone` with the stone names. Add the lang string in `MachineDataParts`. Verify with `gradlew :foundation:build`.
- [x] 4.2 Add to `foundation/.../machine/sink/SinkGameTests.java`:
  - `mixedStonesRefuseForming`: six Stone Bricks blocks and two Deepslate Bricks blocks stay unformed;
  - `oneStoneForms`: eight Polished Deepslate blocks form;
  - `missingStoneCountsAsBricks`: seven Bricks blocks and one without a stone form;
  - `sinkDropKeepsStone`: a dismantled Sandstone block drops Sandstone;
  - `stoneDoesNotChangeRates`: the same chimney height gives the same rates for Bricks and Quartz.

  Verify with `gradlew :foundation:runGameTestServer`.

## 5. Chimney settings

- [x] 5.1 Add `ChimneySmoke` and `ChimneyRedstone` (`StringRepresentable`, mapped to `ChimneyRules`) and the states `SMOKE`, `REDSTONE`, `POWERED`, `ON_SINK` on `ChimneyBlock`, with the defaults and the copy from the block below in `getStateForPlacement` (design.md D6, D10). Keep the blockstate model choice on `TOP` only in `SinkModels.chimney`. Run `gradlew :foundation:runData`. Verify with `gradlew :foundation:build`.
- [x] 5.2 Add the column walk and the events from design.md D7: `ON_SINK` through `updateShape` (DOWN); `POWERED` through `neighborChanged`, `onPlace` and `affectNeighborsAfterRemoval`, set on every block of the column with `Block.UPDATE_CLIENTS`. Verify with `gradlew :foundation:build`.
- [x] 5.3 Make `ChimneyBlock` a `Wrenchable` with the carrot actions of design.md D8 and add the lang strings for the messages, the setting names and the mode names in `MachineDataParts`. Verify with `gradlew :foundation:build`.
- [x] 5.4 Add `ChimneyBlock.animateTick` with the particles of design.md D11. Verify with `gradlew :foundation:build`.
- [x] 5.5 Add `foundation/.../machine/sink/ChimneyGameTests.java`, registered like `SinkGameTests`:
  - `carrotStepsSmokeOverColumn`: a click on the middle of three blocks sets all three;
  - `sneakCarrotStepsRedstoneMode`;
  - `redstoneOnAnyBlockPowersColumn`: a redstone block beside the lowest block sets `POWERED` on the top, and removing it clears it;
  - `newChimneyCopiesBelow`;
  - `carrotIgnoredOnSink`;
  - `settingsReturnAfterUnform`: breaking a Sink block clears `ON_SINK` on the whole column and keeps `SMOKE`;
  - `chimneyOnSinkStillCounts`: three mixed stone blocks give height 3;
  - `chimneyKeepsStoneOnDrop`.

  Verify with `gradlew :foundation:runGameTestServer`.

## 6. Data and docs

- [x] 6.1 Run `gradlew :alchemy:runData` and keep the regenerated `docs/src/data/gamedata.json` (the Sink and Chimney recipes). Run the docs tests (`npx vitest run` in `docs/`).
- [x] 6.2 Update the Sink and Chimney entry in `docs/src/content/docs/mods/foundation.md`:
  - any stone of the tag, the recipes and their counts;
  - one stone per formed Sink, mixed chimneys allowed;
  - the free standing chimney: smoke settings, redstone modes, copy on stack, no change on a Sink.

  Verify with the docs build (`npm run build` in `docs/`).

## 7. Integration

- [x] 7.1 Full `gradlew build` and `gradlew test` green, `:foundation:runGameTestServer` and `:shuttles:runGameTestServer` green (or failures shown to be pre-existing on main), `openspec validate m9-stone-sink-chimney --strict` passes, and the game launches with all mods (the GameTest servers loaded every mod; the client launch is part of the play check 7.2, from the main checkout).
- [ ] 7.2 Play check by the user from the main checkout:
  - craft Sink and Chimney blocks from several stones and check that the tooltips name the stone;
  - build a cube of one stone and form it; try a mixed cube and read the refusal;
  - check the casing and formed faces in the stone, with the iron frame, grate and fan unchanged;
  - stack mixed stone chimneys on the Sink and check that the height counts and the Sink smoke is as before;
  - build a free standing column, step the smoke and redstone modes with the carrot, power it with a lever on several blocks, grow it and check that the new top copies the settings;
  - look for flicker on the casing and the cap from several angles.

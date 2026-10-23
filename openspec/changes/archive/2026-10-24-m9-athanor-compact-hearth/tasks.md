# Tasks

## 1. Layout and tiers (pure Java)

- [x] 1.1 `alchemy/.../athanor/AthanorLayout.java`: hearth by role (Bricks under and in front, seven `brick_slab`, `brick_stairs` step) turned by facing; tanks as three high columns (dy -1..1); one `Duct` per side on the Athanor level (drop `overTank`); `schematic` with the new layers and a hollow bottom layer. Update `AthanorLayoutTest`: `hearthRolesTurnWithTheFront`, `tanksAreThreeHigh`, `oneDuctPerTankOnTheAthanorLevel`, `nigredoSchematicLayers`, `albedoSchematicHasTheRingBelow`, `rubedoSchematicHoldsVatsAndPillars`. Run `gradlew :alchemy:test`.
- [x] 1.2 `alchemy/.../athanor/AthanorTiers.java`: `turn(BlockAt, Facing)` and `tierOf` with a facing. Update `AthanorTiersTest`: `offsetsTurnWithTheFront`, `eastFacingReachesRubedo`, `vatsWithoutSaltStayNigredo`, `shortPillarDropsToCitrinitas`. Run `gradlew :alchemy:test`.
- [x] 1.3 `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/athanor_tier/albedo.json`, `citrinitas.json`, `rubedo.json`: sixteen salt at dy -2; vats at dy -1 with amethyst at dy 0 beside the tanks; Crying Obsidian at (±1, 2) and (±2, 1) for dy -1 and 0. `AthanorTierData` unchanged apart from passing the facing. Run `gradlew :alchemy:test`.

## 2. Forming and checks

- [x] 2.1 `alchemy/.../athanor/AthanorBlockEntity.java`: check hearth roles, the step, three high tanks and one duct per side; missing messages for slab, step and short tank (lang in `ServusAlchemyLanguageProvider`); tier check with the facing; `setDucts` for the new duct cells. `alchemy/.../structure/CopperDuctBlock.java`: one formed `ATHANOR` shape replacing `ATHANOR_NEAR` and `ATHANOR_OVER`. Run `gradlew :alchemy:build`.
- [x] 2.2 `alchemy/.../gametest/AlchemyGameTests.java`: fixture builds the new hearth, step, tanks and ducts; update `athanorFormsOnItsHearth`, `athanorUnformsWithoutTheBrickBelow`, `athanorTierFollowsTheRing`; add `athanorNamesTheMissingStep`, `athanorNamesAShortTank`, `turnedAthanorReachesRubedo`. Check the other alchemy GameTests that use the fixture (Dropper, Homunculus, Prelum, Salamander) still pass. Run `gradlew :alchemy:runGameTestServer`.

## 3. Look

- [x] 3.1 `alchemy/.../datagen/AlchemyModels.java`: the new formed Athanor model per tier and lit state as in design, steel and black iron from the family textures; new `athanor_fire_<tier>` textures with a flicker `.mcmeta` in `alchemy/src/main/resources/assets/servus_alchemy/textures/block/`; the formed `ATHANOR` duct model as a straight pipe into the tank side; remove the old over-tank duct model. List every overlapping pair of boxes in the model and whether faces share a plane. Run `gradlew :alchemy:runData` and `gradlew :alchemy:build`.
  Overlap check of the formed tower (no two faces of the same direction share a plane where they overlap):
  - Body, four posts and lower band stand on the plinth top (y 3): they have no bottom face.
  - Posts (1..2.5, 13.5..15) and body (1.5..14.5): post sides at 1, 2.5, 13.5 and 15 against body sides at 1.5 and 14.5, no shared plane; posts end at 17.25 above the body top at 17.
  - Bands (0.9..15.1) and posts or body: band sides outside the posts at 1 and 15; band tops at 4 and 16 are inside the posts.
  - Door frame (z 0.8..1.5), black iron plate (0.6..0.8), window lips (0.45..0.6), handle (0.3..0.6): stacked front to back, touching only with opposite faces; the four plate pieces and the four lips meet edge to edge.
  - Fire plate (z 0.75..0.78): north face only, behind the plate front at 0.6 and in front of the frame at 0.8.
  - Flanges (x 0..1.5, 14.5..16, z 14.5..16) touch the body sides with opposite faces only, and no post or band.
  - Dome steps (brick 2.5..13.5, copper 4..12 and 5.5..10.5) and plate (3.5..12.5): each rests on the one below, every side at its own plane.
  - Vents (0.2 px deep) lie on the brick dome step and have no face toward it.
  - Duct pipe (y 6..10) and collar (5.5..10.5): no end faces where they meet the flange and the tank.
- [x] 3.2 `alchemy/.../athanor/AthanorBlock.java`: `TOWER` collision for the plinth, body, dome and plate; formed duct collision in `CopperDuctBlock`. Run `gradlew :alchemy:build`.

## 4. Journal

- [x] 4.1 `alchemy/.../datagen/ServusAlchemyMultiblockProvider.java`: schematics from the new layout, `0` anchor on the hollow ring as an any-matcher (verify `DenseMultiblockBuilder` in the Modonomicon 26.1 source; fallback per design); journal text in `datagen/book/WorkshopCategory.java` and lang for the new hearth, step, tanks and stage pages. Update `athanorSchematicsMatchTheBuilds`. Run `gradlew :alchemy:runData` and `gradlew :alchemy:runGameTestServer`.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test` and `gradlew :alchemy:runGameTestServer` green; launch the game with all mods and check in game: forming on the new hearth, each stage's flame and vent colour, no flicker on any part of the formed model from several angles, stepping onto the pipes and the plate, the four Visualize pages.

## Workflow follow-up

- Archive with a Changed line in `alchemy/CHANGELOG.md` under Unreleased (new Athanor build: rebuild existing Athanors).
- Update PLAN.md: the Athanor stage structure is decided; the rituals are the open follow-ups.

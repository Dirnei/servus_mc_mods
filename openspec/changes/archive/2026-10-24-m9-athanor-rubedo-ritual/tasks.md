# Tasks

## 1. Data (pure Java)

- [x] 1.1 `alchemy/.../athanor/AthanorTiers.java` and `ritual/AthanorRitual.java`: property values as lists in `BlockAt` and turn `from`; air turns; turned blocks count as in place (`RitualPlan`); `stream` per turn and the ritual's sounds as data; `AthanorLayout.place` for messages. Add `AthanorTiersTest.acceptedValueListsMatch`, `AthanorRitualTest.airAboveIsATurn`, `AthanorRitualTest.orangeVatsTurnAgain`, `AthanorRitualTest.turnedBlocksCountAsInPlace`, `AthanorLayoutTest.placeNamesTheSideOfTheTank`. Run `gradlew :alchemy:test`.
- [x] 1.2 `athanor_ritual/rubedo.json` (Ghast Tear, tier 2, four obsidian to obelisk lower halves, four air to upper halves, three vats citrine or rubedo to rubedo, phase times, sounds), `athanor_ritual/citrinitas.json` (its sounds), `athanor_tier/citrinitas.json` (citrine or rubedo), `athanor_tier/rubedo.json` (obelisk halves, orange vats). Update `AthanorTiersTest`, `AthanorLayoutTest`. Run `gradlew :alchemy:test`.

## 2. Blocks

- [x] 2.1 `alchemy/.../athanor/ritual/RubedoObeliskBlock.java` (double block, light, no item, loot one Obsidian from the lower half), `RubedoObeliskBlockEntity.java`, registration, tags, lang; `VatBlock.CRYSTAL` gains `rubedo` with loot like citrine; ritual messages name the place and the free space above (`AthanorBlockEntity`). Run `gradlew :alchemy:build`.
- [x] 2.2 GameTests: `rubedoRitualGrowsObelisks`, `rubedoRitualNeedsFreeSpaceAbove`, `brokenObeliskDropsObsidianAndTheStage`, `rubedoRitualCastsAgainAfterABrokenObelisk`, `brokenOrangeVatDropsToAlbedo`; the Athanor GameTests use the obelisks and the Obsidian schematic. Run `gradlew :alchemy:runGameTestServer`.

## 3. Look

- [x] 3.1 Obelisk models per half (datagen: obsidian plinth, empty upper half), textures `rubedo_obelisk` and `rubedo_veins` from `tools/athanor-textures/gen.py`, `client/ObeliskDrawing.java` (shell, bands, shard, cracks as quads), `client/RubedoObeliskRenderer.java` (core pulse, light bands, floating shard, plinth cracks), embers in `animateTick`; list overlapping boxes and shared planes (design.md, Overlap check). Run `gradlew :alchemy:runData` and `gradlew :alchemy:build`.
- [x] 3.2 `client/GrownVatRenderer.java` with `client/VatTints.java`: orange cluster and the yellow to orange blend during the ritual; `client/RitualLooks.java` and `client/AthanorRenderer.java`: tear, drops with trails to streamed turns only, veins, rising obelisks, from the ritual's phase times (`RitualTimeline.cracks`, `rise`, `recolor`, `answer`, with `RitualTimelineTest`); the red flash from the server. Run `gradlew :alchemy:build`.

## 4. Journal

- [x] 4.1 Rubedo entry: schematic with Obsidian on the pillar cells, text naming the Ghast Tear (`WorkshopCategory`, `JournalEntries`, `JournalAdvancements`, `ServusAlchemyMultiblockProvider`, lang). Run `gradlew :alchemy:runData` and `gradlew :alchemy:runGameTestServer`.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test` and `gradlew :alchemy:runGameTestServer` green; launch the game with all mods and play the Rubedo ritual: watch it from several angles, break and recast an obelisk, check the idle obelisks and the orange vats for flicker.

## Workflow follow-up

- Archive with an Added line in `alchemy/CHANGELOG.md` (Rubedo ritual) after `m9-athanor-citrinitas-ritual` is archived; update PLAN.md (Athanor stages done).

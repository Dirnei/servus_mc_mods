# Tasks

## 1. Data (pure Java)

- [x] 1.1 `alchemy/.../athanor/AthanorTiers.java`: `BlockAt` with an optional properties map; matcher compares id and named properties; `AthanorTierData` codec reads `properties`. Add `BlockAtPropertiesTest`: `propertiesMustMatch`, `idOnlyMatchesAnyState`. Run `gradlew :alchemy:test`.
- [x] 1.2 New `alchemy/.../athanor/ritual/AthanorRitual.java`, `RitualPlan.java`, `AthanorRitualData.java` (reload listener registered like `AthanorTierData`); `athanor_ritual/citrinitas.json` (Amethyst Block, tier 1, three vat turns `crystal=none` to `crystal=citrine`, 160 ticks with phase times). Add `AthanorRitualTest`: `turnsAreTurnedWithTheFront`, `firstMissingTurnIsNamed`, `wrongTierRefuses`. Run `gradlew :alchemy:test`.
- [x] 1.3 `athanor_tier/citrinitas.json`: three vats with `crystal=citrine` instead of vat plus Amethyst Block. Update `AthanorTiersTest`. Run `gradlew :alchemy:test`.

## 2. Vat and ritual on the server

- [x] 2.1 `alchemy/.../coagulator/VatBlock.java`: `CRYSTAL` property, light by state, `EntityBlock` returning `GrownVatBlockEntity` only when grown; `CoagulatorStructure` expects `crystal=none`; loot table with shards for grown vats (`ServusAlchemyBlockLoot`); register the block entity. Run `gradlew :alchemy:build`.
- [x] 2.2 `alchemy/.../athanor/AthanorBlockEntity.java`, `AthanorBlock.java`: casting on use (checks, messages in lang), ritual state saved and synced, phase sounds, end turns the blocks still in place and re-checks the tier, no crafts while running. Run `gradlew :alchemy:build`.
- [x] 2.3 GameTests in `alchemy/.../gametest/AlchemyGameTests.java` (or a new `RitualGameTests.java`): `ritualGrowsCitrine`, `ritualKeepsTheOfferingWhenAVatIsMissing`, `ritualOnNigredoRefuses`, `brokenGrownVatDropsShardsAndTheStage`, `grownVatDoesNotFormACoagulator`, `vatTakenDuringTheRitualStaysPlain`. Run `gradlew :alchemy:runGameTestServer`.

## 3. Look

- [x] 3.1 Cluster as in the mock: `athanor/ritual/CrystalCluster.java` (spike table, growth), `client/CrystalDrawing.java` drawing the prisms as quads with `ritual_crystal` from `tools/athanor-textures/gen.py` (a block model cannot hold tilted six sided prisms, see design), `client/GrownVatRenderer.java` drawing it (render box three blocks tall), idle glints in `VatBlock.animateTick`. List overlapping prism pairs and shared planes. Run `gradlew :alchemy:runData` and `gradlew :alchemy:build`.
- [x] 3.2 `client/AthanorRenderer.java`: offering, flame colour, burst, arcs, liquid and growth on the turned vats from the ritual state and game time, as in the mock's timeline; register in `ServusAlchemyClient`. Run `gradlew :alchemy:build`.

## 4. Journal

- [x] 4.1 Citrinitas entry: schematic with empty vats, text naming the Amethyst Block offering (`datagen/book/WorkshopCategory.java`, `ServusAlchemyMultiblockProvider`, lang). Run `gradlew :alchemy:runData` and `gradlew :alchemy:runGameTestServer`.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test` and `gradlew :alchemy:runGameTestServer` green; launch the game with all mods and play the ritual: watch it from several angles, walk up halfway through, break a grown vat, check for flicker on the cluster.

## Workflow follow-up

- Archive with an Added line in `alchemy/CHANGELOG.md` (Citrinitas ritual) after `m9-athanor-compact-hearth` is archived.

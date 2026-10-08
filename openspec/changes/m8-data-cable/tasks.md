# Tasks

The first version (standalone Data Cable block, pure `DataMembers`, `DataNetwork`, `DataNetworks`, summary, assist, GameTests) was built and committed before `m8-foundation-merge`. These tasks turn it into the duct Data line.

## 1. Data line in the duct

- [ ] 1.1 Add `foundation.data.DataNode` and `DataMembership.isMember`; make `DataNetwork` a `ServusNetwork<DataNode, DataEvent>` with a connection rule on open sides and members read only on open sides toward non data nodes; add `DataSummary`. Add `Line.DATA` (`data`), make `UtilityDuctBlockEntity` implement `DataNode`, join the data network through a `DataLineNode`, compute data arms with `DataMembership`, refresh the data node and post `SidesChanged` when a data side mode changes, keep the data mode to `BOTH` and `DISABLED`. Add `ServusLogisticsItems.DATA_CABLE` (`LineItem`, id `data_cable`) and `lineItem(DATA)`. Delete `DataCableBlock`, `DataCableBlockEntity`, `DataCableAssist`, `ServusFoundationBlocks`, `ServusFoundationBlockEntities` and their registration and datagen. Verify with `gradlew build` and `gradlew test` (update `DuctPartsTest` for four lines).

## 2. Interaction and GUI

- [ ] 2.1 `UtilityDuctBlock.useWithoutItem` shows the summary for an empty hand on a duct with the data line. `UtilityDuctMenu` toggles the data mode between on and off; `UtilityDuctScreen` shows the Data page with the on and off button and the remove button only. `DuctBlockColor` returns the fixed teal for data ring tints. Lang entries for the Data Cable, the data tab, on and off, remove. Verify with `gradlew build`.

## 3. Appearance and datagen

- [ ] 3.1 Generate `duct_data.png`, the band textures of every multi line set containing data and `item/data_cable.png` with a helper script; datagen recipe, item model and lang in `LogisticsDataParts`; remove the standalone cable's datagen. Delete the old generated cable files, run `:foundation:runData` and `:manufacturing:runData`, verify 15 line sets of part models exist and the recipe output is `servus_foundation:data_cable`, then `gradlew build`.

## 4. Tests

- [ ] 4.1 Rewrite `storage/.../gametest/DataCableGameTests.java` with ducts carrying the data line (row, split, gap in the data line, station and controller, chest, member broken, touched twice, side off for member and duct link, summary, moves nothing, networkAt, sneak extends a data run, broken duct drops Data Cable) and add a Foundation GameTest that adds the data line to an item duct. Verify with `:foundation:runGameTestServer`, `:manufacturing:runGameTestServer` and `:storage:runGameTestServer`, all green.

## 5. Docs and integration

- [ ] 5.1 Add a Data line section to `docs/src/content/docs/mods/foundation.md`; run `npx vitest run` in `docs/`. Full `gradlew build` and `gradlew test` green, all GameTest runs green, and `runClientAll` reaches the title screen.

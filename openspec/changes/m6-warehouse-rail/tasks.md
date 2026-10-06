# Tasks

## 1. Pure candidate logic

- [x] 1.1 Add `storage/.../index/CandidateNeighbours.java` with `compute(Set<NodePos> members, Set<NodePos> connectors)` and `storage/src/test/.../index/CandidateNeighboursTest.java` covering a single member, two touching members sharing a neighbour, a connector between two members whose neighbours are excluded, and a connector only set. Verify `gradlew :storage:test` is green.

## 2. Rail block and network membership

- [x] 2.1 Add `storage/.../rail/RailBlock.java` (extends `ConnectableBlock`, horizontal rail only connections, flat shape, floor support) and `RailBlockEntity.java` (`holdsStock()` false); add `holdsStock()` to `StorageNodeBlockEntity`; register block, item, block entity in `ServusStorageBlocks`, `ServusStorageItems` (creative tab order) and `ServusStorageBlockEntities`. Verify `gradlew :storage:compileJava` passes.
- [x] 2.2 Update `WarehouseNetwork`: `connectors` set, skip sources and candidates for connector nodes in `onNodeAdded`, `onNodeRemoved`, `onAbsorb`, `onDisposed`, `handle`; use `CandidateNeighbours` in `rebuildCandidates` and `addCandidatesAround`; add the rail count to `summary()`. Verify `gradlew build` and `gradlew test` are green.

- [x] 2.3 Add the split handover to `lib/.../network/core/NetworkSet.java` (`handOver` after `apply` in `nodeRemoved` and `nodeRefreshed`) and the JUnit test `splitHandsMovedNodesOver` in `NetworkSetTest`. Verify `gradlew :lib:test` is green.

## 3. Assets and data

- [x] 3.1 Add `RailModels` to the storage datagen package and call it from `ServusStorageBlockStateProvider`; add the rail to `ServusStorageLanguageProvider`, `ServusStorageRecipeProvider`, `ServusStorageBlockLoot` and `ServusStorageBlockTagsProvider`; draw `textures/block/rail.png` with a scratchpad script. Run `gradlew :storage:runData` and verify the generated blockstate has five multipart entries, and the recipe, loot table, tag and lang entries for `rail` exist.

## 4. Verification

- [x] 4.1 Add `railsLinkCrateAndController` to `ServusStorageGameTests` per design.md (online through two rails, bread beside the rail not indexed, removal of a rail drops the crate stock). Verify `gradlew :storage:runGameTestServer` passes both storage tests.
- [x] 4.2 Update `PLAN.md`: section 2.3 states floor rails that auto connect, section 5 drops the "Exact rail block design" bullet. Verify `openspec validate m6-warehouse-rail` passes.
- [ ] 4.3 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: place a run of rails with a curve and a crossing, walk over them, put a crate at one end and the controller plus terminal at the other, confirm the terminal lists the crate, sneak dismantle a rail with the Configuration Carrot. Commit.

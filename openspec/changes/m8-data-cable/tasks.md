# Tasks

## 1. Pure membership logic

- [x] 1.1 Add `DataRole` and `DataMembers` in `lib/src/main/java/io/github/dirnei/mc/servus/lib/data/` (no Minecraft imports): add and remove a touch (cable position, side, member position, roles), one entry per member with the union of roles, entry removed with its last touch, query all members and members by role. Add `lib/src/test/java/.../lib/data/DataMembersTest.java` with tests `oneTouchMakesAMember`, `twoTouchesOfOneMemberCountOnce`, `rolesAreTheUnionOfTouches`, `lastTouchRemovesTheMember`, `removingACableDropsAllItsTouches`, `membersByRole`. Verify with `gradlew :lib:test`.

## 2. Lib block registration

- [x] 2.1 Add `registry/ServusLibBlocks` and `registry/ServusLibBlockEntities` and register both in `ServusLib` before items. Verify with `gradlew :lib:build`.
- [x] 2.2 Add `data/DataCableBlock` (extends `ConnectableBlock`, radius 2, cable to cable arms only for now), `data/DataCableBlockEntity` (extends `NetworkBlockEntity`), `data/DataEvent`, `data/DataNetwork` with `TYPE` and a cable to cable `ConnectionRule`, registered in `ServusLib` through `NetworkTypes.register`; the `data_cable` block item in `ServusLibItems` and the Lib creative tab. Verify with `gradlew :lib:build`.

## 3. Datagen and assets

- [x] 3.1 Add `ServusLibBlockStateProvider` (`ConnectableModels.connectable(..., 2, true)`), `ServusLibBlockLoot` with a `LootTableProvider`, `ServusLibBlockTagsProvider` (`minecraft:mineable/pickaxe`), the shaped recipe (copper ingot, redstone, copper ingot gives 4) in `ServusLibRecipeProvider` and lang entries for the block and `message.servus_lib.data_cable.summary` in `ServusLibLanguageProvider`; wire them in `ServusLibDataGen`. Add the placeholder texture `lib/src/main/resources/assets/servus_lib/textures/block/data_cable.png`. Run the Lib data run and verify the generated blockstate, models, loot table, tag and recipe files exist, then `gradlew build`.

## 4. Members on the network

- [x] 4.1 In `DataNetwork`: per cable side three `BlockCapabilityCache`s (production, stock provider, stock sink, context `side.getOpposite()`) whose invalidation listener posts `SidesChanged`; `onNodeAdded`, `onNodeRemoved`, `onAbsorb` and `handle` keep `DataMembers` current; expose `dataMembers()` and `dataMembers(DataRole)` as `DataMember` records. Add `data/DataNetworks.networkAt(ServerLevel, BlockPos)` for cable and member positions. Verify with `gradlew :lib:build`.
- [x] 4.2 `DataCableBlock.connectsTo` also returns true toward a neighbour exposing one of the three capabilities on the facing side, only on a server `Level`, keeping the flag otherwise; `neighborChanged` recomputes the six flags. Verify with `gradlew build`.
- [x] 4.3 Add GameTests in a new `storage/src/main/java/.../storage/gametest/DataCableGameTests.java` (same empty template as the existing tests): `dataCableRowIsOneNetwork`, `dataCableSplitsWhenBroken`, `dataCableFindsStationAndController`, `dataCableIgnoresChest` (no member, no arm), `dataCableMemberBrokenLeaves`, `dataCableTouchedTwiceCountsOnce`, `dataCableMovesNothing` (controller with planks and a sticks station linked for 100 ticks), `dataNetworksAtMemberAndStone`. Verify with `gradlew :storage:runGameTestServer`, all Storage GameTests green.

## 5. Player interaction

- [x] 5.1 `DataCableBlock.useWithoutItem` sends the summary (cables, producers, stock) to the action bar on the server; sneak carrot dismantle comes from `ConnectableBlock`. Any held item skips the summary so cables place against cables. GameTest `dataCableSummaryCounts`. Verify with `gradlew build` and the Storage GameTests.
- [ ] 5.2 Add `data/DataCableAssist` (cables start and continue a run, direction from `PlacementAssists.alongRun` over arms toward cables, else the clicked face) and make the cable item an `AssistedBlockItem`. Verify with `gradlew build`.

## 6. Integration

- [ ] 6.1 Full `gradlew build` and `gradlew test` green, Storage and Manufacturing GameTests green, and the game launches with all four mods. The user checks in game from the main checkout: arms toward cables, Crafting Station, Warehouse Controller and Assembler but not toward a chest; summary text; sneak placement of a cable run; a member chunk unloading drops out of the summary; Lib alone shows "2 cables, 0 producers, 0 stock".

# Tasks

## 1. Pure sample logic

- [x] 1.1 Add `storage/.../rack/TopCounts.java` (`pick(Map<K, Integer> counts, int n)` with insertion order ties) and `storage/src/test/.../rack/TopCountsTest.java` covering fewer types than n, more types than n, ties by order and an empty map. Verify `gradlew :storage:test` is green.

## 2. Rack block, inventory and GUI

- [x] 2.1 Add `HeavyRackBlock` (facing, lamp, left, right, up, down flags from same facing neighbours, GUI on right click, dismantle keeps contents, break spills) and `HeavyRackBlockEntity` (item handler sized from `rack.json`, throttled sync, `Display` sample in the update tag computed with `TopCounts`); add `rackSlots()` and `rackDisplayCount()` to `StorageStatsManager` with `data/servus_storage/storage/rack.json`; register block, item (tab after the crates), block entity, and the item handler capability. Verify `gradlew :storage:compileJava` passes.
- [x] 2.2 Add `HeavyRackMenu` with rack rows, player inventory and shift click, `HeavyRackScreen` on the vanilla generic container texture, the menu registration in `ServusStorageMenus`, the screen registration in `ServusStorageClient` and `HeavyRackRenderer` drawing the sample on two shelf levels. Verify `gradlew build` passes.

## 3. Assets and data

- [x] 3.1 Add `HeavyRackModels` to the datagen package (shelves, conditional posts, top and bottom, lamp part, item model) and call it from `ServusStorageBlockStateProvider`; add recipe, loot, tag and lang entries; draw the steel and board textures with a scratchpad script. Run `gradlew :storage:runData` and verify the blockstate, models, recipe, loot table, tag and lang entries for `heavy_rack` exist.

## 4. Verification

- [x] 4.1 Add `heavyRackIndexesAndDisplays` to `ServusStorageGameTests` per design.md. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 4.2 Update `PLAN.md`: section 2.3 and the M4 list mention the Heavy Duty Rack. Verify `openspec validate m6-heavy-duty-rack` passes.
- [x] 4.3 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: craft a rack, open it, store mixed items, check the sample on the front, place three racks in a row and two stacked to see the continuous frame, rotate one and see it separate, dock a rail onto a rack, dismantle and replace a filled rack. Commit.

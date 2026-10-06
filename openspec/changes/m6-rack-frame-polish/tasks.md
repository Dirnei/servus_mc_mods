# Tasks

## 1. Pure post rhythm

- [x] 1.1 Add `storage/.../rack/RackRow.java` (`leftPostHidden`, `rightPostHidden`) and `storage/src/test/.../rack/RackRowTest.java` (single bay, row of three, row of five with posts every three, posts every one). Verify `gradlew :storage:test` is green.

## 2. Frame, row and rail links

- [x] 2.1 Rework `HeavyRackBlock` (bay index and count along the row, flags from `RackRow`, row refresh on place, remove and rotate, `facingChanged` on the block entity), add `acceptsRail(Side)` to `StorageNodeBlockEntity` with the facing based override in `HeavyRackBlockEntity`, apply it in the `WarehouseNetwork` rule and in `RailBlock.docksTo`, add `posts_every` to `rack.json` and `StorageStatsManager`. Rebuild `HeavyRackModels` with the disjoint boxes and extension parts. Run `gradlew :storage:runData` and verify the rack blockstate has forty four multipart entries.
- [x] 2.2 Add the GameTest `rackLinksRailOnlyFromFront` per design.md. Verify `gradlew :storage:runGameTestServer` passes all seven storage tests.

## 3. Verification

- [x] 3.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: build a row of five racks and see posts at the ends and after the third bay with continuous shelves, break the second bay and watch the posts move, check no flicker on the frame, run a rail along the back of a rack and see no dock, turn the rack towards it and see the dock. Commit.

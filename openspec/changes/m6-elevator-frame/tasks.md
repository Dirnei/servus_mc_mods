# Tasks

## 1. Frame shape and models

- [x] 1.1 Change the shape in `storage/.../rail/ElevatorBlock.java` to four corner posts plus the rail centre plate and the arm boxes; rewrite `storage/.../datagen/ElevatorModels.java` to write `elevator_frame` with four post elements and a multipart blockstate of frame, `rail_center` and conditional `rail_arm` parts, item model `elevator_frame`. Run `gradlew :storage:runData` and verify the generated elevator blockstate has six multipart entries and `elevator_center.json` is gone.

- [x] 1.2 Add the pure `storage/.../network/WarehouseConnectionRule.java` with `NodeKind` and `WarehouseConnectionRuleTest` (stock links, no stock to elevator link, rails sideways only, elevators vertical and rails sideways, symmetry); add `kind()` to `StorageNodeBlockEntity` with overrides in `RailBlockEntity` and `ElevatorBlockEntity`; use the rule in `WarehouseNetwork.TYPE`. Verify `gradlew :storage:test` is green.
- [x] 1.3 Add `canSurvive` and the `neighborChanged` drop to `ElevatorBlock`, the `neighborChanged` drop to `RailBlock` without the air shortcut; extend `elevatorLinksFloors` with a crate touching only a segment that stays out with a dark lamp. Verify `gradlew :storage:runGameTestServer` passes all three storage tests.

## 2. Verification

- [ ] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew :storage:runGameTestServer` passes all three storage tests, `gradlew runClientAll` launches with all four mods. In game: stack three segments with rails on two sides, walk into the segment and stand in its middle, check the posts line up and the rail runs through; try to place a segment in the air (refused), break the block under a shaft (all segments drop), put a crate against a segment (lamp stays dark). Commit.

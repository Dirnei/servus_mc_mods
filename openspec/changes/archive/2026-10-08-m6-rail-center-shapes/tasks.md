# Tasks

## 1. Centre shapes

- [x] 1.1 Add `rail_curve.png` and `rail_cross.png` under `storage/src/main/resources/assets/servus_storage/textures/block/`, build the straight, curve and cross centre models and the sixteen conditioned centre parts in `RailModels.java`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

- [x] 1.2 Add pure `RailCenter.java` with `RailCenterTest.java` (lone, straight, single side, corners, crossings) and use it in `RailModels.java`.
- [x] 1.3 `ElevatorRenderer.java`: rail pieces with shape and rotation, rotated top face UVs, ground floor stubs kept under the parked car. Verify `gradlew :storage:build` is green.

## 2. Verification

- [x] 2.1 Run `openspec validate m6-rail-center-shapes --strict`. Commit.
- [x] 2.2 Play check: east west run shows no crossing, an L shows a curve, a T and a cross show a `#`, a rail past a rack stays straight, elevator floors look the same and the parked car leaves no gap.

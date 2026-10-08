# Tasks

## 1. Centre shapes

- [x] 1.1 Add `rail_curve.png` and `rail_cross.png` under `storage/src/main/resources/assets/servus_storage/textures/block/`, build the straight, curve and cross centre models and the sixteen conditioned centre parts in `RailModels.java`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 2. Verification

- [x] 2.1 Run `openspec validate m6-rail-center-shapes --strict`. Commit.
- [ ] 2.2 Play check: east west run shows no crossing, an L shows a curve, a T and a cross show a `#`.

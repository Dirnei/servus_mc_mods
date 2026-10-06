# Tasks

## 1. Frame shape and models

- [ ] 1.1 Change the shape in `storage/.../rail/ElevatorBlock.java` to four corner posts plus the rail centre plate and the arm boxes; rewrite `storage/.../datagen/ElevatorModels.java` to write `elevator_frame` with four post elements and a multipart blockstate of frame, `rail_center` and conditional `rail_arm` parts, item model `elevator_frame`. Run `gradlew :storage:runData` and verify the generated elevator blockstate has six multipart entries and `elevator_center.json` is gone.

## 2. Verification

- [ ] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew :storage:runGameTestServer` passes all three storage tests, `gradlew runClientAll` launches with all four mods. In game: stack three segments with rails on two sides, walk into the segment and stand in its middle, check the posts line up and the rail runs through. Commit.

# Tasks

## 1. Pure climbing rules

- [x] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/rack/RackClimb.java` with `Box`, `Contact`, `contacts`, `isSide` and `verticalMotion` per design.md, and `storage/src/test/java/io/github/dirnei/mc/servus/storage/rack/RackClimbTest.java` with tests: a box flush on its west face lists the two rows and one column beyond it; a box straddling two columns lists both; a box on no boundary lists nothing; a box flush on two faces (corner) lists both; `isSide` is true for the two perpendicular steps and false for front and back; `verticalMotion` returns `(0.2 - g) * 0.98` when pushing, clamps -0.5 to -0.15 when hanging, returns 0 when hanging and sneaking, and leaves 0.05 alone when hanging. Verify `gradlew :storage:test` is green.

## 2. Player tick handler

- [x] 2.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/rack/RackClimbing.java`: `@EventBusSubscriber` on the game bus handling `PlayerTickEvent.Post`, with a public static `tick(Player)` that applies the early exits, finds contacts, keeps those on a `HeavyRackBlock` side, and writes vertical motion and fall reset per design.md. Add the GameTest `rackSideClimbsAndFrontDoesNot` to `ServusStorageGameTests`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 2.2 Append "climbable side posts" to the Heavy Duty Rack line of the M4 list in `PLAN.md` and verify `openspec validate m6-rack-side-ladder` passes.

## 3. Verification

- [x] 3.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: stack three racks, climb the side posts to the top, step off while hugging the side and slide down without damage, sneak to hold, push against the front and back and stay on the ground, let a zombie stand against the side. Commit.

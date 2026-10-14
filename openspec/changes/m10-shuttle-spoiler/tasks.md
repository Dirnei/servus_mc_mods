# Tasks

## 1. Spoiler model

- [x] 1.1 In `storage/src/main/java/io/github/dirnei/mc/servus/storage/datagen/ShuttleModels.java`, replace the copper stacks of `drive_3` with the spoiler from design.md: steel wing and end plates in `heavy_rack_iron`, and two steel struts turned about X around their bottom centre by a box variant with a rotation (UV sizing stays in `box`). Update the class Javadoc and drop the unused `COPPER` constant. List every overlapping box pair (wing and plates, struts and wing, struts and hold, lid and gold corners, wing and Handling mast and crane) with whether faces share a plane. Verify: `gradlew :storage:runData` writes `models/shuttle/drive_3.json` with two rotated elements, and `gradlew build` is green.

## 2. Docs and plan

- [x] 2.1 In `docs/src/content/docs/mods/storage.md`, change the shuttle looks line from "tracks with exhaust stacks from 6" to the spoiler (steel wing, dark iron end plates, leaning struts). In `PLAN.md` M10, add `m10-shuttle-spoiler` as a follow up of item 2. Verify: docs tests green.

## 3. Integration

- [x] 3.1 Full `gradlew build`, unit tests and storage GameTests green. The game launches with all three mods.
- [x] 3.2 Play check in game by the user from main. Check a Mk3 with 6 or more Speed on the rails and as an item in the inventory, garage and workshop: the struts lean back into the wing, the wing clears the Handling mast and crane, and no copper stacks remain. Check that it passes a rack without clipping, rides the lift car, and shows no flicker on the wing, plates and struts from several angles while driving. Verify: the user confirms in game.

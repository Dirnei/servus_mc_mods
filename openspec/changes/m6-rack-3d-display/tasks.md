# Tasks

## 1. Renderer and lamp removal

- [x] 1.1 Rewrite `HeavyRackRenderer` to pose the sample items on the two shelf boards inside the frame per design.md; remove `ONLINE` from `HeavyRackBlock`; drop the lamp models and parts from `HeavyRackModels`; delete `heavy_rack_lamp_off.png` and `heavy_rack_lamp_on.png`; remove the rack lamp assertion from `heavyRackIndexesAndDisplays`. Run `gradlew :storage:runData` and verify the rack blockstate has twenty multipart entries and no lamp models remain.

## 2. Verification

- [x] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew :storage:runGameTestServer` passes all five storage tests, `gradlew runClientAll` launches with all four mods. In game: fill a rack with cobblestone, an ingot and two more types, see the items standing on the shelves from the aisle and from the side, and confirm the rack shows no lamp. Commit.

# Tasks

## 1. Look steps

- [x] 1.1 Add pure `ShuttleLook` (`speedStep`, `handlingStep`, `openHold`, `Key.of`) in `storage/src/main/java/.../storage/shuttle/module/` and JUnit `ShuttleLookTest` in `storage/src/test/java/.../storage/shuttle/module/` (step boundaries for Speed 0, 1, 2, 3, 5, 6, 8 and Handling 0, 1, 3, 4, 8, negative counts, open hold per tier, key equality). Verify: `gradlew :storage:test` green.

## 2. Part models

- [x] 2.1 Add datagen helper `storage/.../datagen/ShuttleModels.java` with the body, drive, handling and tool part models under `models/shuttle/` (vanilla placeholder textures) and make the `shuttle`, `shuttle_mk2` and `shuttle_mk3` item models children of their body; call it from `ServusStorageBlockStateProvider` in place of the old shuttle item models; delete `textures/item/shuttle*.png`. Verify: `gradlew :storage:runData` writes the models, `gradlew build` green.

## 3. Composed rendering

- [x] 3.1 Add `storage/.../client/ShuttleItemModel.java` (wrapper with per look key overrides and the composed model) and register the part models in `ModelEvent.RegisterAdditional` and the wrapper in `ModelEvent.ModifyBakingResult` in `ServusStorageClient`. Verify: `gradlew build` green.
- [x] 3.2 Update `storage/.../client/ShuttleGarageRenderer.java`: full scale, cargo only for open holds, park spacing 0.25, heading `atan2(dx, dz)`. Verify: `gradlew build` and the storage GameTests green.
- [x] 3.3 Describe the shuttle looks in `docs/src/content/docs/mods/storage.md`. Verify: docs tests green.

## 4. Integration

- [ ] 4.1 Full `gradlew build`, unit tests and storage GameTests green.
- [ ] 4.2 Play check in game by the user from main (the client launches with all three mods): Mk1, Mk2 and Mk3 shuttles look different in the inventory and on the rails, Speed and Handling counts change the drive and the back, a Harvester shows its reel, the reel faces the way the shuttle drives on north, south, east and west legs, cargo shows only on Mk1, shuttles fit beside racks and on the lift car. Verify: the user confirms in game.

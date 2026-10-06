# Tasks

## 1. Elevator block and network membership

- [x] 1.1 Add `storage/.../rail/ElevatorBlock.java` (extends `ConnectableBlock`, vertical elevator and horizontal rail connections, pillar plus rail arm shape) and `ElevatorBlockEntity.java` (`holdsStock()` false); let `RailBlock.connectsTo` accept elevator neighbours; rename the "rails" count in `WarehouseNetwork.summary()` to "connectors"; register block, item (creative tab order after the rail) and block entity in the three storage registries. Verify `gradlew :storage:compileJava` passes.

## 2. Assets and data

- [x] 2.1 Add `ElevatorModels` to the storage datagen package reusing `rail_arm`, call it from `ServusStorageBlockStateProvider`; add the elevator to `ServusStorageLanguageProvider`, `ServusStorageRecipeProvider`, `ServusStorageBlockLoot` and `ServusStorageBlockTagsProvider`; draw `textures/block/elevator.png` with a scratchpad script. Run `gradlew :storage:runData` and verify the generated blockstate has five multipart entries, and the recipe, loot table, tag and lang entries for `elevator` exist.

## 3. Verification

- [x] 3.1 Add `elevatorLinksFloors` to `ServusStorageGameTests` per design.md (online across three segments, bread beside the shaft not indexed, removal of the middle segment drops the crate stock). Verify `gradlew :storage:runGameTestServer` passes all three storage tests.
- [x] 3.2 Update `PLAN.md`: M6 gains "Rail Elevator for multi level warehouses" after the rails item, section 2.3 states that shafts of stacked segments link rail levels. Verify `openspec validate m6-rail-elevator` passes.
- [ ] 3.3 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: build a three segment shaft with rails on two floors, check the shaft looks continuous and the rails show arms into it, put a crate on one floor and the controller plus terminal on the other, confirm the terminal lists the crate, sneak dismantle a segment. Commit.

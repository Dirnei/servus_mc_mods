# Tasks

## 1. Pure compartment mapping

- [ ] 1.1 Add `storage/.../crate/CrateCompartments.java` (index from compartment count, facing and hit, front face check, centre positions) and `storage/src/test/.../crate/CrateCompartmentsTest.java` covering one, two and four compartments, all four facings, side face hits and the centre positions; delete `RackCompartments.java` and `RackCompartmentsTest.java`. Verify `gradlew :storage:test` is green.

## 2. Crate family code

- [ ] 2.1 Rework `CrateBlockEntity` to compartment arrays with per compartment insert, extract, type, count and emptiness plus totals, NBT list `Compartments`, double click per compartment; `CrateItemHandler` with one slot per compartment; `CrateBlock` with a compartment count, `DoubleCrateBlock` and `QuadCrateBlock` subclasses, right click mapped to a compartment; `ServusStorageEvents.onLeftClickBlock` mapped to a compartment; `CrateRenderer` drawing every compartment. Delete the `rack` package and `RackRenderer`. Update `ServusStorageBlocks`, `ServusStorageItems` (tab order Crate, Double Crate, Quad Crate), `ServusStorageBlockEntities` (one type for three blocks), `ServusStorageEvents` capabilities and `ServusStorageClient`. Verify `gradlew :storage:compileJava` passes.

## 3. Assets and data

- [ ] 3.1 Draw `double_crate_front`, `quad_crate_front` and their `_online` copies with a scratchpad script, delete the rack textures; update `ServusStorageBlockStateProvider` (three crates sharing the crate side and top), `ServusStorageRecipeProvider` (double and quad crate shapeless recipes, rack recipe removed), `ServusStorageBlockLoot`, `ServusStorageBlockTagsProvider` and `ServusStorageLanguageProvider`. Run `gradlew :storage:runData` and verify blockstates, models, recipes, loot and lang exist for all three crates and no rack files remain.

## 4. Verification

- [ ] 4.1 Add a GameTest `quadCrateCompartments` in `ServusStorageGameTests`: insert cobblestone into compartment 0 and dirt into compartment 3 of a quad crate through the block entity, assert the handler reports four slots with those two stacks, that compartment 0 refuses dirt, and that the counter terminal lists both. Verify `gradlew :storage:runGameTestServer` passes all four storage tests.
- [ ] 4.2 Update `PLAN.md`: section 2.3 progression item 1 and the M4 list name the crate family. Verify `openspec validate m6-crate-family` passes.
- [ ] 4.3 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: craft all three crates, fill compartments by aiming at them, double click to insert all, left click to take, check each compartment's display, check the lamp and the rail dock on a quad crate, dismantle a filled crate and place it again. Commit.

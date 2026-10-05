# Tasks

## 1. Lib helpers

- [x] 1.1 Add `IngredientGatherer` and `RoundRobin` under `lib/.../craft/` with JUnit `IngredientGathererTest` and `RoundRobinTest`. Verify `gradlew :lib:test` passes.

## 2. Assembler

- [x] 2.1 Add `recipe_slots` to `MachineStats`, `ConfiguredRecipe`, rewrite `AssemblerBlockEntity` (nine inputs, configured recipes, planning, round robin, remainders, update tag sync), `PatternSlot`, rewrite `AssemblerMenu` (ghost clicks, buttons), `ConfigureRecipePayload` and its handler, update the GameTest to `addRecipe`. Verify `gradlew build` passes.
- [x] 2.2 Rewrite `AssemblerScreen` (pattern buttons, progress bar, configured recipe panel), regenerate the assembler GUI texture and stats, add the JEI transfer handlers and catalyst, add lang keys. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 3. Verification

- [x] 3.1 Server check over RCON: assembler with the stick and iron gear recipes merged in, planks and iron ingots in the inputs, powered by a generator; after a few seconds sticks and gears alternate in the output and the inputs shrink accordingly. Verify with `data get block`.
- [x] 3.2 `gradlew :manufacturing:runGameTestServer` passes the reference block test. `gradlew clean build` green and `gradlew runClientAll` reaches the title screen with JEI loaded and no errors. Commit.

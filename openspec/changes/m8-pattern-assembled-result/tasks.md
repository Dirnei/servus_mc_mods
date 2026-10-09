# Tasks

## 1. Patterns keep the assembled result

- [x] 1.1 Add `EncodedPattern.assembledResult` and `assembled(Level)`, store the assembled pattern in `PatternEditing.save`, and refresh kept patterns lazily in `PatternStorageBlockEntity` (`storage/.../pattern/`). Add Storage GameTests in a new `PatternResultGameTests`: `rackPatternResultHasWood`, `oldPatternResultRefreshed`, `rackRequestArrives` (2 racks requested through a CPU arrive in the terminal), `woodBlockRequestArrives` (Pattern Storage from birch planks). Verify with `gradlew build test` and `:storage:runGameTestServer`.

## 2. Assembler reports what it crafts

- [ ] 2.1 Add `ConfiguredRecipe.producedResult(Level)` and report it in `AssemblerBlockEntity.production()` (`manufacturing/.../machine/assembler/`); Manufacturing GameTest `assemblerReportsWoodResult`. Verify with `gradlew build test` and `:manufacturing:runGameTestServer`.

## 3. Integration

- [ ] 3.1 Full `gradlew build test`, Foundation, Manufacturing and Storage GameTests green, `openspec validate --strict`.

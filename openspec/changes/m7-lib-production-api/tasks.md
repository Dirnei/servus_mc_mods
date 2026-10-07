# Tasks

## 1. Lib API and planner

- [x] 1.1 Add `lib/src/main/java/io/github/dirnei/mc/servus/lib/production/` with `ProductionRecipe.java`, `ProductionProvider.java`, `StockProvider.java`, `StockSink.java` and `ServusLibCapabilities.java` (three `BlockCapability.createSided` constants) per design.md. Verify `gradlew :lib:build` is green.
- [x] 1.2 Add `OrderPlanner.java` to the same package and `lib/src/test/java/io/github/dirnei/mc/servus/lib/production/OrderPlannerTest.java` with tests: in stock gives no steps and reserves; one level; two levels leaves first; surplus reused; missing raw material; depth limit on a cycle; a recipe with two outputs reserves and produces correctly. Verify `gradlew :lib:test` is green.

## 2. Assembler provider

- [x] 2.1 Add `production()` to `manufacturing/src/main/java/io/github/dirnei/mc/servus/manufacturing/machine/assembler/AssemblerBlockEntity.java` and register `ServusLibCapabilities.PRODUCTION` for the assembler in `ServusManufacturingEvents.java`. Add the GameTest `assemblerReportsItsRecipes` to `ServusManufacturingGameTests.java`. Verify `gradlew :manufacturing:runGameTestServer` passes.

## 3. Warehouse provider and sink

- [x] 3.1 Add `stock()` and `sink()` to `storage/src/main/java/io/github/dirnei/mc/servus/storage/controller/WarehouseControllerBlockEntity.java` and register both capabilities in `ServusStorageEvents.java`. Add the GameTest `controllerExposesStock` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [x] 4.1 Run `openspec validate m7-lib-production-api --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.

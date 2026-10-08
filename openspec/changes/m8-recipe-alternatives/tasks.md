# Tasks

## 1. Planner

- [x] 1.1 In `foundation/src/main/java/.../foundation/production/OrderPlanner.java` keep the single recipe path, add phase 1 (stock covered alternatives with split) and phase 2 (simulated fallback with the missing and tie rules) for several alternatives, and keep steps leaves first. Add JUnit tests in `OrderPlannerTest`: `stockedAlternativeIsUsed`, `splitsAcrossAlternatives`, `producesInputsOfAnAlternative`, `missingFollowsTheBestAlternative`, `tieGoesByRecipeOrder`. Verify with `gradlew :foundation:test` and `gradlew build`.

## 2. Game behaviour

- [x] 2.1 Add Storage GameTests in `storage/src/main/java/.../storage/gametest/AdapterOrderGameTests.java`: `charcoalFromBirchOnly` (furnace adapter, only birch logs, the charcoal request completes) and `charcoalPreviewSplitsLogs` (oak plus birch, the craft preview lists both from stock and nothing missing). Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer` and `:manufacturing:runGameTestServer` green.

## 3. Integration

- [ ] 3.1 Full `gradlew build` and `gradlew test` green and all three GameTest runs green; docs mention that the planner picks alternatives by stock (Storage page, craft section). Verify with `npx vitest run` in `docs/`.

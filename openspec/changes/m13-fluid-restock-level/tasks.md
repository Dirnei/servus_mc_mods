# Tasks

## 1. Pure restock logic

- [x] 1.1 `shuttles/.../fluidstation/FluidKeep.java`: add `restockAt`, clamp it to the amount, let it follow amount steps while equal, add `stepRestock`; update callers (`FluidOutboundBlockEntity`, `FluidOutboundMenu`, GameTests) to the new constructor. JUnit in `FluidKeepTest`: `restockLevelFollowsKeepWhileEqual`, `lowerKeepClampsRestockLevel`, `restockLevelStaysWithinKeep`, `withFluidKeepsRestockLevel`. Run `:shuttles:test`.
- [x] 1.2 `shuttles/.../fluid/FluidRestockPlan.java`: add `BUCKET`, round `amount` down to whole buckets with the min load rule against the whole buckets missing, add `restocking(was, have, restockAt, keep)`. JUnit in `FluidRestockPlanTest`: `runsCarryWholeBuckets`, `lessThanABucketIsNoRun`, `minLoadWaitsUnlessAllWholeBucketsCome`, `restockStartsAtTheLevel`, `restockGoesOnUntilLessThanABucketIsMissing`. Run `:shuttles:test`.

## 2. Station and warehouse

- [x] 2.1 `shuttles/.../fluidstation/FluidOutboundBlockEntity.java`: `stepRestock`, the `restocking` flag with getter and setter, save and load `RestockAt` (default the keep amount) and `Restocking`. Run `:shuttles:compileJava`.
- [x] 2.2 `shuttles/.../network/WarehouseNetwork.java` `restockFluids`: update the station's flag with `FluidRestockPlan.restocking` from the have, skip stations that are not restocking, plan runs with the new `amount`. Run `:shuttles:build`.
- [x] 2.3 `shuttles/.../fluidstation/FluidStationReport.java` and `datagen/ServusShuttlesLanguageProvider.java`: the report names the restock level (`keeps_restock`). Run `:shuttles:runData` and `:shuttles:build`.

## 3. GUI

- [x] 3.1 `shuttles/.../menu/FluidOutboundMenu.java`: sync the restock level (`EXTRA_COUNT` 5), `restockButton(step)` ids after the keep steps, `clickMenuButton` steps it, layout constants for the second label and button block. Run `:shuttles:compileJava`.
- [x] 3.2 `shuttles/.../client/FluidOutboundScreen.java` and the lang provider: "Restock at N B" label, its four buttons, the orange restock mark on the gauge. Run `:shuttles:runData` and `:shuttles:build`.

## 4. GameTests

- [x] 4.1 `shuttles/.../gametest/TankerRestockGameTests.java`: `restockWaitsForTheLevel`, `refillGoesOnAboveTheLevel`, `runsCarryWholeBuckets`, `lessThanABucketInTheRackIsNoRun`; fix existing restock tests whose amounts are not whole buckets. Run `:shuttles:runGameTestServer`.
- [x] 4.2 `shuttles/.../gametest/FluidOutboundGameTests.java`: `restockLevelSurvivesBreak`, `oldSaveRestocksAtKeep`, `restockButtonsStepWithinKeep`, report line with the restock level. Run `:shuttles:runGameTestServer`.

## 5. Verify

- [ ] 5.1 Full `gradlew build`, all tests and GameTests green, the game launches with all mods; play check: set a restock level, drain the tank with a pipe, watch tankers start at the level, bring whole buckets and fill back to the keep.

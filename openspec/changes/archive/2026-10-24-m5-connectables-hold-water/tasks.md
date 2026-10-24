# Tasks

## 1. Foundation: connectable blocks hold back fluids

- [x] 1.1 `foundation/.../block/ConnectableBlock.java`: implement `LiquidBlockContainer` with `canPlaceLiquid` and `placeLiquid` returning false; `getStateForPlacement` returns null when the clicked position holds a fluid source. Verify with `gradlew :foundation:build`.
- [x] 1.2 `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`: GameTests `waterStopsAtDuct` (water source beside a duct, after the flow the duct stands and its position has no fluid), `lavaStopsAtDuct`, `ductRefusedInWaterSource` (placing a line item into a water source places nothing and keeps the source) and `ductDamsFlowingWater`. Verify with `gradlew :foundation:runGameTestServer`.

- [x] 1.3 `foundation/.../block/ConnectableBlock.java` and `logistics/duct/UtilityDuctBlock.java`: placement hook `placedInto`; the duct implements `SimpleWaterloggedBlock` (property, `getFluidState`, `updateShape` water tick, `canPlaceLiquid` and `placeLiquid` through the interface defaults), waterlogs in a water source and refuses other sources. Verify with `gradlew :foundation:build`.
- [x] 1.4 `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`: replace `ductRefusedInWaterSource` with `ductWaterloggedInWaterSource` and `ductRefusedInLavaSource`; add `brokenWaterloggedDuctLeavesWater`; `ductDamsFlowingWater` checks the duct is dry. Verify with `gradlew :foundation:runGameTestServer`.

## 2. Shuttles: rails and elevator

- [x] 2.1 `shuttles/.../rail/RailBlock.java`: `getStateForPlacement` returns null when the base state is null. Verify with `gradlew :shuttles:build`.
- [x] 2.2 `shuttles/.../gametest/RailPlacementGameTests.java` and `ElevatorGameTests.java`: GameTests `waterStopsAtRail`, `railRefusedInWaterSource`, `waterStopsAtShaftTop` and `segmentRefusedInWaterSource`. Verify with `gradlew :shuttles:runGameTestServer`.

## 3. Integration

- [x] 3.1 Full `gradlew build` and `gradlew test` green, every mod's GameTests green, and the game launches with all mods. Play check: duct, rail and elevator next to flowing water and lava; a duct placed into a pond is waterlogged, buckets fill and empty it, no flicker between water and duct; a bucket on a rail or segment places the water beside it; rails and segments refused in a pond source; placement into flowing water dams it.

## Workflow follow-up

- Archive the change after the play check; add a Fixed line under `## Unreleased` in `foundation/CHANGELOG.md` and `shuttles/CHANGELOG.md`.

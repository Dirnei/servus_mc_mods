# Tasks

## 1. Reproduce

- [x] 1.1 Add `PushedOutputGameTests` in `storage/src/main/java/.../storage/gametest/`: `pushedOutputsCountForTheStep` (station beside the Access Point, all station sides Input, CPU sticks order, the test moves the crafted sticks into the Access Point through the face touching the station; the order must finish) and `otherFaceCountsNothing` (sticks inserted through another face of the Access Point leave the step's count at 0 and are put away). Verify the first test fails before the fix with `:storage:runGameTestServer`.

## 2. Credit pushed outputs

- [x] 2.1 `OrderStep.creditPushed(ItemKey, int)` capped at the step's need; `CpuOrders.creditPushed(ServerLevel, BlockPos accessPoint, BlockPos neighbour, ItemKey, int)` over the line's CPUs and open orders; `AccessPointBlockEntity.handlerFor(side)` returns cached per side wrappers that credit real inserts. Verify with `gradlew build`, `gradlew test` and `:storage:runGameTestServer` (both new tests green, all others green), plus `:foundation:` and `:manufacturing:runGameTestServer`.

## 3. Integration

- [x] 3.1 Full `gradlew build` and `gradlew test`, all three GameTest runs, docs `npx vitest run`, and `runClientAll` reaches the title screen.

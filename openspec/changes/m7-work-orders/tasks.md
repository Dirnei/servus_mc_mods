# Tasks

## 1. Order model and executor

- [ ] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/order/WorkOrder.java`, `OrderStep.java` and `OrderBook.java` per design.md (create with the Lib planner, reservations, tick executor with waiting, feeding, producing, done, collected hook, cancel, pruning). Verify `gradlew :storage:build` is green.

## 2. Network and terminal integration

- [ ] 2.1 In `WarehouseNetwork.java`: own an `OrderBook`, tick it every 20 ticks, make `planAndStart` return planned and unservable counts, create an order for a request's shortfall, show `PRODUCING` in `requestRows` (add the state to `CounterTerminalBlockEntity.RequestState`), cancel the order with the request, credit collected outputs from `drain`, and release reservations as feeds extract stock. Add lang for the new request state. Add `orders` and `order` to `ServusStorageCommands.java`. Remove the "Order prioritization" bullet from `PLAN.md` section 5. Verify `gradlew :storage:build` is green.

## 3. Tests

- [ ] 3.1 Add GameTests `orderCraftsMissingItems`, `orderWithoutProducerIsRefused`, `twoLevelOrderRunsLeavesFirst` and `cancelReleasesReservation` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [ ] 4.1 Run `openspec validate m7-work-orders --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.

# Tasks

## 1. Ledger and shipments

- [x] 1.1 Add `storage/.../order/ProductionLedger.java` (SavedData: transfers by id, next transfer id, next request id, pruning) and pure `storage/.../access/ShipmentCap.java` with `storage/src/test/.../access/ShipmentCapTest.java` (`capsInOrder`, `nothingToCap`, `firstShipmentShort`). Verify with `gradlew :storage:test`.
- [x] 1.2 Back `AccessPointBlockEntity.Shipment` with ledger transfers, save shipment ids, resolve and cap them on the first tick after loading; `isSending` and `cancel` by id; `WarehouseNetwork.Shipping` and its users (`WarehouseSupplier`, `ServusStorageCommands`, `AccessPointGameTests`) follow. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 2. Requests, orders and jobs

- [x] 2.1 Save terminal requests in `CounterTerminalBlockEntity` (ids from the ledger) and book their shipments by id in `TerminalRequests`. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [x] 2.2 Save `WorkOrder`, `OrderStep` and `OrderBook` (feedings and collections by transfer id, `StepRef`, next id); replace `OrderBook.Pending` with `Host.transfer`; `CpuBlockEntity` saves the book, stops cancelling on unload, cancels when it loads unformed, tracks collections in the ledger. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [ ] 2.3 Save the Crafting Station job with its `StepRef` owner and end orphaned jobs every 100 ticks. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 3. Tests and docs

- [ ] 3.1 Add `storage/.../gametest/PersistGameTests.java`: terminal request round trip with ids, CPU order mid step round trip and finishing after the CPU block entity was replaced from its saved tag, station job round trip, orphaned job ends, Access Point shipment round trip with cap. Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer` and `:manufacturing:runGameTestServer` all green.
- [ ] 3.2 Note in the Storage docs page that requests, orders and jobs survive restarts and add `m8-persist-production` to the PLAN.md M8 list. Verify with `npx vitest run` in `docs/` and `gradlew build test`.

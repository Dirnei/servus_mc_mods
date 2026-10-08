# Tasks

## 1. Warehouse side

- [x] 1.1 In `storage/network/WarehouseNetwork.java` remove the terminal paths (`terminals`, `request(terminal, ...)`, `retryMissing` and its timer, `cancelRequest`, `requestRows`, terminal branches in `deliverTo`, `pickLoaded`, `pickDropped`) and add `available(ItemKey)`, `ship(target, key, amount, requestId)`, `controllerPos()` and the order helpers; in `storage/access/AccessPointBlockEntity.java` add `isSending(Shipment)` and `cancel(Shipment)`. Verify with `gradlew :storage:compileJava` (the terminal still compiles against the new API after 1.2).

## 2. Terminal on the data line

- [x] 2.1 `storage/terminal`: `CounterTerminalBlockEntity` extends `BlockEntity`, implements `DataLink`, owns requests (global ids, tracked shipments, order warehouse, waiting flag), lamp, insert only output handler; `TerminalWarehouses` (reachable warehouses, merged stock); `TerminalRequests` (request, retry, bookkeeping, states, cancel); `CounterTerminalBlock` ticker and no warehouse neighbour updates; `RequestState` with `SENDING` and without `WAITING_SHUTTLE`. Register `DATA_LINK` and the item handler in `ServusStorageEvents`. Update `CounterTerminalMenu` to use them. Lang: `sending`, `no_warehouse`, drop `waiting_shuttle` and the controller reasons; run `:storage:runData`. Verify with `gradlew build`.

## 3. Tests

- [x] 3.1 Add `gametest/TerminalWiring.java` and rewire every Storage GameTest that places a Counter Terminal (Access Point at the old spot, data duct above, terminal on top), adjusting waits for packet travel. Verify with `:storage:runGameTestServer` all green.
- [ ] 3.2 Add `gametest/TerminalCableGameTests.java`: `terminalRequestArrivesAsPackets`, `terminalBesideControllerIsOffline`, `twoWarehousesMergeAndSplit`, `terminalCancelPutsPicksBack`, `hopperCannotTakeTerminalOutput`, `terminalRequestWithoutAccessPointIsRefused`. Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer` and `:manufacturing:runGameTestServer` all green and `gradlew test` green.

## 4. Docs

- [ ] 4.1 Update `docs/src/content/docs/mods/storage.md` (the terminal sits on the data line, deliveries arrive as packets through an Access Point). Verify with `npx vitest run` in `docs/` and `npm run build`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, all three GameTest suites green, `runClientAll` reaches the title screen.

# Tasks

## 1. Order link and collection

- [ ] 1.1 `storage/.../order/WorkOrder.java`: `deliverTo`, `directSent`, saved and loaded; `TerminalRequests.request` and `craft` set `deliverTo`. Verify with `gradlew build`.
- [ ] 1.2 `CpuBlockEntity.Host.collect`: send the order's item up to the request's need straight to the terminal, rest to the Access Point, fall back when the move is refused; `CounterTerminalBlockEntity.need(requestId)`. Verify with `gradlew build`.
- [ ] 1.3 `TerminalRequests`: `retry` skips requests with an open order; `book` lowers `missing` to what is still needed. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 2. Tests and docs

- [ ] 2.1 Add `storage/.../gametest/DirectDeliveryGameTests.java` (sticksGoStraightToTerminal, surplusIsPutAway, chainDeliversOnlyFinal, deliverToSaved). Verify with `:storage:runGameTestServer`.
- [ ] 2.2 Note direct delivery in `docs/src/content/docs/mods/storage.md`; docs tests; full `gradlew build test` green.

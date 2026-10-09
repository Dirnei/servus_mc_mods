# Tasks

## 1. Order link and collection

- [x] 1.1 `storage/.../order/WorkOrder.java`: `deliverTo`, `directSent`, saved and loaded; `TerminalRequests.request` and `craft` set `deliverTo`. Verify with `gradlew build`.
- [x] 1.2 `CpuBlockEntity.Host.collect`: send the order's item up to the request's need straight to the terminal, rest to the Access Point, fall back when the move is refused; `TerminalRequests.need`, `expectDirect` and `directShort`. Verify with `gradlew build`.
- [x] 1.3 `TerminalRequests`: `retry` skips requests with an open order; the direct send lowers `missing` and a short delivery adds it back. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 2. Tests and docs

- [x] 2.1 Add to `storage/.../gametest/CraftingCpuGameTests.java` (sticksGoStraightToTerminal, surplusIsPutAway, chainDeliversOnlyFinal, deliverToSaved). Verify with `:storage:runGameTestServer`.
- [ ] 2.2 Note direct delivery in `docs/src/content/docs/mods/storage.md`; docs tests; full `gradlew build test` green.

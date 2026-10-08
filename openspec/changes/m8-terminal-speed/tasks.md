# Tasks

## 1. Send on open

- [ ] 1.1 Add `storage/.../gametest/TerminalSpeedGameTests.java` with `counterSendsOnOpen` and `productionSendsOnOpen` that open through `setSynchronizer` (the real open path) and fail today; switch `TerminalCraftGameTests.terminalSendsOnOpen` to the same path. Override `sendAllDataToRemote()` in `CounterTerminalMenu` and `ProductionTerminalMenu` with a shared `sendNow()` also used by `broadcastFullState()`. Verify with `gradlew build` and `:storage:runGameTestServer` green.

## 2. Server view cache

- [ ] 2.1 `storage/.../index/WarehouseIndex.java`: `version()`, no-op `update` for an unchanged snapshot, sorted `entries()` cached per version. JUnit in `WarehouseIndexTest`: `unchangedSnapshotKeepsVersion`, `changeBumpsVersion`, `removeBumpsVersion`, `entriesCachedPerVersion`. Verify with `gradlew :storage:test`.
- [ ] 2.2 Cache the merged stock in `TerminalWarehouses.stock` by warehouse identities and index versions, and the craftable list in `TerminalCraftables.of` per data network by producer recipe signatures. GameTest `craftablesCachedOnLine` (same list instance while nothing changed, new list after a pattern is saved and after the adapter is removed). Verify with `gradlew build test` and `:storage:runGameTestServer`.

## 3. Client cache

- [ ] 3.1 Add the terminal position (and dimension) to `StockSyncPayload` and `ProductionPayload`; keep content per terminal in `ClientStock` and `ClientProduction` with `open(pos)` instead of `clear()` in the screen constructors; clear the maps on client logout. Verify with `gradlew build`, `:storage:runGameTestServer`, and a grep that every payload type is registered.

## 4. Docs and wrap up

- [ ] 4.1 Note in `docs/src/content/docs/mods/storage.md` that terminals open with their last content at once; run the docs tests. Verify with `npx vitest run` in `docs/` and `gradlew build test`.

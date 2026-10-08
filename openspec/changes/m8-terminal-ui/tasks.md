# Tasks

## 1. Server side crafting

- [x] 1.1 Extract `CpuOrders.producers(level, network)` from the CPU host; add `OrderBook.preview(key, amount)` sharing the planning with `create`, `CpuOrders.preview`, `TerminalRequests.craft` and the craftable list (`TerminalCraftables`). Files: `cpu/CpuOrders.java`, `cpu/CpuBlockEntity.java`, `order/OrderBook.java`, `terminal/TerminalRequests.java`, new `terminal/TerminalCraftables.java`. Verify with `gradlew build`.
- [x] 1.2 Payloads: `StockSyncPayload` gets `craftables` with a hand written codec; new `CraftPreviewRequestPayload`, `CraftPreviewPayload`, `CraftStartPayload`, registered in `ServusStoragePayloads`; `CounterTerminalMenu.preview` and `craft`. Verify with `gradlew build` and a grep that every payload TYPE is registered.

## 2. Instant content and Access Point GUI

- [ ] 2.1 `broadcastFullState` sends the payload at once in `CounterTerminalMenu` and `ProductionTerminalMenu` (with `firstSyncTick`), client `loaded` flags and a Loading hint; `InboundStationMenu.stillValid` accepts the Access Point. Verify with `gradlew build`.

## 3. Layout

- [x] 3.1 Pure `terminal/TerminalLayout` and `storage/src/test/.../terminal/TerminalLayoutTest` (tests `counterAreasDoNotOverlap`, `counterAreasInsidePanel`, `productionAreasDoNotOverlap`, `productionAreasInsidePanel`, `panelsFit1080p`); menus take slot positions from it. Verify with `gradlew :storage:test`.
- [x] 3.2 Counter Terminal screen: painted panel from the layout, filter button, craftable entries and marker, craft dialog with preview, Start and Cancel, click rules, Loading hint; delete `textures/gui/counter_terminal.png`; lang. Verify with `gradlew build`.
- [ ] 3.3 Production Terminal screen: title row, tab bar below, list, editor and inventory from the layout, painted panel, Loading hint; delete `textures/gui/production_terminal.png`; lang. Verify with `gradlew build`.

## 4. Tests and docs

- [ ] 4.1 GameTests in a new `gametest/TerminalCraftGameTests.java`: `craftablesListSticks`, `craftPreviewFromStock`, `craftPreviewMissing`, `craftPreviewNoCpu`, `craftStartDeliversSticks`, `terminalSendsOnOpen`, `accessPointMenuStaysValid`. Verify with `gradlew :storage:runGameTestServer`, all green.
- [ ] 4.2 Storage docs page: crafting from the Counter Terminal, filter, preview. Verify with `npx vitest run` in `docs/`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, Storage, Foundation and Manufacturing GameTests green. (The client is not launched by this change; the user checks in game.)

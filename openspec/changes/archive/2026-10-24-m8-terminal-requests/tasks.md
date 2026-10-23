## 1. Pure logic

- [x] 1.1 `shuttles/.../index/ReservationTally.java`: per source amounts, totals, version that moves only on a change, `set`, `remove`, `absorb`, `clear`, `reserved`, `totals`, static `requestable(entries, reserved)`. Add `ReservationTallyTest`: set and totals over two sources, replace lowers a total, remove, same amounts keep the version, requestable subtracts, drops entries at 0 and resorts. Check: `gradlew :shuttles:test`.
- [x] 1.2 `shuttles/.../terminal/RequestSplit.java`: `prefill`, `cap`, `of`. Add `RequestSplitTest`: prefill a stack when craftable, min of stack and requestable otherwise, at least 1; cap at requestable or 99999; split all from stock, part crafted, capped when the rest is not craftable, nothing available. Check: `gradlew :shuttles:test`.
- [x] 1.3 `shuttles/.../terminal/TerminalLayout.java`: order grid parts (`ORDER_GRID`, `ORDER_ARROW`, `ORDER_RESULT`, amount, status, steps, preview, buttons) fixed in the base dialog area; remove the old popup parts. Extend `TerminalLayoutTest`: order parts apart, inside the base dialog, clear of the inventory. Check: `gradlew :shuttles:test`.

## 2. Requestable stock

- [x] 2.1 `lock/LockedSlots.outLocked()`; `network/WarehouseNetwork`: tally fed by `rackLocksChanged`, `onNodeAdded`, `dropSource`, `onAbsorb`, `onDisposed`, with `reservedOut`, `reservationVersion`, `reservedTotals`; `rack/HeavyRackBlockEntity.onLocksChanged` reports to its network. Check: `gradlew :shuttles:build`.
- [x] 2.2 `terminal/TerminalWarehouses.stock` caches on index and tally versions and returns requestable stock; `cpu/CpuBlockEntity` host stock uses it. Check: `gradlew :shuttles:build`.

## 3. Requests capped and split

- [x] 3.1 `terminal/StorageTerminalBlockEntity.Request.requested` mutable; `terminal/TerminalRequests.request` caps with `RequestSplit`, crafts the rest only when the preview is startable, shrinks to what was planned, refuses `out_of_stock`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `terminal/CraftPreviewRequestPayload` with a mode, `terminal/CraftPreviewPayload` with `inStock` and `patternSaved` and a hand written codec; `menu/StorageTerminalMenu.preview` per mode; `client/StorageTerminalScreen` request prompt prefill, clamp, split line and preview. Check: `gradlew :shuttles:build`, every payload type registered.
- [x] 3.3 GameTests `TerminalRequestGameTests`: the grid count drops on a request and a fully reserved item leaves the grid; a second request for a single item is refused; a request above stock without a CPU is capped with nothing missing; update `terminalRequestsAndPushes`. `TerminalCraftGameTests`: a request above stock with a sticks pattern ships the stock and makes a craft order for the rest. Check: `gradlew :shuttles:runGameTestServer`.

## 4. One-off orders

- [x] 4.1 `order/WorkOrder` one-off pattern saved and loaded; `order/OrderBook` `create` and `preview` with a one-off, `Host.producersWith`, target recipes limited to the one-off, `relocate` with it; `cpu/CpuOrders` `producers` with an extra pattern, `createOneOff`, `previewOneOff`; `cpu/CpuBlockEntity.patternFor` uses the order's one-off. Check: `gradlew :shuttles:build`.
- [x] 4.2 `pattern/PatternEditing.encode` shared by `save`; `terminal/TerminalRequests.craftOneOff`; `terminal/TerminalCraftables.canOrder`. Check: `gradlew :shuttles:build`.

## 5. Order grid

- [x] 5.1 `menu/StorageTerminalMenu`: ghost grid with `GhostSlot`, `clicked`, `fillGhosts`, `startOrder(amount, save)` with the save after the order, ORDER preview; remove the crafting container, result slot, clear and store on close; `terminal/StockSyncPayload` `orderGrid` flag; `terminal/OrderStartPayload`; `GridFillPayload` without max; delete `GridClearPayload`; `terminal/ServusShuttlesPayloads` registrations. Check: `gradlew :shuttles:build`, every payload type registered.
- [x] 5.2 `client/StorageTerminalScreen`: `Dialog.ORDER` with ghost cells, result, amount, steps, preview, status, Start with the save question, Close; Craft button only with `orderGrid`; `client/ClientStock.orderGrid`; JEI `StorageTerminalRecipeTransferHandler` with the transfer helper refusing without a CPU and station. Check: `gradlew :shuttles:build`.
- [x] 5.3 Datagen lang for the new strings and removal of the old grid strings (`ServusShuttlesLanguageProvider` or where terminal strings live), `runData`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.
- [x] 5.4 GameTests replacing `CraftingGridGameTests` with `OrderGridGameTests`: Craft flag off without CPU and station, on with them; ghost click sets one copy and leaves the cursor; JEI fill picks the stocked option and takes nothing; a one-off crafting table order creates a producing request and the crafting table lands in the output; Save as pattern stores the pattern, Only this once does not; a one-off order survives a save and load of the CPU's book. Check: `gradlew :shuttles:runGameTestServer`.

## 6. Plan and integration

- [x] 6.1 `PLAN.md`: note the change in the Shuttles open list; `shuttles/CHANGELOG.md` line goes in at archive. Check: `openspec validate m8-terminal-requests --strict`.
- [x] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green twice; launching the game with all mods is left to the user's play check.

## 7. Play check

- [x] 7.0 Play check fix: the no-shuttle reason on the Inbound Station, Outbound Station and terminal reads "no shuttle available" (`ServusShuttlesLanguageProvider`, generated `en_us.json`); `gradlew :shuttles:runData`, `gradlew build`, `gradlew test` once the client is closed.

- [x] 7.1 Play check in game by the user: request the last item of something and see it leave the grid; a second click is refused; the request prompt stops at the stock or shows "from stock, crafted" with a CPU; the Craft button shows only with a CPU and a Crafting Station; ghost cells by click and by JEI; a one-off order lands in the output; Save as pattern and Only this once; nothing overlaps in the order grid at every height.

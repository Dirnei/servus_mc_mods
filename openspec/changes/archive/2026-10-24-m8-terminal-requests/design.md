## Context

See proposal.md for why. The pieces this change builds on:

- `m8-slot-locks` (applied, not archived): a pick locks the exact rack slots it empties at planning time (`LockedSlots.lockOut` from `WarehouseNetwork.startJobs`), `WarehouseNetwork.available(key)` counts unlocked items, a lock change calls `LockedSlots.onLocksChanged`, which a rack turns into `setChanged()`. Racks save their locks.
- The terminal grid comes from `TerminalWarehouses.stock`, the merged `WarehouseIndex.entries()` of the reachable warehouses, cached by index versions. Locks do not move the index version.
- `TerminalRequests.request` ships `min(available, amount)` from the warehouses with the most stock and turns the rest into a CPU order when `CpuOrders.create` plans one, else leaves it missing. A planned order whose raw material is missing is created BLOCKED.
- The crafting popup is a real `TransientCraftingContainer` with a `ResultSlot` in `StorageTerminalMenu`; JEI's `StorageTerminalRecipeTransferHandler` sends `GridFillPayload` to take items from the player inventory; `GridClearPayload` and `removed()` store the grid.
- The Production Terminal's pattern editor already has a ghost grid (`GhostSlot` over an `ItemSlots(9)`, `clicked` override, client side result through `CraftingGrid.match`) and `PatternEditing.save`.
- `OrderBook` plans with `Host.producers()`, whose Crafting Stations carry one recipe per pattern of the line (`PatternRunners.withPatterns`). When a step starts, `CpuBlockEntity.patternFor(step)` looks the step's recipe up in the line's `PatternLibrary` and hands the `EncodedPattern` to the station (`CraftingStationBlockEntity.assign`). A recipe that is no pattern cannot run.

No open decision of PLAN.md section 5 is settled here. The user decided the behaviour on 2026-10-23: requestable count with a cheap tally (A), requests beyond stock only when the rest is crafted (B), the crafting popup becomes a one-off order grid (C) that offers to save its pattern (D).

## Goals / Non-Goals

**Goals:**
- The grid count is what can still be requested, cheap enough for the half second refresh.
- A request never promises items that are not there; the server is the judge, the client only helps.
- A one-off order runs exactly the grid's recipe and survives a reload without being a saved pattern.

**Non-Goals:**
- No change to how picks lock, plan or travel.
- CPU reservations (`OrderBook.reservedTotal`) stay out of the grid count; they are not locks.
- No fluid entries.

## Decisions

### 1. A per warehouse tally of OUT locked items

`index/ReservationTally<S, K>` (pure, no Minecraft imports) keeps the OUT locked amount per key for every source and the totals per key, with a version that moves whenever a total changes. `set(source, amounts)` replaces one source's amounts (no change, no version bump), `remove(source)`, `reserved(key)`, `totals()`, `version()`, `clear()`.

`WarehouseNetwork` owns one tally keyed by rack position:
- `LockedSlots.outLocked()` sums the OUT locks of one inventory per key.
- `HeavyRackBlockEntity`'s `onLocksChanged` calls `WarehouseNetwork.rackLocksChanged(pos, slots)` through `network()`, which re-reads that rack only (a rack is one chest, at most a few dozen slots).
- `onNodeAdded` of a rack source reads its locks (covers load, chunk load, splits and joins, since nodes are handed to the new network through `onNodeAdded`); `dropSource` removes it; `onAbsorb` takes the other network's per rack amounts; `onDisposed` clears.
Release paths (pick done, cancel, orphan sweep, job drop, load) all end in `LockedSlots.release` or `loadLocks`, which call `onLocksChanged`, so no path needs its own bookkeeping.

Alternative: walk every rack's locks when the terminal syncs. Rejected by the user (cost grows with the warehouse) and it would still need a change signal for the cache.

### 2. Requestable stock merge

`TerminalWarehouses.stock` keys its cache on two versions per warehouse, index and tally, and returns `ReservationTally.requestable(entries, reserved)`: every entry less its reserved total, entries at 0 or below dropped, largest first. With no reservations the index list is returned as is, so the common case costs nothing. `PayloadSync` already rebuilds the payload every 10 ticks and compares a signature, so a lock change reaches the screen within half a second; a request or cancel also forces a sync.

The Crafting CPU's `Host.stock()` uses the same requestable counts, so an order never plans with items a pick has already locked.

Incoming items need no rule: the index counts only rack contents, and IN locks are not in the tally.

### 3. Request split and the server cap

Pure `terminal/RequestSplit`:
- `prefill(stackSize, requestable, craftable)`: a stack when craftable, else `min(stack, requestable)`, at least 1.
- `cap(amount, requestable, craftable)`: the highest amount the prompt allows (99999 when craftable, else `requestable`, at least 1).
- `of(asked, available, restCraftable)` returns `fromStock` and `toCraft`; `toCraft` is 0 and `fromStock` is `min(asked, available)` when the rest cannot be crafted.

`TerminalRequests.request` sums `available(key)` of the reachable warehouses (lock aware), and when the amount is above it asks `TerminalCraftables.preview` for the rest. The rest is crafted only when the preview is startable (CPU, producer, nothing missing). Otherwise the request is made for what is available, and refused `out_of_stock` when that is 0. When shipping plans fewer than asked (a refusal such as `needs_shuttle` from one warehouse) and no order takes the rest, the request's `requested` shrinks to what was planned (the field becomes mutable) instead of leaving it missing. `missing` then only grows from `book()` when a shipment in flight ends short, which `retry` ships again.

### 4. The request prompt asks the server

`CraftPreviewRequestPayload` gains a `mode`: `CRAFT` (as today), `REQUEST` (the amount is the whole request) and `ORDER` (the item is ignored; the server reads the menu's ghost grid). `CraftPreviewPayload` gains `inStock` (how many a request ships from stock) and `patternSaved` (a pattern of the line has the order grid's recipe and items). Eight fields exceed what we want to spell with `StreamCodec.composite`, so it gets a `StreamCodec.of` writer and reader like `StockSyncPayload`.

The client clamps the prompt with `RequestSplit.cap` using the entry's requestable count and the craftable flag of the cell, asks a `REQUEST` preview only while the amount is above the requestable count, and draws "10 from stock, 54 crafted" plus the preview columns and status. Request is disabled while that preview is missing or not startable.

### 5. One-off orders

`WorkOrder` gets an optional `EncodedPattern oneOff`, saved under `OneOff` with `EncodedPattern.CODEC` through `registries.createSerializationContext(NbtOps.INSTANCE)` (the way `PatternStorageBlockEntity` and `CraftingStationBlockEntity` save patterns). Orders saved without it load with none.

`OrderBook.create(key, amount, requestId, oneOff)` and `preview(key, amount, oneOff)` plan with `Host.producersWith(oneOff)`: the line's producers with the one-off added to every Crafting Station as one more pattern (`CpuOrders.producers(level, network, extra)` passes the extra entry into `PatternRunners.withPatterns`, which skips it when a saved pattern is the same work). Recipes that make the target are then limited to the one-off's recipe id with its exact inputs, so the order crafts what the grid shows even when another pattern makes the same item. `relocate` uses the same producer list for a one-off order's steps. `CpuBlockEntity.patternFor(step)` returns the owning order's one-off when the step runs that recipe with those inputs, else looks in the library as before.

`TerminalRequests.craftOneOff(level, terminal, pattern, amount)` mirrors `craft`: preview, refuse on refusal or missing, new request for the result item, `CpuOrders.createOneOff`, deliver to the terminal. The amount counts result items like the craft dialog; a craft that makes four sticks for an amount of 5 runs twice and the spare sticks go to storage like any craft surplus.

Alternative: save every grid as a hidden pattern. Rejected: patterns are visible data the player manages (PLAN, "patterns are data"), and a hidden pattern would fill Pattern Storage places.

### 6. Ghost grid in the Storage Terminal menu

`StorageTerminalMenu` drops the crafting container, the result slot, `clearGrid`, `fillGrid` and the store on close. Nine `GhostSlot`s over an `ItemSlots(9)` sit at the order grid's positions; `isActive` follows the client's order dialog (`setCraftOpen`), `clicked` on a ghost slot sets a single copy of the cursor or clears it on the server (as `ProductionTerminalMenu.clicked`), vanilla slot sync shows it on the client, and `quickMoveStack` ignores ghost slots. `GridFillPayload` loses `max`; `fillGhosts(options)` picks per cell the option with the most requestable stock, else the first. `GridClearPayload` is deleted.

The result is matched on the client with `CraftingGrid.match` for drawing, and on the server with a new `PatternEditing.encode(level, cells)` that both `save` and the order grid use (assembled result, template with count 1).

### 7. Order dialog on the screen

The order grid is a fourth `Dialog` kind, `ORDER`, so it shares the dialog panel, the amount field, the step buttons, Start (confirm) and Close (cancel) with the request and craft dialogs. `applyDialog` moves those widgets to the order layout. All positions come from new `TerminalLayout.Storage.ORDER_*` rectangles fixed to the base rows (the panel origin at y 18), because menu slots cannot move with the height setting; on taller settings the dialog panel still covers the extra rows and the order parts sit in its lower 126 pixels. The ghost items are drawn again after the dialog panel, since the panel is painted over the slots. Start asks the server for nothing new: when the preview says `patternSaved` it sends `OrderStartPayload(amount, false)` at once; otherwise the button row turns into "Save as pattern" and "Only this once", which send `save` true or false. The dialog closes after a start.

`TerminalLayoutTest` checks that the order parts stay apart, inside the base dialog area and clear of the player inventory.

### 8. When the Craft button shows

`TerminalCraftables.canOrder(level, terminal)`: the terminal's data line has a formed CPU (`CpuOrders.cpus`) and a Crafting Station among its producer members. `StockSyncPayload` gets a 13th field `orderGrid`, part of its signature; the screen hides Craft and closes an open order dialog when it turns false, and the server refuses `OrderStartPayload` with `no_cpu` or `no_station`. JEI's handler gets `registration.getTransferHelper()` and returns `createUserErrorWithTooltip` while `ClientStock.orderGrid()` is false (JEI 26.1.2 common API 29.43.0.107, `IRecipeTransferHandlerHelper`, checked with javap).

### 9. Saving from the order grid

`StorageTerminalMenu.startOrder(amount, save)` encodes the grid, starts the one-off order and, only when the order was made and `save` is set, calls `PatternEditing.save(level, line, player, cells, null)`. `duplicate` is silent (the pattern is there); `no_storage` and `full` set the message line to `pattern_no_storage` or `pattern_full` ("Ordered once: ..."). Saving after the order keeps a refused order from leaving a pattern behind.

### APIs used

- `AbstractContainerMenu.clicked`, `Slot.isActive`, vanilla slot sync (`broadcastChanges`); `GhostSlot` from this mod.
- `StreamCodec.of`, `ByteBufCodecs`, `ItemStack.OPTIONAL_LIST_STREAM_CODEC` (already used by `GridFillPayload`).
- `HolderLookup.Provider.createSerializationContext(NbtOps.INSTANCE)` with `EncodedPattern.CODEC` (as in `PatternStorageBlockEntity`).
- JEI `IRecipeTransferRegistration.getTransferHelper()`, `IRecipeTransferHandlerHelper.createUserErrorWithTooltip(Component)`.
- NeoForge `ClientPacketDistributor.sendToServer` and the mod's `toMenu` payload registration in `ServusShuttlesPayloads`.

### Pure and Minecraft parts

- Pure, JUnit: `ReservationTally` (set, replace, remove, totals, version only on change, requestable merge), `RequestSplit` (prefill, cap, split), `TerminalLayout` order parts.
- Minecraft, GameTests: the grid count after a request, the second request for the last item, the cap without CPU, the split order, the Craft flag, the one-off order and its save, the reload of a one-off order; the screen itself in the user's play check.

## Risks / Trade-offs

- [The index is refreshed in batches, the tally at once] When a shuttle takes its locked items the lock goes before the index sees the slot change, so the count can rise for a tick or two before it falls. → The index re-reads a dirty rack on the next drain; the sync runs every 10 ticks, so the bump rarely reaches the screen.
- [Rack chunk unloaded] An unloaded rack leaves the network with its tally entry, as it leaves the index. → Both come back with the node on load.
- [Stale client] The client clamps with a count up to half a second old. → The server caps every request again and says out of stock when nothing is left.
- [One-off order and saved patterns diverge] A one-off order keeps its pattern even when a saved pattern of the same recipe is deleted mid order. → Intended: the order runs what it was started with.
- [Players who crafted by hand in the terminal] They lose it. → Decided by the user; a Crafting Table does that.

## Migration Plan

No data migration. Work orders gain an optional field; old saves load without it. Terminal saves are unchanged (the grid was never saved).

## Open Questions

None.

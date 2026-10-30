# Design

## Context

- `m6-warehouse-handheld` (applied, play checked, not archived) owns the handheld: `HandheldItem`, `HandheldActions`, `HandheldMode` (ZONE, FIND, INFO), data components in `ServusShuttlesComponents`, the device GUI `HandheldScreen` with a FIND stock list, the HUD `HandheldHud`, effects in `ClientHandheld` and `HandheldRenderer`, and scans of whole shelves through `ShelfZones`. Scans reach `handheld_scan_range` blocks (16) by a custom ray (`HandheldItem.aim`).
- Shuttles already move items from racks to a destination: `WarehouseNetwork.planAndStart` plans `RequestPlan` jobs from `stockOf(key, destination)` (unlocked stock per rack with travel cost), locks OUT the source slots (`LockedSlots.lockOut`), and queues a `PickJob(id, requestId, key, amount, source, terminal)`. Delivery goes to any `DeliveryTarget` (its `deliveries()` `LockedSlots`): `readyToSend` locks IN room there, `deliverTo` inserts with `insertAs(jobId)`.
- Racks hold their items in `LockedSlots` (`HeavyRackBlockEntity.items()`); `SlotFill` has the pure slot arithmetic (`in`, `room`, `available`).
- Zone filters: `StorageNodeBlockEntity.zoneFilter()`; the put-away routing checks the same filter (`zones` spec, "Zone filters on blocks").

## Goals / Non-Goals

**Goals:**
- A basket on the handheld that a later Request action reuses unchanged.
- Rack to rack moves through the existing pick path, so shuttle traffic, locks, travel cost and the Data Potato behave as for terminal requests.

**Non-Goals:**
- A new shuttle job type; a relocation is a set of `PickJob`s whose destination is a rack bay.
- Rebalancing or choosing sources by hand.

## Decisions

### Basket on the item
New data component `HANDHELD_BASKET`: a `List<BasketEntry(ItemStack item, int amount)>` with a `Codec` and `StreamCodec`, at most `Basket.MAX_ENTRIES = 9` (a GUI limit like `ZoneFilter.SLOTS`, not balance). Pure helper `Basket` (no Minecraft types, generic over the key) adds, merges, sets, removes and clears entries; JUnit covers it. The stack keeps the basket through logout like the other handheld components. See the two baskets below for what a relocation does to them.
Alternative: basket in a per player saved data. Rejected: the handheld already keeps mode, zone and binding on the stack, and a second handheld should have its own basket.

### Editing the basket
FIND screen, under the stock list: a row of basket cells (icon, short count), a Clear and a Relocate softkey-style CRT button. Clicking a stock item, in the warehouse list or a scanned shelf's list, opens an amount popup on the Foundation `NumberStepper` for it (default one stack, Cancel and Add; shift click adds a stack at once); right clicking a stock item lights up its shelves, the FIND pick that a left click did before. Clicking a basket cell opens the popup for that entry (Remove and Save), right click removes it. A hint over the basket row says which click does what, for a stock cell or a basket cell. The stock list shrinks from six to four rows to make room for the basket row and the Clear and Relocate buttons. A new item for a full basket is refused on the screen with a BASKET FULL note. Every edit goes to the server as `HandheldBasketPayload(op, item, amount)`; the server edits the component of the handheld in the open `HandheldMenu`, the screen reads it back from the menu state. Relocate sends `HandheldRelocatePayload`; the server sets `HANDHELD_PICK_TARGET` (boolean component) and closes the menu.

### Two baskets and a stashed relocation (play check)
The play check split the basket by the list it belongs to:
- `HANDHELD_BASKET` stays the warehouse basket: filled while FIND lists the whole warehouse, kept for the later Request, emptied only by Clear. That view has Clear but no Relocate.
- New component `HANDHELD_SHELF_BASKET`: the scanned shelf's bay position plus a `Basket` list, filled while FIND lists that shelf. `HandheldBasketPayload` edits the basket of the list the menu shows (the menu knows whether it shows a shelf). It is emptied when the list leaves the shelf: the ALL button (`HandheldMenu.clickMenuButton`), a FIND scan of another shelf, `setMode`. Closing the GUI keeps it; opening the GUI on another shelf empties it.
- Relocate (only in the shelf view, with entries and a binding) copies the shelf basket and its bay into a new component `HANDHELD_RELOCATION` (source bay plus entries), empties the shelf basket and sets pick target mode. `setMode` removes `HANDHELD_RELOCATION` with the flag. A successful target scan removes it; a refusal keeps it.
- Source: `WarehouseNetwork.relocate` gets the source shelf and plans picks only from its bays; the target equal to the source shelf is refused as `NOT_A_TARGET`. Shortfall is what the source shelf no longer holds. The pure `BasketFit` is unchanged.

### Pick target mode is a flag, not a mode
`HANDHELD_PICK_TARGET` is set only in FIND. `setMode` clears it, so sneak scroll and the softkeys leave it. `HandheldActions.scan` in FIND checks the flag first: set means target scan, unset means the FIND scan of today. The server side of the basket, Relocate and the target scan lives in `HandheldRelocation`. A right click in pick target mode on a block that is no rack bay buzzes instead of opening the GUI; a click into the air still opens it. The panel shows `PICK TARGET` and the entry count in place of the FIND lines.
Alternative: a fourth `HandheldMode`. Rejected: it would join the sneak scroll cycle and the softkeys, and a mode the player cannot pick directly is confusing.

### Target scan
On the server, for the shelf of the scanned bay (`ShelfZones.shelf`):
1. Refuse unless every bay is a `HeavyRackBlockEntity` of the bound warehouse.
2. Per entry: refuse it when the shelf's zone filter does not take the item (same rule as put-away); else `movable = min(amount, available outside the shelf)` from `stockOf`, with the shelf's bays removed.
3. Plan: per entry, `RequestPlan.plan` over the stock outside the shelf gives the picks (up to a stack from one source each).
4. Fit: `BasketFit.place(shelfSlots, picks, maxStack)` on the `SlotFill.Slot` states of all bays of the shelf, one list over the whole shelf. It places the picks one after another in `SlotFill.in` order (partial stacks first, then empty slots, over the whole shelf), each pick closing the slots it lands in as its IN lock would. No fit refuses the whole scan and places no lock.
5. Start: per pick and per bay its slots lie in, lock OUT at the source and IN on exactly the slots the fit chose; queue `PickJob(id, 0, key, count, source, bay)`. A pick whose slots lie in two bays becomes two picks.
6. Outcome: `WarehouseNetwork.relocate(shelf, Map<ItemKey, Integer>)` returns a `Relocation`: started, offline, not a target, no room, or nothing to move (every entry refused or short), and per entry moved, short and refused. Only a start ends pick target mode; every other outcome buzzes red and keeps it.
The zone rule is `WarehouseZones.takes(pos, key)`: no filter takes anything, a filter takes what it matches by item or tag, as in put-away.
`HeavyRackBlockEntity` implements `DeliveryTarget` with `deliveries() = items()`; `readyToSend` then finds the IN lock already held (`slots.holds(id)`) and sends at once. `pickLoaded` and `pickDropped` only act for Packing Stations, so a short pick on a rack needs no new code; a lost source is replanned by `replanBrokenSources` as today.
Alternative: a `RelocateJob` record. Rejected: it would repeat the pick path (load, travel, unload, held loads, saves) for the same movement.

### Pure logic
- `Basket`: add and merge, set amount, remove, clear, capacity. JUnit `BasketTest`.
- `BasketFit`: where a list of (key, count) loads goes in `SlotFill.Slot` states with a stack limit per key, partial stacks first, locked slots skipped, a slot taken by one load closed to the next; empty when one does not fit. JUnit `BasketFitTest` (shared empty slots, partial top up, locked slot ignored, exact fit, one too many).
Everything touching worlds, menus and shuttles is covered by GameTests: zone refusal, no room refusal leaves locks untouched, shortfall, target shelf excluded as source, pick jobs delivered into the target shelf, Fluid Rack refused, mode switch clears pick target and keeps the basket.

### APIs
- Data components: `DataComponentType.builder().persistent(codec).networkSynchronized(streamCodec)` through `DeferredRegister.DataComponents`, as the existing handheld components.
- Payloads: `RegisterPayloadHandlersEvent`, `PayloadRegistrar.playToServer`, as `HandheldModePayload`.
- No new capabilities; racks keep `Capabilities.Item` as today.

### PLAN.md
Settles nothing open in "Open decisions"; adds the open point "Relocate stock with the handheld basket" under Shuttles, and records the Request action on the same basket as the next step.

## Risks / Trade-offs

- [A relocation of a full basket queues up to 9 x many stacks of picks and blocks shuttles for terminal requests] → picks share the normal queue; one relocation is bounded by the shelf's room (a shelf of 3 bays), so it is small.
- [Room locked on the target shelf for picks whose source runs dry] → the pick's IN lock is released when the job ends short, as for terminal picks; GameTest checks the target slots are free again.
- [Two handhelds relocating onto the same shelf at once] → the second fit sees the first one's IN locks as taken and refuses for no room.
- [The other session's uncommitted handheld GUI work] → this change starts only after `m6-warehouse-handheld` is archived.

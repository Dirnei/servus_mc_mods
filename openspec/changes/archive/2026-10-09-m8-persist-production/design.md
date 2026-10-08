# Design

## Context

Progress of production is spread over several block entities that point at each other with live Java objects: a terminal `Request` holds `AccessPointBlockEntity.Shipment` objects, an `OrderStep` holds anonymous `OrderBook.Pending` views on those shipments and `Delivery.onEnd` listeners for outputs moving back, a Crafting Station holds its job owner as an `OrderStep` object identity. None of that is saved, and the CPU cancels its orders in `onChunkUnloaded`. See proposal.md for why this changes.

## Goals / Non-Goals

**Goals:**
- Requests, orders, station jobs and shipments survive a server restart and chunk unloads, and the pieces find each other again by ids.
- Progress that happens while an owner is unloaded (packets landing, shipments ending) is not lost.

**Non-Goals:**
- Saving packets in flight or shuttle pick jobs (see proposal).

## Decisions

### A level wide ledger for transfers
`order/ProductionLedger` is a `SavedData` per dimension (`level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(...), "servus_storage_production")`, NeoForge 21.1 / vanilla 1.21.1 `SavedData.Factory(Supplier, BiFunction<CompoundTag, HolderLookup.Provider, T>, DataFixTypes)`). It keeps:
- `Transfer` entries by `long` id: asked, delivered, ended, ended at. Used for Access Point shipments (asked = planned) and for outputs a CPU moves from a producer to an Access Point (asked = sent).
- the next transfer id and the next request id.

Owners keep only ids. The Access Point's delivery listener and the CPU's collection listener write into the `Transfer`, which lives as long as the level, so a listener firing while its owner is unloaded is not lost. Packets are settled before the world saves at server stop (cable-packets), so their listeners run before the ledger is saved.

Ended transfers are pruned 24000 ticks after they end, when a new transfer is made. An owner that asks for an id that is gone treats it as ended: an input shipment as delivering nothing (it is shipped again; at worst the producer keeps a spare input), a terminal shipment as delivering nothing (shipped again; spare items stay in the output slots), an output collection as fully landed (no items are duplicated).

Alternative considered: every block entity saves its own copies. Rejected: listeners of unloaded owners would write into dead objects and counts would drift.

### Shipments
`AccessPointBlockEntity.Shipment` keeps id, target and item; planned, delivered and ended are read from and written to its `Transfer`. The Access Point saves its shipments by id and resolves them on its first tick after loading. On that first tick it caps every open shipment to what its outgoing buffer holds for that item, oldest shipment first (pure `ShipmentCap`, JUnit); the rest is booked as a shortfall. This covers shuttle picks that were lost with the restart; picks that still arrive later find no shipment and are put away as orphans (existing rule). `WarehouseNetwork.Shipping` carries the shipment id; `isSending` and `cancel` take ids.

### Terminal requests
`Request` gets saved fields (id, item, requested, delivered, missing, order CPU, shipments as access point position, shipment id and asked). Ids come from the ledger's request counter, so `newRequest` needs the server level. `book` reads ended shipments from the ledger.

### CPU orders
`WorkOrder` and `OrderStep` get `save`/`load` methods writing NBT with the registry provider (item stacks for keys and recipe stacks). A step saves its producer, recipe (id, inputs, outputs, ticks), batches, state, reason, fed and collected maps, open feedings (item, transfer id, asked) and open collections (item, transfer id, sent); in flight counts are rebuilt from the open collections. `OrderBook` saves its orders and next id; the `Pending` interface is replaced by transfer ids and `Host.transfer(id)`. Collections are booked from the ledger in `advance`, not by listeners on step objects.

`CpuBlockEntity` saves the book on the master. It no longer cancels in `onChunkUnloaded` or `setRemoved`; cancelling on break stays in `CpuBlock.onRemove`. A master that loads unformed cancels what it saved on its first tick.

### Step references and station jobs
Each step has a `StepRef(cpu master, order id, step index)` assigned by the book. The station's job owner becomes that `StepRef` (value equality) instead of an object, and is saved with the pattern (EncodedPattern codec) and crafts left. Every 100 ticks a station with a job checks its owner: when the CPU master is loaded and has no open order with that step, or the step is done, the job ends. An unloaded CPU leaves the job alone.

### Order of loading
Block entities may load in any order. Nothing is resolved in `loadAdditional` (no level there); ids are resolved lazily against the ledger on the first tick or the first query.

### Tests
- JUnit: `ShipmentCapTest` (cap in order, nothing to cap, buffer short for the first shipment).
- GameTests (Storage): save and load round trips with `saveWithFullMetadata` and `loadWithComponents` for a terminal request, a CPU order mid step, a station job and an Access Point shipment; an order that finishes after its CPU block entity was saved, removed and placed again with the saved tag; an orphaned station job ends; a new request id is above the saved one.

### PLAN.md open decisions
None. PLAN.md does not promise in memory production; the M8 list gets this follow-up item.

## Risks / Trade-offs

- [The cap after loading also cuts shipments whose picks are still on a shuttle when only the Access Point's chunk reloads] -> the owner ships the rest again and the late picks are put away; slower, never lost.
- [Unknown transfer ids after 24000 ticks unloaded] -> rules above choose shipping again over waiting forever.
- [A crash still loses packets in flight] -> unchanged from cable-packets.

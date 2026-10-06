# Design

## Context

See proposal.md for motivation. Today `WarehouseControllerBlockEntity` persists a shuttle count, `WarehouseControllerBlock` adds and takes Shuttle items, and `ShuttleFleet` (owned by `WarehouseNetwork`) keeps one list of shuttles, one home rail (the rail touching the controller) and one `ArrayDeque<ShuttleJob>` of `PickJob` and `PutAwayJob`. `CounterTerminalMenu.pull` either extracts instantly or queues a `PickJob(key, amount, requester, terminal)` whose delivery goes into the requester's inventory. `WarehouseNetwork.insert`, `extract` and the station's `chooseTarget` treat every source alike. `WarehouseConnectionRule` has kinds `STOCK`, `RAIL`, `ELEVATOR`, `STATION`; `StorageNodeBlockEntity.docksRail()` is true for `STOCK` and `STATION`. The index knows sources by position; `HeavyRackBlockEntity` is the only rack block entity. `StorageStatsManager` reads `warehouse.json`. The terminal screen is a search box, a 9 by 4 stock grid and the player inventory; `StockSyncPayload` goes to the player with the menu open.

## Goals / Non-Goals

**Goals:**
- Shuttles become a real building block: a garage per aisle, visible fleet size, racks that need them.
- One pure planner decides how a request becomes jobs, unit tested.
- The fleet keeps its data only design: no entities, O(shuttles) per tick, one shared queue.
- The counter is a pickup desk with visible progress.

**Non-Goals:**
- See proposal non-goals. No request persistence, no traffic, no reservations.

## Decisions

### Settles no PLAN.md open decision, rewords section 2.3
The "decided" sentence about shuttles on the controller and the instant fallback is replaced: shuttles live in garages, crates stay instant, racks need shuttles, requests are collected at the counter. The M6 list gains "Shuttle Garage, rack picks by shuttle, terminal requests".

### Garage as a node kind
`WarehouseConnectionRule.NodeKind` gains `GARAGE` with the same links as `STATION`: to `STOCK`, `STATION`, `GARAGE` and `RAIL` on any side, never to `ELEVATOR`. `docksRail()` includes it. `WarehouseNetwork.onNodeAdded` keeps garages in a `garages` set (neither source nor connector, candidates skip them like stations) and tells the fleet `fleet.adoptGarage(pos, count)`; removal calls `fleet.releaseGarage(pos)`. `ShuttleGarageBlockEntity` (kind `GARAGE`) persists `"Shuttles"` and has `addShuttle`, `takeShuttle`, `clearShuttles` like the controller had; the block handles the item clicks, the summary message, `onRemove` dropping shuttle items, and the carrot. Capacity is `StorageStatsManager.garageCapacity()`.

### Fleet keyed by garage
`ShuttleFleet` holds `Map<BlockPos, Garage>` where `Garage` has its home rail (first travel neighbour, refreshed on membership changes) and its shuttles. Each `Shuttle` remembers its garage and parks at that garage's home. `tick` assigns queued jobs to the free shuttle with the shortest `RailPaths.find` to the job's first goal rails, trying garages with a home rail only. `releaseGarage` drops cargo at the garage, requeues jobs at the front and forgets the shuttles. `absorb` merges the maps. `canCarry(station, target)` and the new `canReach(pos)` ask whether any garage with a home rail and at least one shuttle has a path to rails beside the position. The controller position is no longer needed by the fleet; sync packets go to players tracking each garage's chunk. `summary()` reports garages, shuttles, busy, queued, put-aways and shuttles without a rail.

### Racks are shuttle only
`WarehouseNetwork` learns `isRack(pos)`: the source's block entity is a `HeavyRackBlockEntity`. Instant paths skip racks: `extract` and `extractAnywhere` never touch them, `insert` and `chooseTarget` skip rack positions unless the caller is a shuttle path (`insertAt` and `extractFrom` still work for shuttles). The station drain, when the first block with room is a rack, requires `fleet.canCarry(station, rack)`; otherwise it looks for the next non rack target and, finding none, reports `Reason.NO_SHUTTLE`. `PutAwayPlan.Reason` gains `NO_SHUTTLE`.

### Pure request planning: `RequestPlan`
Package `io.github.dirnei.mc.servus.storage.shuttle`, no Minecraft imports:
- `record Stock<S>(S source, int count, boolean rack, boolean reachable)`.
- `RequestPlan.plan(int amount, int maxStack, boolean hasShuttle, List<Stock<S>> stock)` returns `Plan<S>(List<Job<S>> jobs, int unservable, boolean needsShuttle)` where `Job(S source, int count, boolean byShuttle)`. Instant jobs come first from non rack, unreachable sources (they are immediate), then shuttle jobs from reachable sources in stock order, each at most `maxStack`. Rack stock that is unreachable, or reachable without a shuttle, counts as `needsShuttle` and is never a job. Stock short of the amount leaves `unservable`.
- `RequestPlanTest`: splits into stacks, instant before shuttle, rack without shuttle is refused, unreachable rack ignored, short stock reported, zero amount gives no jobs.

### Requests on the terminal
`CounterTerminalBlockEntity` gets an `ItemStackHandler output` of `terminalOutputSlots()` (no capability registered, so never indexed and never hopper reachable; `onRemove` drops it) and a transient `List<Request>` with `Request(id, key, requested, delivered, state)` and states `WAITING_SHUTTLE`, `PICKING`, `WAITING_ROOM`, `MISSING`. `CounterTerminalMenu.request(key, amount)` builds `Stock` from `index.sourcesHolding(key)` and the snapshots, asks `fleet.canReach` for each source, plans, and either refuses (message in the sync payload: `needs_shuttle`, `no_rail`) or creates the request and its jobs. Instant jobs run at once through `extractFrom` and `deliver(terminal, stack)`; shuttle jobs are `PickJob(requestId, key, amount, source, terminal)` (the requester is gone from the record). A terminal touching no rail cannot take shuttle jobs: the plan is made with `hasShuttle = false` for it. `cancel(id)` removes queued jobs of that request from the fleet and marks the request done.

### Delivery into output slots
`ShuttleFleet.Host` gains `deliverToTerminal(BlockPos terminal, ItemStack stack)` returning the remainder, and `terminalRails(pos)` through the usual `railsBeside`. A shuttle's pick legs are `TO_SOURCE` (load) then `TO_TERMINAL` (unload) then `HOME`. If the remainder is not empty the shuttle enters `WAITING_ROOM` at the terminal rail and retries every tick until it unloads; the request shows `WAITING_ROOM`. If the terminal is gone the cargo pops at that position. Instant deliveries that do not fit wait in a per terminal pending list drained on `WarehouseEvent.OutputChanged`. Each delivery updates the request's `delivered`; a request with everything delivered leaves the list; a request whose jobs found no stock shows `MISSING` for the rest.

### Terminal GUI
The screen grows to 176 by 240: search 18, grid 34 to 106 (4 rows), open requests list 110 to 150 (4 rows of 10 px, scrollable, click cancels), output slots row at 154, inventory at 176 and hotbar at 234. Clicking a grid entry opens an amount prompt drawn over the grid: an `EditBox` with the amount, buttons `+1`, `+10`, `+64` (labelled stack), `Request` and `Cancel`; Enter confirms. Shift click sends `RequestItemPayload(stack, maxStack)` directly. Payloads: `RequestItemPayload(ItemStack, int)` and `CancelRequestPayload(int)` to the server; `StockSyncPayload` gains the request rows and a `message` key for refusals. The menu adds `SlotItemHandler`s for the output slots; `quickMoveStack` from an output slot goes to the inventory, from the inventory pushes into the warehouse as today.

### Controller cleanup
`WarehouseControllerBlockEntity` loses the shuttle count and its NBT; `WarehouseControllerBlock` loses the shuttle item handling and drop; the summary comes from the network. `max_shuttles` leaves `StorageStatsManager` and `warehouse.json`; `garage_capacity` and `terminal_output_slots` arrive.

### Datagen
Garage blockstate with `FACING` and `ONLINE` through the existing `oriented` helper, item, loot, recipe (rail between iron ingots over planks), pickaxe tag, lang; terminal GUI texture redrawn with the request list and output row; lang for states and refusal reasons.

### Minecraft side verification
GameTests: `garageHoldsShuttles` (add, cap, take back, break drops), `shuttlesParkAtTheirGarage` (two garages, the nearer one serves, both return home), `rackNeedsShuttle` (rack on the controller, no garage: request refused; instant insert skips it), `rackPickByShuttle` (rack front on a rail, garage with one shuttle, request lands in the output slots), `requestSplitsAndDelivers` (100 from one crate by shuttle, two loads, progress rows), `outputFullShuttleWaits`, `terminalPushSkipsRacks`, `stationWaitsForShuttleOnRacks`; existing `shuttlePicksFromRail` and `shuttleCarriesPutAway` are rewritten for garages. Manual in the client: the amount prompt, the request list, carts parked at each garage.

## Risks / Trade-offs

- [Rack warehouses stall without a garage] → every refusal names the reason, the controller summary shows garages, and crates still work instantly for the early game.
- [Shuttle waiting at a full terminal blocks the fleet] → only that shuttle waits; the request list shows "waiting for room" so the player empties the slots.
- [Requests lost on reload] → accepted and documented; stock is never lost, only the order.
- [Nearest shuttle search runs a path search per free shuttle] → fleets are small (a few garages) and jobs are rare; the graph search is bounded by the rail count.
- [Existing worlds with shuttles on controllers] → unreleased, no migration: shuttles on a controller are dropped as items on load by the controller's cleanup once.

## Open Questions

None that change the specs or tasks. GUI layout details are decided during apply.

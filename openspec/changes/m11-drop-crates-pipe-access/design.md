# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116):

- `storage/network/WarehouseNetwork` keeps a `Source` per indexed block with a `BlockCapabilityCache` on `Capabilities.Item.BLOCK`, for its own nodes (racks and crates) and for every block discovered next to a stock node (`addCandidatesAround`, `pullsAround`, `indexedForeign`). `NoStock` (added today in `m11-wireless-packing-station`) keeps the Storage Terminal and the Crafting Station out of that discovery.
- Instant paths: `WarehouseNetwork.extract` and `insert` (controller stock provider and sink, `/servus_warehouse take`, the terminal grid refill and Alt pull in `menu/StorageTerminalMenu`, JEI transfer), `RequestPlan` instant jobs for non rack stock, `startPicks` and the `deferred` picks to a Packing Station, `ShuttleFleet.serveInstantly` for jobs no shuttle can take, and the Inbound Station's instant put-away. `WarehouseNetwork.order` ranks empty crates and foreign inventories in use inside a put-away tier.
- The racks are already reached only by shuttles, but the warehouse reads and writes them through their item capability.
- Data line packets reach a block through `PacketIo` (`foundation/data/packet`): `accepts`, `insert` and `acceptable` look at `Capabilities.Item.BLOCK`, then `ServusFoundationCapabilities.STOCK_SINK`; `available` and `extract` look at `STOCK_PROVIDER`, then `Capabilities.Item.BLOCK`. `CpuBlockEntity.extractable` reads producers through `Capabilities.Item.BLOCK` directly. Data membership (`DataMembership`) does not depend on the item capability.
- `ServusStorageEvents.onRegisterCapabilities` registers `Capabilities.Item.BLOCK` for FIELD_HEAD, CRATE, HEAVY_RACK, INBOUND_STATION, PACKING_STATION (per face `PushCredit`, which credits pushed outputs to a running CPU step through `CpuOrders.creditPushed`), WIRELESS_PACKING_STATION, STORAGE_TERMINAL (`packetHandler`), OUTBOUND_STATION and CRAFTING_STATION. The Crafting CPU registers none. Foundation's Machine Adapter registers its own item handler, which forwards into the machine.
- `UtilityDuctBlockEntity.computeArm` shows an item arm toward a neighbour exactly when `level.getCapability(Capabilities.Item.BLOCK, ...)` is not null, recomputed on neighbour changes. `DuctItemHandler` takes inserts on sides whose mode allows it and never gives items out.
- `SideConfig` has fixed mode lists: `MACHINE_MODES` (Crafting Station), `INBOUND_MODES` (Input, Pull, Off) and `OUTBOUND_MODES` (Push, Output, Off).
- `crate/StorageStatsManager` (all Storage JSON stats) and `crate/ShuttleStatsReader` live in the crate package.

## Goals / Non-Goals

**Goals:**
- One answer to "what is an inventory": the warehouse's own racks are stock; blocks register the item capability only where pipes belong.
- Data line packets keep reaching every block they reach today, without the item capability on Servus blocks.
- Every pick and put-away runs through racks and shuttles; no instant path is left.
- The rack's pipe access follows its rails through placement events, never polling.

**Non-Goals:**
- No new early game blocks or a cheaper path into the warehouse (see the balance note below).
- No change to Foundation machines, Manufacturing machines or the Machine Adapter beyond the shared `PacketIo` lookup.

## Decisions

### D1. The packet inlet is an item resource handler under its own capability id

`ServusFoundationCapabilities.PACKET_INLET` is a `BlockCapability<ResourceHandler<ItemResource>, Direction>` created with `BlockCapability.createSided(Identifier.fromNamespaceAndPath("servus_foundation", "packet_inlet"), ResourceHandler.asClass())`, exactly how NeoForge 26.1.2.116 defines `Capabilities.Item.BLOCK` (`net/neoforged/neoforge/capabilities/Capabilities.java`, line 38 of the sources jar; `BlockCapability.createSided` line 119). Blocks register it with `RegisterCapabilitiesEvent.registerBlockEntity` like the other Servus capabilities. Because the type is the same as the item capability's, each block keeps the handler object it registers today (`handlerFor(side)`, `packetHandler()`, the Packing Station intake) and only the capability key changes; `ItemView` wraps it as before. Item pipes, hoppers and other mods only ever ask `Capabilities.Item.BLOCK`, so they never see it, and `computeArm` shows no item arm.

The name follows the user and `m8-cpu-buffer`: it is the door through which packets come in. A block whose items the data line collects (the Crafting Station's outputs) lets its inlet handler give those items out as well; a block that only receives (terminal, Packing Station, Field Head, CPU buffer) gives nothing out.

Alternatives: a new `PacketInlet` interface with insert and extract methods (every block writes an adapter, no reuse of `ItemView`); `STOCK_SINK` (gives the block the `STOCK` data role, which the duct summary counts as storage); an insert only item capability (rejected by the user: pipes show arms toward blocks that refuse items).

### D2. Lookup order in `PacketIo`

A new public helper `PacketInlets.handler(level, pos, side)` in `foundation/data/packet` returns the packet inlet on that face, else the item capability, wrapped in `ItemView`. `PacketIo.accepts`, `insert`, `acceptable` and `settle` use it before `STOCK_SINK`; `available` and `extract` ask `STOCK_PROVIDER` first, then the helper. `CpuBlockEntity.extractable` uses the helper instead of `Capabilities.Item.BLOCK`. So foreign machines, the Machine Adapter (which keeps its item handler and forwards into the machine on its profile's sides, `MachineAdapterBlockEntity.machineHandler`) and Manufacturing machines keep working unchanged through the fallback. The inlet adds no data membership: `DataMembership` is untouched.

### D3. Who registers what

| Block | Item capability | Packet inlet |
| --- | --- | --- |
| Inbound Station, Outbound Station | removed | none (not data members) |
| Packing Station | removed (with `PushCredit`) | intake, rate limited as today |
| Wireless Packing Station (while it exists) | removed | none (never on a data line) |
| Storage Terminal | removed | `packetHandler()` (requested items to Output, the rest to Input) |
| Crafting Station | removed | job filtered inputs, outputs extractable, on every face |
| Field Head | removed | anti-corrosion slot, insert only |
| Crafting CPU | none today | registered later by `m8-cpu-buffer` |
| Heavy Duty Rack | only while standalone (D5) | none |
| Machine Adapter, Foundation and Manufacturing machines, ducts | unchanged | none |

The fluid capability of the Field Head and the energy capability of the Wireless Packing Station stay.

### D4. Stations act on their neighbours; passive side modes go

`SideConfig.INBOUND_MODES` becomes Pull, Off (fallback Off; the Inbound Station sets its back to Pull in its constructor like the Outbound Station sets Push), `OUTBOUND_MODES` becomes Push, Off, and a new `STATION_MODES` (Push, Pull, Off, fallback Push) replaces `MACHINE_MODES` on the Crafting Station. `SideConfig.set` already maps a mode outside the list to the fallback, so saved dev configs load without special code. The existing pull and push loops (`InboundStationBlockEntity` line 181, `OutboundStationBlockEntity` line 258, `CraftingStationBlockEntity` line 439) keep using the neighbour's `Capabilities.Item.BLOCK`, so stations still work with chests, hoppers, machines and ducts that take items in. A duct gives nothing out (`DuctItemHandler.extractItem`), so a pipe line that should feed an Inbound Station ends in a chest or hopper beside it; this goes into the docs.

Inbound default back Pull and the other sides Off: Input on all sides was harmless, Pull on all sides would drain every chest touching the station. The Outbound default stays back Push.

### D5. Rack pipe access follows its front rails

The provider is `(rack, side) -> rack.standalone() ? rack.items() : null`. `standalone()` walks the section with `HeavyRackBlock.section(level, pos)` (at most `bays_per_section` bays) and checks whether a `RailBlock` (Warehouse Rail and `FarmRailBlock`, which extends it) stands at `bay.relative(facing)`. The check is live and cheap, so a stale flag can never open a docked rack. Whether the rail belongs to an online warehouse does not matter, so the rule needs no network state.

Notification is event driven: each bay keeps an unsaved `frontRail` flag, set in `onLoad`. `HeavyRackBlock.neighborChanged` (already overridden) compares it with the block in front; when it changed, and in `onPlace` and `affectNeighborsAfterRemoval` (section joins and leaves), a new `rack/RackDock.refresh(level, pos)` calls `Level.invalidateCapabilities(bay)` (NeoForge patch on `Level`, already used by `CpuBlock` and `SinkBlock`) and `Level.updateNeighborsAt(bay, block, null)` (26.1.2 three argument form, `net/minecraft/world/level/Level.java` line 338) for every bay of the section before and after the change. The neighbour update makes ducts recompute their arms and hopper caches ask again; it does not loop, because the bays' flags no longer change on the second pass.

Pure part: `RackDock.closed(int[] offsets, IntPredicate frontRail)` (any bay of the section has a front rail), JUnit `RackDockTest`.

Alternatives: section closed only for the bays with a rail (inconsistent with the shared GUI and the shuttle serving the whole section); closing on network membership instead of the rail (needs network state in a capability provider and an extra event path).

### D6. The warehouse reads its racks directly

Since every member rack is docked, its item capability is null. `Source` drops its `BlockCapabilityCache` and reads `StorageNodeBlockEntity.stock()`, a new method that returns an `ItemView` over the node's own slots (the rack returns its `ItemSlots`; other nodes return null). Snapshot polling, `extractFrom`, `insertAt`, shuttle loading and unloading and the op `take` command go through it.

### D7. Only racks are sources

`WarehouseNetwork` loses `addCandidatesAround`, `pullsAround`, `indexedForeign`, the non node branch of `addSource` and the foreign part of the site span; sources are nodes whose `stock()` is not null. `index/CandidateNeighbours` and `CandidateNeighboursTest` go. `NoStock` and its two implementations go. `WarehouseConnectionRule.NodeKind.STOCK` now only describes the controller (links to rails and to another controller, so two touching controllers still make the network offline); Javadoc and `WarehouseConnectionRuleTest` follow. This settles the open question in PLAN.md 2.3 (Network topology: "whether crates, the controller and foreign inventories should become rail only as well"): crates and foreign inventories are gone, the controller keeps linking to touching rails.

### D8. No instant path

- `RequestPlan.Stock` drops its `rack` flag; `plan` makes only shuttle jobs from reachable stock and reports `needsShuttle` when stock exists that no shuttle reaches. `RequestPlanTest` follows.
- `WarehouseNetwork.order` keeps "holding the item first, then the other racks"; empty crates and foreign tiers go. `PutAwayPlan` and `PutAwayPlanTest` follow where they name those tiers.
- `startPicks` only queues shuttle picks; `deferred`, `releaseDeferred` and `dropDeferred` go. `held` and `deliverInstant` stay only as the safety path the fleet uses when a terminal lost its rail while a shuttle carries its loads.
- `ShuttleFleet.serveInstantly` becomes a refusal: a pick no shuttle can take ends as a shortfall with the reason needs a shuttle; a put-away no shuttle can take releases its claim and the station shows needs a shuttle.
- `WarehouseNetwork.insert` goes; `extract` becomes an op only `takeDirect` for `/servus_warehouse take`. The controller's stock provider answers counts and gives nothing on extract; its `STOCK_SINK` registration and `sink()` go.
- `StorageTerminalMenu` loses `refill` and the Alt pull; `StorageTerminalRecipeTransferHandler` fills only from the player inventory.
- `PackingStationBlockEntity.PushCredit` and `CpuOrders.creditPushed` go; `PushedOutputGameTests` is deleted.

### D9. Crates and the stats package

Deleted: `crate/CrateBlock`, `DoubleCrateBlock`, `QuadCrateBlock`, `CrateBlockEntity`, `CrateItemHandler`, `CrateCompartments`, `client/CrateRenderer`, their registry entries, the crate branch of `ServusStorageEvents.onLeftClickBlock`, datagen (blockstates, models, loot, recipes, lang, tags), textures `crate_*`, `double_crate_*`, `quad_crate_*`, `data/servus_storage/storage/crate.json` and `CrateCompartmentsTest`. `StorageStatsManager` and `ShuttleStatsReader` move to `storage/stats`, and `StorageStatsManager` drops the crate keys. The Mk1 shuttle keeps its "open wooden crate" look; that is a model, not the block.

### D10. GameTests move to a minimal shuttle warehouse

A shared helper `gametest/MiniWarehouse` builds the smallest real warehouse: controller, a short rail run, a rack facing the rail, a Shuttle Garage at the rail with one Mk1 shuttle, and optionally a Packing Station with a data duct and a Storage Terminal through `TerminalWiring`. `stock(helper, stacks...)` fills the rack's slots through `HeavyRackBlockEntity.items()` as test setup. Rails stay 2 to 3 blocks long and timeouts grow by the shuttle trip time. About 200 of the 374 Storage GameTests build crates, chests beside nodes or use the item capability of the listed blocks; they move class by class.

### D11. Archive order and the other open changes

- After `m11-wireless-packing-station`: its MODIFIED "Sources are nodes and adjacent inventories" and "Input row" are the base. This change REMOVES the former and rewrites "Input row" on its wording.
- Before `m8-cpu-buffer`: it registers the CPU's inlet through `PACKET_INLET`. It needs two follow-ups (not edited here): its D9 unform puts the buffer away with `WarehouseNetwork.insert`, which this change removes (send the buffer as packets to the collection Packing Station before the blocks unform, or drop at the block), and its Impact names `storage/crate/StorageStatsManager`, now `storage/stats/StorageStatsManager`.
- `m11-controller-wireless` (open point, not edited): its MODIFIED "Sources are nodes and adjacent inventories" fails if it archives after this change and must be dropped; its ADDED "Transfers between sites" says picks are planned "instant from crates ..." and uses "the outpost's crate" in a scenario, both need racks and shuttles. If it archives first, nothing here breaks, and the Wireless Packing Station registration task here becomes moot.
- `m11-warehouse-power`: its ADDED shuttles scenario "drives to a crate six rails away" should say rack; wording only.

### What is pure and what is tested how

Pure Java with JUnit: `RequestPlan`, `PutAwayPlan`, `WarehouseConnectionRule`, `RackDock`. Everything else touches Minecraft and is covered by GameTests (`MiniWarehouse` based rewrites, new tests in `PipeAccessGameTests` and `RackDockGameTests`, a Foundation `PacketInletGameTests`) and the final play check.

### Balance note: the first shuttle setup

Current recipes (generated `data/servus_storage/recipe`): Heavy Duty Rack 4 iron, 3 planks; Warehouse Rail 6 iron, 1 plank for 16; Shuttle Garage 2 iron, 1 rail, 3 planks; Mk1 shuttle 5 iron, 2 redstone, 1 rail; Warehouse Controller 3 iron, 2 redstone, chest, 3 planks; Storage Terminal 2 iron, 3 glass, chest, 3 planks; Packing Station 2 iron, 2 copper, 1 redstone, chest, 3 planks; plus Data Cable. A first warehouse with a terminal costs about 24 iron, 5 redstone, 2 copper and 3 chests, against 8 planks and a chest for a crate before. That is reachable in the early iron age, and racks open by hand from the start, so nothing must get cheaper for this change. Candidates for the M9 balance pass: the Mk1 shuttle (for example 3 iron), the Packing Station's copper, and the rack's iron. Not a requirement.

## Risks / Trade-offs

- [Big test rewrite] About 200 GameTests change. → One shared helper, class by class tasks, each ending with the Storage GameTest run.
- [Slower tests] Shuttle trips add ticks per test. → Short rails, raised timeouts only where a shuttle runs.
- [Pipe lines into storage] A duct can no longer insert into an Inbound Station. → The station pulls from a chest or hopper the duct fills; docs explain the pattern.
- [Outbound into a duct] The station pushes into a duct that shows no arm toward it, because the station has no item capability. → Accepted; the push works (`DuctItemHandler.insertItem` checks only the side mode).
- [Lost conveniences] Grid refill, Alt pull and warehouse fed JEI transfer go. → Listed as a non-goal; may return as requests.
- [Hoppers into a docked rack stop] Players who fed racks by hopper see it stop when they lay a rail. → Intended; the docs explain it.
- [Ordering with other changes] See D11.

## Migration Plan

Nothing is released, so no world migration. Dev worlds lose placed crates (unknown block) and keep everything else; old side modes fall back as D4 says. After archiving, the empty `openspec/specs/storage/crate/` spec is deleted.

## Open Questions

- Exact balance values for the first warehouse (M9 balance pass).

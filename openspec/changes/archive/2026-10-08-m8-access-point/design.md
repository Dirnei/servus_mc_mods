# Design

## Context

The data network (`foundation.data`) finds members through capability caches per duct side; `DataPackets.send` moves a stack the caller holds from a member to another member and reports a `Delivery` with `onEnd` listeners. In Storage, the Inbound Station (`InboundStationBlockEntity`, node kind STATION) is a rail linked intake buffer that `WarehouseNetwork` drains by zone rules; terminal requests are planned by `RequestPlan` and run by `startJobs`, delivering instantly from crates or by shuttle (`ShuttleFleet.requestPick`) into a `DeliveryTarget`'s buffer. See proposal.md for why and the specs for behavior.

## Goals / Non-Goals

**Goals:** a link role in Foundation; an Access Point that reuses the station put-away and the request planner; a shipping API later changes call; shortest shuttle trip choice.

**Non-Goals:** terminal and CPU integration, an Access Point GUI of its own, packet rendering.

## Decisions

### data_link capability (Foundation)
`ServusFoundationCapabilities.DATA_LINK = BlockCapability.createSided(servus_foundation:data_link, DataLink.class)` with `DataLink` an empty marker interface in `foundation.production` next to the other capabilities. `DataRole.LINK`; `DataMembership.isMember` and the `DataNetwork` probe gain a fourth cache. `DataSummary` keeps counting producers and stock only (links are counted as members but not shown, to keep the summary line short).

### Access Point extends the Inbound Station
`AccessPointBlockEntity extends InboundStationBlockEntity implements DeliveryTarget`. Inheriting gives the rail linking (kind STATION), the intake buffer, the put-away drain in `WarehouseNetwork`, the lamps and the intake GUI for free. Overrides: the item capability is the intake on every side (packets may arrive from any face, the front included, so the station's locked front does not apply), a second `ItemStackHandler outgoing` (same size, `inbound_buffer_slots`) is the `DeliveryTarget` buffer, and both buffers drop when broken. `AccessPointBlock extends InboundStationBlock` with its own codec, block entity, ticker and the carrot only dismantling (no side cycling, sides do not matter). The intake GUI is the Inbound Station GUI titled "Access Point". Alternative: a new block from scratch; rejected, it would duplicate the drain and lamp code.

### Shipping
`AccessPointBlockEntity` keeps a FIFO of `Shipment` records (`id`, `target`, `key`, `planned`, `delivered`, `ended`), saved in its NBT without the id object identity. Its server tick, when no packet delivery of its own is running, takes items of the head shipment's key out of `outgoing` (up to what the shipment still needs) and calls `DataPackets.send(level, self, target, stack)`; the delivery's `onEnd` adds the delivered count and puts undelivered items back into `outgoing`. A shipment ends when `delivered >= planned`, or when the target cannot be reached (then the rest stays in `outgoing` and is returned to the intake so the warehouse puts it away). One running delivery per Access Point is the throughput rule. When `outgoing` changes, the Access Point posts `WarehouseEvent.OutputChanged` so held instant loads retry, like terminals do.

`WarehouseNetwork.ship(BlockPos target, ItemKey key, int amount)` returns `Shipping(refusal, Optional<AccessPointBlockEntity.Shipment>)`: offline, `no_access_point` (none of the warehouse's Access Points is a member of a data network that reaches the target), `out_of_stock`, `needs_shuttle`, `no_rail`; otherwise it chooses an Access Point, plans with `RequestPlan` against that Access Point as destination (like `planAndStart`) and registers a shipment of the planned amount on it. `ServusStorageShipping.ship(ServerLevel, BlockPos controller, BlockPos target, ItemStack like, int amount)` is the entry point other code and the command use.

### Choosing the Access Point
Candidates are the warehouse's Access Points whose data network `reaches` the target. The trip of a candidate is the shortest rail path (`RailPaths.find` over `travelNeighbours`) from the dock rails of any rack holding the item to the candidate's dock rails; when no rack holds it (crates, foreign inventories), every trip is 0. Pure `AccessPointChoice.choose(List<C>, ToIntFunction<C>)` returns the first candidate with the lowest finite trip; unreachable candidates (no path) only win when no candidate has a path. JUnit tested.

### Registration, data and docs
Block `access_point`, block entity, item, creative tab entry after the Inbound Station, capabilities (item handler on all sides, `DATA_LINK` on all sides), lang, loot (drop self; contents dropped in code), pickaxe tag, recipe (iron ingots, chest, redstone, copper ingot), textures made from the Inbound Station textures with a teal tint by a helper script. `/servus_warehouse ship <controller> <target> <item> <count>`. Docs: an Access Point section on the Storage page.

### APIs used
`BlockCapability.createSided`, `RegisterCapabilitiesEvent.registerBlockEntity` (NeoForge 21.1, already used by the Storage capabilities), `ItemStackHandler`, existing `DataPackets`, `RequestPlan`, `RailPaths`.

### Testing
Pure: `AccessPointChoiceTest` (lowest trip, tie keeps first, unreachable loses, all unreachable picks first). GameTests (Storage, `AccessPointGameTests`): data_link detection (link member), joins warehouse and data network, packets put away into a crate, ship from a crate to a Crafting Station, ship from a rack by shuttle, choice of the nearer of two Access Points, refusal without an Access Point, broken Access Point drops its buffers.

### PLAN.md open decisions
None of section 5. Follows 2.4 (Access Point, shortest shuttle trip, tie any).

## Risks / Trade-offs

- [Reusing the Inbound Station means the Access Point shows the inbound GUI] -> acceptable until a later change needs more.
- [Shipments are not restored with their ticket objects after a restart] -> the Access Point resumes sending what it has; later changes poll counts, not objects.
- [Instant crate deliveries for a shipment can arrive before the shipment is registered] -> the shipment is registered before `startJobs` runs.
- [Items left over when the target vanished] -> moved back into the intake so they are put away, never voided.

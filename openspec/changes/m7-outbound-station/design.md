# Design

## Context

See proposal.md for motivation. Today `MachineDockBlockEntity` extends `InboundStationBlockEntity`, adds a feed buffer pushed into `facingPos()` every `dock_feed_interval` ticks, implements `DeliveryTarget`, and reports `producer()` from the faced block's `PRODUCTION` capability. `WarehouseNetwork.producers()` iterates `stations` and picks docks; `OrderBook.Host.feed` delivers via `planAndStart(dockPos, ...)`; `drain` credits `orders.collected(pos, key, count)` for docks only. The Utility Duct already has per side modes cycled by the wrench on the clicked face.

## Goals / Non-Goals

**Goals:**
- One direction per block. Shared side mode handling for both stations.
- Keep the delivery, order and board code paths; only the key changes from dock to outbound station.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision
Section 2.4 and the M7 item are reworded to the station pair.

### Side modes
`SideModes` in `io.github.dirnei.mc.servus.storage.station`: an `EnumMap<Direction, Mode>` with `Mode {OFF, ACCEPT, PUSH, EXTRACT}`, NBT as six bytes, `cycle(Direction, List<Mode> order)`. Inbound uses the order accept, off with every side accept by default. Outbound uses push, extract, off with the front push and the rest off. The capability provider for both stations returns a handler only for sides whose mode is not off; the inbound handler inserts only, the outbound handler extracts only. Sides are relative to the world, not the facing, so the GUI labels them Up, Down, North, South, West, East and marks the front.

### Carrot
`onWrench` on both station blocks: sneak dismantles with `saveToItem` so modes and keep slots travel; otherwise the clicked face's mode cycles, the block entity marks itself changed and syncs, and an action bar message names the side and mode. The stations no longer rotate; `getStateForPlacement` still sets the facing.

### Outbound Station
`OutboundStationBlockEntity extends StorageNodeBlockEntity implements MenuProvider, DeliveryTarget` in `io.github.dirnei.mc.servus.storage.outbound`: `buffer` (ItemStackHandler, `inboundBufferSlots`), `keep` (ItemStackHandler of ghost stacks whose count is the target, slot limit raised to 4096), `SideModes`, `pushTimer`, `shortage` (first unfilled keep key plus reason, for the GUI and lamp). `serverTick` pushes the buffer into every push side's neighbour every `push_interval` ticks. `deliveries()` returns the buffer. `producer()` reads the `PRODUCTION` capability of the block in front. `kind()` is `STATION`, so the network drops candidate sources around it and lists it under `stations`; `acceptsRail` stays true on every side. The activity property is set by the network like for inbound stations: green when no shortage and nothing on its way, orange with deliveries pending, red with a shortage.

### Restocking
`WarehouseNetwork.tickOutbound()` every `outbound_check_interval` ticks over `stations` that are outbound: for each keep slot, `have = buffer count + heldFor(station, key) + fleet.ridingTo(station, key) + fleet.queuedTo(station, key)`; when `have < target`, `planAndStart(station, key, target - have, 0)`; the refusal is stored as the slot's shortage. Pending counts reuse the terminal's held loads and the fleet's per destination queries, which get a key filter.

### Orders
`Producer.dock` becomes `station` and `producers()` iterates outbound stations. `OrderStep.dock` becomes `station`. `OrderBook.collected(machine, key, count)` is called from `drain` for every inbound station with the station's position; the book credits the oldest feeding or producing step whose recipe outputs the key and whose machine (station facing position) touches the collecting inbound station, falling back to the oldest such step anywhere. Step states are renamed `WAITING_STATION`, `DELIVERING`, `PRODUCING`, `DONE`; the board and commands follow.

### GUI
`OutboundStationMenu`: keep row at the top as ghost slots (click sets the carried item with its count, right click clears, scroll adjusts by one, shift scroll by a stack, through a `KeepSlotPayload(slot, item, amount)`), buffer row below, six side buttons sending `CycleSidePayload(direction)`, state line from a `StationStatePayload` extension that carries the shortage. The inbound GUI gains the six side buttons and uses the same payload. Side buttons show the mode as text and are tinted: off grey, accept and push green, extract orange.

### Removal
Delete the `dock` package, `MachineDockMenu`, `MachineDockScreen`, registrations, datagen lines, textures and the machine dock GUI texture. GameTests `dockFeedsTheBlockItFaces`, `dockCollectsAndPutsAway`, `dockFacingAChestIsNoProducer`, `feedDockFromCrate`, `feedDockByShuttle`, `dockFrontTakesNoRail` and `dockServesCraftingStation` are replaced by outbound tests; the order, rule and board tests use an outbound station in front of the Crafting Station and an inbound station beside it.

### Minecraft side verification
GameTests: `outboundKeepsBufferStocked`, `outboundToppedUpAfterExtraction`, `outboundPushesIntoFront`, `outboundSideModes` (extract side hopper, off side, insert refused, carrot cycle), `outboundReportsProducer`, `outboundFacingChestIsNoProducer`, `feedOutboundFromCrate`, `feedOutboundByShuttle`, `inboundSideOff`, `outboundServesCraftingStation`, plus the rewritten order tests.

## Risks / Trade-offs

- [Step inputs delivered on top of keep amounts never leave if the machine refuses them] → they stay visible in the buffer; the step shows producing and never completes, which the board shows. Same behaviour as the dock had.
- [Outputs credited to the wrong step when two machines make the same item] → prefer the inbound station touching the step's machine; identical outputs from two machines on one inbound station are counted in order, which still finishes both steps once both produced.
- [A keep slot and a step both want the same item] → both go through the delivery path; the reservation of the order covers the step, the keep slot takes what is left.

## Open Questions

None.

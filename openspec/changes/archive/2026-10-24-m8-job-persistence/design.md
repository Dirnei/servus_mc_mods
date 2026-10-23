## Context

`ShuttleFleet` lives on `WarehouseNetwork`, which Foundation rebuilds from block entities joining and leaving (`NetworkBlockEntity.onLoad`, `setRemoved`, `onChunkUnloaded`, and `NetworkSet` hand overs on splits and link refreshes). Today every leave of a garage calls `releaseGarage`, which drops cargo at the garage and queues jobs again; the queue, held loads (`WarehouseNetwork.held`) and `InboundStationBlockEntity.claimed[]` are memory only. Requesters already save their side: Packing Station shipments, terminal requests and CPU orders are in their block entities, and a `PickJob` names its destination and request id, so a restored pick credits them as before.

This is the second change of the shuttle job series (decided 2026-10-23). `m8-slot-locks` will key slot locks by job id and release locks whose job is gone; this change gives it the ids, `ShuttleFleet.knows(id)` and the orphan sweep.

## Goals / Non-Goals

**Goals:**
- Stable job ids, saved queue, saved shuttles with cargo and runs, saved held loads, saved claims.
- Restore after the network formed again, whatever order chunks and block entities load in.
- No item loss on chunk unload, splits or link refreshes.
- Old saves load as an empty fleet.

**Non-Goals:**
- Saving lift car queues, a stop's remaining ticks, a gantry spout's pumping state or speed: they restart.
- Resuming a field run mid plot list: the run decides again from where the shuttle stands.
- The garage error state for stranded cargo (`m8-slot-locks`).

## Decisions

- **Job ids.** `ShuttleJob.id()`; every job record gets `int id` as its first component. Ids come from a world counter in the overworld's data storage. The counter SavedData of `m8-shuttle-numbers` becomes a generic `CounterData` with one SavedData type per kind (`shuttle_numbers` keeps its name and `Last` key, `job_ids` is new), both wrapping the pure `ShuttleCounter`. `WarehouseNetwork.nextJobId()` takes the next id. `int` is enough: two billion jobs.
- **State lives with the node it belongs to.** `StorageNodeBlockEntity` keeps a `Warehouse` compound tag: written from the live network on save (`WarehouseNetwork.saveState(node)`), and while the node is not in a network the stashed tag is written back unchanged. Garage: its out shuttles by slot. Controller: the queue. Packing and Outbound Station: the loads held for them. Inbound Station claims are its own `Claims` int array (job id per slot, 0 free), since the station owns the slots. Alternative rejected: one SavedData per dimension keyed by controller; networks have no stable identity and partial site loads would need merging and deduplicating snapshots.
- **Leave for good or leave to sleep.** `StorageNodeBlockEntity.setRemoved` marks the node as leaving for good before `NetworkBlockEntity` leaves the network; a chunk unload leaves first through `onChunkUnloaded`, and splits and refreshes never call `setRemoved`. In `onNodeRemoved` a node that leaves for good keeps today's rules (garage cargo drops, jobs queued again) and held loads now drop at the destination instead of vanishing. Any other leave hands the node's state to its block entity (`stashWarehouseState`) and the fleet forgets it without drops: `releaseGarage(pos, false)`, the queue is cleared when the controller leaves. `onNodeAdded` takes the stash back (`takeWarehouseState`). A test that swaps a block entity calls `onChunkUnloaded` on the old one first, as the game does.
- **Saved before unload.** `ChunkMap.scheduleUnload` saves the chunk before `level.unload` calls `onChunkUnloaded`, and only when the chunk is unsaved. The fleet calls `Host.stateChanged(pos)` (`Level.blockEntityChanged`, marks the chunk unsaved) every tick for a garage with shuttles out and for the controller when the queue changed; the network does so for a destination whose held loads changed and the station on claim changes (`setChanged`).
- **Save format.** `FleetSaves` (package `shuttle`) writes compound tags with `ValueIo`: a job as `Type` plus its fields (item keys as one item stacks, fluid keys as `FluidStack.OPTIONAL_CODEC`, positions as `BlockPos.CODEC`); a shuttle as `Slot`, `Leg`, `Path` (long array of `BlockPos.asLong`), `Segment`, `Progress`, `Loads` (job, `Cargo`, `Fluid`, `FluidTarget`, `Tried`) and `Run` (`Kind` harvest, spread or supply, `Head`, `Cargo` as stacks, `Store`, `Tally` for a harvest, `Fertilizer`, `Doses`, `Fetched` for a spread, `Fetched` and `Unloaded` for a supply). Unknown types and broken entries are skipped; an entry that is skipped never takes items with it, because only cargo of a readable entry is restored. Only shuttles that are out are saved.
- **Restore.** `ShuttleFleet.restoreGarage` runs after `adoptGarage` and rebuilds the out shuttles of slots that hold a shuttle item. A restored shuttle starts with a `Settle` countdown (pure, 20 ticks) that counts only while every chunk of the rest of its path is loaded and starts over when one is not. Then: a field run is parked on its current node and resumes through `FieldRuns.resume` (stored cargo first, then its run's turn, which ends the run at the garage when the node is no rail any more); any other shuttle drives on when every remaining node is a travel node, else it is routed again from its current node to its leg's goal (source, terminal, target, station or the fluid target), else home, else recalled (`recallShuttle`: cargo pops at the garage like `dropCargo`). Transient state (lift binding, stop, handover, pumping, speed, payment) starts fresh: a riding shuttle stands at the start of its ride segment, so the gate calls the car to that floor again; a stopping shuttle arrives again and stops again. `restoreQueue` appends saved jobs whose id the fleet does not know. Job assignment waits 20 ticks after any restore.
- **Edge of loaded chunks.** `tickShuttle` holds a shuttle whose current node or next node lies in a chunk that is not loaded (`Level.isLoaded`); arrival and field work happen only in loaded chunks. `deliverTo` keeps the stack while the destination's chunk is not loaded. `refreshHome` keeps a lost home while the old home's chunk is not loaded instead of recalling.
- **Claims.** `InboundStationBlockEntity.claim(slot, jobId)`, `claimedBy(slot)`; saved as `Claims`. `WarehouseNetwork` sweeps every 100 ticks while online and while every chunk of the site span is loaded (like `snapshotStock`): `OrphanSweep` (pure) releases a claim whose job id `ShuttleFleet.knows` denies at two sweeps in a row. A legacy tag without `Claims` loads with every slot free.
- **Held loads.** Saved per destination as a list of `Job` plus `Stack`; restored when the destination joins.

## Risks / Trade-offs

- A put-away whose garage stays unloaded while the station and controller are loaded keeps its claim until the site span is fully loaded; a site loaded only in part does not sweep, so the claim waits rather than doubling the job.
- If the garage of a shuttle on its way to a station stays unloaded while the whole span is loaded (not possible, the span contains the garage) the sweep could plan the stack twice; the second shuttle would find nothing and go home empty. No item is lost.
- A restore after a long time may find the world changed: targets gone or full. Those go through the existing re-route and return rules; stranded cargo drops at the garage until `m8-slot-locks` parks it with an error state.
- Marking garage chunks unsaved every tick while shuttles are out means they are written on every autosave. That is the price of being right at unload.
- Chunks are saved one by one during an autosave. A crash between the save of a garage and the save of the controller can bring a finished job back or lose a queued one; a clean stop saves every chunk in one go.
- A node refresh (link change) now hands shuttles over instead of dropping their cargo, and they wait the settle second before moving on.

## Assumptions

- Dropping stranded cargo at the garage (as `dropCargo` does today) is acceptable until `m8-slot-locks` adds the parked error state.
- The fleet's queue goes to sleep with the controller: when the controller's chunk unloads while garages stay loaded, shuttles already out finish their legs, nothing new is assigned (as today, without a controller nothing is).
- One second (20 ticks) of settle is enough for block entities of freshly loaded chunks to join; a site woken by a wake loads all its chunks at once.
- Restored field runs lose their current plot list and visited rails; they decide again, which may revisit a rail.

## Open Questions

- None for this change. `m8-slot-locks` decides how locks join the saved state (likely next to claims, keyed by job id).

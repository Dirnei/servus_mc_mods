## Why

Nothing of the shuttle fleet is saved. On a restart or when a garage's chunk unloads, cargo riding on shuttles, queued jobs and deliveries held for a full Packing Station vanish without a trace, and Inbound Station claims reset so stacks get planned twice. The next change of the shuttle job series (`m8-slot-locks`) keys slot locks by job, so jobs also need a stable id that survives a reload.

## What Changes

- Every shuttle job gets an id, counted up per world and never given twice. Jobs keep it while queued, on a shuttle and after a reload.
- Shuttles out on a job are saved with their garage: leg, path and position on it, every load with its cargo items or fluid, and a field, fertilize or supply run with its cargo. After a reload a driving shuttle drives on along its path; a field shuttle goes on with its run from where it stands.
- The job queue is saved with the Warehouse Controller; deliveries held for a full destination are saved with that destination. Both come back when the block loads again.
- Inbound Station claims are saved with the job id that holds them. A claim whose job is gone is released once the whole site is loaded.
- A shuttle whose way or target is gone after a reload drives home or, without a way home, is put back into its garage with its cargo dropped at the garage, like a recall today. The parked error state comes with `m8-slot-locks`.
- Chunk unloading no longer loses anything: a shuttle never drives into or arrives in a chunk that is not loaded, it waits at the edge; a garage whose chunk unloads takes its shuttles to sleep with it; a home rail whose chunk unloads does not recall the shuttles. Held deliveries to a destination that is broken drop there instead of vanishing.
- Old saves (Shuttles 0.3.0) load without errors: no saved state means an empty fleet and free station slots.

## Capabilities

### New Capabilities
- `shuttles/fleet-persistence`: job ids, what of the fleet is saved where, how it is restored after a reload or chunk load, what happens when a restore cannot go on, and how shuttles behave at the edge of loaded chunks.

### Modified Capabilities
- `shuttles/shuttle-garage`: "Home rail per garage" no longer recalls shuttles when the home rail only unloads with its chunk.

## Impact

- Shuttles mod: job records gain an `id` component; `ShuttleFleet` saves and restores garages and the queue; a save helper for jobs, loads and runs; `StorageNodeBlockEntity` keeps a node's warehouse state across leave and join; `WarehouseNetwork` hands state to and from nodes, saves held loads and sweeps orphan claims; `InboundStationBlockEntity` claims hold job ids; a world counter for job ids next to the shuttle numbers.
- Pure logic: motion restore, settle countdown and orphan sweep, covered by JUnit.
- GameTests for reloads of a garage mid trip, the controller with a queue, a station with claims, held deliveries and old tags.
- No network payload or datapack change.

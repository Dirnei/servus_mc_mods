## Why

Items spilled at an Inbound Station with a chest and a backpack on Pull sides. Put-away planning reserves no room in the rack, so a shuttle can arrive at a rack that filled up meanwhile; what does not fit rides back to the station, whose Pull sides have refilled the buffer, and the rest pops on the ground. Picks have the same blind spot: they never count other picks on their way, and a pick whose destination is full waits on the rail forever. Jobs promise nothing, so items get lost or stuck.

## What Changes

- **Slot locks**: every put-away and pick locks the exact slots it will fill (IN) or empty (OUT) when it is planned, on Heavy Rack bays, the Inbound Station buffer, the Outbound Station buffer and both Packing Station buffers. A lock names its direction, item, amount and job. A locked slot is closed to everyone but its job: hands in the GUI, shift click, hoppers, pipes, the stations' own Pull and Push sides, wireless arrivals and other mods. Locks are released when the job loads or unloads there or ends, are saved with the block, and a lock whose job is gone is released once the site is loaded. Inbound Station claims become OUT locks.
- **Planning reserves**: a put-away locks room in its target bay (a top up of a partial stack and free slots in the same bay) and the station slot it takes from; a pick locks the bay slots it takes from and a free slot of its destination. Stock counts for planning leave out OUT locked items, room leaves out locked slots. Outbound restock asks only for what its free slots take. A pick is sent only once its destination slot is locked, so a delivery always fits; the endless wait beside a full destination is gone.
- **Put-away target rule**: zone tiers (exact, tag, general) as before; within a tier first the bays already holding the item (a partial stack of it first, then the cheapest), then every other bay with room by most free slots in buckets (`free_slot_bucket` in `warehouse.json`, quarters by default), then the cheapest. Join order breaks exact ties only. Picks take from the cheapest bay holding the item.
- **Travel time cost**: rail paths become a weighted cheapest path search. Static weights per rail block, corner, lift block, lift handover and stop come from `warehouse.json`; the wait at a lift adds the car's distance to the floor and the shuttles already queued at or planned through that shaft. Shuttles drive the cheapest path, the free shuttle with the cheapest way takes a job, put-away targets and pick sources are ranked by it.
- **Leftovers never go back or down**: what a target does not take on arrival is routed again by the same rule with new locks. With nowhere to go the shuttle drives into its garage and parks there with the cargo: the garage lamp turns red, the garage GUI names the shuttle, the item and the reason, the shuttle takes no other job and tries again when room may have appeared and every few seconds. The cargo is saved and drops only when the garage is broken. A pick whose destination is gone stores its cargo the same way. A restored shuttle that cannot go on parks its cargo the same way instead of dropping it.
- **GUI**: locked slots show a colour wash (IN and OUT colours), an empty IN slot shows the incoming item faded, and the tooltip names the shuttle and the job ("Reserved: Shuttle #12 brings 64 Cobblestone"). Rack section, Inbound, Outbound and Packing Station menus sync their locks.
- **Fixes**: a garage's home rail compared by identity; an Inbound Station Pull side that could lose items when the buffer took less than expected; a wireless transfer that could lose items it put back into the To network buffer.

## Capabilities

### New Capabilities
- `shuttles/slot-locks`: what a slot lock is, which blocks have them, when jobs place and release them, how they close a slot to every other access, how they survive reloads and orphans, and how the GUI shows them.
- `shuttles/travel-cost`: the travel time cost of a rail path, its static weights and lift wait, and where it decides (shuttle paths, dispatch, put-away targets, pick sources, Packing Station choice).

### Modified Capabilities
- `shuttles/zones`: "Put-away routing" gains the holding, partial, free slot bucket and cost order and counts only unlocked room.
- `shuttles/inbound-station`: "Put-away by shuttle" locks the stack and the target room, leftovers are routed again or parked, never returned; "Station block and buffer" Pull sides respect locks and never lose items.
- `shuttles/outbound-station`: "Buffer is restocked by shuttle" asks only for what free slots take.
- `shuttles/shuttles`: "Shuttles move on the rail graph" drives the cheapest path; "Pick jobs" plan from unlocked stock of the cheapest bays and lock source and destination; "Delivery" always fits and stores the cargo when the destination is gone.
- `shuttles/shuttle-garage`: "Home rail per garage" picks the shuttle with the cheapest way; new requirement for shuttles parked with cargo.
- `shuttles/packing-station`: "Shortest shuttle trip" becomes the cheapest trip.
- `shuttles/fleet-persistence` (from `m8-job-persistence`): station claims become slot locks; a restore that cannot go on parks the cargo in the garage instead of dropping it.

## Impact

- Shuttles mod: new `lock` package (lockable slots on the NeoForge transfer API, lock planning, menu lock sync), Heavy Rack, Inbound, Outbound and Packing Station inventories become lockable; `WarehouseNetwork` plans with locks and the new target rule; `ShuttleFleet` routes with the travel cost, re-routes leftovers and parks stranded shuttles; garage block gets a red lamp state and a GUI line; four screens draw locks.
- Pure logic with JUnit: slot fill planning, put-away ranking, weighted path search with lift wait, free slot buckets.
- Data: `warehouse.json` gains `travel_costs` and `free_slot_bucket`. Lang: lock tooltips and garage lines. New payload `SlotLocksPayload` and `GarageStrandedPayload`.
- Saves: locks saved with the blocks; worlds from Shuttles 0.3.0 load with every slot unlocked.

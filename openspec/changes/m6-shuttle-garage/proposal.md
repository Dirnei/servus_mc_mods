# Proposal

## Why

Shuttles are a throughput layer bolted onto the controller: you add them to the controller block, they park on its rail, and every pull quietly falls back to instant when no shuttle fits, so a warehouse full of racks never needs them. The play test of the dispatch station (2026-10-07) made the user want the opposite: shuttles should be the thing that makes a rack warehouse work, with a dedicated garage block that scales by building more garages, and a counter where you order items and wait for the cart to bring them. This change adds that to the M6 "Storage automation" milestone as the item "Shuttle Garage, rack picks by shuttle, terminal requests" (new line in `PLAN.md`).

## What Changes

- **Shuttle Garage block.** A new warehouse node that holds up to `garage_capacity` shuttles (example 4, from `warehouse.json`). Right click it with a Shuttle item to add one, sneak right click with an empty hand to take one back, break it and they drop. Shuttles park on the rail touching their garage and return there after every job. A warehouse gets more shuttles by building more garages. The Warehouse Controller no longer takes shuttles.
- **Racks are served only by shuttles.** A Heavy Duty Rack is picked from and filled only by a shuttle driving to its front rail. No instant extract or insert touches a rack. Crates and foreign inventories keep working instantly when no shuttle can reach them, and ride shuttles when one can. A rack no shuttle can reach is still indexed but cannot be served, and the GUIs say so.
- **Terminal requests.** Clicking an item at the Counter Terminal opens an amount prompt with +1, +10 and +stack buttons; shift click requests one stack at once. A request is split into pick jobs of up to a stack each, served by shuttles for rail reached stock and instantly for the rest. Deliveries land in new output slots at the terminal, never on the cursor or in the player's inventory. Open requests are listed in the GUI with their progress and can be cancelled. When the output slots are full a shuttle waits beside the terminal until there is room.
- **Terminal pushes skip racks.** Stacks handed in at the counter go instantly into crates and foreign inventories by the zone routing; racks are filled through a Dispatch Station and its shuttles.
- **Dispatch Station and racks.** A station stack whose only room is in racks waits for a shuttle instead of moving instantly, with the reason "needs a shuttle" when none can reach them.

## Capabilities

### New Capabilities
- `storage/shuttle-garage`: the Shuttle Garage block, its shuttle count and capacity, the home rail per garage, adding, taking and dropping shuttles, linking, lamp and crafting.

### Modified Capabilities
- `storage/shuttles`: shuttles live in garages instead of the controller; each shuttle parks at and returns to its garage's rail; pick jobs come from terminal requests and may serve several garages; delivery goes into terminal output slots and waits when they are full; the fleet is drawn parked at its garages.
- `storage/counter-terminal`: the pull to cursor or inventory is removed; requests with an amount prompt, output slots and an open requests list are added; pushes skip racks; offline refuses requests.
- `storage/warehouse-controller`: the controller no longer holds the fleet; instant stock operations skip racks; the summary counts garages.
- `storage/heavy-duty-rack`: a rack is served only by shuttles.
- `storage/dispatch-station`: put-away never moves a stack into a rack instantly and reports "needs a shuttle".

## Non-goals

Deferred to later changes:
- Pick rate stats screen (next M6 item).
- Persisting open requests and jobs in flight across a reload. A reload clears them; stock stays where it is.
- Shuttle traffic, collisions or reservations of target room.
- Hopper or duct access to the terminal output slots. Items are collected at the counter.
- Delivering requests anywhere but the terminal they were made at (wireless and docks are M8).
- Recipes or research gating for garages beyond vanilla materials.

## Impact

- `storage` module only. No Lib API changes, no Manufacturing or Logistics changes.
- New block `shuttle_garage` with block entity, datagen and a `GARAGE` node kind in `WarehouseConnectionRule`; `ShuttleFleet` keyed by garage with per shuttle home rails and a shared queue; a pure `RequestPlan` that splits a request into instant and shuttle jobs; `WarehouseNetwork` instant paths skip racks; `CounterTerminalBlockEntity` gains an output buffer and an open request list; `CounterTerminalMenu` and `CounterTerminalScreen` gain the amount prompt, request list and output slots; new payloads for request and cancel; `WarehouseControllerBlock` and its block entity lose the shuttle handling.
- Datapack: `warehouse.json` loses `max_shuttles` and gains `garage_capacity` (example 4) and `terminal_output_slots` (example 9).
- Specs change as listed. `PLAN.md` section 2.3 is reworded: shuttles live in garages, racks need shuttles, crates stay instant, requests are collected at the counter; the M6 list gains the item.

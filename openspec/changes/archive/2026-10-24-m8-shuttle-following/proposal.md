## Why

Shuttles drive through each other today: two shuttles on one aisle overlap, a shuttle loading at a bay is passed by the one behind it, and two shuttles leaving one garage start inside each other. With slot locks and travel cost in place (`m8-slot-locks`), the warehouse plans well but its traffic still looks broken, and the planner cannot see a busy aisle. This is the fourth change of the shuttle job series decided on 2026-10-23.

## What Changes

- **Following**: shuttles with the same heading on the same rail keep a gap (`follow_gap` in `warehouse.json`, example one block from centre to centre) and brake to the speed of the shuttle ahead; they never overlap. Shuttles with opposite headings pass through each other. A standing shuttle (stopping at a bay, station or gantry, working a plot, waiting for the lift car or for power) holds every shuttle behind it whose heading matches.
- **Merging**: two shuttles that enter one rail from different rails with the same heading afterwards go in the order they get there; on a tie the lower shuttle number goes first.
- **Rings**: a closed ring of shuttles that all wait for the one ahead moves together at the speed of its slowest member instead of standing forever. A shuttle that leaves the lift car never waits for a queue that ends at a shuttle waiting for that same car.
- **Garage**: a shuttle given a job waits inside its garage until the home rail is clear, and only then appears on the rail; parked and waiting shuttles are never drawn on the home rail.
- **Aisle traffic in the travel cost**: a step costs more for every shuttle on it with the same heading (`traffic_per_shuttle` in `travel_costs`, example 4) and for the stop ticks left of a shuttle standing there, read when the way is planned, so shuttles take a free aisle over a busy one.
- **Clients**: a shuttle that brakes, stops or starts again behind another is synced at once, and clients draw it braking for the shuttle ahead between syncs, so it does not run into the one ahead and jump back.
- **Fixes**: standing shuttles were drawn shifted along the world x axis by a quarter block per standing shuttle (left from the time idle shuttles stood on the home rail), which made shuttles jump sideways when another one stopped or started; that shift is gone. Field GameTests that waited for replanted crops of age 0 failed when a random tick grew one in time; they now accept any crop that is not ripe.

## Capabilities

### New Capabilities
- `shuttles/shuttle-traffic`: how shuttles share rails: following with a gap, passing on opposite headings, standing shuttles holding the ones behind, merge order, rings, leaving the garage, the lift exit, and how clients draw it.

### Modified Capabilities
- `shuttles/travel-cost` (new in `m8-slot-locks`, archived before this change): adds a requirement that aisle traffic costs time.

## Impact

- `shuttles`: `shuttle/ShuttleFleet` (traffic snapshot per tick, drive limit, ring speed, garage departure, sync, traffic cost snapshot), `shuttle/Shuttle` (heading, departing), new pure `shuttle/ShuttleTraffic` with JUnit tests, `shuttle/TravelWeights` (traffic per step), `shuttle/ShuttleDrive` (glide behind a moving point), `shuttle/ShuttleView` (lead), `client/ShuttleGarageRenderer` (lead, no parked shift), `stats/ShuttlesStatsManager` and `warehouse.json` (`follow_gap`, `traffic_per_shuttle`).
- GameTests: new `ShuttleTrafficGameTests`; fixed tick checkpoints of existing tests with several shuttles may move later; field tests accept grown crops.
- No save format change, no new payload type (the existing `ShuttleSyncPayload` view gains a field).

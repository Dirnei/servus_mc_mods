## Context

See proposal.md for the why. State after `m8-slot-locks` (applied, not archived): `ShuttleFleet.tick` moves every shuttle of every garage once per tick through `tickShuttle`; a shuttle's position is a `ShuttleMotion` (node path, segment, progress), its speed comes from the pure `ShuttleDrive` (accelerate, brake for the end of the path and for a vertical step, corner limit), and `ShuttleMotion.advance` asks a gate (`pay` and `ShuttleLifts.mayStart`) before every segment. Nothing looks at other shuttles. Paths come from `ShuttleFleet.path` over the weighted Dijkstra `RailCosts` with `TravelWeights` and a lift load snapshot per search (`liftLoads`). Clients get a `ShuttleView` per out shuttle every 5 ticks while anything moves (`ShuttleSyncPayload` per garage) and extrapolate with `ShuttleDrive.glide`.

Idle shuttles have a one node motion parked on the home rail and are not drawn; stranded ones (leg `STRANDED`, arrived at home) are not drawn either. A shuttle given a job starts its path on the home rail and is drawn there at once.

This is the fourth change of the shuttle job series. Shuttle numbers come from `m8-shuttle-numbers` (`ShuttleItem.number`).

## Goals / Non-Goals

**Goals:**
- No overlap of shuttles that share a lane, no deadlock among followers.
- One traffic snapshot per tick: one node to shuttles map, pure logic, JUnit tested.
- Clients draw followers in step with the server.
- The planner sees busy aisles.

**Non-Goals:**
- Lanes or passing places: opposite headings simply pass through each other, as decided.
- Avoiding every deadlock in any rail layout: following, merges and rings of followers are covered; a cycle that runs through a lift car is broken at the car exit; cycles through other gates (power, decks) are not expected and wait.
- Saving traffic state: it is rebuilt each tick from positions.

## Decisions

### Headings and lanes
- A direction is the step between two path nodes (x, y and z signs packed into one int). A shuttle's heading at a path node is the step into it (`in`) and the step out of it (`out`); a shuttle at the start of its path takes `in` from its last driven step (`Shuttle.lastHeading`, kept after every advance), and one at the end has no `out`.
- Two shuttles share a lane at a node when one of `{in, out}` of the first equals one of `{in, out}` of the second. Opposite headings and plain crossings share nothing and pass. Comparing both directions catches rear ends at a turn and merges onto a lane in one rule, for moving and standing shuttles alike. Alternative rejected: comparing only the out step, which lets a shuttle turning at a bay drive into the shuttle stopping there.

### Pure `ShuttleTraffic`
- Input: one `Car` per out shuttle: id, order key, path, segment, progress, last heading, in garage, obstacle (false for a shuttle riding the car or about to ride), speed, accel. Output per car: the room in blocks it may still drive before it is the follow gap behind its nearest leader, and that leader with its speed.
- Snapshot: every obstacle car adds entries for the nodes from the one it is on (distance minus its progress) to the window ahead (`node -> List<Entry(car, distance, in, out)>`, one map per tick). Every car then walks its own path over the same window and checks the entries at each node.
- Priority at a shared lane: the car with the shorter distance to that node goes first; on equal distance the lower order key. Order key: the shuttle number, or `2^32 + Shuttle.id` for a shuttle without one (stable for the session). A leader `O` met at node `m` limits follower `F` to `room = dF(m) - dO(m) - gap`; the smallest room over all entries wins. A car on the same edge ahead is found at the edge's end node with the same formula, so following and merging are one rule.
- Window: `max braking distance + top speed + gap + 1` blocks over the fleet's shuttles, at most 32. Both the entries of a car and a follower's own walk end at the node where the next step goes up or down: the lift car decides who rides first, and comparing distances through a shaft let a shuttle that had called the car wait for one that had not, while the car waited for the first (found by `ElevatorGameTests.shuttlesQueueForCar`).
- Rings: the fleet gives the pure `rings(leaderOf)` the leaders that bind (follow room makes the next speed lower than the free one); every cycle is a ring. `chainReaches(leaderOf, start, test)` walks a leader chain for the lift rule.
- Aisle loads for the cost: `stepLoads(cars, perShuttle, stopTicks)`: a car between two nodes loads that step with `perShuttle`; a car standing on a node loads the step into it with `perShuttle` plus its stop ticks left. Cars in their garage and riding cars load nothing.

### Fleet wiring
- `tick()` builds the snapshot once before the shuttles move (`trafficTick`), from every shuttle that is out or departing, then works out binding leaders, rings and the lift exception, and keeps per shuttle the room (`Shuttle.room`), the leader and its speed, and whether it is in a ring.
- `drive()`: the stop distance becomes `min(toStop, room)`. A ring member drives at the ring speed instead: the lowest free next speed of its members. A shuttle whose room is below 1e-4 does not advance at all this tick (no gate is asked, nothing is paid), so it never creeps into the gap with the minimum step `drive` returns for gates.
- Standing shuttles (arrived, in a stop, field work, handover, restoring, held at a gate the last tick) are obstacles with speed 0 and are never followers that tick.
- Riding the car or standing in the car before a ride (next step vertical): not an obstacle and not limited; the car serializes them. After the ride the shuttle on the car is limited like any other, unless its leader chain reaches a shuttle that calls the same car (`lift` equal, not inside, not crossing): then it ignores the room and drives off, freeing the car.
- Merges need no extra code: first come is the distance rule.

### Garage
- `Shuttle.inGarage`: true when a shuttle is created in its garage, parks (`park`, `recallShuttle`, a stranded park or a stranded arrival at home), false for restored shuttles and as soon as a motion leaves its first node. A shuttle counts as in the garage only while it is still at segment 0, progress 0.
- An in garage car is no obstacle for cars out on the rails and is not drawn (`views`). To another in garage car it is a car standing on its home rail: the nearer one to a shared node goes first, at the same home rail the lower order key, so two shuttles of one garage leave one after the other and a shuttle never leaves into the home rail of a garage ahead whose shuttle leaves too. Garages around a full loop all waiting for each other form a ring and leave together.
- An in garage car yields to an out car only when that car is moving and could not stop before it (`dO - dF - gap < speed^2 / (2 accel) + speed`), or when that car stands in the lane on or ahead of the home rail. Otherwise it leaves and the out car brakes for it. Alternative rejected: always yielding, which would keep a shuttle in its garage while another stands for a long stop a few blocks behind the home rail.

### Client sync and drawing
- `ShuttleView` gains `Optional<Lead> lead` (`at`: the path position, segment plus progress, the shuttle must stay behind at the sync; `speed`: how fast that point moves, the leader's speed). `ShuttleDrive.glide` gets an overload that limits the stop distance each simulated tick by `lead.at + lead.speed * t - position`, the same rule the server applies with the leader's real position. A follower behind a leader at constant speed is drawn exactly; any change of the leader is caught by the sync below.
- Sync at once (`syncPending`) when a shuttle's leader changes, when its room starts or stops binding, or when its leader starts or stops standing. The 5 tick sync stays for everything else.
- The parked shift is removed from `ShuttleGarageRenderer`: every standing view after the first one was drawn `0.2` blocks further along world x. Standing shuttles were idle ones on the home rail when it was added (m6); idle shuttles are no longer drawn and standing out shuttles now keep their gap, so the shift only moved shuttles sideways (on rails along z) or along the rail when another shuttle stopped or started, which explains the reported jump.

### Aisle traffic in the cost
- `travel_costs.traffic_per_shuttle` (example 4 ticks) and `follow_gap` (example 1.0) in `warehouse.json`, read by `ShuttlesStatsManager.trafficPerShuttle()` and `followGap()`. `TravelWeights` stays as it is; the fleet adds the step load from a `ShuttleTraffic.stepLoads` snapshot per search (taken next to `liftLoads`), leaving out the shuttle the path is planned for. Network choices (`costsFrom` without a shuttle) see every shuttle.

### Flaky field GameTests (found before this change)
- `harvesterHarvestsIntoStorage`, `harvesterTillsInsideStripOnly`, `produceIsLearned` and `harvestResumesBelowTheLimit` waited for every replanted crop to be exactly age 0. Random ticks grow wheat on moist farmland (per block about 3 / 4096 per tick times a growth chance near 1 in 5), so with four plots and 100 to 150 ticks between replant and the check about one run in fourteen lost a crop to age 1 and the wait ran into its 550 tick timeout. Reproduced with `--tests ... --repeatCount 40` (one failure, "rack holds 4 wheat, harvester out 0": the work was done, only the age check failed). The checks now accept wheat that is not ripe, which is what replanted means here because the plots start ripe; the failure messages print the ages. 100 repeats then passed.
- The shuttles seen standing "at unloaded chunks" for thousands of ticks belonged to earlier GameTest batches: the runner unforces a batch's chunks when the batch ends, networks tick from the level tick, and a shuttle of a finished test whose next node lies in an unloaded chunk waits there as `m8-job-persistence` wants. They do not touch later tests (separate networks, at least five blocks between structures). Not a bug.

### GameTest hooks
- `WarehouseNetwork.sendShuttleAlong(garage, slot, path)` sends an idle shuttle along given rails without a job (it parks when it arrives) and `plannedWay(from, to)` returns the cheapest way with traffic, so `ShuttleTrafficGameTests` can set up following, merging and rings exactly; jobs alone cannot place shuttles that precisely.

## Risks / Trade-offs

- [Several shuttles of one garage now leave one block apart instead of together] → fixed tick GameTests with several shuttles may need later checkpoints; assertions stay.
- [A lane check by direction only] → two shuttles on parallel rails never interact, which is right; two shuttles meeting head on at a dead end pass through each other, as decided.
- [Room computed from positions at the start of the tick] → a leader that moves this tick only widens the gap, so followers are conservative by at most one tick of the leader's travel.
- [A ring member held by a gate in the tick it starts a segment] → the member behind it moves the ring speed once more; at most one tick of travel (well under the gap minus a shuttle's length).
- [Cost snapshot per search] → one pass over the fleet per search, like the lift loads.

## Migration Plan

Nothing is saved. `follow_gap` and `traffic_per_shuttle` default when missing.

## Archive order

Archive after `m8-shuttle-numbers`, `m8-job-persistence` and `m8-slot-locks`: the travel cost delta adds a requirement to `shuttles/travel-cost`, which `m8-slot-locks` creates.

## Deviations found while applying

- The lookahead stops at the start of a lift ride (see Pure `ShuttleTraffic`).
- In garage cars of different garages wait for each other like standing cars (see Garage); the first version ignored them and let a fast shuttle start into a slow one leaving the next garage.
- A tick task must not register further tick tasks in GameTests (the scheduler crashes iterating its map), so the traffic GameTests register their watchers while they set up and switch them on later.
- Root `build.gradle` got a temporary `gtTests` / `gtRepeat` property to run single GameTests with `--tests` and `--repeatCount`; it is not committed.

## Assumptions

- The follow gap is measured from centre to centre along the rail; one block keeps shuttles of 0.8 scale apart and lets a queue stand on whole rails.
- A shuttle standing at the end of its path keeps the heading it arrived with, also during field work.
- A shuttle that does a zero length leg at its home rail (a bay docked on the home rail) stays in the garage for that stop.
- Shuttles of different warehouses never share rails, so traffic is per fleet.

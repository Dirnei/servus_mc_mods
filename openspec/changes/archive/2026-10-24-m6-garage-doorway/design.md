# Design

## Context

`ShuttleFleet` parks a shuttle with `ShuttleMotion.parked(garage.home)` on the home rail node and marks it `inGarage`; `views()` hides shuttles that are `insideGarage()` (in the garage, segment 0, progress 0) or idle and arrived. Paths come from `path(shuttle, from, goal)` over `ways(shuttle)` (`host::travelNeighbours`, rail and shaft nodes only; the garage is not a travel node). Syncs go out every `SYNC_INTERVAL` (5) ticks while anything moves, or at once when `syncPending` is set; a job's first movement does not set it, so the client sees a leaving shuttle 1 to 5 ticks late, already moving. `ShuttleGarageRenderer` draws every view by extrapolating with `ShuttleDrive.glide`. The garage model is two full cubes (wood board and the frame textures). Agreed look: `temp/garage-door.html`.

## Goals / Non-Goals

**Goals:**
- Shuttles park in the garage cell and drive through a real doorway.
- A roll-up shutter drawn from client state only.
- No pop-in and no visible turning.

**Non-Goals:**
- Any server state for the door.

## Decisions

- **Parked in the garage cell.** `ShuttleMotion.parked(garageNode)` where `garageNode` is the garage's own `NodePos`. Every place that parks or sends a shuttle home (`park`, `recallShuttle`, the restore and home-path code around `path(shuttle, at, home::equals)`) uses the garage node: home paths end at the garage node, job paths of a shuttle inside start there. One helper (`homeNode(garage)`, `homeGoal`) replaces the scattered `garage.home` uses so the rule lives in one place.
- **The doorway step is private.** `ways(shuttle)` adds the step garage node to home rail and back only for the shuttle's own garage; `host::travelNeighbours` stays as it is, so no other shuttle and no cost search for stations ever routes through a garage. `canReach` and dock costs keep starting at the home rail. The step costs like a rail block in `TravelWeights` and is drawn as a straight segment.
- **Door time as a wait.** A shuttle standing inside at the start of a path out of its garage node waits `door_ticks` (new `warehouse.json` stat, example 10) once per path, in its own counter (`Shuttle.door`, `doorFor`) like the lift handover pause rather than `shuttle.stop`, which would call `arrive` when it ends. The check sits in `tickShuttle`, so every way out (job, field run, stranded retry, restore) gets it. The view sends `Drive.STILL` while it waits and the client opens the shutter. The time counts toward trip time like any stop.
- **Instant sync on start and arrival.** `syncPending = true` when a shuttle leaves the garage node (first move after the door wait) and when it parks; `noteMove` sets it as it clears `inGarage`.
- **Views of shuttles inside.** `views()` sends a shuttle inside its garage once its door wait has started (so the client knows to open and draws it facing out) and drops it once parked; the client keeps drawing the last view of a shuttle that ended inside until the shutter is down (`ClientShuttles` keeps per garage the parked views with the tick they arrived; a view counts as parked when it is no longer sent, its path ended in the garage and it was drawn within 2.5 blocks of it). Heading inside is the heading of the doorway step it drove (in when coming home, out when leaving), so it never turns in sight.
- **Shutter state on the client.** `ShuttleGarageRenderer` keeps per garage an openness 0..1 (in `ClientShuttles.Fleet`) that moves toward 1 while any drawn shuttle wants it open and toward 0 otherwise, at a fixed speed: fully open in 8 ticks, less than `door_ticks` so the shuttle never meets a closed door. A shuttle on a path out wants it open until it is 1.5 blocks past the garage cell; one on a path in wants it open from 1.5 blocks plus the distance it covers at top speed in the 8 ticks until it stands inside (`GarageShutter`, pure). Eight slats of `electric_pump_steel` drawn just inside the opening, stacking up into the lintel. Nothing saved, nothing sent.
- **A shuttle pulling in parks first.** A shuttle on a path into its garage cell that is within the shutter's opening range (`GarageShutter.pullingIn`: 1.5 blocks plus what it drives at its top speed in the shutter's 8 ticks, the same numbers the client opens by) is left out of `freeShuttles`. It parks, is free on the next tick and leaves with the door time, so it never stops dead in front of an open shutter. The server uses the shuttle's own top speed even while the view sends it standing, so its range is never shorter than the client's. Shuttles further from home stay free and turn round on the rails.
- **Garage model.** Datagen elements as in the mock: front wall, side walls, roof, lintel and posts with the existing frame textures over wood board, an opening 2..14 by 0..12 on the back with the rail model running through; inner walls wood board. The block keeps its full block collision and outline, which covers the walls and keeps players out of the tunnel; it is `noOcclusion` so neighbours do not cull their faces against the open back. Faces of the frame and board layers must not share a plane: the frame layer is thin planes 0.02 px outside the board faces, the back plane cut round the opening (z-fighting list in the model task).
- **PLAN.md decisions settled:** none open; this fixes a play test finding.

## NeoForge APIs

- `BlockEntityRenderer` through the Foundation `LiveBlockEntityRenderer` already used by `ShuttleGarageRenderer`; models via the existing datagen block model builder; `VoxelShape` for the collision. No new payloads: `ShuttleSyncPayload` is sent more often.

## Testing

- JUnit: path helper tests for the private doorway step (pure parts of `ShuttleTraffic` / path code where they exist), `ShuttleDrive` unchanged.
- GameTests: `shuttleParksInsideItsGarage`, `shuttleWaitsTheDoorTimeBeforeLeaving`, `noShortcutThroughAGarage`, existing trip-time and pick GameTests updated for the extra step and the door time.
- Manual: watch a shuttle leave and come home from several angles, a garage with three shuttles taking jobs one after another, flicker check on the shutter slats against the lintel and posts.

## Risks / Trade-offs

- [Trip times grow by the doorway step and the door time] → Both small and from `warehouse.json`; GameTests with fixed times get the new numbers.
- [Saved shuttles parked on the home rail from before] → Only shuttles parked with cargo (stranded) are saved; on load a stranded shuttle that stands at the end of its path is moved to the garage node. One saved driving home to the old home rail parks inside when it arrives. No world needs fixing.
- [A fleet fill of a gantry and the restock check in the same tick] → The fleet's fill and drain mark the gantry for reading in the same tick, so the restock check never sees the level from before the fill (found by the restock GameTests once trip times changed).
- [Several shuttles leaving at once] → They leave one after another through the private step; traffic already queues shuttles on a shared step.

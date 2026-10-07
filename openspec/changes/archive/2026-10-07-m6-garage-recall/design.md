# Design

## Context

`ShuttleFleet.tick` returns after `serveInstantly()` when `hasShuttles()` is false, which requires a garage with a home; motions are only advanced after that return. `refreshHome` nulls a garage's home when no travel neighbour links and parks idle shuttles inside, but leaves shuttles with jobs alone. `dropCargo(shuttle)` already drops cargo at the garage, requeues unloaded jobs and counts loaded picks as missing. The garage GUI has shuttle slots, an `out` data slot and a status line.

## Goals / Non-Goals

**Goals:**
- No frozen shuttle, ever: the fleet moves while anything is out, and a lost home recalls.
- Manual recall and pause in the GUI.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Tick
`tick` runs `assignJobs()` only when `hasShuttles()`; when it is false the queue is served instantly. Motion advance and arrivals run in every case. `goHome` without a home reverses the motion as today, and `park` puts the shuttle inside.

### Recall
`ShuttleFleet.recall(BlockPos garage)`: for every shuttle of the garage, `dropCargo`, then `leg = HOME`, `motion = home == null ? null : parked(home)`, `syncPending`. `refreshHome` calls it when a home becomes null and any shuttle has a job or an unfinished motion. The GUI button reaches it through `WarehouseNetwork.recallGarage(pos)`.

### Pause
`ShuttleGarageBlockEntity.active` (NBT `Active`, default true) with `setActive` that marks changed and calls `network.garageChanged(this)`; `WarehouseNetwork.garageChanged` and the join path pass `garage.isActive()` to `fleet.adoptGarage(pos, count, active)`, which stores it on the fleet's `Garage`. `freeShuttles` skips inactive garages. `garageSummary` appends ", paused".

### GUI
`ShuttleGarageMenu`: button ids `BUTTON_RECALL = 1`, `BUTTON_TOGGLE = 2` in `clickMenuButton`, an `active` data slot. `ShuttleGarageScreen`: two buttons under the status line, Recall and Active / Paused, the label following the data slot; status line adds "paused". Lang keys `gui.servus_storage.garage.recall`, `.active`, `.paused`.

### Minecraft side verification
GameTests: `garageRecallBringsShuttlesHome` (one shuttle out on a pick from a rack, recall, out is 0, slot unlocked, the pick is queued again or served), `pausedGarageTakesNoJobs` (two garages, A paused: B's shuttle moves, A's never leaves), `losingTheRailRecalls` (break the rail behind the garage while its shuttle is out: out is 0 within a tick). A fourth test for shuttles out without any home is not needed: the recall on home loss makes that state unreachable; the tick change stays as belt and braces.

## Risks / Trade-offs

- [Recall drops cargo as items at the garage] → same rule as a destroyed garage; the player picks it up.

## Open Questions

None.

# Design

## Context

Paths: `S` = `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles`.

- Tools: `S/shuttle/module/ModuleSpec(id, fieldOnly, wide)` with item `ShuttleModuleItem`; `ShuttleProfile.liftless` comes from `wide`; jobs declare `JobNeeds(modules, field)` and `fits(profile)` is `modules ⊆ profile.modules && (field || !profile.fieldOnly)`. The workshop installs any `ShuttleModuleItem` as the tool.
- Jobs: `S/shuttle/ShuttleJob` is sealed (`PickJob, PutAwayJob, FieldJob, FertilizeJob, SupplyJob`). `ShuttleFleet.assignJobs` gives a job to the nearest free shuttle whose profile fits; `firstGoals` maps the job to dock rails; `arrivePutAway` walks `TO_SOURCE` then `TO_TARGET`; `holdForStop` waits `stopTicks`; `go(shuttle, leg, goals)` drives. `Shuttle.Load` holds the job and an item cargo. Amounts on the way are summed by `sumQueued` and `sumRiding`. `ShuttleFleet.Host` is implemented by `WarehouseNetwork`.
- Gantries (from `m13-fluid-gantry`): `FluidGantryBlockEntity` (master), `GantryMode`, `GantryTanks`, `TankRef`, the fluid index and per tank views in `WarehouseNetwork`, `FluidGantryRenderer`.

## Decisions

### D1. Tanker as a tool that only takes its own jobs

`ModuleSpec.TANKER = "tanker"`, registered as `new ModuleSpec(TANKER, true, false)`: the existing `fieldOnly` flag already means "takes no warehouse picks or put-aways", and `wide` false keeps lifts. `JobNeeds.TANKER = new JobNeeds(Set.of(TANKER), true)`. Field jobs need the Harvester or Spreader module, so a Tanker never takes them. The javadoc of `ModuleSpec.fieldOnly` is updated to say "takes only the jobs of its own tool".

### D2. Fluid capacity per tier

`ShuttleTiers.Tier` gets `tankerCapacity`, read from `tanker_capacity` in each tier of `shuttle.json` (4000, 8000, 16000) with a default for packs that do not set it.

### D3. One new job type

`record FluidJob(BlockPos source, int sourceTank, BlockPos target, int targetTank, FluidKey key, int amount) implements ShuttleJob` with `needs()` `TANKER` and no batching (`sameWay` false). `firstGoals` is the source gantry's dock rail. `Shuttle.Load` gets a `FluidStack fluid` next to the item cargo. Legs reuse `TO_SOURCE` and `TO_TARGET` with a new `arriveFluid` like `arrivePutAway`; the stop uses `holdForStop` with `pump_ticks` and Handling. `Host` gets `drainGantry(pos, tank, key, max)`, `fillGantry(pos, tank, stack)`, `fluidTargets(key, from)` and `gantryPumping(pos, tank, direction, fluid, on)`. Fleet sums `fluidQueuedFrom(source, tank)` and `fluidIncomingTo(target, tank)` cover queued and riding runs.

### D4. Planning is pure

- `gantry/FluidRouting<K, T>`: given target candidates (`T` id, lock, content key, amount, capacity, incoming, distance) and a key, returns the candidates with room in the order of the spec (locked match, Any holding, empty Any; then distance).
- `gantry/FluidPutAwayPlan`: `amount(free, full, minLoad, shuttleCapacity, room)` returns 0 when the run should wait.
`WarehouseNetwork.tickFluids()` (every `fluid_check_interval`) builds the candidates from the per tank views and the fleet sums, distances from `RailPaths` between the gantries' dock rails, and queues `FluidJob`s. It skips when the warehouse has no Tanker shuttle (`fleet.tankerCapacity()` is 0).

Settled during apply:
- A run is queued before a shuttle is chosen, so its amount uses the largest `tanker_capacity` among the warehouse's Tanker shuttles; the shuttle that takes it loads at most its own capacity, and the rest stays in the source for the next run. `ShuttleProfile` carries `fluidCapacity` (0 without a Tanker).
- Runs on their way count against a tank's room with their fluid: an empty tank on Any that a lava run is bound for counts as holding lava (`FluidRouting.Candidate.incomingKey`). Fleet sums are `fluidQueuedFrom(TankRef)`, `fluidIncomingTo(TankRef)` and `fluidIncomingKey(TankRef)`; `hasFluidRun(TankRef)` keeps one run per Import tank.
- The lock rules key by `Fluid`; a Storage tank holding the same fluid with other components is left out of the candidates, since `FluidSlot` keeps one fluid per tank.
- Each load remembers its current target and the tanks it was offered to; leftovers try the run's target while it is still Storage, then `fluidTargets` from the gantry the tanker stands at, skipping tried tanks, then drive back to the source (`TO_STATION` leg). A stop under a gantry that no longer serves the leg (source not Import, target not Storage) is skipped.
- `WarehouseNetwork.setTankerMinLoadForTests` sets the min load of one warehouse, so the full tank test does not change the stats of tests running beside it.

### D5. Lamps and state line

`FluidGantryBlockEntity.setActivity`: WORKING while a run for one of its tanks is queued or under way, BLOCKED for an Import gantry holding fluid with no room (the network tells it the fluid name for the state line), IDLE otherwise. The network sets these in `tickFluids`, not per tick, after all runs of that check are queued. Red wins over orange. An Import gantry holding fluid that no Tanker shuttle reaches is red too, with its own state line ("No Tanker shuttle reaches this gantry"); the menu syncs the waiting fluid (or no Tanker) as one data slot.

### D6. Spout animation

The static gantry model keeps only the spout sleeve; the inner pipe and nozzle move to `FluidGantryRenderer`. The fleet calls `gantryPumping` at the start and end of a stop; the master syncs `pumping` (tank, direction, fluid) in its update tag. The renderer eases the nozzle from its rest height down to the tanker's cap height (a constant of the tanker model) over a few ticks of client time and draws a thin column of the fluid's still sprite whose UV scrolls up or down by direction. Settled during apply: the nozzle stops `GantryLook.STREAM_GAP` (1 px) above the cap so the stream shows; the spout geometry lives in `GantryLook` (shared by the item model and the renderer) and the cap height in a pure `shuttle/module/TankerLook`; the spout lowers in 8 ticks of client time, a render constant.

### D7. Tanker look

`datagen/ShuttleModels`: `tool_tanker` with a steel tank of boxes lying along the body (an octagon of boxes or stacked boxes, inside the body width and below 16 px), two blackened iron bands inset into the tank surface by 0.05 px so no planes are shared, and a copper filler cap. Built as: three tank boxes each ending 0.05 px shorter than the one before, bands standing 0.2 px proud of each box with the same stagger (so they read as bands from the side), two iron saddles down to the chassis and iron sight glass frames on both sides; shared numbers in pure `shuttle/module/TankerLook`. `ShuttleView` gets an optional `SightGlass(fluid, share)`, drawn by `ShuttleGarageRenderer` in the item's model space. The sight glass is drawn by the shuttle renderer from the load's fluid share (shuttles are drawn on the client from `ShuttleSyncPayload`; the payload gets the fluid id and share for loads that carry fluid).

### D8. Data

`warehouse.json`: `fluid_check_interval` 40, `tanker_min_load` 1000. `gantry.json`: `pump_ticks` 40. Recipe of the Tanker: `IBI / ITI / I I` style with a Fluid Tank and two buckets (example; in recipe JSON).

## NeoForge APIs

- `FluidStack` and `FluidResource` transfer through `LegacyFluidHandler` and `FluidSlot` (Foundation, 26.1.2).
- No new events; the fleet runs in the warehouse network tick (mailbox pattern).

## Pure and Minecraft

- Pure, JUnit: `FluidRouting`, `FluidPutAwayPlan`, the tier reader default.
- Minecraft, GameTests: install, no item jobs, full run, lock order, room with runs on the way, small amounts wait, no room blocks with red lamp, partial pick, leftover to next tank, aborted runs, recall, lift ride.
- Manual: spout and stream, tanker look, sight glass, flicker.

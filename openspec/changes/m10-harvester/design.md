# Design

## Context

Current state, read from the code on 2026-10-11 (both earlier M10 changes are applied on main, not archived):

- `ShuttleFleet` keeps one `Shuttle` per garage slot with a `ShuttleProfile` (modules, field only, stacks). Jobs are `ShuttleJob` (sealed: `PickJob`, `PutAwayJob`) with `needs()`; `assignJobs` hands each queued job to the nearest free shuttle that fits, and serves a job no shuttle fits instantly. A busy shuttle holds `Load`s; legs `TO_SOURCE`, `TO_TARGET`, `TO_STATION`, `TO_TERMINAL`, `WAITING_ROOM`, `HOME`. A busy shuttle that has arrived gets `arrive` every tick. `dropCargo` pops loads at the garage and requeues unloaded jobs; it runs on garage removal, recall, profile change, lost home and broken lifts.
- `ModuleSpec(id, slot, fieldOnly, cargoHold)`, `JobNeeds(modules, field)`; the workshop accepts any `ShuttleModuleItem` of the slot kind.
- `FieldHeadBlockEntity` serves a field: `status()`, `rails()`, `plots()` (rail level positions within 4 of a rail, rails excluded), ripeness from events, `crop()`, `harvestOnly()`, `hydrated()`, `harvestDue()`, `recheck(pos)`. `FieldHeads.heads(level)` lists the loaded heads. `FieldEvents` forwards crop growth, bone meal, break, place and trample events.
- `WarehouseNetwork` implements `ShuttleFleet.Host`. Inbound put-away picks a target with `chooseTarget(tiers(key), key, stack, rackAllowed)`; `available(key)` counts stock; `index().sourcesHolding(key)` lists sources.

Vanilla and NeoForge APIs, checked in `neoforge-21.1.255-sources.jar`:
- `Block.getDrops(BlockState, ServerLevel, BlockPos, @Nullable BlockEntity)` builds `LootParams` with `ORIGIN` and an empty `TOOL` and returns the drops without spawning them.
- `Level.destroyBlock(BlockPos, boolean drop)` plays level event 2001 (break particles and sound) and sets the fluid state's block; with `false` nothing drops.
- `CropBlock.isMaxAge(BlockState)` (public final), `getStateForAge(int)`, `defaultBlockState()` is age 0; `canSurvive` asks the soil's `canSustainPlant` and needs light 8 or more.
- `HoeItem.TILLABLES` turns `GRASS_BLOCK`, `DIRT_PATH` and `DIRT` into `FARMLAND` when the block above is air and plays `SoundEvents.HOE_TILL`. The harvester tills only `DIRT` and `GRASS_BLOCK`, as the plan says, with the same sound.
- `BlockEvent.BlockToolModificationEvent` fires before a tool changes a block (hoe tilling), cancellable; `getPos()` is the changed block.

## Goals / Non-Goals

**Goals:**
- Harvester shuttles that work a field rail by rail without scanning blocks every tick and without new payloads.
- Pure planning (which rail next, which plots from a rail, what to do on a plot, cargo, seeds, next step of a run) with JUnit tests.

**Non-Goals:** trees, several harvesters per field, saving runs (see the proposal).

## Decisions

### D1. Harvester module

`ServusStorageItems.HARVESTER` (`harvester`, stacks to 16) is a `ShuttleModuleItem` with `new ModuleSpec("harvester", TOOL, true, false)`. The workshop needs no change. `JobNeeds.HARVESTER = new JobNeeds(Set.of("harvester"), true)`. Recipe: iron ingots, an iron hoe, shears and a hopper (shaped `ISI`, `IHI`, ` P `: S shears, H iron hoe, P hopper).

### D2. Open plots in the head

New pure enum `PlotState { BLOCKED, TILLABLE, PLANTABLE, GROWING, RIPE }`. `FieldRipeness` gets `update(T, PlotState)` (the old boolean `update` maps to it), `state(T)`, `tillable()` and `plantable()` sets. `FieldHeadBlockEntity.update` classifies a plot: a `CropBlock` is `GROWING` or `RIPE`; an air plot on `FARMLAND` is `PLANTABLE`, on `DIRT` or `GRASS_BLOCK` `TILLABLE`; anything else `BLOCKED`. New reads: `plotState(pos)`, `tillableCount()`, `plantableCount()`.

`FieldEvents` also queues `pos.above()` for break and place events and for `BlockToolModificationEvent` (the hoe), so changes at the farmland layer are seen. Farmland drying to dirt fires no event; a hydrated field does not dry, and the harvester reads the live block anyway (D5), so a stale state only affects when a job is queued.

### D3. Field jobs

`FieldJob(BlockPos head)` joins the sealed `ShuttleJob`; `needs()` is `JobNeeds.HARVESTER`, `sameWay` is false. `WarehouseNetwork.tickFields()` runs every `field_check_interval` ticks: for each loaded serving head whose first rail is a member of this network, when `fleet.hasFieldShuttles()` and `!fleet.hasFieldJob(headPos)` and `FieldDue.due(...)` holds, it calls `fleet.requestField(new FieldJob(headPos))`. Polling the few loaded heads is cheap and touches no blocks.

`FieldDue.due(harvestDue, plantMode, hydrated, tillable, plantable, seedsInWarehouse)` is `harvestDue || plantMode && hydrated && (tillable > 0 || plantable > 0 && seedsInWarehouse)`. `plantMode` is crop set and not harvest only.

In `assignJobs` a field job's goals are the field's rails that are travel nodes and have work (D5). With no free fitting shuttle but a busy one it waits like other jobs; when no shuttle fits, or no free one has a path, the job is dropped instead of served instantly (modified shuttles requirement).

### D4. Reach and route (pure)

- `PlotReach.assign(Collection<FieldPos> rails, Collection<FieldPos> plots)` returns, per rail, the plots worked from it: each plot goes to its nearest rail by Chebyshev distance, then Manhattan distance, then a fixed offset order, searching offsets within `FieldStrip.RADIUS` only (at most 81 lookups per plot). Within a rail the plots are sorted nearest first. So the reach is the strip itself: 4 blocks, and on a straight run the harvester works the 8 plots across each rail, plus the end columns from the end rails. The map is built once per run and again when the field's rail set changes.
- `FieldRoute.next(T from, Function<T, Collection<T>> neighbours, Predicate<T> hasWork)` is a breadth first search over the field's rails from the rail the harvester stands on and returns the nearest rail with work (the start itself counts). `FieldRoute.order` repeats it for tests and the planner. The leg to that rail uses `RailPaths.find` over the warehouse's travel nodes as every leg does.

### D5. Plot work (pure decision, world side action)

`PlotWork.plan(PlotState state, boolean plantMode, boolean haveSeed)` returns an `Action(harvest, till, plant)`: `RIPE` harvests and plants when `plantMode`; `TILLABLE` tills when `plantMode` and plants when also `haveSeed`; `PLANTABLE` plants when `plantMode && haveSeed`; else nothing. A rail has work when one of its plots' tracked state gives an action (`haveSeed` is "carries the crop item").

`HarvesterWork` (`storage/field`, Minecraft side) reads the live state of a plot with the same classifier as the head and carries the action out on the server thread:
- harvest: `Block.getDrops(state, level, plot, null)`, check the fit (D6), `level.destroyBlock(plot, false)`;
- till: `level.setBlockAndUpdate(below, FARMLAND)` and the hoe sound, only for `DIRT` and `GRASS_BLOCK` under a plot of the field;
- plant: the crop block of `head.crop()` (`BlockItem` of a `CropBlock`) at age 0 when `canSurvive`, with the crop planted sound.
After each worked plot the fleet calls `head.recheck(plot)`. Unloaded plots are skipped.

Each plot that gets an action costs `work_ticks` ticks (`StorageStatsManager.fieldWorkTicks()`, test hook `setFieldWorkTicks`). The harvester stands still on its rail while it works, so the client draws it parked there like a shuttle waiting at a terminal.

### D6. Cargo and seeds (pure)

`FieldCargo<K>` holds up to `slots` stacks of keys with a max stack size each: `fits(List<Entry<K>>)`, `add`, `count(K)`, `take(K, n)`, `room(K, max)`, `unload(K keep, int keepMax)` (everything but up to `keepMax` of `keep`) and `isEmpty`. The fleet uses `ItemKey` as the key and `ItemKey.stack(n)` to turn entries back into stacks. Slots = `max(profile.stacks(), harvester_stacks)`.

Harvesting a ripe plot in plant mode takes one crop item out of the drops for its replant (seed accounting: harvest first), then the rest must fit; otherwise the plot is not touched and the cargo counts as full. An empty cargo always harvests and pops what does not fit at the plot, so no plot blocks a run forever.

`SeedPlan.toFetch(ripe, toPlant, carried, available, room, stackSize)`: 0 while ripe plots remain (the harvest comes first); else `min(toPlant - carried, stackSize, available, room)`, never below 0. `toPlant` counts plantable and, in plant mode, tillable plots.

### D7. Run steps (pure)

`HarvestStep.next(serving, cargoFull, seedsWanted, room, workLeft, hasCargo)` returns `FETCH_SEEDS`, `WORK`, `PUT_AWAY` or `HOME`:
1. not serving: `PUT_AWAY` with cargo, else `HOME`;
2. cargo full: `PUT_AWAY`;
3. seeds wanted: `FETCH_SEEDS` with room, else `PUT_AWAY` with cargo;
4. work left: `WORK`;
5. `PUT_AWAY` with cargo, else `HOME`.

The fleet computes the inputs at every turn: after assignment, after a work segment, after a put-away, after a fetch. Work left is "some rail of the field has work" (D5), false after a segment that did nothing (guard against loops) and false for till and plant work when the field is not hydrated. A fetch is tried once per turn; a failed fetch counts as no seeds. A put-away keeps up to one stack of the crop item while planting work is left, never more than the plots to till and plant; when a harvest found no room because of kept seeds alone, the next put-away keeps none. A plot only counts as worked when something on it changed, so a plot whose crop cannot grow (too dark) ends the run instead of being revisited forever.

### D8. Field runs in the fleet

`Shuttle` gets a `HarvestRun run` (null otherwise) holding the head position, the `FieldCargo`, the reach map, the current rail's plot queue, a work countdown and the turn flags. A field job is one `Load` without cargo, so `busy()`, `out()` and the garage lock work unchanged. New legs: `TO_FIELD`, `WORKING`, `TO_SEEDS`, `TO_STORE`, `WAIT_STORE`.

- `TO_FIELD` arrival: `WORKING` with the rail's plot queue. `WORKING` each tick counts down and works the next plot with an action; when the queue is empty it drives to `FieldRoute.next` or ends the segment.
- `TO_SEEDS`: the first source holding the crop item with a path to a dock rail; a non rack source no rail reaches gives its seeds at once, like instant picks. Arrival extracts with `extractFrom`.
- `TO_STORE`: per cargo stack `host.putAwayTarget(stack, rack reachable)`; the shuttle drives to the dock rails of the first target it can reach and inserts every stack that fits there, then plans the next. Targets no rail reaches take their stack at once.
- `WAIT_STORE`: nothing has room: drive home and retry every 20 ticks, like stations retry.
- Lost: when the shuttle stands on a node that is no longer a travel node, or no path leads anywhere, it drops its cargo at the garage and parks, as `breakLift` does.

`dropCargo` pops the run cargo at the garage and does not requeue the field job. `Host` gains `putAwayTarget(ItemStack, Predicate<BlockPos>)` (wraps `chooseTarget`) and `available(ItemKey)`. `shownCargo` shows the first cargo stack of a run.

### D9. Data

`field.json` gains `work_ticks` 10, `harvester_stacks` 2, `field_check_interval` 40, read by `StorageStatsManager.fieldWorkTicks()`, `harvesterStacks()`, `fieldCheckInterval()` with test setters. Datagen: item model, recipe, lang (item, tooltip line). Placeholder `item/harvester.png` through the png helper.

### PLAN.md open decisions

None of section 5 is settled. This design fixes the reach (each plot from its nearest rail, 4 blocks), the field cargo floor (`harvester_stacks`), one harvester per field, and the full warehouse behaviour (wait at the garage).

## Pure logic and tests

JUnit in `storage/src/test/.../storage/field/`: `PlotReachTest` (nearest rail, straight run gives the cross column, ends get the end columns, nothing beyond 4, ties stable), `FieldRouteTest` (nearest with work, start counts, branch, none), `PlotWorkTest` (each state and mode), `FieldCargoTest` (fits, merge into partial stacks, new slot per type, full, take, unload keeps seeds), `SeedPlanTest` (ripe first, shortfall, capped by stack, stock and room), `HarvestStepTest` (each rule), `FieldDueTest`, and `FieldRipenessTest` additions for open plots.

GameTests in `HarvesterGameTests`: harvest ripe wheat into a crate and replant from the harvest, till dirt in the strip and not 5 out, harvest only mode does not plant, plain shuttles never take field jobs, a harvester never takes a warehouse pick, seeds picked from storage when nothing is ripe, cargo full trip and return, head broken mid run, warehouse full waits. `FieldGameTests` gets open plot counts.

## Risks / Trade-offs

- [ShuttleFleet grows] field runs add legs to an already large class → decisions live in pure classes and world actions in `HarvesterWork`; the fleet only sequences legs.
- [Farmland that dries without an event] is seen when the harvester reaches it or at the next field pass.
- [Drops are random] the fit check uses the drops actually rolled for that plot; when they do not fit the roll is discarded and the crop stays, so nothing is lost or duplicated.
- [A warehouse polling heads every 40 ticks] heads per level are few; nothing touches blocks.
- [Plots where the crop cannot grow] (light below 8) stay plantable in the head; each run ends after one useless visit, but the warehouse sends a new run at a later check. Acceptable for now; a later change may let the head remember such plots.

## Migration Plan

Unreleased: no migration.

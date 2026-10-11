# Design

## Context

Current state, read from the code on 2026-10-11:

- `RailBlock extends ConnectableBlock` with dock flags; it has no waterlogged property. Everything around rails tests `instanceof RailBlock`: connections, anchors, `RailSpan` gap checks, `RailAssist` sneak placement. `RailBlockEntity` is a `StorageNodeBlockEntity` of `NodeKind.RAIL`, so the warehouse graph and `ShuttleFleet` path search see every rail node.
- `m10-shuttle-workshop` gave shuttles modules and jobs `JobNeeds` with a field flag; no field job exists yet.
- The Foundation fluid line pushes into any neighbour that exposes `Capabilities.FluidHandler.BLOCK` (`FluidChannelNetwork`), so a block with an `IFluidHandler` capability is all a Field Head needs to take water from the pipe.
- Storage balance lives in `data/servus_storage/storage/*.json`, read by `StorageStatsManager`.

NeoForge APIs, checked in `neoforge-21.1.255-sources.jar`:
- `FarmlandWaterManager.addAABBTicket(Level, AABB)` returns an `AABBTicket` (a `SimpleTicket<Vec3>`) that is already validated; `SimpleTicket.invalidate()`, `validate()`, `isValid()`. `hasBlockWaterTicket(LevelReader, BlockPos)` tests the block centre with `AABB.contains` (min inclusive, max exclusive) and `FarmBlock.isNearWater` calls it for the farmland position. Tickets live in a weak map, are not saved and are dropped with their master chunk (`removeTickets` on chunk unload).
- `CropGrowEvent.Post(Level, BlockPos, BlockState original, BlockState state)`, fired by `CropBlock.randomTick` through `CommonHooks.fireCropGrowPost`. `CropBlock.growCrops` (bone meal) does not fire it.
- `BonemealEvent(Player, Level, BlockPos, BlockState, ItemStack)`, fired by `BoneMealItem.applyBonemeal` before the growth. Cancellable, not a player event (player may be null).
- `BlockEvent.BreakEvent`, `BlockEvent.EntityPlaceEvent`, `BlockEvent.FarmlandTrampleEvent`, `ChunkEvent.Load`.
- `IBlockEntityExtension.onLoad()` and `onChunkUnloaded()`; `FluidTank` in `neoforge.fluids.capability.templates`.
- `CropBlock.isMaxAge(BlockState)` is public final.

## Goals / Non-Goals

**Goals:**
- A field model that `m10-harvester` can drive: rails, plots, crop or harvest only, ripe plots and a due signal.
- Hydration without water blocks and without per tick block scans.

**Non-Goals:** harvesting, planting, tilling and field jobs (`m10-harvester`).

## Decisions

### D1. Farm Rail extends RailBlock

`FarmRailBlock extends RailBlock` with its own codec and an `onPlace` / `onRemove` that tells loaded Field Heads (D4). Because it is a `RailBlock`, connections, anchors, spans, docks and sneak placement work unchanged, and both rail kinds connect to each other. The Farm Rail uses the existing `RailBlockEntity` and its block entity type, which gets the Farm Rail as a second valid block, so it is a `NodeKind.RAIL` node of the warehouse graph and shuttles path over it with no fleet change. The item is an `AssistedBlockItem` with `RailAssist.INSTANCE`. Models come from `RailModels.rail(provider, block, "farm_rail")` with green tinted copies of the rail textures. Like the Warehouse Rail it has no `WATERLOGGED` property.

Alternative: a separate block class beside `RailBlock` with its own block entity. Rejected: every `instanceof RailBlock` check would need a second branch.

### D2. Pure field logic (no Minecraft imports)

Package `io.github.dirnei.mc.servus.storage.field`:

- `FieldPos(int x, int y, int z)` record.
- `FieldGraph.collect(T start, Function<T, Collection<T>> neighbours, int max)` returns `FieldGraph.Result<T>(Set<T> members, boolean tooLarge)`: breadth first over farm rail neighbours, stops when more than `max` members are found.
- `FieldStrip` with `RADIUS = 4`: `plots(Collection<FieldPos> rails)` is every position at the rails' level within 4 blocks in x and z of a rail, minus the rails; `boxes(Collection<FieldPos> rails)` returns `Box(minX, minY, minZ, maxX, maxY, maxZ)` hydration boxes, one per maximal run of rails along x in the same row (z), each from `x - 4, y - 1, z - 4` to `x + 5, y, z + 5` (exclusive max), covering the farmland layer one below the rails. Merging rows keeps the ticket count low.
- `FieldRipeness<T>`: planted and ripe sets of plots; `update(T plot, boolean planted, boolean ripe)`, `clear()`, `planted()`, `ripe()`, `ripePlots()`, `share()` (0 when nothing is planted) and `due(double threshold)` (at least one planted and `share >= threshold`).
- `FieldWater.use(int rails, int perRail)` the mB one interval uses.

The exact box: farmland at `y - 1` for rails at `y`, x and z within 4 of a rail. Blocks of the floor under the rails are inside the box too, which is harmless because only farmland asks.

### D3. Field Head block entity

`FieldHeadBlock` (full cube, pickaxe, `useWithoutItem` opens the menu with the position as extra data) and `FieldHeadBlockEntity` with a server ticker.

State kept in NBT: water amount, crop stack (one item) and harvest only. Not kept: field, plots, ripeness, tickets; they are rebuilt.

Status enum `FieldStatus { SERVING, NO_FIELD, TOO_LARGE, OTHER_HEAD }`.

Rebuild (`refreshField`) runs on the next server tick after `onLoad`, after a farm rail or head change near it and after a chunk load inside its field's bounds:
1. Find the first farm rail beside the head (north, east, south, west). None: `NO_FIELD`.
2. `FieldGraph.collect` over loaded farm rail neighbours on the same level with `max_rails`. Too large: `TOO_LARGE`.
3. Every other loaded Field Head touching a rail of the field is a rival. The head with the lowest `BlockPos.asLong()` serves; the others are `OTHER_HEAD`. Rivals whose view may now differ are marked for refresh too.
4. Serving: plots from `FieldStrip.plots`, then the full pass (D5); the hydration boxes are recomputed and, if the field is hydrated, the tickets replaced.

Water (`FluidTank` of `capacity`, validator `FluidTags.WATER`): `fill` delegates to the tank, `drain` always returns empty. In the ticker, when serving and not hydrated and the tank holds at least one interval's use, it uses that and starts hydration with a countdown of `water_interval`; when the countdown ends it uses the next interval's water or stops. Every interval it also re-adds tickets that NeoForge invalidated (master chunk unloaded). Hydration starting adds one `AABBTicket` per box; stopping, losing the field, `setRemoved` and `onChunkUnloaded` invalidate them all.

Not serving drops tickets and the ripeness.

### D4. Loaded heads and events

`FieldHeads` keeps the loaded heads per level (`WeakHashMap<Level, Set<FieldHeadBlockEntity>>`), added in `onLoad` on the server and removed in `setRemoved` / `onChunkUnloaded`. It offers `heads(level)`, `servingAt(level, railPos)` (the serving head whose field holds that rail, for `m10-harvester`) and the change hooks:

- `railChanged(level, pos)`: every head whose field holds `pos` or a horizontal neighbour of it, or that touches `pos`, refreshes next tick.
- `headRemoved(level, head)`: heads sharing a rail of its field refresh.

`FieldEvents` (game bus) forwards:
- `CropGrowEvent.Post`: the serving head whose plots hold the position updates that plot from the event's new state at once.
- `BonemealEvent`, `BlockEvent.BreakEvent`, `BlockEvent.EntityPlaceEvent`: the plot is queued for a recheck on the head's next tick (the block has not changed yet when these fire).
- `BlockEvent.FarmlandTrampleEvent`: the plot above the farmland is queued.
- `ChunkEvent.Load` (server): heads whose field bounds touch the chunk refresh.

Heads per level are few, so each event walks them and tests a `HashSet` of plot positions.

### D5. Ripeness

A plot is planted when its block is a `CropBlock`, ripe when also `isMaxAge`. The pass walks the plots once and skips unloaded positions (`level.isLoaded`). Rechecks queued by events are drained in the head's tick, at most one queue per tick, no scanning otherwise. `harvestDue()` is `(crop set or harvest only) and ripeness.due(harvest_share)`.

API for `m10-harvester` on `FieldHeadBlockEntity`: `status()`, `rails()`, `plots()`, `ripePlots()`, `plantedCount()`, `ripeCount()`, `crop()` (the crop item, empty for none), `harvestOnly()`, `hydrated()`, `harvestDue()`, `recheck(BlockPos)` and `recheckAll()`; and `FieldHeads.servingAt`.

### D6. Menu and screen

`FieldHeadMenu` with one `GhostSlot` over the head's one slot crop handler and the player inventory. `clicked` on the ghost slot sets the carried item when `FieldHeadBlockEntity.isCropItem` (a `BlockItem` whose block is a `CropBlock`) or clears it with an empty hand; anything else is ignored. Setting a crop clears harvest only. Button 0 toggles harvest only (turning it on clears the crop). Numbers travel as `DataSlot`s: water and capacity each split in two 16 bit slots (container data is sent as a short), rails, plots, planted, ripe (clamped to 32767), status, flags (hydrated, harvest only, due). Shift click on a seed sets it as the crop. No new payloads. `FieldHeadScreen` reuses the garage background like the workshop screen and draws a water bar, text lines and the toggle button.

### D7. Data

`data/servus_storage/storage/field.json`: `capacity` 8000, `water_per_rail` 1, `water_interval` 100, `max_rails` 128, `harvest_share` 0.5; read by `StorageStatsManager` (`fieldCapacity()`, `fieldWaterPerRail()`, `fieldWaterInterval()`, `fieldMaxRails()`, `fieldHarvestShare()`). Recipes: 8 Farm Rails from 8 Warehouse Rails around a composter; Field Head from iron ingots, a bucket, a composter and a Farm Rail. Datagen: rail multipart for the Farm Rail, cube bottom top for the head, item models, loot, pickaxe tag, lang. Placeholder textures through the png helper.

### PLAN.md open decisions

"Field kinds beyond crops" stays open. The field work paragraph in 2.3 is implemented as written; this design fixes the hydrated box (9 by 9 per rail at farmland height) and the ripe share denominator (planted plots).

## Pure logic and tests

- JUnit in `storage/src/test/.../storage/field/`: `FieldGraphTest` (connected members only, stops past max and flags too large, single rail), `FieldStripTest` (plots exclude rails and reach 4 not 5, boxes merge a straight x run into one box, a run along z gives one box per rail, box bounds one level below), `FieldRipenessTest` (share and counts, update from ripe to unripe and to empty, due at the threshold, never due with nothing planted), `FieldWaterTest`.
- GameTests in `FieldGameTests`: farm rail joins a warehouse and links to a Warehouse Rail; field split and join by rails; head status with no rail and with a second head; water accepted, lava refused, no drain; hydration on, out of reach, off after the water runs out and after a rail is removed; ripe after a growth event, after bone meal, less after a break; harvest due with a crop set and not without; menu sets wheat, refuses cobblestone, harvest only clears the crop.

## Risks / Trade-offs

- [Plots in unloaded chunks are not seen by the pass] the chunk load hook refreshes the field once the chunk is back.
- [Crops changed by other mods without events] are seen at the next full pass (field change, reload or the recheck command); `m10-harvester` rechecks plots it works on.
- [A rail whose ticket master chunk unloads while the head stays loaded loses hydration] the interval check re-adds invalid tickets.
- [Large fields walk up to `max_rails` rails and a few thousand plots on a change] only on placement, removal and load, never per tick.

## Migration Plan

Unreleased: no migration.

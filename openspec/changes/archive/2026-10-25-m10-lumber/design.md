# Design

## Context

Crop fields today: `field/FieldHeadBlockEntity` keeps the crop item in a one slot `crop`, classifies every candidate plot with `HarvesterWork.classify` into a `PlotState` (`BLOCKED`, `TILLABLE`, `PLANTABLE`, `GROWING`, `RIPE`), keeps the counts in `FieldRipeness`, works out the plots the rails see with `FieldReach.connected(..., state != BLOCKED, ...)` and learns changes from `field/FieldEvents` (crop grow, bone meal, break, place, tool, trample, chunk load), queued as `recheck` and read on the head's next tick. `network/WarehouseNetwork.tickFields` queues a `FieldJob` (needs `JobNeeds.HARVESTER`) through `FieldDue.due`, and `shuttle/FieldRuns` runs it: `turn` with `HarvestStep`, `workPlot` with `PlotWork`, seeds kept from the drops (`takeOne`), spare seeds into the supply buffer (`fillBufferFirst`) and, since `m10-field-harvest-buffer`, everything else into the head's To storage buffer (`storeAtHead`, `HeadUnload`). Tool modules are `ShuttleModuleItem` with a `ModuleSpec(id, fieldOnly, wide)`; the shuttle's look is composed from `tool_<id>` model parts in `datagen/ShuttleModels`.

This change depends on `m10-field-harvest-buffer` (implemented on main, play check pending): it must be archived first, and this change's `shuttles/harvester` "Field jobs" delta starts from that change's text.

## Goals / Non-Goals

**Goals:**
- Trees fields that run with the same head, rails, buffers, statuses and stock levels as crop fields.
- No per tick scanning: tree growth and existing trees are learned from events and the existing passes.
- A felled tree leaves nothing to decay and drops nothing on the ground.

**Non-Goals:**
- The felling animation (`m10-lumber-felling-look`).
- Nether fungi, other field kinds, Fortune or shears.

## Decisions

### PLAN.md open decision settled
"Field kinds beyond crops (trees first, then others) and which modules they need": a field has a kind, Crops or Trees, on its head; Trees need the Lumber module. Other kinds (fungi first) stay open. The archive grooms PLAN.md: `m10-lumber` leaves the Shuttles list, the open decision keeps only "other field kinds", and a new open point for Crimson and Warped fungi on nylium is added.

### Field kind on the head
New pure `field/FieldKind` (`CROPS`, `TREES`), saved as `Kind` (missing reads `CROPS`) with `TwoByTwo` for the 2x2 choice. The sapling lives in the existing `crop` slot, so the supply buffer's seed part, supply runs, `cropKey`, the crop icon on top and the Stock tab follow it without change. `isCropItem` becomes kind aware: Crops keep the `CropBlock` rule, Trees take items of `ItemTags.SAPLINGS` that are a `BlockItem`. `setKind` clears the slot, harvest only and the 2x2 flag, forgets the produce and calls `recheckAll`. Harvest only reads "fell only" in Trees mode; it is the same flag. A new `ChooseFieldKindPayload` (client to server) sets the kind; `ChooseFieldCropPayload` gets a `twoByTwo` boolean. Both refuse a player who can no longer use the head.

Two item tags by datagen in `ServusShuttlesItemTagsProvider`: `servus_shuttles:saplings_2x2` (spruce, jungle, dark oak, pale oak) and `servus_shuttles:saplings_2x2_only` (dark oak, pale oak). Vanilla `TreeGrower` keeps its mega features private, so the tags are the data driven way to say which saplings grow big; packs add modded ones.

### Trees need no water
`useWater` returns early for a Trees field (no tickets, no drain, no aging); `hydrated()` stays false and every caller that gates planting on water (`FieldDue`, `FieldWork`, `FieldRuns.plants`) takes "watered or Trees". The Water tab says that trees need no water; corrosion stays as it is, so a Trees field's rails never age.

### Spots and squares
New pure `field/TreeSpots`: `isSpot(x, z)` for single saplings (`floorMod(x, 2) == 0 && floorMod(z, 2) == 0`), `square(x, z)` returning the square a plot belongs to (smaller corner with `floorMod(x, 3) == 0 && floorMod(z, 3) == 0`, plots at offsets 0 and 1, the third column and row are the gap), and `complete(square, plots)` for "all four plots are field plots". World coordinates keep spots stable when rails are added. With a gap of one plot, no two squares touch, so vanilla's `isTwoByTwoSapling` (which looks at the four 2x2 groups around a sapling) never grows a tree across two squares. JUnit `TreeSpotsTest`: even grid, negative coordinates, square members and gap, incomplete square.

### Reading tree plots
`HarvesterWork.classify` gets the field's view (`kind`, sapling block, 2x2) through a small `PlotRules` record the head passes; in Trees mode:
- a sapling block (`BlockTags.SAPLINGS`) on a spot: `GROWING`;
- a block of `servus_shuttles:tree_logs` at plot level, spot or not: `RIPE`, unless the head marked the plot `unfellable` (below);
- clear space on a spot whose ground lets the sapling survive (`sapling.defaultBlockState().canSurvive`): `PLANTABLE`; in fell only mode and on squares that are incomplete or without a Range Upgrade: `IDLE`;
- leaves, hanging parts, clear space on soil off the spots: `IDLE`;
- everything else: `BLOCKED`.
`PlotState` gets `IDLE` (open for sight, no work, not planted). `FieldRipeness.update` ignores it like `BLOCKED` for the counts; `refreshReach` keeps it open (`!= BLOCKED`). `TILLABLE` never occurs in Trees mode.

Block tags by datagen in `ServusShuttlesBlockTagsProvider`: `servus_shuttles:tree_logs` (`#minecraft:logs`, `minecraft:mangrove_roots`) and `servus_shuttles:tree_attachments` (`minecraft:vine`, `minecraft:cocoa`, `minecraft:mangrove_propagule`, `minecraft:pale_hanging_moss`, `minecraft:moss_carpet`).

### Ripeness without scanning: the grow event
NeoForge 26.1.2.116 fires `net.neoforged.neoforge.event.level.BlockGrowFeatureEvent` (source: `neoforge-26.1.2.116-sources.jar`, fired from `TreeGrower.growTree` through `EventHooks.fireBlockGrowFeature`) whenever a sapling grows into a feature, from random ticks, a player's bone meal and `BoneMealItem.applyBonemeal` used by the Spreader. It fires before the tree is placed and may be cancelled or fail for lack of room, so `FieldEvents.onGrowFeature` (priority `LOWEST`, skipped when cancelled) only queues `recheck` for the event position and its eight horizontal neighbours (the 2x2 tree may start from any of its four saplings); the head reads them on its next tick, after the placement, and sees logs (`RIPE`) or the sapling still there (`GROWING`). Trees already standing are read by `recheckAll` when the field is loaded, changed or rechecked; a player felling by hand is caught by the existing break event. No new per tick work.

### Lumber module and jobs
`ModuleSpec.LUMBER = "lumber"`, `ServusShuttlesItems.LUMBER` (`fieldOnly`, `wide`), `JobNeeds.LUMBER`. `FieldJob` gets `kind` and `needs()` returns `HARVESTER` or `LUMBER` from it; `FleetSaves` writes `Kind` with the job (missing reads `CROPS`). `tickFields` checks `fleet.hasFieldShuttles(needs)` and `fieldReachable(pos, needs)` per head kind and calls `FieldDue.due` with "watered" true and no tillable plots for Trees. `FieldWork.of` takes the kind's shuttle flags and two more inputs, `needsRange` and `tooLarge`; new values `NO_LUMBER`, `NO_LUMBER_WAY`, `NEEDS_RANGE`, `TREE_TOO_LARGE`, and Trees fields never return `DRY`. `NO_LUMBER`, `NO_LUMBER_WAY` and `NEEDS_RANGE` are problems (red marker, red bar), `TREE_TOO_LARGE` is not. JUnit extends `FieldWorkTest` and `FieldDueTest`.

One shuttle never does both: the shuttle has one tool place, and splitting keeps `JobNeeds` a plain module match.

Recipe (datagen): iron ingots around an iron axe, a stonecutter and a piston, unlocked by the Mk1 shuttle.

### Felling
New pure `field/TreeWalk`: input the base plots (one, or the four of a square), a lookup `pos -> Kind` (`LOG`, `NATURAL_LEAVES`, `ATTACHMENT`, `OTHER`), the set of other spots and squares at the plot level, `maxLogs` and `maxBlocks`. It walks logs from the base through all 26 neighbours, never below the base level and never into a log on another spot's or square's base; then collects candidate leaves within leaf reach (6 steps through leaves, the vanilla decay distance) of the felled logs, runs a multi source search from every log that is not felled through leaves for 6 steps, and keeps the candidates no other log reaches: the leaves that would decay. Attachments touching any removed block join. Result: `Felled(logs, leaves, attachments)`, `TooLarge` or `NotATree` (no natural leaves). JUnit `TreeWalkTest`: oak, two touching crowns, diagonal acacia branch, 2x2 base, a neighbour trunk at base level, the log limit, the block limit, a log pillar, vines.

`HarvesterWork.fell(level, base)` snapshots the blocks the walk asks for (bounded by the limits), runs `TreeWalk`, rolls every block's drops with `Block.getDrops(state, level, pos, blockEntity)` (no tool, so leaves give saplings, sticks and apples by their loot tables), then removes leaves and attachments first and logs last with `level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS)`: no drops, no neighbour updates, but shape updates still let outer leaves recompute their distance, and waterlogged mangrove leaves leave their water. It plays `SoundEvents` wood break and `LevelEvent.PARTICLES_DESTROY_BLOCK` (2001) at the base log only. `TooLarge` and `NotATree` mark the plot (or square) `unfellable` in the head, cleared when a block of it changes, so the ripe count drops and the status shows `TREE_TOO_LARGE`; `unfellable` is not saved, the next pass finds it again.

In `FieldRuns.workPlot` a Trees field goes to `fellAndPlant`: plan with new pure `field/TreePlotWork.plan(state, plantMode, saplingsCarried, saplingsNeeded, harvest)` (fell, plant), roll the drops first, return `FULL` when they do not fit and the cargo is not empty (the existing rule), keep one sapling (four for a square) out of the drops as `takeOne` does for seeds, then fell and plant. A square is planted only with four saplings, all in one step. `SeedPlan.toFetch` counts plots to plant per spot or four per square. Felling uses `fell_ticks` (`ShuttlesStatsManager`, `field.json` example 40) shortened by Handling instead of `work_ticks`. The kind switching to Crops mid run makes `workLeft` false, so `turn` unloads and goes home. JUnit `TreePlotWorkTest`.

### Spreader
`dosePlot` and `fertilizeTargets` use the same `classify` with `PlotRules`, so saplings read `GROWING` and grown trees `RIPE`: the existing "only while GROWING" stop covers trees. The head's `unfed` rule (a plot turning `GROWING` from something else) covers newly planted saplings.

### GUI
`client/FieldHeadScreen` Field tab: two kind buttons (Crops, Trees) above the picker; the picker takes its entries from `CropPickerEntries` for Crops and a new sapling list for Trees (fell only first, 2x2 entries after their sapling, greyed without a Range Upgrade with the hint). The head sends `twoByTwo` and the kind with its other synced state (`FieldHeadMenu` opening data and the existing update). The Water tab shows "Trees need no water" in a Trees field. Stock tab and pickers otherwise unchanged. Lang via `ServusShuttlesLanguageProvider`.

### Model
`ShuttleModels.lumber`: a turret (dark iron ring and steel post) at the front of the hold, a two part steel arm folded back along the top inside x 6..10, and the felling head at its end (steel saw disc between two steel claws), all `STEEL` or `DARK_IRON` on every face. It stays below the spoiler's end plates (y 12.5) and in front of the Handling mast (z 1..2) and its crane beam (z 1..10, y 12..13), so the arm ends before z 10.5. Boxes that touch end exactly at each other's faces or are inset 0.05 px; the task lists every overlapping pair.

### APIs
- `BlockGrowFeatureEvent` (NeoForge 26.1.2.116, verified in the sources jar).
- `Block.getDrops(BlockState, ServerLevel, BlockPos, BlockEntity)`, `Level.setBlock(..., Block.UPDATE_CLIENTS)`, `LeavesBlock.PERSISTENT`, `LevelEvent.PARTICLES_DESTROY_BLOCK`, `ItemTags.SAPLINGS`, `BlockTags.SAPLINGS`, `BlockTags.LOGS` (Minecraft 26.1.2 sources).
- `BoneMealItem.applyBonemeal` on saplings (already used by the Spreader).

Pure Java with JUnit: `FieldKind`, `TreeSpots`, `TreeWalk`, `TreePlotWork`, the extended `FieldWork`, `FieldDue`, `SeedPlan`. Minecraft side, by GameTest: classification, felling, drops, jobs, saves.

## Risks / Trade-offs

- [A neighbour's branch grown into the felled crown] → The walk never enters another trunk on the base level, so at worst a branch higher up is taken with the tree; acceptable for a farm.
- [Bee nests on birch and oak with flowers nearby] → They stay where they hung; the player can keep flowers away from the field. Non-goal.
- [Mangrove roots spread wide and a mangrove can exceed the block limit] → The limit is data; the status says which tree is too large so the player can raise it or fell it by hand.
- [A partly planted spruce square grows single spruces] → The shuttle plants squares all at once; only a player breaking one sapling leaves a partial square.
- [Leaves of crowns over the farm rails] → Shuttles are drawn, not entities; leaves do not stop them.

## Migration Plan

Heads load as Crops and field jobs as crop jobs when their kind is missing. No other save data changes.

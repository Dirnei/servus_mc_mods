# Design: m10-field-range

## Context

See proposal.md for the why. Today `FieldStrip.RADIUS = 4` is shared by `FieldStrip.plots` (the head's candidates), `FieldStrip.boxes` (hydration tickets), `FieldReach.connected` (plots in sight) and `PlotReach` (which rail works which plot). The head rebuilds all of it in `serve()` whenever `refreshField()` runs, which already happens on load and on rail changes, never per tick. Shuttle ways come from `RailPaths.find(from, goal, host::travelNeighbours)`; a step whose two nodes share x and z is a ride in a shaft, and `ShuttleFleet.mayStart` makes the shuttle call the car before such a step.

## Goals / Non-Goals

Goals: one radius per head that every field calculation reads; harvesters whose ways have no vertical steps; nothing scanned per tick.

Non-goals: see proposal.md. Also no change to how plain shuttles ride.

## Decisions

### D1. The radius is an argument of the pure field classes

`FieldStrip.plots(rails, radius)`, `FieldStrip.boxes(rails, radius)`, `FieldReach.connected(rails, candidates, open, radius)` and `PlotReach.assign(rails, plots, radius)`. `RADIUS` goes away. `PlotReach` keeps its nearest first offset order per radius in a small cache (radius 0 to 16), since the ordering is the costly part. A new pure `FieldRadius.of(base, max, upgrades)` returns `clamp(base + upgrades, 0, max)` with `max` raised to `base` when a pack sets it lower, and `FieldRadius.maxUpgrades(base, max)` says how many upgrades fit in all.

Alternative: keep the constant at 4 and filter by radius afterwards. Rejected: hydration boxes and candidates would still be built for 4, so events and tickets would cover ground the field does not own.

### D2. A Storage Range Upgrade in its own head slot, not the Foundation upgrade API

The Foundation `UpgradeItem`/`UpgradeSlots` API is about speed: every `UpgradeItem` carries a speed multiplier from `upgrades.json`, `UpgradeSlots` accepts any upgrade and combines multipliers. A range is a count, not a multiplier, and a Speed Upgrade must not go into a Field Head. So the head gets its own one slot `ItemStackHandler` that takes only `range_upgrade` and has a slot limit of `FieldRadius.maxUpgrades(...)` (so the stack never grows past `max_radius - radius`). `onContentsChanged` marks the block changed and calls `requestRefresh()`, so the next head tick runs `refreshField()` and `serve()` rebuilds candidates, plots and boxes; when hydrated, `serve()` already drops and re-adds the water tickets. The upgrades are saved as `RangeUpgrades` and dropped in `FieldHeadBlock.onRemove` with `Containers.dropItemStack` (vanilla).

A pack that lowers `max_radius` below what a head holds keeps the extra items in the slot; the radius is clamped.

### D3. `field.json` keys

`radius` (default 2) and `max_radius` (default 4), read by `StorageStatsManager` like the other field keys. The head reads them when it serves, so a datapack reload applies at the next refresh of each field.

### D4. Wide tools and liftless ways

`ModuleSpec` gains `wide`. The Harvester registers as `new ModuleSpec(HARVESTER, true, true)`. `ShuttleProfile` gains `liftless`, true when the tool is wide. A new pure `LiftlessSteps.filter(node, neighbours)` drops every neighbour with the same x and z as the node (the steps up or down a shaft) and keeps the rest, so a horizontal pass through a shaft node stays.

`ShuttleFleet` gets `ways(Shuttle)`, a neighbour function: `host::travelNeighbours` for ordinary shuttles, the filtered one for liftless ones. Every way a single shuttle drives uses it: job assignment, going to the field, between rails, to seeds, to storage, home, waiting home and recall. Garage level questions (`refreshHome`, `canReach`) stay on the full graph. Since a liftless way never has a vertical step, `mayStart` never makes such a shuttle call a car.

Alternative: forbid riding in `mayStart` and let the shuttle stand. Rejected: it would find a way through the shaft and then wait forever.

### D5. Storage only where the shuttle can drive

`Host.putAwayTarget(stack, allowed)` now applies its predicate to every target (it was racks only). The fleet passes `storable(shuttle, from)`: a target without dock rails is allowed unless it is a rack (unchanged: it takes its part at once); a target with dock rails is allowed when `ways(shuttle)` finds a way to one of them; otherwise it is allowed only for an ordinary shuttle and not for a rack (the old instant fallback). For a liftless harvester this skips crates behind a lift instead of filling them at once. Inbound station put-aways keep `chooseTarget` with their rack only predicate.

### D6. A field no harvester can reach

`ShuttleFleet.fieldReachable(head)`: true when some garage with a home holds a Harvester shuttle whose `ways` lead from the home to a rail of the field. `WarehouseNetwork.tickFields` skips fields that fail it, and `FieldWork.of` gets a `reachable` flag checked right after `harvester`, giving the new reason `NO_WAY`. The menu already calls `work()` when it broadcasts; one path search per open GUI per broadcast over a warehouse graph is cheap.

## PLAN.md decisions settled

- Field range: the head's radius, 2 at the start, upgrades to 4 (PLAN.md section 2.3, Field range). Numbers in `field.json`.
- The Harvester crosses shafts but never rides (PLAN.md section 2.3, Harvester shuttles).
- PLAN.md M10 item 4 is marked built with these choices.

## NeoForge and vanilla APIs

- `net.neoforged.neoforge.items.ItemStackHandler` (slot limit, `isItemValid`, `onContentsChanged`), already used by the head's crop slot.
- `net.neoforged.neoforge.common.FarmlandWaterManager.addAABBTicket` and `AABBTicket`, already used; nothing new.
- `net.minecraft.world.inventory.Slot` subclass for the upgrade slot, `net.minecraft.world.Containers.dropItemStack` on removal (vanilla 1.21.1, used by vanilla containers).

## Pure and Minecraft parts

Pure (JUnit): `FieldStrip`, `FieldReach`, `PlotReach` with the radius, `FieldRadius`, `LiftlessSteps`, `FieldWork` with the new reason, `ShuttleProfile` with `liftless`.
Minecraft (GameTests): the head's radius and upgrade slot, hydration at radius 2 and 4, a harvester crossing a shaft, a harvester refusing a lift, put-away skipping storage behind a lift.

## Risks / Trade-offs

- [A head refresh rereads every candidate after each upgrade change] → only when the player changes the slot; the same work as a rail change.
- [Existing dev world fields shrink from 4 to 2] → expected for an unreleased mod; two Range Upgrades restore them.
- [`fieldReachable` searches a path per GUI broadcast] → bounded by the warehouse graph; only while a GUI is open, and once per field check in `tickFields`.

## Migration Plan

None: nothing is released.

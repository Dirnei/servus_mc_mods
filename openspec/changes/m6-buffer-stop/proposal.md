# Proposal

## Why

A player asked for a buffer stop at the end of a rail. Today a run of rails just stops with a stub of track, which looks unfinished in a built out warehouse or field. The stop is purely decorative: it changes nothing about how shuttles drive or how the warehouse is built.

This adds to PLAN.md M6 Warehouse automation (rails). It is not an open point in PLAN.md; it comes from a player request.

## What Changes

- **New block, the Buffer Stop:** a steel frame on a steel sole with stepped braces, a beam in the yellow and black dock stripe and two buffer heads on the lines of the rail heads, plus a short stub of track under the beam. It takes its own block beyond the last rail, because a parked shuttle and its Harvester reach past the end of the last rail block.
- **Placed by the player only:** it never appears on its own at an open rail end.
- **Turns to the rail:** when placed it faces its buffers to a rail by the shared rail facing rule; sneaking faces it to the player. The Configuration Carrot turns it a quarter and sneak dismantles it.
- **The rail runs into it:** a Warehouse Rail or Farm Rail that the stop's front faces draws full track up to the stop instead of an end stub. It is not a dock and the stop never joins the warehouse network; shuttles never drive into it.
- **Stands on a floor:** it needs a solid block below and drops itself when that block goes.
- **A stop for each rail:** the Buffer Stop has a steel track stub like the Warehouse Rail; the Farm Buffer Stop has a copper stub like the Farm Rail. Both stand at either kind of rail. Each shows the wood of the rail it was crafted from. The Farm Buffer Stop stays fresh copper and never corrodes.
- **Recipe:** three iron ingots over one Warehouse Rail make one Buffer Stop; three iron ingots over one Farm Rail make one Farm Buffer Stop, whatever the Farm Rail's corrosion stage. No dye.

## Capabilities

### New Capabilities
- `shuttles/buffer-stop`: the Buffer Stop and Farm Buffer Stop blocks: look, placement and facing, standing, recipe and dismantling, and that it stays outside the network.

### Modified Capabilities
- `shuttles/warehouse-rail`: "Rails auto connect" gains both stops as a side that carries rail when its front faces the rail.
- `shuttles/rail-facing`: "Which blocks turn to a rail" gains both stops.

## Impact

- Shuttles code: new `rail/BufferStopBlock`, `rail/BufferStopBlockEntity`, pure `rail/BufferStopLook` (plus a new frame part in `RailLook`), `RailBlock.connectsTo`, registries (block, block entity, item, creative tab).
- Farm variant: a second registration `farm_buffer_stop` of the same block class and block entity type, its own model with copper heads and the farm dock stripe, recipe from the Farm Rail.
- Datagen: new `BufferStopModels`, blockstate, item model, loot table, recipe, lang.
- Tests: `BufferStopLookTest` (JUnit), `BufferStopGameTests`.
- Overlap with the unarchived `m10-rail-corrosion-per-rail`: both edit files in `rail/` and the datagen providers, but different files in `rail/` and different specs. Whichever lands second rebases.
- The Farm Buffer Stop recipe takes a Farm Rail in any corrosion stage (from `m10-rail-corrosion-per-rail`); the stop itself carries no stage.
- Shuttles CHANGELOG gets an Added line at archive.

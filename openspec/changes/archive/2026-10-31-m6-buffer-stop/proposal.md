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
- **One stop that remembers its rail:** the Buffer Stop is one block and one item. It saves which rail it was crafted from (Warehouse Rail or Farm Rail) next to its wood, and its track stub follows it: steel heads for the Warehouse Rail, fresh copper heads for the Farm Rail. The beam is yellow and black for both. It stands at either kind of rail and never corrodes. The tooltip names the rail.
- **Recipe:** three iron ingots over one Warehouse Rail make one Buffer Stop; three iron ingots over one Farm Rail make one Buffer Stop with the Farm Rail look, whatever the Farm Rail's corrosion stage. No dye.

## Capabilities

### New Capabilities
- `shuttles/buffer-stop`: the Buffer Stop block: look, placement and facing, standing, recipe and dismantling, and that it stays outside the network.

### Modified Capabilities
- `shuttles/warehouse-rail`: "Rails auto connect" gains the Buffer Stop as a side that carries rail when its front faces the rail.
- `shuttles/rail-facing`: "Which blocks turn to a rail" gains the Buffer Stop.

## Impact

- Shuttles code: new `rail/BufferStopBlock`, `rail/BufferStopBlockEntity`, pure `rail/BufferStopLook` (plus a new frame part in `RailLook`), `RailBlock.connectsTo`, registries (block, block entity, item, creative tab).
- Rail kind: a `rail` block state property (`warehouse` or `farm`) carried on the item, a model per kind, a second recipe from the Farm Rail, a tooltip line.
- Datagen: new `BufferStopModels`, blockstate, item model, loot table, recipe, lang.
- Tests: `BufferStopLookTest` (JUnit), `BufferStopGameTests`.
- Overlap with the unarchived `m10-rail-corrosion-per-rail`: both edit files in `rail/` and the datagen providers, but different files in `rail/` and different specs. Whichever lands second rebases.
- The Farm Rail recipe takes a Farm Rail in any corrosion stage (from `m10-rail-corrosion-per-rail`); the stop itself carries no stage.
- Shuttles CHANGELOG gets an Added line at archive.

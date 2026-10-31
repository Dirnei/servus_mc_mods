# Design

## Context

Rails are `RailBlock`s (a `ConnectableBlock`). `connectsTo` sets the side flags `north`/`east`/`south`/`west`, and the multipart blockstate from `RailModels` picks the `RailLook` piece from those flags. The END piece runs z 0..12 (track stops at the far side of the centre). The network does not read these flags: rails link through their `StorageNodeBlockEntity`, so a block that is only a connected side for the look does not join anything. Nothing outside `ConnectableBlock` and datagen reads the flags.

A parked shuttle body spans z 2..14 of its rail block and the Harvester tool reaches z 18. The stop cannot sit in the free end of the END rail block, so it takes the block beyond it. The exploration mockup (`temp/buffer-stop.html`, option A) fixed the look.

## Goals / Non-Goals

**Goals:** a decorative Buffer Stop block that the player places beyond a rail end, turns to the rail, makes the rail draw full track into it, and shows the dock stripe in the family's steel.

**Non-Goals:** any effect on shuttles, routes, the network or fields. No stop placed by itself. No Farm Rail variant with copper heads or corrosion. No dye colours. No waterlogging.

## Decisions

### Own block with a small material block entity
`BufferStopBlock` extends `BaseEntityBlock` with a horizontal `FACING` (front = the side with the buffer heads). `BufferStopBlockEntity` holds only a `MaterialSlot` (`MaterialHolder`), like the `ElevatorBlockEntity` does, so the track stub's sleepers show the wood of the rail it was crafted from. It is registered as a wood material block (`Material.register(id, MaterialKind.WOOD)` in `ServusShuttles`, next to the rail), so Foundation's `MaterialModels` wraps its block models and its item uses the material item model.

It is not a `StorageNodeBlockEntity`, so the network never sees it. Alternative: no block entity and fixed sleepers. Rejected because a stub in the wrong wood stands right next to the rail's sleepers.

### Facing
`getStateForPlacement` uses `RailFacings.frontToRail(context, RailFacings.ANY_RAIL)`, the shared rule (clicked rail, look direction, only rail, else face the player; sneaking faces the player). `onWrench` (Foundation `Wrenchable`) turns `FACING` clockwise; a sneak wrench dismantles it into its item with its wood, as rails do.

### Rail counts the stop as a track side
`RailBlock.connectsTo` returns true for a `BufferStopBlock` neighbour whose `FACING` is the direction from the stop to the rail (`direction.getOpposite()` seen from the rail). The rail then shows STRAIGHT instead of END, with the track running to the block edge. The dock flags stay false, so no beam is drawn on the rail. When the stop turns or breaks, the neighbour update recomputes the flag through `ConnectableBlock.updateShape`; turning the stop with the carrot uses `setBlock` with neighbour updates (flag 3).

Because the network ignores the flags, nothing else changes. Field code finds Farm Rail neighbours by `instanceof FarmRailBlock`, so field sizes and corrosion are not affected either.

### Standing
`canSurvive` is `canSupportRigidBlock(level, pos.below())`, the rail's standing test. `updateShape` on a DOWN change returns air when it can no longer survive, and the loot table drops the item. `RailBlock.isAnchor` stays as it is, so a stop never holds up a hung rail.

### Look: pure `BufferStopLook`
The boxes live in a pure Java `BufferStopLook` (no Minecraft imports) next to `RailLook`, reusing `RailLook.Box` and its `rotated`. `RailLook.Part` gains `FRAME` (steel). Unrotated, the front faces north (z 0), like the END and dock pieces. In pixels:

| Part | x | y | z | Texture |
|---|---|---|---|---|
| Stub sleepers | 1..15 | 0..1 | 1..3, 5..7 | wood |
| Stub heads | 3..5, 11..13 | 1..2 | 0..8 | rail head |
| Sole | 2..14 | 0..1 | 8..15 | steel |
| Uprights | 3..5, 11..13 | 1..10 | 8..10 | steel |
| Brace, high step | 3..5, 11..13 | 1..7 | 10..12 | steel |
| Brace, low step | 3..5, 11..13 | 1..4 | 12..14 | steel |
| Beam | 1..15 | 6..9 | 7..8 | dock stripe |
| Buffer heads | 2.5..5.5, 10.5..13.5 | 6.25..8.75 | 6..7 | steel |

Steel is `servus_shuttles:block/heavy_rack_iron` on every face, the stripe is `servus_shuttles:block/rail_dock`, heads are `servus_shuttles:block/rail_head`, sleepers the wood placeholder. The stub always uses Warehouse Rail heads, also in front of a Farm Rail.

Overlap check (no two boxes share volume; where boxes touch, their touching faces point opposite ways and hide each other):
- Stub heads end at z 8 where the sole starts; heads are at y 1..2, the sole at y 0..1, so they meet only along an edge.
- Stub sleepers end at z 7, before the sole at z 8.
- Uprights stand on the sole top (y 1); braces stand on the sole top and touch the upright back face at z 10, the low step touches the high step at z 12.
- The beam (z 7..8) touches the upright fronts at z 8 and passes over the stub heads (y 6 above y 2).
- Buffer heads (z 6..7) touch the beam front at z 7; at y 6.25..8.75 they sit inside the beam's height, so no top or bottom plane is shared.
- No face of one box lies in the plane of a same facing face of another box over a shared area.

`BufferStopLookTest` checks these rules mechanically.

### Shape
Selection and collision use one shape: the sole plus a box around the frame and beam (x 1..15, y 0..10, z 6..15) and the stub as a 2 px slab. The shuttle never enters the block, so the shape only matters for the player.

### Recipe
A `MaterialRecipe` (shaped) via `MaterialRecipes.output(..., MaterialKind.WOOD)`: `III` over ` R `, `I` iron ingot, `R` Warehouse Rail; the wood comes from the rail, like the Rail Elevator recipe. Result count 1.

## Risks / Trade-offs

- A stop facing away from the rail leaves the rail's end stub. That is intended: it reads as "not attached" and the carrot fixes it.
- `m10-rail-corrosion-per-rail` is unarchived and edits other files in `rail/` and the same datagen providers (loot, lang, blockstate). Different lines; whichever lands second rebases.

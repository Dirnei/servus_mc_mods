# Design

## Context

See proposal.md for the why. Today only `ShuttleGarageBlock.getStateForPlacement` turns to a rail, through the pure `garage/GarageFacing` (clicked rail, rail in the look direction, only rail, else look) plus `clickedRail` and `railsBeside` helpers in the block. `StationBlock` (parent of Inbound, Outbound and Packing), `FluidStationBlock`, `FieldHeadBlock` face the player; `HeavyRackBlock` and `FluidRackBlock` copy the rack below, else face the player, then decide section joins (`RackRow`, `joinFromBelow`). The Foundation placement assist (`PlacementAssist`, `AssistedBlockItem`, `PlacementAssists`, client ghost, `ServusFoundationConfig.placementAssistReach`) already drives rails (`RailAssist`) and the elevator. `PlacementAssists.Target.context` builds a place context as if the player clicked the last run block towards the target, keeping the player, so the placed block sees a clicked neighbour and a sneaking player. `FluidTankItem.placeLayer` places a whole tank layer on a top click. Rack and station fronts open a GUI through `useItemOn` returning `TRY_WITH_EMPTY_HAND`; vanilla skips `useItemOn` for a sneaking player with an item, so only unsneaked clicks need the new tag.

PLAN.md leaves no open decision here; the choices come from the explores of 2026-10-29.

## Goals / Non-Goals

**Goals:** one facing rule for every rail docking block, a rack wall in few clicks, no surprise GUIs while building.

**Non-Goals:** turning placed blocks, a layer over a whole row, a preview for the layer.

## Decisions

### One pure rule, `rail/RailFacing`
`GarageFacing` moves to `rail/RailFacing.railSide(Optional<Side> clickedRail, Set<Side> railsBeside, Side looking, boolean sneaking)` and returns the rail side, or empty when no rail decides it or the player sneaks; the block then faces its front to the player. (Returning the look side would turn a station's front away from the player, since for stations the rail side is the front.) `GarageFacingTest` becomes `RailFacingTest` with sneak cases added. A small Minecraft side helper `RailFacings` (same package) builds the inputs from a `BlockPlaceContext` and a rail predicate, so the garage's private `clickedRail`/`railsBeside` move there and every block calls one method:
- garage: rail side is the doorway, `FACING` is its opposite (unchanged);
- Inbound, Outbound, Packing (in `StationBlock`), Fluid stations, racks, Field Head: rail side is the front, `FACING` is the rail side itself.
The rail predicate is `RailBlock` (which includes `FarmRailBlock`) for all but the Field Head, which passes `FarmRailBlock` only. Fluid stations check rails beside the clicked (lower) position, which is what the context gives anyway.
Alternative: a shared base block class. Rejected: the blocks already have different parents (`StationBlock`, `BaseEntityBlock` racks).

### Rack facing order
`HeavyRackBlock` and `FluidRackBlock` get one helper each (or a shared `RackFacing` in `rack/`): the clicked block when the context does not replace it (`clickedPos.relative(clickedFace.getOpposite())`) is a rack of that kind → its facing; else the rack below → its facing; else `RailFacings`. The clicked rack check comes before sneaking, so an assist placement (which keeps the sneaking player and clicks the last rack of the run) copies the row. Section joins stay as they are and run on the chosen facing.

### Rack row assist
`rack/RackAssist` (one instance per rack kind, or one taking the block class) implements `PlacementAssist`:
- `isRun`: a rack of the same kind with the clicked rack's facing; Foundation's `PlacementAssist` gets a default `isRun(level, start, pos, stack)` that `PlacementAssists.target` calls, so a run can depend on the clicked block (existing assists keep the default);
- `direction`: the row axis is perpendicular to the clicked rack's facing; a hit on a side face (on the row axis) walks that way; a hit on the top or bottom walks up or down the column (added in the play check); a hit on the front or back walks towards the edge of the face nearest to the hit: along the row for the left and right edges, up or down the column for the top and bottom edges (changed in the play check, since the tops of a rack wall are rarely in view).
`HeavyRackItem` and `FluidRackItem` extend `AssistedBlockItem` instead of `BlockItem`, so the client ghost and arrow come for free (`PlacementAssistPreview` looks items up through `AssistedItem`).

### Section layer
`HeavyRackItem.place` and `FluidRackItem.place` first try a layer, like `FluidTankItem.placeLayer`, server side only: not sneaking, clicked face is UP, the clicked block is a rack of the same kind; collect the clicked bay's section (existing section walk in each block, left and right through the join flags); every spot above free and replaceable; the stack count at least the bay count (or creative); for the Fluid Rack, the held item carries no fluid. Then place one item per bay through `super.place` with a context at each spot clicking the top of the bay below, left to right, so each new bay copies the facing below and `joinFromBelow` copies the section boundaries. Otherwise fall through to the assist path, which falls through to a normal place.
Alternative: place all blocks in one go with `setBlock`. Rejected: per bay `super.place` keeps the item components (wood), sounds, block entity data and advancement hooks.

### Warehouse parts tag
`ServusShuttlesTags.PLACES_ON_RACKS = ItemTags.create(servus_shuttles:places_on_racks)` filled in `ServusShuttlesItemTagsProvider` with the Heavy Duty Rack, the Fluid Rack and the Warehouse Rail. `HeavyRackBlock.useItemOn` returns `PASS` when the stack is in the tag (`ServusShuttlesTags.placesOnRack(stack)`), which lets vanilla run the item's `useOn` and place the block. The Fluid Rack already passes everything but fluid containers. Narrowed in the play check: placing against every warehouse block made stations, terminals and the garage hard to open with a block in hand.

### Tests
- Pure: `RailFacingTest` (all garage cases plus sneak).
- GameTests with the place helper used in `FluidTankGameTests` (a mock player, a hit vector and `BlockItem.place`): station from the aisle, station sneaking, Field Head with a Warehouse and a Farm Rail, Fluid Inbound beside a rail, rack against a rack's side and front, rack layer over a section of three (and a short stack, a taken spot, two sections), Fluid Rack layer port position, rack row assist to the end and blocked, tag places a rack against a rack front and cobblestone opens the GUI (menu open check).
- Manual: ghost and arrow on racks, placing from the aisle, sneak, garage still right.

## Risks / Trade-offs

- [A player who wants a block facing them by a rail must sneak; muscle memory] → said in the changelog line.
- [Rack sneak now extends the row instead of facing the player] → only while looking at a rack; elsewhere sneak still faces the player, and a rack placed by the assist copies its row anyway.
- [Rack layer places several blocks in one click and may surprise] → only on an unsneaked top click with enough items; at most one section (3 bays by default).
- [`AssistedBlockItem` placement runs the assist before `place`] → the rack items override `place` to try the layer first, then hand over to `PlacementAssists.place`.

## Migration Plan

None: only new placements change; placed blocks keep their facing.

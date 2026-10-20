# Design

## Context

Paths: `S` = `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles`.

- `S/garage/ShuttleGarageBlock.getStateForPlacement` sets `FACING` to `context.getHorizontalDirection().getOpposite()`. `FACING` is the front (lamp); the doorway is the back, and `ShuttleGarageBlockEntity.acceptsRail` takes only the back side.
- Warehouse rails are `S/rail/RailBlock` (the Farm Rail extends it). `HeavyRackBlock.railInFront` already tests `instanceof RailBlock`.
- `S/rail/RailBlock` updates its arm towards a neighbour on shape updates, so a garage placed with its back to a rail gets the arm without extra code.
- `foundation/graph/Side` is the pure side enum; `foundation/network/Conversions` maps it to `Direction`.

## Decisions

### D1. Pure choice of the doorway side

`S/garage/GarageFacing.doorway(Optional<Side> clickedRail, Set<Side> railsBeside, Side looking)` returns the side the doorway should face, all horizontal `Side`s, no Minecraft types:
1. `clickedRail` present: that side.
2. `looking` in `railsBeside`: `looking`.
3. exactly one side in `railsBeside`: that side.
4. otherwise `looking` (the front faces the player, so the back faces where the player looks).

`getStateForPlacement` sets `FACING` to the opposite of the result.

### D2. Inputs from the placement context

- `clickedRail`: the block at `context.getClickedPos().relative(context.getClickedFace().getOpposite())` is a `RailBlock` and the clicked face is horizontal; the side is the clicked face's opposite (from the garage towards the rail). Clicking the top of a rail places the garage on top of it, which is not a doorway case.
- `railsBeside`: the four horizontal neighbours of the placed position that are `RailBlock`s.
- `looking`: `context.getHorizontalDirection()`.

Elevator segments are not rails here: a garage doorway onto a shaft is not a normal build, and the rule only needs to cover rails.

## NeoForge APIs

- `BlockPlaceContext.getClickedPos`, `getClickedFace`, `getHorizontalDirection`, `getLevel` (vanilla, 26.1.2; already used by `RailBlock` and `HeavyRackBlock`).

## Pure and Minecraft

- Pure, JUnit: `GarageFacing`.
- Minecraft, GameTests: placement by a mock player through the item, for the rail clicked, one rail, look direction, several rails and no rail cases.

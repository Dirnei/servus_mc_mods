# Design

## Context

See proposal.md for the motivation. Today the Warehouse Rail item is a plain `BlockItem` (`ServusStorageItems.RAIL`, `registerSimpleBlockItem`). The duct line items are `LineItem extends BlockItem`. On a non-sneak click on a duct that lacks the line, `LineItem.useOn` adds the line. Every other click goes through `BlockItem.useOn`, then `place`.

Vanilla `BlockItem.place(BlockPlaceContext)` (NeoForge 21.1.255 sources) works in this order:
1. Checks `context.canPlace()` on the original context.
2. Calls `updatePlacementContext`.
3. Picks the state and places.

`BlockPlaceContext.at(context, pos, direction)` builds a context whose clicked position is `pos.relative(direction)` and whose clicked face is `direction`. `ScaffoldingBlockItem` uses it the same way to place away from the clicked block.

PLAN.md leaves nothing open for this item. It is a new M6 item that this change adds to the plan.

## Goals / Non-Goals

**Goals:**
- One Lib component that any Servus block item can use with two small hooks: what counts as its run, and how the hit picks a direction.
- The walk and direction logic is pure Java with JUnit tests.
- Server and client run the same code. The client result only feeds the preview and the predicted placement.

**Non-Goals:**
- A generic multi-block placer, a registry of assists, or support for items from other mods.
- Custom networking. There are no payloads.

## Decisions

### Pure logic in `lib/placement`

- **`RunWalk`**: `walk(start, step, isRun, isFree, maxReach)` uses an int vector `(x, y, z)` for the start and the step, plus predicates on positions. It returns the target position or empty. It starts at the block after `start`, skips every position where `isRun` holds, and stops at the first position where it does not. That position is the target if `isFree` holds and its distance is at most `maxReach`. Otherwise the result is empty. It uses its own small record for positions, so it has no Minecraft types.
- **`QuadrantDirection`**: `nearestEdge(double x, double z)` takes a hit point in block-local coordinates (0 to 1) and returns the side (north, east, south or west, as an enum or index of its own) whose edge is nearest. Ties go to a fixed order so the result is deterministic.

Alternative: walk with `BlockPos` directly in the Minecraft glue. Rejected, because the project rule wants pure logic tested in JUnit, and the walk is the part most likely to have off-by-one errors around reach.

### Item hook: `PlacementAssist` plus `AssistedBlockItem`

```java
public interface PlacementAssist {
    /** Whether a sneak click on the block at pos starts a walk. */
    default boolean isStart(Level level, BlockPos pos, ItemStack stack) { return isRun(level, pos, stack); }
    /** Whether the block at pos continues this item's run. */
    boolean isRun(Level level, BlockPos pos, ItemStack stack);
    /** Walk direction for a click on a block that starts a walk. */
    Direction direction(Level level, BlockHitResult hit, ItemStack stack);
}
```

`PlacementAssists.target(level, player, stack, hit, assist)` holds the Minecraft glue. It returns `Optional<BlockPos>`:
- It returns empty unless the player is secondary use active (sneaking) and `assist.isStart` holds at the clicked block.
- Otherwise it runs `RunWalk` with `isFree = level.getBlockState(p).canBeReplaced(context at p)` and `maxReach = ServusLibConfig.placementAssistReach()`.

Items take part by overriding `BlockItem.place`:

```java
@Override
public InteractionResult place(BlockPlaceContext context) {
    return PlacementAssists.place(context, assist, super::place);
}
```

`PlacementAssists.place` does three things:
- Without an assist situation, it returns `super.place(context)` unchanged.
- With one and a target, it calls `super.place(BlockPlaceContext.at(context, target.relative(dir.getOpposite()), dir))`.
- With one and no target, it returns `InteractionResult.FAIL`. Nothing is placed and nothing is consumed.

`place` is overridden, not `updatePlacementContext`. Vanilla checks `canPlace()` on the original context before it calls `updatePlacementContext`, so a blocked spot beside the clicked face would veto a valid far target.

Lib ships `AssistedBlockItem extends BlockItem`, which takes a `PlacementAssist` in its constructor. The rail item becomes an `AssistedBlockItem`. `LineItem` already extends `BlockItem`, so it overrides `place` itself and holds its own assist. Both implement a small marker interface `AssistedItem { PlacementAssist placementAssist(); }`, which the client renderer looks up.

Alternative: a global `PlayerInteractEvent.RightClickBlock` handler that redirects placement. Rejected, because cancelling and re-dispatching the event duplicates vanilla placement logic and breaks client prediction.

### Straight runs keep their axis

`lib/placement/StraightAxis.along(int connections, int face, double x, double y, double z)` is pure. Sides are numbered like `Direction.ordinal()` (down, up, north, south, west, east), and the hit point is block local. It returns a side or empty:
- It returns empty when the connection mask is empty, spans more than one axis, or the face lies on the connection axis.
- Otherwise it returns the positive side of the axis (east, up, south) when the hit coordinate on that axis is at least 0.5, and the negative side when it is not.

Rails and ducts call it first and fall back to their own rule on empty. This is Create's shaft rule. With sneak meaning "keep going straight", a hit on the top of a straight pipe or on the middle of a rail line is almost never meant as a branch. A plain click still branches.

Alternative: keep the clicked face and ask players to aim at the end cap. Rejected after the play check: on a horizontal pipe the top face is what players look at, and on a straight rail the quadrant rule picks north or south for nearly every hit near the middle.

### Rail, duct and elevator assists

- **Rail** (`storage/rail/RailAssist`):
  - `isRun`: the block is a `RailBlock`.
  - `direction`: `StraightAxis.along` with a mask of the horizontal sides that hold a `RailBlock` or an `ElevatorBlock`. Fallback: a hit on a horizontal face uses that face, and a hit on the top or bottom face uses `QuadrantDirection.nearestEdge` on the local hit x and z.
  - Rails keep their stand, hang and gap behaviour through the normal `onPlace` path and the scheduled `tick` with `RailSpan`. Nothing changes there.
- **Duct** (`logistics/duct/DuctAssist`, constructed per `LineItem` with its `Line`):
  - `isRun`: the block entity is a `UtilityDuctBlockEntity` whose `has(line)` holds. The walk never tests the clicked block with `isRun`, because it begins with the next block.
  - `direction`: `StraightAxis.along` with a mask of the sides where the clicked duct `hasArm(line, side)`. Fallback: `hit.getDirection()`.
- **Elevator** (`storage/rail/ElevatorAssist`):
  - `isRun`: the block is an `ElevatorBlock`.
  - `direction`: always `UP`, because `ElevatorBlock.canSurvive` needs a segment or a solid block below, so there is nothing to build downwards or sideways.
  - `ServusStorageItems.ELEVATOR` becomes an `AssistedBlockItem`.
- **Start check:** the rail keeps the default `isStart` (a rail). The duct overrides `isStart` to "any `UtilityDuctBlock`", so a sneak click on a duct that lacks the line walks from it, which keeps today's sneak behaviour as the case where the next block is free.

`LineItem.useOn` keeps its add-line branch for non-sneak clicks. Sneak clicks already fall through to `super.useOn`, then `place`, so they reach the assist without other changes.

### Config

`ServusLibConfig` gets a `placement` section with `placementAssistReach`, an int in the range 1 to 64 with a default of 16. It is a common config. Each side reads its own copy, so a client with a different value may preview a different target than the server places. The server result wins, which the spec allows. A synced server config was considered and rejected: Lib only has a common config today, and a mismatch only affects the preview.

### Client preview

`lib/client/PlacementAssistRenderer` subscribes to `RenderLevelStageEvent` at `AFTER_TRANSLUCENT_BLOCKS` (`Dist.CLIENT`, in Lib). Each frame it does the following:
- It checks that the player is sneaking, the main hand item is an `AssistedItem`, and `Minecraft.hitResult` is a `BlockHitResult`. If not, it returns.
- It computes the target with the same `PlacementAssists.target`. If there is none, it returns.
- It computes the placement state with `item.getBlock().getStateForPlacement(BlockPlaceContext.at(...))`.
- It renders the state's block model at the target with `BlockRenderDispatcher.renderSingleBlock` into a translucent buffer, through a vertex consumer wrapper that forces alpha to about 0.5. The duct's dynamic model draws without block entity data, so the ghost shows a bare centre. That is acceptable for a preview.
- It draws an arrow as flat quads (`RenderType.debugQuads()`, like `SideMarkerRenderer`) through the middle of the target, pointing in the walk direction, in a fixed accent colour.

This is one walk per frame, of at most `placementAssistReach` block state reads, and only while sneaking with an assisted item. It is not a per-tick world scan.

### PLAN.md

- Add "Sneak placement assist for rails and ducts" to the M6 list, before the pick rate stats screen.
- Add "Placement assist (sneak extends a straight run)" to the Lib API sketch.

## Risks / Trade-offs

- [The client predicts a placement that the server rejects] → The vanilla block update corrects the client. FAIL on the server consumes nothing.
- [The ghost of a duct looks bare because it has no block entity data] → It is acceptable for a preview. Model data could be passed later if it bothers players.
- [A `renderSingleBlock` alpha wrapper fights the render type and draws opaque] → If so, fall back to drawing the target's shape outline with the arrow. Note it in the play check task.
- [Sneak already means "place against the face" for duct items] → That behaviour stays as the reach-zero case of the walk, so existing habits keep working.

## Migration Plan

None. The mod is unreleased, and no saved data changes.

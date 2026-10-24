# Design

## Context

Utility Duct, Warehouse Rail (and Farm Rail, a subclass) and the Elevator segment all extend `foundation.block.ConnectableBlock`. Their shapes are thin, so `BlockState.blocksMotion()` is false. In 26.1.2 `FlowingFluid.canHoldAnyFluid` (from `minecraft-patched-26.1.2.116-sources.jar`) treats such a block as replaceable unless it is a door, sign, ladder, sugar cane or portal, so spreading water or lava destroys it with drops.

The same method returns true for any `LiquidBlockContainer`, and `canHoldSpecificFluid` then asks `LiquidBlockContainer.canPlaceLiquid`. Returning false there makes the fluid treat the block as a wall. `BucketItem` asks the same `canPlaceLiquid` and, on false, places the fluid at the neighbour in front of the clicked face.

PLAN.md has no open decision on this. The change settles a gap that was never decided: these blocks hold fluids back and refuse fluid sources on placement; only the duct is waterloggable (decided in the play check).

## Goals / Non-Goals

**Goals:**
- One implementation in `ConnectableBlock` covers all four blocks; the duct adds waterlogging on top.

**Non-Goals:**
- No `WATERLOGGED` property on rails or elevator segments.
- Blocks that do not extend `ConnectableBlock`.

## Decisions

**`ConnectableBlock implements LiquidBlockContainer`** (`net.minecraft.world.level.block.LiquidBlockContainer`, vanilla, verified in the 26.1.2 sources): `canPlaceLiquid` returns false for every fluid and `placeLiquid` returns false. Alternatives: `Properties.forceSolidOn()` also makes `blocksMotion()` true, but it changes more than fluids (mob spawning checks, `isSolid` users such as support and redstone logic) and would also be needed on every subclass registration. `SimpleWaterloggedBlock` was rejected by the user.

**The duct implements `SimpleWaterloggedBlock`** (vanilla, extends `LiquidBlockContainer` and `BucketPickup`, verified in the 26.1.2 sources). It adds `BlockStateProperties.WATERLOGGED`, reports a water source from `getFluidState` and schedules a water tick from `updateShape` while waterlogged, like vanilla slabs. Because `ConnectableBlock` already implements `canPlaceLiquid` and `placeLiquid`, the duct overrides both to call the `SimpleWaterloggedBlock` defaults, which accept only still water. Flowing water and lava are still refused, so a dry duct dams them. The duct blockstate file has a single `""` variant, so the new property needs no datagen change. Rails and the elevator were asked to stay as walls (play check), so waterlogging is not in the base class.

**Refuse fluid sources in `ConnectableBlock.getStateForPlacement`**: return null when `level.getFluidState(clickedPos).isSource()`, through a hook `placedInto(state, fluid)` that the duct overrides to waterlog in a water source and refuse only other sources. This covers water, lava and modded source blocks the same way, and every placement path that goes through the block item, including the placement assist on line items and the sneak run extension. Flowing fluid is replaceable and simply overwritten; the placed block then dams the rest.

**`RailBlock.getStateForPlacement` passes null through.** It currently decorates the base state with dock flags and would throw on null. `ElevatorBlock` does not override placement.

**No hint on refusal.** A null placement state makes `BlockItem.place` return a failed result, like vanilla blocks that can not be placed; the item stays in hand.

**Tests.** Nothing here is pure logic, so there are no JUnit tests. GameTests cover the behaviour: water flowing against a dry duct, a rail and a shaft top keeps the block and leaves its position dry; a duct placed into a water source is waterlogged and into a lava source is refused; rails and segments placed into a water source are refused; a duct placed into flowing water is dry and dams it; a broken waterlogged duct leaves water. The bucket behaviour comes from vanilla `BucketItem` and is checked in the play check.

## Risks / Trade-offs

- [Other mods' builders place blocks without `getStateForPlacement`] → They overwrite the source; the block then holds the water back. Harmless.
- [A block already sitting in a world next to still water] → Nothing changes for it; water never re-enters, it only no longer destroys it.
- [Players can no longer lay ducts across a pond] → Intended; they fill the pond or run the duct above it.

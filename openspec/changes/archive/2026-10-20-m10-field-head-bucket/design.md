# Design

## Context

`FieldHeadBlock` overrides only `useWithoutItem`, which opens the GUI. Vanilla's default `useItemOn` returns `TRY_WITH_EMPTY_HAND`, so a bucket falls through to the GUI. The head's buffer is a `FluidSlot` behind a fill only `LegacyFluidHandler` (`FillOnly`), registered as `Capabilities.Fluid.BLOCK` for the head's block entity in `ServusShuttlesEvents`.

## Decisions

### D1. Same interaction as the Fluid Tank

`FieldHeadBlock.useItemOn` copies the shape of `FluidTankBlock.useItemOn` (Foundation): an empty stack returns `TRY_WITH_EMPTY_HAND`; a stack that is not a fluid container (`Transfer.isFluidContainer`) returns `TRY_WITH_EMPTY_HAND` so the GUI still opens; a fluid container calls `FluidUtil.interactWithFluidHandler(player, hand, level, pos, side, null)` on the server and returns `SUCCESS`. That helper looks up the block's fluid capability, which is the fill only handler, so draining into an empty bucket finds nothing and the creative rule of `ItemAccess` keeps the bucket for creative players, as in the tank.

Returning `SUCCESS` for every fluid container (also when nothing moved) is what keeps the GUI shut for an empty or lava bucket, as the spec asks.

### D2. Not a new helper

The tank and the head share four lines. A shared helper in Foundation would need a name and a home for one more caller; the duplication stays small and local.

## NeoForge APIs

- `BlockBehaviour.useItemOn` and `InteractionResult.TRY_WITH_EMPTY_HAND` (vanilla 26.1).
- `net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler`, already used by `FluidTankBlock` and `SinkBlock` on 26.1.2.

## Testing

GameTests (Minecraft): water bucket fills, lava bucket refused, empty bucket gets nothing, full head keeps the bucket. No pure logic.

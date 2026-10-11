## Context

The Field Head (`storage/field/FieldHeadBlock`, `FieldHeadBlockEntity`) is a `BaseEntityBlock` without block state properties, drawn as a `cubeBottomTop` with placeholder textures. Stations already have the look we want: `LayeredModels` in Foundation builds a full cube of `Wood.BOARD` panels under a cutout overlay per face, `WoodModel` (wrapped around every block id given to `Wood.register`) retextures the boards to the planks of the wood in the block entity's model data, and `WoodSlot` / `WoodHolder` / `WoodContent.WOOD` / `WoodRecipes.output` carry the wood from the recipe through the item to the block and back into the drop. The head already learns ripeness from events (`FieldEvents`, `FieldRipeness`) and keeps its water in a `FluidTank`.

PLAN.md leaves the art direction open ("Art style and texture direction"); this change only follows the station family that the user picked in explore (variant B), it does not settle the open decision.

## Goals / Non-Goals

**Goals:**
- Station look with wood from the recipe, horizontal facing.
- Ripe share bar on the front, water window on all four sides, crop icon on a plate on top.
- No per tick work for the look: state and sync change only on events.

**Non-Goals:**
- Pumpkins and melons (stems are not `CropBlock`).
- Rail looks and copper corrosion (separate m10 changes).
- Showing water in the item model (items have no block entity).

## Decisions

### Wood exactly like the stations
`FieldHeadBlockEntity implements WoodHolder` with a `WoodSlot`, forwarding `saveAdditional`, `loadAdditional`, `getUpdateTag`, `applyImplicitComponents`, `collectImplicitComponents`, `removeComponentsFromTag` and `getModelData`, copied from `ShuttleGarageBlockEntity`. The item becomes a `WoodBlockItem`, the block id goes into the `Wood.register` list in `ServusStorage`, the loot table moves from `SELF_DROPPING` to `WOODEN` (copies `WoodContent.WOOD` from the block entity), and the recipe is saved through `WoodRecipes.output` with planks in it:

```
IBI
PCP
PRP   I iron ingot, B bucket, C composter, P planks, R farm rail
```

### Facing
`HorizontalDirectionalBlock.FACING`, set in `getStateForPlacement` to `context.getHorizontalDirection().getOpposite()` like `InboundStationBlock`, with `rotate` and `mirror`. Models are built facing north and rotated by `(facing.toYRot() + 180) % 360` like the stations.

### Ripe bar: `RIPE` block state 0 to 5
`IntegerProperty RIPE` on the block. Pure `FieldHeadLook.ripeStep(planted, ripe, thresholdPercent, due)`:
- 0: nothing planted, nothing ripe, or not serving: empty bar.
- 1 to 4: progress `p = share / threshold`, step `1 + floor(4p)` kept in 1..4; the bar (8 texels wide inside a 1 texel rim, at x 4..12, y 10..12 of the front) shows 1, 3, 5 and 7 columns of wheat gold, so gold never fills the window.
- 5 (`READY`): `harvestDue()` is true: all 8 columns green.

One overlay texture per step (`field_head_front_0` .. `_5`), one `LayeredModels.cube` per step, like the station lamp fronts. The head never computes the step per tick: every place that changes the inputs (`refreshReach`, `update`, `unserve`, `setCrop`, `setHarvestOnly`, `stepHarvestPercent`, the crop slot's `onContentsChanged`) sets a `lookDirty` flag, and the next server tick computes the step once and calls `level.setBlock(pos, state.setValue(RIPE, step), Block.UPDATE_CLIENTS)` only if it differs. Deferring to the tick keeps `setBlock` out of other blocks' event handlers.

### Water window: block entity renderer with quantized sync
The overlay side texture paints a dark glass panel in the upper panel (x 2..14, y 2..7) with two glints; the window is opaque in the static model, so it reads as dark glass when empty. `FieldHeadRenderer` (a `BlockEntityRenderer`) draws one water quad per horizontal face, a hair (0.002) in front of the face, covering the panel from its bottom up to `level / 8` of its height. Texture and tint come from `IClientFluidTypeExtensions.of(Fluids.WATER)`: `getStillTexture()` on the block atlas and `getTintColor(FluidState, BlockAndTintGetter, BlockPos)`, which NeoForge's water type answers with the biome water colour (checked in `nfapi/.../IClientFluidTypeExtensions.java`, default methods at lines 208 and 262). The quads go into `Sheets.translucentCullBlockSheet()` like `FluidTankRenderer`. Light is `LevelRenderer.getLightColor(level, pos.relative(face))`, like `CrateRenderer`, because the light inside a full block is dark.

Drawing in front of the face instead of inside the block keeps the shared board cube intact (a hole would need a custom board layer per window) and reads the same at 16 texels.

Pure `FieldHeadLook.waterLevel(amount, capacity)` returns 0..8: 0 when empty, 8 only when full, else `floor(8 * amount / capacity)` kept in 1..7. The tank's `onContentsChanged` (and the capacity check in `tick`) computes it and calls `level.sendBlockUpdated` only when the level differs from the last one sent. The update tag carries `WaterLevel` as a byte; the client keeps it in a field that the renderer reads.

### Crop icon
The top overlay paints a dark inset plate of 10 by 10 texels with a 1 texel rim (x 3..13). The renderer draws the crop stack flat on it with `ItemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, buffers, level, 0)` at 8/16 size, rotated to lie on the top, a hair above it, turned so the item reads upright from the head's front. The update tag carries `Crop` and `HarvestOnly`; changes in the crop slot or mode call `sendBlockUpdated`. The renderer draws nothing when the crop is empty or harvest only is on.

The bottom face uses its own overlay (`field_head_bottom`, the plain frame) so the plate is not drawn underneath.

### Pure logic and tests
`FieldHeadLook` (no Minecraft imports) with JUnit `FieldHeadLookTest`: steps at 0 planted, 0 ripe, quarter, half and three quarter progress, at and beyond the threshold without due, due; water levels empty, 1 mB, half, just below full, full, zero capacity. GameTests in `FieldLookGameTests`: placement faces the player, wood survives place and break, the recipe records birch, `RIPE` follows a ripening crop and a lowered threshold, and the synced water level and crop in the update tag.

## Risks / Trade-offs

- [Water quads in front of the face can z-fight with the overlay at a distance] The offset is 0.002 of a block and the quads are translucent over an opaque dark panel; acceptable, as for crate labels.
- [Crop icon size] 8 texels on a 10 texel plate was chosen in explore; the user settles it in the play check.
- [Existing heads in test worlds] they keep working, face north and show oak boards; nothing is migrated since nothing is released.

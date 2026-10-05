# Design

## Context

The duct renders through a multipart blockstate; the block entity syncs its configuration with an update tag. NeoForge offers `ModelEvent.ModifyBakingResult`, `IDynamicBakedModel` with `ModelData`, `IBlockEntityExtension.getModelData` and `requestModelDataUpdate`, `RenderLevelStageEvent`, data components and `SimpleCraftingRecipeSerializer`. See proposal.md.

## Decisions

### D1: Facade item and recipe
`FacadeItem` carries the block in a `DataComponentType<Block>` (`facade_block`, persistent with the block registry codec, network synced). `FacadeRecipe extends CustomRecipe` matches exactly one iron ingot and one block item whose default state is a solid full cube without a block entity, and assembles six facades of it. Registered with `SimpleCraftingRecipeSerializer` and written by datagen as a special recipe.

### D2: Block entity
`UtilityDuctBlockEntity` gains a nullable `BlockState facade`, saved with `NbtUtils.writeBlockState` under `Facade`, read with the block registry lookup, in the update tag. `getModelData` returns the facade under `UtilityDuctModel.FACADE`. When `loadAdditional` runs on a client level it calls `requestModelDataUpdate` and `sendBlockUpdated` so the chunk rebuilds. `removeFacade` returns the facade item.

### D3: Block
`UtilityDuctBlock.useItemOn` returns skip default for facade and channel items so their `useOn` runs instead of the menu (this also fixes adding a second channel by item). Sneak wrench removes the facade first. `getShape` is the full cube with a facade; `getAppearance` returns the facade state; `onRemove` drops the facade item.

### D4: Dynamic model
`UtilityDuctModel implements IDynamicBakedModel` wraps the baked model of every duct block state (replaced in `ModifyBakingResult` by matching the model location's id). With a facade in the model data it delegates quads, render types and particle icon to the facade block's model with `ModelData.EMPTY`; otherwise to the wrapped model.

### D5: Overlay
`DuctTracker` (common, no client imports) holds the client side set of loaded duct block entities; the block entity registers in `onLoad` and unregisters in `setRemoved` and `onChunkUnloaded` when on a client level. `DuctOverlayRenderer` listens to `RenderLevelStageEvent` after translucent blocks; when `GogglesItem.reveals(player)` it draws, for each tracked duct with a facade within 32 blocks, the centre box and the connected arms as translucent quads through a render type with position colour shader, translucent transparency, no depth test and no culling.

### D6: Goggles
`GogglesItem extends Item implements Equipable` in Lib, head slot, `use` swaps into the slot. `GogglesItem.reveals(player)` is the shared rule. Recipe: leather, iron, glass. Added to the Servus tab after the wrench.

### D7: GameTest
`ServusLogisticsGameTests.facadeRecipeAndApplication`: recipe lookup with stone plus iron ingot yields six stone facades, a slab yields nothing; a duct accepts a facade and `removeFacade` gives it back.

## Pure Java vs Minecraft

All Minecraft. Verified by the Logistics GameTest, an RCON data merge of a facade that persists, and the client launch which exercises the model wrapping at startup.

## Risks / Trade-offs

- [Facade blocks with tinted or special models] -> Quads are taken from the block's own model with the duct's position, so tints resolve through the normal block colour path for most blocks.
- [Overlay cost] -> Only tracked ducts with facades near the player are drawn, only while revealing.

# Design

## Context

- The rack's model comes from datagen (`HeavyRackModels`): a multipart of part models per facing with two textures, `#steel` (posts, beams, edges: `heavy_rack_steel.png`, orange) and `#board` (back panel, floor, shelves: `heavy_rack_board.png`). `HeavyRackRenderer` only draws the 3D samples.
- The recipe is a plain shaped recipe with `ItemTags.PLANKS`, so the wood is lost.
- Precedent for a dynamic model: Logistics wraps the duct's baked model in `UtilityDuctModel` (`IDynamicBakedModel`) during `ModelEvent.ModifyBakingResult` and reads model data from the block entity. Logistics also has a special recipe (facades).
- NeoForge 21.1.255 sources (checked): `IDynamicBakedModel`, `IQuadTransformer` with vertex offsets, `ModelData`/`ModelProperty`, `BakedModel.getParticleIcon(ModelData)`; vanilla `ItemOverrides.resolve(...)` is subclassed by NeoForge's `DynamicFluidContainerModel` for per stack item models.

## Goals / Non-Goals

**Goals:** any planks wood on the boards, one block and one item, no texture generation per wood.

**Non-Goals:** crates; a choosable frame; perfect handling of animated or connected planks textures.

## Decisions

### D1. The wood is a data component and model data
`servus_storage:rack_wood` holds the planks block id (`ResourceLocation`, `ResourceLocation.CODEC` and `STREAM_CODEC`). `HeavyRackBlockEntity` reads it in `applyImplicitComponents`, writes it in `collectImplicitComponents` (so drops and the dismantle path keep it), saves it in NBT, sends it in the update tag, and exposes it through `getModelData()` as a `ModelProperty<ResourceLocation>`; a change calls `requestModelDataUpdate()`. Missing or unknown ids mean oak.
Alternative: one block per wood. Rejected by the user: no modded woods, twelve items.

### D2. A rack recipe that picks the wood
`HeavyRackRecipe extends ShapedRecipe` with its own serializer `servus_storage:heavy_rack` that reuses the shaped codec, so datagen and JEI see an ordinary shaped crafting recipe. `assemble` collects the planks stacks of the grid in reading order and asks pure `RackWood.pick(List<String>)`: the id that appears most often; on a tie, the middle entry. The result gets the component. `getResultItem` stays the plain rack (shows as oak).
`RackWood` is pure Java with JUnit: three equal, two to one in either order, three different (middle), a single entry, an empty list.

### D3. Retexturing baked model for the block
`HeavyRackModel implements IDynamicBakedModel` wraps the rack's baked model for every rack state, installed in `ModelEvent.ModifyBakingResult` like `UtilityDuctModel`. `getQuads` passes frame quads through and replaces every quad whose sprite is the board sprite with a copy whose UVs are remapped onto the wood sprite (per vertex: offset in the old sprite, then the same offset in the new one, through `TextureAtlasSprite.getU/getV` and the matching offset methods, verified in the 1.21.1 sources during apply). The wood sprite is the particle icon of the planks block's default model. Retextured quads are cached per wood, state, side and render type; the cache clears on resource reload. `getParticleIcon(ModelData)` returns the wood sprite, so breaking particles show the wood.

### D4. Item model per stack
The rack's item model is wrapped too; its `ItemOverrides` subclass resolves a stack to a retextured copy of the item model for that stack's wood (oak without one), cached per wood. `HeavyRackItem extends BlockItem` adds the tooltip line with the planks block's name.

### D5. Frame texture and defaults
A new generated texture `heavy_rack_iron.png` (blackened wrought iron, dark grey with a little highlight and rivets) replaces `heavy_rack_steel.png` in `HeavyRackModels`; the old file is removed. `heavy_rack_board.png` stays as the marker sprite the model swaps, repainted to look like oak so any path that misses the swap still looks right. The creative tab keeps one rack without a component (shows oak).

### Pure Java vs Minecraft
- Pure, JUnit: `RackWood.pick`.
- GameTest: assembling the recipe from a 3 by 3 grid records spruce, oak (majority) and spruce (middle of three); a placed item carries its wood into the block entity; `Block.getDrops` of a spruce rack carries spruce; a rack without the component reports oak.
- Manual: board and item look, particles, frame texture, modded wood in a pack.

## Risks / Trade-offs

- [Planks with animated or connected textures] → only the base particle sprite is used; they look plain. Accepted.
- [Quad cache growth with many woods] → bounded by woods times rack states; cleared on reload.
- [Another mod replacing the rack model] → the wrapper only acts on our own baked model; if it is replaced, the boards fall back to the oak-looking marker texture.

## Migration Plan

None, nothing is released. Placed racks show oak until rebuilt.

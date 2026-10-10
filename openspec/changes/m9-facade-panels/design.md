# Design

## Context

Current state, read from the code on 2026-10-10:

- `UtilityDuctBlockEntity` holds one `BlockState facade`, saved as the `Facade` tag. `UtilityDuctBlock.getShape` returns a full block for a facaded duct, `getAppearance` returns the facade, and `onWrench` with sneak removes the facade first, then the duct.
- `UtilityDuctModel` (an `IDynamicBakedModel`) returns the facade block's own quads, render types and particle when `DuctModelData.FACADE` is set, else it composes the duct from part models. `DuctBlockColor` hands tint indices below `DuctParts.RING_TINT_BASE` (100) to the facade block's color.
- Arms come from `UtilityDuctBlockEntity.computeArm(line, side)`: the line is present, the side mode is not `DISABLED`, and the neighbour is a duct with the line or offers the line's capability. `dataSideOpen` decides the data graph's sides.
- The item, energy and fluid networks are created with `ConnectionRule.always()`, so two neighbouring ducts with the same line are always in one network, whatever their arms show. A solid panel therefore needs a connection rule, not only an arm change.
- `DuctOverlayRenderer` draws ducts with `facade() != null`.
- `m9-facade-jei` added `FacadeItem.EXAMPLES`, the JEI subtype interpreter and one example recipe per block. This change builds on it.

## Goals / Non-Goals

**Goals:**
- One panel slot per face, solid or hollow, with the panel's block.
- A solid panel closes its face everywhere a side can connect: arms, endpoints, capabilities and the network graphs.
- Panels look like their block, including tint and overlay layers such as the grass side.

**Non-Goals:**
- Panels on non duct blocks, hollow back to solid, block rendering item icons, light blocking (see the proposal).

## Decisions

### D1. Panel data on the block entity

`UtilityDuctBlockEntity` replaces `facade` with `BlockState[] panels` (indexed by `Direction.ordinal()`, null when empty) and an `int hollowMask`. API: `panel(Direction)`, `isHollow(Direction)`, `hasPanels()`, `setPanel(Direction, BlockState, boolean hollow)`, `removePanel(Direction)` returning the item, and `closed(Direction)`, true for a solid panel. Saved as a `Panels` compound with one entry per face name holding `State` (via `NbtUtils.writeBlockState`) and `Hollow`. The old `Facade` tag is ignored on load: the project is unreleased, no migration.

Setting or removing a panel goes through the existing `changed()` path, which recomputes arms, refreshes the state flags, invalidates capabilities, syncs the client and tells neighbouring ducts.

### D2. Solid panels close the side

- `computeArm(line, side)` returns false when `closed(side)`, and, for a duct neighbour, when `neighbour.closed(side.getOpposite())`.
- `itemHandler(side)` and `fluidHandler(side)` return null for a closed side, so machines, pumps and their stubs see no handler there.
- `dataSideOpen(side)` also requires `!closed(side)`.
- The item, energy and fluid network types get a `ConnectionRule` `(from, fromNode, side, to, toNode) -> !fromNode.closed(direction(side))` instead of `always()`. The graph asks both nodes, so a panel on either touching face splits the networks.
- After a panel change the duct calls `NetworkManager.refresh(worldPosition, node)` for every joined line, the call `refreshData` already uses for the data line, and `DataNetwork.sidesChanged` for data. A neighbouring duct's view changes too, so the neighbour refreshes its edges as well.

A hollow panel changes none of these.

### D3. Placement and removal

`FacadeItem.useOn` places on `context.getClickedFace()` of a Utility Duct, fails when that face already has a panel, and keeps today's creative rule (no consumption in creative). One class serves both items: `FacadeItem(Properties, boolean hollow)`; the hollow item is `ServusLogisticsItems.HOLLOW_FACADE` (`hollow_facade`), with the same `FACADE_BLOCK` component and the name key `item.servus_foundation.hollow_facade.named` ("Hollow Facade (%s)").

Sneak carrot: the pure helper `PanelHit.face(x, y, z, clickedFace, panelMask)` (local hit coordinates in 0..1) returns the face whose one pixel slab contains the hit, preferring the clicked face, or none. With a panel there, `onWrench` drops that panel's item via `removePanel`; otherwise it removes the duct as today. `onRemove` drops every panel.

### D4. Shape

`UtilityDuctBlock.shape` adds a panel box per face: a 16 by 16 by 1 slab for solid, four boxes around an 8 by 8 hole (pixels 4 to 12, the thickest arm is radius 4) for hollow. The cache key grows past 32 bits with two six bit masks, so it becomes a `long`. The pure `DuctShapes.panel(side, hollow)` returns the boxes and is unit tested.

### D5. Rendering

`DuctModelData.PANELS` carries an immutable `PanelSet` (six states and the hollow mask) instead of `FACADE`. `UtilityDuctModel`:

- builds the duct's own quads as today,
- adds, per panel, quads baked with vanilla `FaceBakery.bakeQuad(from, to, BlockElementFace, sprite, direction, BlockModelRotation.X0_Y0, null, true)` for each box of `DuctShapes.panel`. For every box face the sprite and tint come from the panel block's own quads facing that direction (`getQuads(state, direction, ...)` then the unculled ones), one baked quad per source quad, so overlay layers like the grass side survive. UVs are the box's pixel extent on that face, as vanilla picks them for a block element.
- caches the baked panel quads per (block state, face, hollow) in a `ConcurrentHashMap`, cleared with the model on reload.
- `getRenderTypes` returns the union of the duct's types and each panel block's types; `getQuads` for a render type emits duct quads only in the duct's types and a panel's quads only in that panel block's types.

`FaceBakery`, `BlockElementFace(Direction cull, int tintIndex, String texture, BlockFaceUV uv)` and `BlockFaceUV(float[] uvs, int rotation)` were checked in `neoforge-21.1.255-sources.jar`.

Tint: a tinted source quad gets the tint index `PANEL_TINT_BASE + face.ordinal() * 16 + original` with `PANEL_TINT_BASE = 200`, clear of the ring range at 100. `DuctBlockColor` decodes the face, looks up that panel's block and asks the vanilla `BlockColors` with the original index.

`getAppearance(state, level, pos, side, ...)` returns the panel block when `side` carries a solid panel, else the duct.

### D6. Overlay

`DuctOverlayRenderer` draws ducts where `hasPanels()` instead of `facade() != null`.

### D7. Hollow Facade recipe

`HollowFacadeRecipe extends CustomRecipe`, registered beside `FacadeRecipe` in `ServusLogisticsRecipes` and saved by `SpecialRecipeBuilder` in `LogisticsDataParts`. It matches a three by three input whose centre is empty and whose other eight slots hold solid Facades of one block, and assembles eight Hollow Facades of that block. Counts stay in the recipe class like `FacadeRecipe.RESULT_COUNT`; they are a crafting shape, not balance.

### D8. Examples and JEI

`FacadeItem.hollowExamples()` gives one Hollow Facade per `EXAMPLES` block. The creative tab adds them after the Facades. The JEI plugin registers the subtype interpreter for `HOLLOW_FACADE` too, adds one `ShapedRecipe` example per block (`"FFF", "F F", "FFF"` with that block's Facade stack as an ingredient via `DataComponentIngredient` or a strict `Ingredient.of(stack)`), and includes the hollow stacks in the information page. The info text gains a sentence on solid closing and hollow passing.

### PLAN.md open decisions

None of section 5's decisions are touched. Section 2's "Solid or hollow variants" is settled: per face panels, solid closes the side, hollow passes. M9 gets item 1d.

### Pure versus Minecraft

- Pure Java with JUnit: `PanelHit` (slab picking, clicked face preferred, no panel gives none) and `DuctShapes.panel` (solid slab extent per face, hollow frame leaves the 4 to 12 hole open, boxes inside the block).
- GameTests in `ServusLogisticsGameTests`: panel on the clicked face and no second panel; solid panel between two item ducts splits the item network and removes both arms, removing it joins them again; solid panel toward a chest removes the endpoint and the item handler on that side; hollow panel keeps the network; sneak carrot removes one panel; breaking drops lines and both panel kinds; the hollow ring recipe and a mixed ring; `facadeRecipeAndApplication` rewritten for panels.
- Manual in game: panel looks (stone, grass tint, glass not allowed), hollow arm through the hole, overlay, outlines.

## Risks / Trade-offs

- [Baking panel quads from another block's model can miss blocks whose models have no quads facing a direction] → Fall back to the block model's particle sprite for that face.
- [Rotated block textures such as logs show their default orientation] → Accepted; the panel uses the block's default state.
- [Graph refresh on a neighbour that did not change could be costly] → Only the two ducts touching the changed face refresh, on a player action, never per tick.
- [Players who used whole-block facades see them vanish] → Unreleased project, no migration (see the proposal).
- [The gamedata export and the docs recipe graph only know the facade recipe as special] → The hollow recipe is a `CustomRecipe` too, so the exporter marks it special in the same way; the docs Foundation page gets one line on panels.

# Design

## Context

After `m10-shuttle-tiers`:
- The tier is the item (`shuttle` for Mk1, `shuttle_mk2`, `shuttle_mk3`, all `ShuttleItem`); Speed, Handling and the tool sit in the `shuttle_modules` component (`ShuttleModules(Optional<Item> tool, int speed, int handling)`).
- `ShuttleView` carries the shuttle's item stack, and `ShuttleGarageRenderer` draws that stack with `ItemRenderer.renderStatic(stack, ItemDisplayContext.NONE, ...)` at `CART_SCALE` 0.6 with the cargo item at 0.3 on top.
- The three item models are datagen output: Mk1 a 12 by 3 by 12 slab with `item/shuttle`, Mk2 and Mk3 flat `basicItem` icons.
- The Heavy Duty Rack already shows the pattern this change reuses: a `ModelEvent.ModifyBakingResult` wrapper whose `ItemOverrides.resolve` picks a per stack model (`HeavyRackModel`).

## Goals / Non-Goals

**Goals:**
- One composition path for both places a shuttle shows: the rail renderer and every item display go through the item model, so they can never disagree.
- Look steps in pure Java with JUnit tests.
- No server or sync change.

**Non-Goals:**
- Animated parts, the name plate quad (change 3 draws it on the reserved spot), final textures.

## Decisions

### D1. The item model composes the parts
`ShuttleItemModel extends BakedModelWrapper<BakedModel>` wraps the baked inventory model of each shuttle item in `ModelEvent.ModifyBakingResult` (keys `ModelResourceLocation.inventory(servus_storage:shuttle | shuttle_mk2 | shuttle_mk3)`). Its `getOverrides()` returns an `ItemOverrides` subclass whose `resolve(model, stack, level, entity, seed)` computes a `ShuttleLook.Key(tier, speedStep, handlingStep, toolId)` from the stack and returns a cached `ComposedShuttleModel` for that key. The composed model wraps the base (transforms, particle, gui light) and returns, per side, the concatenated quads of its part models; its overrides are `ItemOverrides.EMPTY`, so resolving stops there. A resource reload bakes a new wrapper, which drops the cache.

Because `ItemRenderer.renderStatic` resolves overrides for every call, the garage renderer, inventories, the workshop slot, held and dropped items all show the same composition with no extra code.

Alternatives considered:
- A `BlockEntityWithoutLevelRenderer` through `IClientItemExtensions.getCustomRenderer` and `RegisterClientExtensionsEvent` (used by Manufacturing's `AlchemyClient`). Rejected: it needs a `builtin/entity` item model, draws every frame without baked quad caching and needs its own transforms; the override route reuses vanilla item transforms and the existing rack pattern.
- Drawing parts one by one in `ShuttleGarageRenderer` with `ModelBlockRenderer.renderModel` (as `SinkRenderer` does). Rejected: the inventory item would then need a second path.

APIs, checked in `neoforge-21.1.255-sources.jar`: `ModelEvent.RegisterAdditional#register(ModelResourceLocation)`, `ModelResourceLocation.standalone(ResourceLocation)` and `ModelResourceLocation.inventory(ResourceLocation)`, `ModelEvent.ModifyBakingResult#getModels()`, `ModelManager#getModel(ModelResourceLocation)` and `getMissingModel()`, `net.neoforged.neoforge.client.model.BakedModelWrapper`, `ItemOverrides#resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)` and `ItemOverrides.EMPTY`.

### D2. Part models
Standalone models under `assets/servus_storage/models/shuttle/`, made by a new datagen helper `ShuttleModels` (like `ElevatorModels`), all with parent `minecraft:block/block` and registered in `ModelEvent.RegisterAdditional`:

| Part | Models |
|---|---|
| Body and hold | `body_mk1` (chassis plate and an open crate of four walls), `body_mk2` (chassis and a closed box), `body_mk3` (chassis, closed box, gold corner caps) |
| Drive | `drive_0` four wheels, `drive_1` six wheels, `drive_2` tracks, `drive_3` tracks with two exhaust stacks |
| Handling | `handling_1` loading mast and forks at the back, `handling_2` the mast with a crane beam and hook over the hold |
| Tool | `tool_<id>` per tool module id; `tool_harvester` a reel and cutter bar across the front |

Coordinates: the body spans x and z 2 to 14 (12 pixels), wheels and tracks x 1 to 15, height up to 13 with the crane; model +z is the front. Only a tool goes outside x 1 to 15 (the reel spans x -2 to 18) and past the front (z up to 18). The name plate spot is the left side of the hold (x = 2, y 5 to 8, z 5 to 11), kept free of parts; change 3 draws its plate there.

A tool without a `tool_<id>` model (the missing model) is skipped, so a future tool shows nothing until it gets one.

Textures: vanilla block textures as placeholders (`oak_planks`, `iron_block`, `diamond_block`, `gold_block`, `smooth_stone`, `coal_block`, `blackstone`, `copper_block`, `yellow_terracotta`, `hay_block_side`, `iron_bars`), so no new PNGs. The old `item/shuttle`, `item/shuttle_mk2` and `item/shuttle_mk3` textures go away. The three item models become `parent: servus_storage:shuttle/body_mk<n>`, so the base model already shows the body and carries the `block/block` display transforms (GUI 30/225 at 0.625).

### D3. Look steps
Pure `ShuttleLook` in `storage/shuttle/module`: `speedStep(count)` gives 0 for 0, 1 for 1 to 2, 2 for 3 to 5, 3 for 6 or more; `handlingStep(count)` gives 0 for 0, 1 for 1 to 3, 2 for 4 or more; `openHold(tier)` is true for tier 1 only; `Key(int tier, int speedStep, int handlingStep, String tool)` with `of(tier, speed, handling, tool)`. The steps are visual, not balance, so they live in code, not in `shuttle.json`; a pack that changes max counts still gets sensible looks because the last step is open ended.

### D4. Renderer
`ShuttleGarageRenderer` draws the stack at full scale (`ItemDisplayContext.NONE`, model space equals block space) with its bottom on the rail top, and the cargo item (0.35 scale, `FIXED`) inside the crate only when `ShuttleLook.openHold(tier)`. `PARK_SPACING` grows from 0.15 to 0.25. The heading becomes `atan2(dx, dz)`: the old `atan2(-dx, dz)` turned east and west legs backwards, invisible on the symmetric slab but wrong with a front tool.

### PLAN.md open decisions
Section 5 "Art style and texture direction" stays open; this change uses vanilla textures as placeholders and records the part layout, so final art only swaps textures.

## Pure logic and tests

- JUnit `ShuttleLookTest`: speed and handling step boundaries (0, 1, 2, 3, 5, 6, 8 and 0, 1, 3, 4, 8), negative counts as step 0, open hold per tier, key equality.
- Minecraft side (model wrapper, renderer) is client only and checked in the final user play check; the storage GameTests must stay green as a guard that nothing server side moved.

## Risks / Trade-offs

- [Composed quads are built once per look key] → about 4 by 4 by 3 by tools keys at most, cached per bake; cheap.
- [Vanilla textures look like blocks, not machines] → acceptable as placeholders; textures are one key per part in datagen.
- [Item override models do not get block entity style lighting per part] → the rail renderer passes the light at the rail, as before.

## Migration Plan

Unreleased: none.

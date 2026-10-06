# Design

## Context

See proposal.md for motivation. Today `CrateBlockEntity` holds one `type` stack and an `int count`, with `insert(stack, simulate)`, `extract(amount, simulate)`, `matches`, a double click tracker per player, a throttled client sync every ten ticks and NBT keys `Type` and `Count`. `CrateItemHandler` is one virtual slot. `CrateBlock` handles right click and the left click comes from `ServusStorageEvents.onLeftClickBlock`, which cancels breaking while the crate holds items. `RackBlockEntity` is an `ItemStackHandler` of four slots; `RackCompartments` is a pure helper that maps a hit on the front face to a compartment index from the block's 2D facing and has `centreFromLeft` and `centreY` for the renderer, with `RackCompartmentsTest`. `CrateRenderer` and `RackRenderer` draw on the front face through the same pose setup. `WarehouseNetwork.insert` prefers empty crates through `CrateBlockEntity.isEmpty`. Storage blocks carry `FACING` and the `ONLINE` lamp property, datagen writes two orientable models per block.

## Goals / Non-Goals

**Goals:**
- One code path for all three crates. The compartment count is the only difference.
- Keep the crate's protection and bulk feel; drop the rack's spill and stack limits.
- Keep the pure hit mapping pure and tested.

**Non-Goals:**
- Changing the warehouse index or the terminal. They already read any item handler.

## Decisions

### One block class with a compartment count
`CrateBlock(Properties, int compartments)` keeps `CODEC` through `simpleCodec` by registering three blocks with lambdas that fix the count: `crate` with 1, `double_crate` with 2, `quad_crate` with 4. `simpleCodec` needs a one argument factory, so each block gets its own small subclass `DoubleCrateBlock`, `QuadCrateBlock` that only passes the count, or the codec is built with `RecordCodecBuilder` over the count. Subclasses are simpler and keep `CrateBlock` as the type for all three. `compartments()` is read by the block entity from its block state, so one `BlockEntityType` created with `.of(CrateBlockEntity::new, CRATE, DOUBLE_CRATE, QUAD_CRATE)` serves all three.

### Compartments in the block entity
`CrateBlockEntity` holds `ItemStack[] types` and `int[] counts` sized by `compartments()`. `capacityPerCompartment()` is `crateCapacity / compartments`. API: `insert(int compartment, ItemStack, boolean simulate)`, `insert(ItemStack, boolean simulate)` which tries matching compartments first then empty ones, `extract(int compartment, int amount, boolean simulate)`, `type(int)`, `count(int)`, `isEmpty(int)`, `isEmpty()` for all, `count()` as the total for callers that only care about one compartment crates such as the GameTests. NBT: a list `Compartments` of compounds with `Type` and `Count`; the old `Type` and `Count` keys are not read, nothing is released. `registerRightClick` tracks the double click per player and compartment.

### Pure compartment mapping
`CrateCompartments` in the `crate` package replaces `RackCompartments`: `index(int compartments, int facing2d, double x, double y, double z, boolean frontFace)` returns 0 for one compartment or when the hit is not on the front face, left or right for two, the quadrant for four; `centreFromLeft(compartments, index)` and `centreY(compartments, index)` give display positions (two compartments sit at a quarter and three quarters across, mid height). `CrateCompartmentsTest` covers each count, each facing and the front face check. `RackCompartments` and its test are deleted.

### Interaction
`CrateBlock.useItemOn` maps the hit to a compartment and inserts there, with the double click moving matching inventory stacks into that compartment. `ServusStorageEvents.onLeftClickBlock` maps the hit the same way and extracts from that compartment; it keeps cancelling the break while `isEmpty()` is false. Side faces map to compartment 0 for taking and to "first matching, then first empty" for inserting.

### Handler and renderer
`CrateItemHandler.getSlots()` returns the compartment count, each slot delegating to its compartment. `CrateRenderer` loops over compartments and draws item and count at the pure helper's centres, scaled 0.5 for one, 0.35 for two and four compartments, with the count text under the item. `RackRenderer` is deleted and the renderer registration points the single block entity type at `CrateRenderer`.

### Datagen, textures, data
Three blockstates through the existing lamp aware `oriented` helper, with `double_crate` and `quad_crate` front textures drawn by a scratchpad script from the crate front plus one vertical divider or a cross, each with the dark lamp and a lit copy. Sides and top reuse the crate textures by passing the crate name for them. Recipes: crate unchanged, `double_crate` shapeless from a crate and a stick, `quad_crate` shapeless from a crate and two sticks. Loot: self dropping with contents kept by the existing dismantle path. Tags: axe mineable. Lang: "Crate", "Double Crate", "Quad Crate". The `rack` recipe, loot, tag and lang entries go, and `rack.png` textures are deleted.

### Warehouse insert preference
`WarehouseNetwork.insert` keeps preferring crates with `isEmpty()` true. A crate with a free compartment counts as "other storage node" and is still used after empty crates.

## Pure versus Minecraft logic

Pure: `CrateCompartments` with `CrateCompartmentsTest`. Minecraft: block, block entity, handler, events, renderer, datagen, GameTests (the existing ones use one compartment crates and keep passing through `insert(stack, simulate)`, `isEmpty()` and `count()`).

## Risks / Trade-offs

- [Existing dev worlds hold racks] -> They turn into air, nothing is released.
- [Hit mapping on the side faces] -> Side faces always mean the whole crate; only the front face picks a compartment.
- [Capacity split makes the quad crate small] -> 1024 per compartment is still sixteen stacks, and the number is one JSON value.

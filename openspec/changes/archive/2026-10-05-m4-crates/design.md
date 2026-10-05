# Design

## Context

`servus_storage` is an empty mod. Lib offers the wrench, side types, team hook and the network manager. See proposal.md.

APIs verified: `EntityRenderersEvent.RegisterRenderers.registerBlockEntityRenderer`, `BlockEntityRenderer.render`, `ItemRenderer.renderStatic`, `Font.drawInBatch`, `PlayerInteractEvent.LeftClickBlock` (cancellable, `getAction`), `BlockBehaviour.attack`, `ItemTags.PLANKS`.

## Decisions

### D1: Storage plumbing
`ServusStorage` entrypoint registers blocks, items, block entities and the warehouse network type. `ServusStorageEvents` adds items to the Servus tab, registers capabilities and the crate stats reload listener. Datagen mirrors Manufacturing: blockstate, item model, loot, tag, recipe and lang providers.

### D2: Crate storage model
`CrateBlockEntity extends StorageNodeBlockEntity` with `ItemStack type` (count 1 template) and `int count`. `CrateItemHandler` is one virtual slot: `getStackInSlot` returns the type with the full count (limit is the capacity), `insertItem` accepts matching items while there is room, `extractItem` gives up to the item's max stack size. Capacity from `CrateStatsManager` (directory `storage`, file `crate.json`, field `capacity`). Changes mark the block entity dirty and schedule a client sync; a server ticker flushes at most every 10 ticks through `sendBlockUpdated` with an update tag carrying type and count.

### D3: Interaction
`CrateBlock.useItemOn` handles insertion and the double click (last click game time per player stored in the block entity). Left click is handled in a `LeftClickBlock` listener: when the crate holds items and the player is not sneaking the event is cancelled and the crate hands out a stack (one when sneaking is impossible here, so the one item case uses sneak with items: sneak left click gives one item and is also cancelled); an empty crate or sneak with an empty hand lets vanilla break it. Breaking drops the contents through `onRemove`; sneak wrench uses `saveToItem` like machines.

### D4: Front display
`CrateRenderer` draws the item flat on the front face with `renderStatic` in `FIXED` context scaled to half a block and the count below it with `Font.drawInBatch`, both offset slightly off the face to avoid z fighting. Light from `LevelRenderer.getLightColor` at the position in front.

### D5: Network node
`WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, Object>` with `ConnectionRule.always()`, no behavior yet, registered as `servus_storage:warehouse`. `StorageNodeBlockEntity extends NetworkBlockEntity<StorageNodeBlockEntity>`.

### D6: Recipe and assets
Crafting: planks around a chest. Textures: wooden sides and a front with a frame; model orientable.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: hopper into crate, hopper out of crate, capacity refusal via `data merge` of a near full count; client launch for the renderer registration.

## Risks / Trade-offs

- [Counts above 64 in a single handler slot] -> Standard practice for bulk storage; machines and ducts use insert and extract calls only.
- [Left click handling through an event] -> The only way to stop creative instant break; documented sneak rule for breaking.

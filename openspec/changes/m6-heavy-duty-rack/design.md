# Design

## Context

See proposal.md for motivation. Storage blocks are `BaseEntityBlock`s with `FACING` and the `ONLINE` lamp property, block entities extend `StorageNodeBlockEntity` (kind `STOCK` by default) and the crate throttles its client sync to every ten ticks. `CounterTerminalMenu` and `CounterTerminalScreen` show the menu and screen pattern of the mod, registered through `ServusStorageMenus` with `IMenuTypeExtension.create` and `RegisterMenuScreensEvent`. `StorageStatsManager` loads `data/<ns>/storage/*.json` into typed fields. `ElevatorModels` shows the multipart approach for frames whose parts depend on connection flags. `ServusStorageBlockTagsProvider` and the loot provider list every block. The crate family change frees the `rack` package and deletes the old `RackRenderer`.

## Goals / Non-Goals

**Goals:**
- A chest that looks like a warehouse bay and reads at a glance.
- Cheap display: the server picks the sample, the client only draws it.

**Non-Goals:**
- Multiblock inventory, sorting, filters.

## Decisions

### Block and connection flags
`HeavyRackBlock extends BaseEntityBlock implements Wrenchable` with `FACING`, `ONLINE` and four boolean properties `LEFT`, `RIGHT`, `UP`, `DOWN`. Left and right are relative to the facing: `LEFT` is set when the neighbour on `facing.getCounterClockWise()` is a rack with the same facing, `RIGHT` on `facing.getClockWise()`, `UP` and `DOWN` when the neighbour above or below is a rack with the same facing. `getStateForPlacement` and `updateShape` compute them, like `ConnectableBlock` does for its flags. Rotation with the Configuration Carrot keeps `ONLINE` and recomputes the flags through a block update. `getShape` is the full block so the GUI click and the dock arm behave like for a crate.

### Block entity, sync and the sample
`HeavyRackBlockEntity extends StorageNodeBlockEntity` with an `ItemStackHandler` sized from `StorageStatsManager.rackSlots()` and the same throttled sync as the crate. The update tag carries only the sample: a list `Display` of up to `display_count` one item stacks. The server computes it in `changed()` through the pure `TopCounts.pick(counts, n)`: counts are gathered per item key in slot order, and `pick` returns the keys with the highest totals, ties broken by first appearance. `displayed()` returns the list on the client for the renderer. The full inventory goes into `saveAdditional` and the dismantle item, not into the update tag.

### Menu and screen
`HeavyRackMenu extends AbstractContainerMenu` with `SlotItemHandler` slots in rows of nine for the rack, then the player inventory and hotbar, `quickMoveStack` moving between the two halves, `stillValid` by distance. Opened from `useWithoutItem` through `player.openMenu` with the block position written for `fromNetwork`. `HeavyRackScreen extends AbstractContainerScreen` draws the vanilla `textures/gui/container/generic_54.png` cropped to the row count, exactly as vanilla chests do, so no new GUI texture is needed.

### Renderer
`HeavyRackRenderer` draws `displayed()` on the front face: two shelf levels, two items per level, left to right then top to bottom, scaled 0.3, using the same pose setup as `CrateRenderer` with the fixed facing rotation. No text.

### Datagen and look
`HeavyRackModels` writes a multipart blockstate: `heavy_rack_shelves` always (back panel, floor board and a middle shelf board between the posts), `heavy_rack_post_left` unless `LEFT`, `heavy_rack_post_right` unless `RIGHT`, `heavy_rack_top` unless `UP`, `heavy_rack_bottom` unless `DOWN`, all rotated by the facing like `horizontalBlock` does, and the lamp as a small `heavy_rack_lamp` part with the `ONLINE` texture variant. The item model is the shelves plus both posts. Textures: `heavy_rack_steel` for posts and beams (dark orange like real pallet racks), `heavy_rack_board` for shelves, drawn by a scratchpad script.

### Registration and data
- `ServusStorageBlocks.HEAVY_RACK`: id `heavy_rack`, `MapColor.COLOR_ORANGE`, strength 3.0, `SoundType.METAL`, `noOcclusion()`.
- `ServusStorageItems.HEAVY_RACK` in the tab after the crates. `ServusStorageBlockEntities.HEAVY_RACK`. `ServusStorageMenus.HEAVY_RACK`.
- `rack.json`: `{"slots": 27, "display_count": 4}` loaded by `StorageStatsManager` into `rackSlots()` and `rackDisplayCount()`.
- Recipe: `"I I"`, `"PPP"`, `"I I"` with iron ingots and planks, output 1, unlocked by having iron ingots. Loot: self dropping with contents on dismantle through `saveToItem`; plain breaking spills through `onRemove` like a chest. Tag: pickaxe mineable. Lang: "Heavy Duty Rack".

### GameTest
`heavyRackIndexesAndDisplays`: a rack beside a controller and a terminal, five item types inserted through the handler with counts 64, 32, 16, 8, 1; after 10 ticks the terminal lists all five, the lamp is lit and `displayed()` on the server side block entity holds the top four in order; then 100 apples are inserted and after 10 more ticks the sample holds apples first and no gravel.

## Pure versus Minecraft logic

Pure: `TopCounts` with `TopCountsTest` (fewer types than n, more types than n, ties by order, empty). Minecraft: block, block entity, menu, screen, renderer, datagen, GameTest and a client check of the connected look.

## Risks / Trade-offs

- [Sample recomputed on every content change] -> One pass over 27 slots per change, after the ten tick sync throttle. Cheap.
- [Seam posts vanish but models overlap at beams] -> Beams end at the block edge; two touching beams read as one.
- [Rotating a rack in a row breaks the row look] -> Intended, the flags require the same facing.

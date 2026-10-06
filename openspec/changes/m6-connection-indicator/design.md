# Design

## Context

See proposal.md for motivation. The four storage blocks are `BaseEntityBlock`s with only a `FACING` property and orientable models from `ServusStorageBlockStateProvider.oriented`, which calls `horizontalBlock` and `simpleBlockItem`. Their block entities extend `StorageNodeBlockEntity`. `WarehouseNetwork` knows its status from the controller count and learns about membership through `onNodeAdded`, `onNodeRemoved`, `onAbsorb` and the Lib split handover; it ticks once per server tick through `onTick`. `RailBlock.connectsTo` accepts horizontal `RailBlock` and `ElevatorBlock` neighbours. Front textures are 16 by 16 RGBA PNGs drawn by scratchpad scripts.

## Goals / Non-Goals

**Goals:**
- Zero per tick cost when nothing changes. State pushes happen only on membership or controller changes.
- No block entity renderer work. The lamp is two textures and a block state.

**Non-Goals:**
- Lamp on connectors, reveal overlays, colour coding of failure reasons.

## Decisions

### A shared ONLINE block state property
`StorageNodeBlocks.ONLINE = BooleanProperty.create("online")` in the `network` package. `CrateBlock`, `RackBlock`, `WarehouseControllerBlock` and `CounterTerminalBlock` add it to their state definition with default false. Placement keeps false; the network sets it. Alternative considered: a synced field on the block entity plus renderer. Rejected, a block state needs no packet code and no renderer and the model swap is free.

### The network pushes the state
`WarehouseNetwork` gets a `lampsDirty` flag set in `onNodeAdded`, `onNodeRemoved` and `onAbsorb`, which also cover every controller count change. In `onTick`, when the flag is set and `level` is known, it computes `online = status() == ONLINE`, then for every member position that is not a connector and whose chunk is loaded (`level.isLoaded(pos)`), reads the state and, when it has `ONLINE` with a different value, calls `level.setBlock(pos, state.setValue(ONLINE, online), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)`. Those flags skip neighbour updates and neighbour shape updates, so no `NeighborsChanged` events bounce back and the rails beside the block are not re evaluated. The flag clears afterwards. Nodes that leave a network (block broken) need nothing; nodes moved by a split get `onNodeAdded` on their new network through the Lib handover, which marks that network dirty. Cost is one pass over the members per change, never per tick.

### Rail docks onto storage blocks
`RailBlock.connectsTo` for horizontal directions: `RailBlock` or `ElevatorBlock` neighbour, or `level.getBlockEntity(neighborPos) instanceof StorageNodeBlockEntity node && node.holdsStock()`. `getStateForPlacement` and `updateShape` both have a `LevelAccessor`, and the neighbour's block entity exists by the time neighbours are notified. The rail arm is the same model as towards a rail; no new model.

### Datagen and textures
`oriented` becomes `orientedWithLamp`: two orientable models per block, `<name>` with `<name>_front` and `<name>_online` with `<name>_front_online`, and a variant builder over `FACING` and `ONLINE` using the same rotation rule as `horizontalBlock` (`toYRot + 180`). The item uses the dark model. A scratchpad script edits the four existing front PNGs to add a dark lamp and writes the four lit copies: crate, controller and terminal get a 2 by 2 lamp at pixels (13, 1) to (14, 2), the rack gets it at the centre of its divider cross, pixels (7, 7) to (8, 8), where no compartment icon is drawn. Dark lamp `(40, 44, 48)`, lit lamp `(96, 232, 96)`.

### Tests
The two existing storage GameTests gain assertions: after the warehouse is online the crate state has `ONLINE` true; after the rail or segment is cut the crate state has `ONLINE` false and the terminal keeps true. No new pure logic.

## Pure versus Minecraft logic

Everything touches block states, the level or datagen. Verified by the GameTests and a client check of the lamp textures and rail arms.

## Risks / Trade-offs

- [setBlock on many members after a change] -> Only on membership or controller changes, which are player actions. A thousand block warehouse costs a thousand state checks once per such action.
- [Block state change rewrites the block entity] -> `setBlock` with the same block keeps the block entity. Contents stay.
- [Rail arm towards a block that is not actually in the network] -> Cannot happen: the graph rule is always connect, so any touching storage node is in the rail's network.

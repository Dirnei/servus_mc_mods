# Design

## Context

Servus Lib already has everything a cable network needs except a block: `NetworkGraph` with split and merge, `NetworkSet`, `ServusNetwork` with a `Mailbox`, `NetworkManager` driven by `NetworkEvents`, `NetworkBlockEntity` that joins on load and leaves on removal or chunk unload, `ConnectableBlock` with six connection flags and `ConnectableModels` for the multipart model. The Utility Duct (Logistics) is the reference: ducts are graph nodes, the things they touch are endpoints kept per duct in the network, each with a `BlockCapabilityCache`.

The production and stock capabilities exist in `lib/production` (`ServusLibCapabilities.PRODUCTION`, `STOCK_PROVIDER`, `STOCK_SINK`). Today they are exposed by the Assembler (Manufacturing), the Crafting Station and the Warehouse Controller (Storage). Lib registers items only; it has no block or block entity register yet.

See proposal.md for why and specs/foundation/data-cable/spec.md for the behavior.

## Goals / Non-Goals

**Goals:**
- A data network that later M8 changes can query for producers and stock without knowing any content mod.
- Membership kept up to date by events and capability invalidation, never by a per tick scan.
- The first block in Lib, registered and datagenned like the content mods do it.

**Non-Goals:**
- Any traffic on the network (packets come in `m8-cable-packets`). `DataNetwork.onTick` does nothing in this change.
- A `data_link` capability (comes with `m8-access-point`).

## Decisions

### Package and classes
New package `io.github.dirnei.mc.servus.lib.data`:
- `DataCableBlock extends ConnectableBlock` (radius 2, a 4 by 4 core, thinner than the duct so the two read differently).
- `DataCableBlockEntity extends NetworkBlockEntity<DataCableBlockEntity>`; the node is the block entity itself, like the duct.
- `DataNetwork extends ServusNetwork<DataCableBlockEntity, DataEvent>` with `TYPE`, registered in the `ServusLib` constructor through `NetworkTypes.register`.
- `DataEvent` sealed interface with `SidesChanged(NodePos cable)`.
- `DataRole` enum `PRODUCER`, `STOCK` (pure).
- `DataMembers` (pure, no Minecraft imports): bookkeeping of which cable sides touch which member position with which roles. Adding and removing a touch keeps one entry per member position, roles are the union over its touches, the entry disappears with its last touch. JUnit tested.
- `DataNetworks`: the query API. `networkAt(ServerLevel, BlockPos)` returns the network of a cable at that position or, failing that, the first network (lowest id) that lists that position as a member; `DataNetwork.dataMembers()` and `dataMembers(DataRole)` (`members()` is the final graph view of `ServusNetwork`) return immutable `DataMember(BlockPos pos, Set<DataRole> roles)` records.

Alternative considered: making members graph nodes too. Rejected: members are foreign block entities that do not implement `NetworkNode`, a member can touch two networks, and the duct already proves the endpoint pattern.

### Member detection
For every side of a cable, the network creates three `BlockCapabilityCache`s (production, stock provider, stock sink) for the neighbour, with context `side.getOpposite()`, an `isValid` supplier tied to the cable block entity and an invalidation listener that posts `SidesChanged(cable)` to the mailbox. Handling `SidesChanged` re-reads that cable's six sides and updates `DataMembers`. `onNodeAdded` builds the caches and reads the sides once; `onNodeRemoved` drops the cable's touches and caches; `onAbsorb` copies the other network's members and caches.

Why invalidation listeners: NeoForge invalidates block capabilities when a block or block entity changes and when chunks load or unload (`Level.invalidateCapabilities`, called by vanilla hooks NeoForge patches in). The listener must not query right away (NeoForge docs: "Block Capabilities", caching section, 21.1), so it only posts to the mailbox and the query happens next tick on the server thread. This covers "member placed", "member broken" and "member chunk unloads" without `neighborChanged` bookkeeping and without a per tick scan.

API reference: `net.neoforged.neoforge.capabilities.BlockCapabilityCache#create(BlockCapability, ServerLevel, BlockPos, Object, BooleanSupplier, Runnable)` in NeoForge 21.1.x, as already used by `ItemChannelNetwork` and `WarehouseNetwork`.

### Arms toward members
`DataCableBlock.connectsTo` returns true for a neighbouring `DataCableBlock` and, when the accessor is a server `Level`, for a neighbour that exposes any of the three capabilities on the facing side. On the client and in world generation accessors it keeps the current flag, so the state is decided on the server and synced. `neighborChanged` recomputes the six flags like `UtilityDuctBlock.refreshConnections`; a newly placed member's block entity already exists when its neighbours are updated. The graph's `ConnectionRule` links cable to cable only; arms toward members are visual and members live in `DataMembers`.

### Summary on click
`DataCableBlock.useWithoutItem` on the server looks up the network and sends `Component.translatable("message.servus_lib.data_cable.summary", cables, producers, stock)` with `player.displayClientMessage(..., true)`. Producers and stock are counted by role, so a member with both roles counts in both.

### Placement assist
`DataCableAssist implements PlacementAssist`: any Data Cable starts and continues a run; the direction comes from `PlacementAssists.alongRun` over the arms toward other cables, else the clicked face. The item is an `AssistedBlockItem`.

### Registration and datagen
- `registry/ServusLibBlocks` (`DeferredRegister.Blocks`) and `registry/ServusLibBlockEntities` (`DeferredRegister<BlockEntityType<?>>`), registered in the `ServusLib` constructor before items; the cable item goes into `ServusLibItems` and the Lib creative tab.
- New datagen providers in `lib/datagen`: `ServusLibBlockStateProvider` (calls `ConnectableModels.connectable(..., 2, true)`), `ServusLibBlockLoot` via `LootTableProvider` (drop self), `ServusLibBlockTagsProvider` (`minecraft:mineable/pickaxe`), plus the shaped recipe in `ServusLibRecipeProvider` (copper ingot, redstone, copper ingot gives 4) and lang entries for the block and the summary.
- Texture `assets/servus_lib/textures/block/data_cable.png` as a placeholder made with the session PNG helper.
- Block properties: copper sound, strength like the duct, `noOcclusion`.

### Testing
- Pure Java, JUnit in `lib/src/test`: `DataMembersTest` (one touch, two touches of one member, union of roles, last touch removes the member, two members).
- Minecraft side, GameTests in a new `DataCableGameTests` class in Storage because Lib has no member blocks: cable row forms one network and splits; Crafting Station is a producer and Warehouse Controller a stock member; chest is not a member and gets no arm; member broken leaves the list (after one tick); two cables touching one controller give one member; linked controller and station move nothing over 100 ticks; `DataNetworks.networkAt` from a member position and from stone.
- Manual in game: summary text, arms, placement assist, Lib only run, chunk unload of a member.

### PLAN.md open decisions
None of section 5 is settled here. The change follows PLAN 2.4 (data cable in Lib, members found by capability) as written.

## Risks / Trade-offs

- [Capability lookups during `updateShape` for arms] -> only on a server `Level`, only three cheap lookups per changed side, never per tick.
- [A member that changes its capabilities without a block change, so no invalidation fires] -> such a block must call `Level.invalidateCapabilities(pos)`, which is the NeoForge contract anyway; the Servus blocks never change theirs.
- [Member arms are visual only; a cable that shows an arm while the member list is one tick behind] -> acceptable, the list catches up on the next tick.
- [`DataNetworks.networkAt` from a member position walks every data network of the level] -> networks are few; a position index can come later if a profile shows a cost.
- [Lib now ships a block, so Lib alone adds content to a pack] -> intended: the cable is the shared wiring, and PLAN.md section 3 lists it in the Lib API.

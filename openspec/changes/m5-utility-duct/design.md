# Design

## Context

Lib offers the network manager (`join`, `leave`, `refresh` with `NetworkNode`), `ServusNetwork` with mailbox and hooks, `EnergyDistributor`, `SideMode`, the wrench. Manufacturing has `ConnectableBlock`, `ItemDuctNetwork` and `PowerCableNetwork` as patterns. See proposal.md.

## Decisions

### D1: Lib move
`ConnectableBlock` moves to `io.github.dirnei.mc.servus.lib.block`; `ConnectableModels.connectable(provider, block, name, radius, withItem)` in `lib/datagen` holds the multipart generation. Manufacturing's provider delegates to it.

### D2: One block entity, two network memberships
`UtilityDuctBlockEntity extends BlockEntity` (not `NetworkBlockEntity`) with an `EnumSet<Channel>`, a `SideMode[channel][side]` table defaulting to BOTH, and one `ChannelNode` adapter per channel implementing `NetworkNode`. `onLoad` joins the networks of present channels, `setRemoved` and `onChunkUnloaded` leave; `addChannel` and `removeChannel` join or leave at once. `ItemChannelNetwork` and `EnergyChannelNetwork` use `ConnectionRule.always()` within their own sets, so a duct without a channel simply is not a node there. `loadAdditional` on a loaded server block entity reconciles memberships so `data merge` works in tests.

### D3: Modes
`setMode` stores, marks dirty, syncs the client, recomputes the connection flags through `UtilityDuctBlock.refreshConnections` and posts `DuctEvent.ModesChanged` to the channel network, which rebuilds that node's endpoints. The mode cycle in the GUI is BOTH, INPUT, OUTPUT, DISABLED; AUTO is mapped to BOTH. Saved as `Modes.<channel>.<side>` strings, only non default entries.

### D4: Networks
`ItemChannelNetwork` mirrors `ItemDuctNetwork`: endpoints per node per non disabled side with their mode and a capability cache; `route` only offers to sides whose mode allows extract and skips the origin and other duct handlers; pulls run for INPUT sides every `pull_interval` ticks. `EnergyChannelNetwork` mirrors `PowerCableNetwork`: sources are storages on sides allowing insert that can extract, sinks are storages on sides allowing extract that can receive; a storage that can extract is never also a sink.

### D5: Capability and connections
The duct exposes `DuctItemHandler` per side when it has the item channel; `insertItem` checks the side mode and routes. No energy capability is exposed; like power cables the network pulls from sources. `connectsTo` treats other utility ducts as connected and otherwise checks, per present channel, mode not disabled plus the matching capability on the neighbour. Placement happens before the channel exists, so `ChannelItem.placeBlock` adds the channel afterwards and `refreshConnections` updates the arms.

### D6: GUI and payloads
`UtilityDuctScreen` is a plain `Screen` opened client side from `useWithoutItem` through `ClientHooks`; it reads the client block entity. Buttons per side and channel cycle modes optimistically and send `SetDuctModePayload`; a Remove button per channel sends `RemoveChannelPayload`. Server handlers check distance and the block entity.

### D7: Stats and assets
`DuctStatsManager` reads `duct/utility_duct.json`: items_per_tick 16, items_per_pull 8, pull_interval 8, energy_per_tick 512. Block texture grey with a lighter stripe; channel item icons are tubes with a brown or red band.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON on `runServerAll` (needs a Manufacturing generator and machine for energy): chest to chest through a pull in side; disabled side receives nothing; generator to electric extractor through three energy ducts; and the channel gap case.

## Risks / Trade-offs

- [Client side screen opening from common code] -> `ClientHooks` is only referenced inside an `isClientSide` branch and lives in a client package; the class never loads on a dedicated server.
- [Capability caches rebuilt on every mode change] -> Six small objects per duct; mode changes are rare.

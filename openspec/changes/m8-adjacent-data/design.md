# Design

## Context

The data network (`foundation.data.DataNetwork`) is a graph of `DataNode`s; today only the Utility Duct's data line is a node. Ducts find members on their sides through capability caches. The M8 devices (terminals, Crafting Station, Pattern Storage, CPU) are members found by an adjacent duct. See proposal.md for why.

## Goals / Non-Goals

**Goals:** touching devices form one network; the lookups that ask "the data line of this block" keep working unchanged; no per tick scans.

**Non-Goals:** carriers that make other blocks members; side configuration for carriers.

## Decisions

### Carrier as a second kind of data node
`DataNode` gets `dataCable()` (default true). `DataCarrier` (Foundation, no Storage imports) implements `DataNode` and `NetworkNode<DataNode>` for a block entity: `dataCable()` false, every side open, `join()` / `leave()` through `NetworkManager`. The graph's `ConnectionRule` is unchanged: both nodes must have the side open, so a duct side that is off still cuts the link. Alternative considered: making carriers members that link networks by a second graph. Rejected: two graphs for one network, and packets could not route through devices.

### Self membership
`DataNetwork` keeps a self probe (capability caches of the node's own position, no side) for each carrier and stores the roles with `DataMembers.self(pos, roles)` (a touch with no side). Cables keep their six side probes; carriers have none, so a chest, Access Point or controller touching only a carrier is never a member. A duct side reading a neighbour skips positions that are graph nodes. Members are keyed by position, so a carrier also seen by a duct (before it joined) is still one member.

### Cable count
`cables()` counts only nodes with `dataCable()`, so the duct summary keeps counting ducts.

### Lookups
`DataNetworks.networkAt` already answers the node's own network first, and every Storage lookup (TerminalWarehouses, TerminalCraftables, ProductionView, PatternLibrary, CpuOrders, lamps) reads members of that network, where carriers now appear through self membership. No lookup changes.

### Lone devices
A device alone is now a network of one node, so a lone Production Terminal reports "No Crafting CPU or warehouse on this data line" (reason `nothing`) instead of "Not on a data line" (`no_line`); its lamp stays dark. The GameTest `productionTerminalAloneIsDark` was updated.

### Packets
Carriers are graph nodes, so packet routes may pass through them. A packet ending at a carrier enters its item handler on the face toward the previous node (existing `Shipment` logic). A Crafting Station's front is fixed off, so a packet that would arrive through the front is refused and settles as before; recorded as a known limit.

### Storage wiring
Counter Terminal, Production Terminal, Crafting Station, Pattern Storage: a `DataCarrier` field, `join()` in `onLoad` on the server, `leave()` in `setRemoved` and `onChunkUnloaded`. Crafting CPU: carries only while formed: `carry(formed)` in `onLoad` and from `setBlockState` when the formed state changes.

### PLAN.md
No open decision touched. PLAN.md 2.4 gets one sentence that M8 devices carry the data network when they touch.

### Testing
Pure: `DataMembersTest` covers self membership. GameTests (Storage): stack without duct, craft through the stack, split, chest between, Access Point and adapter not joining, CPU forming joins. Existing Foundation, Storage and Manufacturing data tests stay green.

## Risks / Trade-offs

- [A carrier joined before a duct read it is listed by both] -> one member per position, union of roles.
- [27 CPU blocks are 27 members] -> lookups dedupe by CPU master already.
- [Packets through a station front] -> refused and settled, never lost.

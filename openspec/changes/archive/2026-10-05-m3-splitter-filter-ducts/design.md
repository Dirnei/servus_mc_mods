# Design

## Context

Plain ducts route inserted items round robin with per duct endpoint caches. See proposal.md.

## Decisions

### D1: Subclasses of the plain duct
`SplitterDuctBlock` and `FilterDuctBlock` extend `ItemDuctBlock` (so `connectsTo` and the network type are shared) and create their own block entities, `SplitterDuctBlockEntity` and `FilterDuctBlockEntity`, both extending `ItemDuctBlockEntity` through a new constructor taking the block entity type. Each gets its own block entity type and item handler capability registration.

### D2: Network changes
`ItemDuctNetwork.Endpoint` gains the owning duct position. `route` skips an endpoint when the owning duct is a `FilterDuctBlockEntity` whose list rejects the stack. The network keeps a set of splitter positions; every 8 ticks `onTick` runs one pull per splitter: for each of its non duct neighbours, for each slot, simulate extracting up to `items_per_tick` of `splitter_duct.json` (default 8), simulate routing with that neighbour as origin, then extract and route the accepted amount. One successful pull per splitter per cycle.

### D3: Filter GUI
`FilterDuctBlockEntity` holds an `ItemStackHandler(9)` of ghost entries and is a `MenuProvider`. `FilterDuctMenu` exposes nine `PatternSlot`s and the player inventory; clicks set or clear ghosts; shift click from the inventory fills the first empty ghost. `FilterDuctScreen` draws a generated `filter_duct.png`. Matching uses `ItemStack.isSameItem`.

### D4: Recipes and tech
Assembling, tech `splitter_filter_ducts`: 4 item ducts plus 1 iron gear give 4 splitter ducts; 4 item ducts plus 2 copper wire give 4 filter ducts; 40 ticks each. Tech icon and unlock ids updated.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: chest, splitter, duct, smelter pull scenario; sorting scenario with one filter duct configured through `data merge`; client launch.

## Risks / Trade-offs

- [Pull scans every slot of a neighbour] -> One neighbour set per splitter every 8 ticks; chests are 27 slots. Fine for Tier 1.

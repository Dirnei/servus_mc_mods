# Design

## Context

- Sections exist only as block state: `HeavyRackBlock.LEFT` (joined on the counter clockwise side) and `RIGHT` (clockwise), fixed at placement, at most `bays_per_section` bays (`rack.json`, example 3). The only walk, `sectionSize`, is private and counts one direction. After m6-rack-wood-material a section never mixes woods, and stacked racks never join.
- Each `HeavyRackBlockEntity` owns an `ItemStackHandler` of `rackSlots()` slots. `ServusStorageEvents` registers it as the block's `Capabilities.ItemHandler.BLOCK`. The warehouse index polls every source through its own `BlockCapabilityCache` (`WarehouseNetwork.poll`), so each bay is indexed once.
- `HeavyRackMenu` adds one `SlotItemHandler` per bay slot in rows of 9. `HeavyRackBlock.useWithoutItem` opens it with `player.openMenu(rack, pos)`, which writes the position into the open buffer. `HeavyRackScreen` blits `generic_54` in two parts and reads past the chest rows for more than 6 rows.
- `ShuttleFleet.railsBeside(node)` returns every travel node (member rail or segment) touching the node on any of its 6 sides. `canReach`, `canCarry`, `firstGoals` and the TO_TERMINAL, TO_TARGET and TO_STATION legs use it. It ignores facing, so a member rail along a rack's side counts as the rack's dock. Picking and putting work on the source's handler and do not need the shuttle to stand beside that exact block.

## Goals / Non-Goals

**Goals:** one GUI and one shuttle reach per section; bays keep storing their own slots; side and back rails never reach a rack.

**Non-Goals:** a section wide capability (see D2); vertical sections; scrolling; changing the rails of crates, stations, terminals or garages.

## Decisions

### D1. Pure section walk
New pure `rack/RackSection.java`: `bays(IntPredicate joinsLeft, IntPredicate joinsRight, int limit)` walks from offset 0 to the left while `joinsLeft(offset)` holds and to the right while `joinsRight(offset)` holds, at most `limit` steps each way, and returns the offsets from leftmost to rightmost (a lone bay is `[0]`). `HeavyRackBlock.section(BlockGetter, BlockPos)` maps offsets to positions along `facing.getCounterClockWise()` (left) and reads `LEFT`/`RIGHT` from the states; it returns the bays from left to right as seen from the front. The limit is the existing walk limit of 64, so bad data cannot loop.
JUnit `RackSectionTest`: lone bay, start at the left end, start in the middle, start at the right end, a join flag that is only set on one side counts only when both sides agree (`joinsLeft(i)` and `joinsRight(i - 1)`), the limit stops the walk.

### D2. Storage stays per bay, the GUI combines
Items stay in each bay's `ItemStackHandler`; NBT, the dismantle path and spilling are unchanged, so a broken bay spills only its own slots. The capability stays per bay: a section wide handler on every bay would make the warehouse index count the section once per bay. Hoppers fill the bay they touch, and the section GUI shows the result.

### D3. Section menu
`HeavyRackBlock.useWithoutItem` computes the section and opens the menu with `IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` (NeoForge 21.1.255 sources), writing the clicked position and, for each bay of the section, its position and slot count. `HeavyRackMenu` takes the list of bays and adds the slots of every bay in order (all of the left bay first), laid out in rows of 9; rows and the player inventory offset follow the total slot count. `quickMoveStack` treats all bay slots as one range, so shift click fills the next bay when one is full. `fromNetwork` reads the positions and builds the same layout from the client's block entities; slot contents arrive through normal container sync. `stillValid` holds while the player is in range of the clicked bay, every bay is still a rack block entity that is not removed, and `HeavyRackBlock.section` from the clicked bay still returns the same positions. Otherwise the menu closes. The title stays the clicked bay's name with its zone.
`HeavyRackScreen` draws the `generic_54` header, then the first slot row strip once per row, then the bottom part. Its height is `17 + rows * 18 + 96` (275 for 9 rows, which fits at GUI scale 3 on a 1080 pixel high window).

### D4. Dock rails come from the host
`ShuttleFleet.Host` gets `Set<NodePos> dockRails(NodePos storage)`, which replaces `railsBeside` in every caller. `WarehouseNetwork` implements it:
- **Heavy Duty Rack:** for each bay of `HeavyRackBlock.section`, the position in front of the bay (`facing` side) when it is a travel node.
- **Any other block:** every touching travel node, as before.

A rack bay with no front rail can then be reached through a rail in front of another bay of its section. A lone rack beside a section, or a rack above it, is reachable only through its own front rail. Rails along a side or back never count, which also closes the old gap where a rail linked through another route counted as a dock.

### Settled PLAN.md decisions
None are open for this item. The change records the user's play check decisions: a section shares one GUI (rows grow, no scrolling) and is served from any front rail of its bays. PLAN.md section 2.3 item 1 and the M6 rack line get the new wording, including the wood rule from m6-rack-wood-material.

### Pure Java vs Minecraft
- Pure, JUnit: `RackSection.bays`.
- GameTest:
  - The section menu of a three bay section has 81 rack slots in left to right order, and shift click spills into the next bay.
  - Breaking one bay spills only its own items.
  - The menu closes (`stillValid` false) after a bay of its section is removed.
  - A shuttle serves bread from the right bay of a section whose only rail is in front of the left bay.
  - A lone rack beside that section, without a front rail, is refused as needing a shuttle.
  - A rack with a member rail only along its side is refused as needing a shuttle.
- Manual: GUI look at 9 rows, shuttle parking at the shared front rail.

## Risks / Trade-offs

- [Hopper sees one bay while the GUI shows the section] → documented in the Automation access requirement; a section capability can come later with an index that de-duplicates.
- [Section menu built from client block entities] → the client already holds the block states and block entities of loaded racks; the open buffer also carries each bay's slot count, and a bay the client cannot find gets an empty stand in handler of that size, so the client's slot layout always matches the server's.
- [Shuttle parks at another bay's rail] → the shuttle model already stops beside the rail tile, so it looks like it serves the bay at that rail. Accepted for now.

## Migration Plan

None, nothing is released.

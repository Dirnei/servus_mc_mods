# Design

## Context

See proposal.md for motivation. The warehouse is `WarehouseNetwork`, a `ServusNetwork` over `StorageNodeBlockEntity` nodes linked by the pure `WarehouseConnectionRule` with kinds `STOCK`, `RAIL`, `ELEVATOR`. The network keeps `sources` (stock nodes plus their foreign neighbours, indexed by `WarehouseIndex`), `connectors` (rails and segments, the travel graph), `extract` and `insert`, and ticks through `onTick` after draining its mailbox of `WarehouseEvent`s. `insert` had a fixed preference: sources holding the item, empty crates, other nodes, foreign inventories in use. `ShuttleFleet` owns the shuttles and a job queue; each shuttle has a `ShuttleMotion` on a path from `RailPaths.find`. `RailBlock.docksTo` docks onto nodes that take rails. `StorageStatsManager` loads `warehouse.json`. The crate and rack block entities sync to clients through `getUpdateTag` and have renderers that draw text with `Font.drawInBatch`. Logistics stores facade data in an item data component registered through `DeferredRegister.createDataComponents` and edits duct filters with a `GhostSlot` reading through a supplier; Storage copies both patterns. Blocks return `SKIP_DEFAULT_BLOCK_INTERACTION` for the Configuration Carrot so the item's `useOn` runs; the Zone Label gets the same treatment.

A first version of this change kept zone rules on the controller and edited them in the station GUI. The user rejected that on 2026-10-06: the label itself should define name and filter, stamping copies that onto the block, unzoned blocks take everything and zoned blocks are filled first. This design is the reworked version.

## Goals / Non-Goals

**Goals:**
- Routing is one pure decision (`PutAwayPlan`) used by every insert path, unit tested without Minecraft.
- No new scanning: the station drains on buffer change events; blocked stations retry on a slow timer and on zone, rule and membership changes.
- Put-away reuses the fleet, paths and rendering; a shuttle is still a motion plus cargo plus a job.
- Zones are in world: the label carries the definition, the block carries its copy. No central rule store.

**Non-Goals:**
- See proposal non-goals. Also no reservation of target room across jobs: two shuttles may be sent to one target and the second brings its leftover back.

## Decisions

### Settles no PLAN.md open decision
Section 5 has no bullet about put-away. Section 2.3 and the M6 item are reworded in this change: zones are stamped with labels and unzoned storage is the overflow.

### The label is the zone definition
`ZoneLabelItem.use` (right click in the air, or on a block that is not zonable) opens `ZoneLabelMenu` through `player.openMenu` with a `SimpleMenuProvider` and the hand written to the buffer. The definition lives in two data components on the stack: the vanilla `DataComponents.CUSTOM_NAME` for the name and a Storage component `servus_storage:zone_filter` holding `ZoneFilter(List<ItemStack> items, List<String> tags)`, registered in `ServusStorageComponents` with `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)` and `registerComponentType("zone_filter", builder -> builder.persistent(CODEC).networkSynchronized(STREAM_CODEC))`, the pattern Logistics uses for `facade_block`. The codec is a `RecordCodecBuilder` over `ItemStack.CODEC.listOf()` and `Codec.STRING.listOf()`. The menu keeps an `ItemStackHandler` of nine slots as the editing buffer: it is loaded from the component when the menu opens and written back (as one item stacks) on every change, together with the tag line, through `stack.set(...)` on `player.getItemInHand(hand)`. `stillValid` checks that hand still holds a Zone Label. Payloads: `SetLabelNamePayload(String)` and `SetLabelTagsPayload(String)`; ghost slots use the vanilla click path with `GhostSlot` semantics (set from cursor, clear on empty cursor). Alternatives rejected: anvil naming only (no place for the filter), a block GUI per rack (crates have none), a central rule store on the controller (the user wants the zone on the rack).

### The block carries a copy
`StorageNodeBlockEntity` gets `ZoneDefinition zone` (`name`, `ZoneFilter filter`), persisted as a compound `"Zone"` and sent in the update tag; `setZone(ZoneDefinition)` calls `setChanged`, `sendBlockUpdated` and posts `WarehouseEvent.ZoneChanged(pos)`. `ZoneLabelItem.useOn` copies the label's name and filter onto a `zonable()` node (crates and racks), sneak clears. `saveToItem` on crate and rack already copies the block entity tag, so the zone survives dismantling. Editing the label later changes nothing on stamped blocks (user decision).

### Pure routing: `PutAwayPlan`
Package `io.github.dirnei.mc.servus.storage.zone`, no Minecraft imports:
- `record Filter<K>(Set<K> items, Set<String> tags)` with `isEmpty()`; `Match match(K key, Set<String> tags)` returning `EXACT`, `TAG` or `NONE`.
- `PutAwayPlan.tiers(K key, Map<S, Filter<K>> filtered, Function<K, Set<String>> tagsOf)` returns `Tiers<S>(Set<S> exact, Set<S> byTag)`: the filtered blocks that take the key, split by match kind. General storage (unzoned blocks, zoned blocks with an empty filter, foreign inventories) is not part of the input; the network appends it as the third tier.
- `Reason` is `NO_ROOM` (something matched or general storage exists but nothing had room) and `NO_STORAGE` (the warehouse has no source at all).
- `ZoneNames.key(String)` and `display(String)` normalise names for grouping and display.
`PutAwayPlanTest` covers: exact match, tag match, both kinds at once go to their tiers, no match gives empty tiers, an empty filter never matches. `ZoneNamesTest` covers trim, case and length.

The network keeps `zoneOf: Map<BlockPos, ZoneDefinition>` for zoned sources (set at `onNodeAdded`, on `ZoneChanged`, on absorb) and builds the filtered map from it per insert. `targets(tier, key)` orders a tier's blocks by the existing four levels: holding the key, empty crates, other nodes, foreign in use. `insert(stack)` walks exact, tag, general. Tag lookup is `stack.getTags().map(t -> t.location().toString())`.

### Station node kind
`WarehouseConnectionRule.NodeKind` gains `STATION`. Rule: `STOCK` and `STATION` link to `STOCK`, `STATION` and `RAIL` on any side and never to `ELEVATOR`; `RAIL` links to `STATION` like to `STOCK`; `ELEVATOR` never links to `STATION`. `StorageNodeBlockEntity.holdsStock()` stays `kind() == STOCK`; `docksRail()` is true for `STOCK` and `STATION` and `RailBlock.docksTo` and `WarehouseNetwork.railSideAccepted` use it. In `WarehouseNetwork`, `onNodeAdded` puts a station into a `stations` set (neither source nor connector), drops any candidate source a stock neighbour had listed at that position, `addCandidatesAround` skips connectors and stations, and `rebuildCandidates` passes stations with connectors.

### Intake and drain
`DispatchStationBlockEntity` (kind `STATION`, `MenuProvider`) holds an `ItemStackHandler buffer` sized from `StorageStatsManager.dispatchBufferSlots()` and a `boolean[] claimed`. The capability registered with `RegisterCapabilitiesEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ...)` returns an insert only view whose `extractItem` returns `ItemStack.EMPTY`. `onContentsChanged` posts `WarehouseEvent.Intake(pos)`; the network adds the station to `pendingStations`. `onTick` drains each pending station: for up to `put_away_per_tick` unclaimed stacks, pick the first block in tier order whose handler accepts at least one item (`ItemHandlerHelper.insertItem(handler, stack, true)`). If none accepts, the stack stays, the station records the reason, the item and the matched zone name for its GUI, sets its `BACKED_UP` block state, and the network keeps it in `blockedStations`, retried every 20 ticks and at once on `ZoneChanged`, on a new stock node, and when a poll shows stock leaving a source. If a target is found and `fleet.canCarry(stationPos, targetPos)` holds, the slot is claimed and `PutAwayJob(station, slot, key, amount, target)` is queued; otherwise the stack moves at once and the remainder goes back into the buffer. `BACKED_UP` clears when no stack is blocked. The buffer pops at the position in `onRemove`.

### Fleet: one queue, two job kinds
`ShuttleJob` is a sealed interface over `PickJob` and `PutAwayJob`. Each shuttle has a `Leg`: `TO_SOURCE`, `TO_TARGET`, `TO_STATION`, `HOME`. A pick is `TO_SOURCE` then `HOME`. A put-away is `TO_SOURCE` (station rail; `host.takeFromStation` clears the claim), `TO_TARGET` (`host.insertAt`), then `HOME`, or `TO_STATION` first when cargo is left (`host.returnToStation`, popping at the station if it does not fit). Goal rails come from `railsBeside(pos)`. Jobs are polled in order from one `ArrayDeque`; `release()` frees the claims of queued put-aways through `host.releaseClaim`. `ShuttleView` is unchanged; cargo is empty on legs where nothing is carried. `summary()` adds the put-away count.

### Station GUI
`DispatchStationMenu` has `SlotItemHandler`s for the buffer and the player inventory; `quickMoveStack` moves between them. `broadcastChanges` sends a `StationStatePayload` on signature change with the online state and the station state (blocked reason, item, zone, idle). The screen is an `AbstractContainerScreen` with the buffer row, the status line, and the offline reason like the terminal. The user asked on 2026-10-07 for the zone list to go: zones are visible on the blocks and through the `zones` command.

### Datapack and datagen
`warehouse.json` gains `dispatch_buffer_slots` (9) and `put_away_per_tick` (1). Datagen: station blockstate with `FACING`, `ONLINE` and `BACKED_UP` (dark, lit and red fronts), item models for the station and label, loot tables, recipes (station: chest over hopper framed by iron ingots; label: paper and a dye, shapeless), block tags, lang. Textures are placeholders drawn with the `png.py` helper.

### Minecraft side verification
GameTests in `ServusStorageGameTests`: `zoneLabelStampsAndClears` (label with name and filter stamps crate and rack through the item click path, restamp, sneak clear, unnamed does nothing, dismantle keeps the zone), `zoneLabelMenuEditsDefinition` (menu sets name, tags and a ghost item on the held stack), `insertRespectsZones` (exact and tag tiers, general storage as overflow, nothing stored when all full), `stationPutsAwayInstantly`, `stationBacksUpWhenZoneFull`, `chestOnStationIsNotStock`, `stationDocksAndDismantles`, `shuttleCarriesPutAway`, `stationMenuFeedsBuffer`. Manual in the client: the label GUI, the drawn zone name, hopper and Utility Duct feeding, the red lamp, the cart carrying cargo.

## Risks / Trade-offs

- [Claimed stacks and players editing the buffer] → claims are per slot and re-validated at loading time; a missing stack ends the job and the shuttle goes home.
- [Two jobs to one target fill it] → the second shuttle brings its leftover back to the station; no reservations in this change.
- [Blocked station retry is a timer] → one check per blocked station every 20 ticks and only while something is blocked; zone, membership and stock events also wake it.
- [Stamped copies drift from the label] → the user chose this for predictability; the station GUI shows what each zone's blocks carry so drift is visible.
- [Tags typed wrong] → unknown tags match nothing; the label GUI keeps them so the player can correct them.

## Open Questions

None that change the specs or tasks. Texture polish and the exact GUI layouts are decided during apply.

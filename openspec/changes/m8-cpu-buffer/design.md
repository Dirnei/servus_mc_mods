# Design

## Context

See proposal.md (Why). Current state in the 26.1.2 worktree:

- `storage/cpu/CpuShape` fixes the cube at 3x3x3; `CpuBlock.PART` (0..26) is the block's index in the cube, part 0 (the lowest corner) is the master that owns the `OrderBook`, and `CpuBlock.masterPos` computes the master from `PART`. `CpuTiles` picks the face tile from the part's offset; datagen (`ServusStorageBlockStateProvider.cpu`) builds one model per part and `ACTIVE`.
- `CpuBlockEntity.slots()` reads `order_slots` from `cpu.json` through `StorageStatsManager.cpuOrderSlots()`.
- `CpuBlockEntity.Host.collect` moves a step's outputs in this order: `sendDirect` (request item to the terminal), `handOff` (pure `order/Handoff.split`: to a later step delivering on another producer, a hold of up to `handoff_wait_ticks` for a busy other producer, the rest to storage; a later step on the same producer gets nothing), then `DataPackets.move` to the Packing Station from `collectionPoint`. Every move is a `ProductionLedger` transfer booked on the step (`OrderStep.collecting`, `settleCollections`).
- `OrderBook.feed` asks `Host.feed` for every owed input, which ships from the line's warehouses through a Packing Station.
- The unarchived change `m0-port-minecraft-26-1-2` already modifies the "Order slots" requirement of `storage/crafting-cpu` (CPU blocks place against CPU blocks without sneaking). The delta here builds on that wording.

## Goals / Non-Goals

**Goals:**
- Any solid box of at least 2x2x2 of CPU and Buffer blocks forms with the carrot, with a reason when it does not.
- Intermediates of an order never touch the warehouse while the buffer has room, also on a single producer.
- Nothing is lost: buffered items survive reloads, cancels and unforming.
- The buffer is visible in the action bar summary and in the Production Terminal.

**Non-Goals:**
- Balance (values, recipes). See Risks.
- Buffering raw materials from storage ahead of a step, fluids, or moving buffered items between CPUs.

## Decisions

### D1. Box geometry stays in the blockstate as a place class

`PART` keeps its 27 values but means the block's place class in the box: per axis low, middle or high (`x + 3z + 9y` with 0 = low, 1 = middle, 2 = high). In a box with a side of 2 there is no middle on that axis. This is exactly what `CpuTiles` already needs to pick a tile, so the tile logic, the 27 x 2 models per block and the existing textures stay. The lowest corner is the only block with class 0 and stays the master, so `getTicker` keeps its `PART == 0` check.

Where the master is can no longer be computed from `PART`. Every formed block entity saves the offset to its master (`MasterOffset`), the master saves the box size and its counts of CPU and Buffer blocks. `CpuBlock.master(level, pos)` reads the offset from the block entity.

Alternative: one boolean property per side (on boundary) or model data from the block entity. Rejected: more states for the same information, and model data needs a client sync of the block entity before the first frame.

### D2. Forming is a bounded flood fill on the carrot click

On the carrot click (never per tick) the block collects the unformed CPU and Buffer blocks joined face to face to the clicked one, breadth first, and stops after `max_blocks + 1` blocks. Pure `cpu/CpuBox.check(Set<Vec> joined, Set<Vec> cpuBlocks, int maxBlocks)` returns either a box (lowest corner, size, counts) or a reason, checked in the spec's order:

1. `too_big`: more than `max_blocks` joined.
2. `too_small`: the smallest box around them has a side shorter than 2.
3. `not_a_box`: a corner of that box is not joined.
4. `gap` with the lowest missing position (by y, z, x): another place of the box is not joined.
5. `no_cpu_block`: no CPU block among them.

"Not a box" versus "gap" is decided by the corners because the corners are what makes a box: an L shape or a staircase misses a corner, a box with a hole does not. Formed blocks never join (they are skipped by the flood fill), so a new box beside a formed CPU forms on its own, and two touching formed CPUs stay two CPUs. Two unformed boxes that touch are checked as one: when together they fill a box they form one CPU, else the reason is shown and the player forms one of them first.

`max_blocks` (example 512) only guards the flood fill; it is not a balance number but lives in `cpu.json` with the others.

### D3. One block entity type for both blocks

`CpuBufferBlock` extends `CpuBlock` (same `FORMED`, `PART`, `ACTIVE`, carrot and summary behaviour) and shares the `crafting_cpu` block entity type: `new BlockEntityType<>(CpuBlockEntity::new, CRAFTING_CPU.get(), CPU_BUFFER.get())`, the form already used for the rails in `ServusStorageBlockEntities`. Every formed block of either kind is a data link member and a data carrier as before. `CpuBlock.useItemOn` passes for the carrot and for a block item of any `CpuBlock`, so a Buffer block places against CPU blocks and the other way round.

### D4. Slots and room are sums over the blocks

`slots = cpuBlocks x orders_per_cpu_block`, `room = bufferBlocks x stacks_per_buffer_block`, both from `cpu.json` (`StorageStatsManager` loses `cpuOrderSlots` and gains `cpuOrdersPerCpuBlock`, `cpuStacksPerBufferBlock`, `cpuMaxBlocks`). The counts are saved on the master at forming, so a datapack reload changes the values without a re-form. `OrderBook.Host.slots()` and `CpuChoice` are unchanged. A CPU with no CPU block cannot exist (D2), so the old `Math.max(1, ...)` floor goes away.

### D5. The buffer: pure book on the master

Pure `cpu/CpuBuffer<K>` (no Minecraft imports, keys are generic) keeps, per order id and key, the items held, the items on their way in (by transfer id) and the item's max stack size. Rules:

- A stack is one key of one order up to its max stack size; used stacks are `ceil(count / maxStack)` per (order, key), summed. Items on their way in count as held for this sum, so they hold their room.
- `room(order, key, maxStack)` is how many more items of that key the order can put in: the free space of its last stack plus whole free stacks.
- The room is shared by all orders of the CPU, first come first served. No quota per order: orders run oldest first, so the oldest order gets the room first, and a full buffer only means today's behaviour.
- Arrivals are credited to their order when their transfer ends, by the delivered count from the ledger (`arrived(transfer, delivered)`), and the unused rest of their room is released. Taking items out (`take`) is immediate.
- Entries of orders that are done or cancelled become leftovers (owner id kept, flagged leaving).

The master wraps the book with `ItemKey` keys and saves it next to `Orders` in its tag (`Buffer`: entries with order id, item, count; arrivals with transfer id, order id, item, count). Buffered items belong to their order only: they are not part of `Host.stock()`, are not indexed by any warehouse, are not reserved, and are never fed to another order.

### D6. How items get in: a packet inlet, not an inventory

Decided with the user: the CPU registers no item capability, so item pipes and hoppers never see it and no duct shows an item arm toward it. Packets reach it through a Servus packet inlet instead of `Capabilities.Item.BLOCK`. That inlet is `ServusFoundationCapabilities.PACKET_INLET`, introduced by `m11-drop-crates-pipe-access` (its D1 to D3: a `ResourceHandler<ItemResource>` under its own sided capability id, which item pipes and hoppers never ask). There `PacketIo.accepts`, `insert`, `acceptable` and `settle` look up the target through the shared helper `PacketInlets.handler(level, pos, side)` (packet inlet first, then `Capabilities.Item.BLOCK`) before `STOCK_SINK`, and `available` and `extract` ask `STOCK_PROVIDER` first, then the same helper. This change archives after it and only registers the CPU's inlet with `RegisterCapabilitiesEvent.registerBlockEntity`. The inlet:

- accepts a key only up to the items of that key still expected by open arrivals of the master's buffer,
- gives nothing out,
- is absent on unformed blocks (the provider returns null), so `PacketIo.accepts` refuses them; `CpuBlock` calls `Level.invalidateCapabilities` on the blocks of the box when it forms and unforms.

The collection to the buffer is `DataPackets.move(level, producer, cpuMember, like, count)` from the producer to the CPU block that is a member of the line (`memberPos`, cached on the master and checked again when it is no longer a member), booked as `ledger.open(count)` and `step.collecting(key, transfer, count, cpuPos)`. It counts as collected for the step when it lands, like a collection to a Packing Station; `DataPackets.move` takes items from the producer only as far as the CPU accepts them, so nothing is in flight.

Alternatives: an insert only `Capabilities.Item.BLOCK` (rejected by the user: pipes show arms toward a block that refuses them) and `ServusFoundationCapabilities.STOCK_SINK` (rejected: it gives the CPU the `STOCK` data role, which the duct summary counts as storage).

### D7. Where outputs go: `Handoff.split` learns the buffer

`Handoff.split` takes the buffer room the order has for the key and what it already holds there, and returns a new `buffered` part. Order of the split:

1. `handed`: to later steps delivering on another producer, up to what each still owes (unchanged).
2. `buffered`: to the buffer, up to what later steps that are not delivering (busy other producer or same producer) still owe minus what the order already holds of the key, and at most the room.
3. `hold`: what is still owed to a busy other producer beyond that waits at the producer while `heldFor < waitLimit` (unchanged rule, now only for the part the buffer could not take).
4. `toStorage`: the rest. The part of it that later steps still owed is added to the order's `overflowed` count per key (saved with the order) for the terminal's note.

A same producer dependant no longer makes outputs go to storage at once when the buffer has room; with no room it does, as before.

### D8. How items get out: feeding from the buffer first

`OrderBook.feed` first asks a new `Host.feedFromBuffer(order, producer, key, owed)`. The CPU moves up to that many of the order's buffered items with `DataPackets.move(level, cpuMember, producer, like, count, source)`, where `source` is an `ItemView` over a `LegacyItemHandler` that exposes only that order's entry for the key and takes from it on extract. The source form exists for members whose public handler gives nothing out (see its Javadoc in `foundation/data/packet/DataPackets`). It is booked as a feeding like a shipment (`step.addFed` with the ledger id); a feeding that ends short is fed again, from the buffer or from storage. Only what the buffer did not cover goes to `Host.feed`. `madeEarlier` keeps a step from reporting buffered inputs as missing, unchanged.

### D9. Leftovers, cancel and unform

- Done or cancelled orders: their entries become leftovers. On each order book run (every 20 ticks) the CPU moves leftovers as packets to its `collectionPoint` (lowest Packing Station that reaches it), booked in the ledger and removed from the buffer by the source view as they leave. The order's step `release` is unchanged.
- Unforming (`CpuBlock.affectNeighborsAfterRemoval` then `unform`): before the blocks change state, the master cancels all orders, takes every buffered stack out of the book and hands it to `DataPackets.send(level, memberPos, collectionPoint, stack)`, the form that sends a stack the caller holds. The send starts while the CPU is still a member, so the network owns the stack from then on. When the blocks unform in the same tick, the route from the CPU breaks and the shipment settles by the line's own rule (`Shipment.settle`, `PacketIo.settle`): back into the source (the unformed CPU has no inlet, so nothing), into the Packing Station's packet inlet as far as its To storage buffer takes it, and the rest dropped at the Packing Station, the first loaded place of that list. What lands in the To storage buffer is put away by shuttle like any packets. When no Packing Station reaches the line (`collectionPoint` empty) or the send fails at once, the master drops the stack at the removed block with `Containers.dropItemStack`.
  - Decided in the update of 2026-10-12: `m11-drop-crates-pipe-access` removes `WarehouseNetwork.insert` and every instant put-away, so the buffer leaves by the only way into a warehouse that is left, a Packing Station and shuttles. Rejected: inserting straight into the Packing Station's inlet from the master (bypasses the line and its transfer gates for no gain), and keeping the CPU a member for one more tick to move the buffer with `DataPackets.move` (a formed state on blocks that are already unformed).
- A master that loads unformed drops its saved buffer at its position in `onLoad` on the server, like it cancels its saved orders.

### D10. Production Terminal

- `ProductionRows.CpuRow` gains the box size, the counts of CPU and Buffer blocks, `room`, `used` and a list of buffered items (a one item stack, a count, the order id, a leaving flag). `JobRow` gains the buffer lines (machine name of the later step, items with counts) and the overflow list (items with counts). Counts travel as var ints next to one item stacks, never as stack counts, so counts above the max stack size travel safely.
- The screen gets a fourth tab, `cpus`, after Machines. `TerminalLayout.Production.TAB_WIDTH` drops from 64 to 54 so four tabs fit the 224 wide tab row (4 x 54 + 3 x 2 = 222); `TerminalLayoutTest` checks four tabs and the new CPU row areas. The Crafting tab draws buffer and overflow lines under the job row as extra lines of the existing `Line` list.
- `ClientProduction` keeps the payload as before; the live view rule (send when changed, every half second) covers the new fields.

### D11. Look

The Buffer block reuses the frame of the CPU and has amber storage lights instead of teal data lights: textures `crafting_cpu_buffer` (casing, a chest like front in the riveted casing) and `crafting_cpu_buffer_{idle,active}_{tile}` for the 9 tiles, made by recolouring the CPU tile textures, with the same `.mcmeta` animations. Datagen builds its models like `cpu(...)`. `updateActive` walks the box from the saved size instead of `CpuShape.PARTS`.

### D12. Which logic is pure and which is not

- Pure, JUnit: `CpuBox` (flood result check, reasons, place class per axis), `CpuBuffer` (stacks, room, arrivals, take, leftovers), `Handoff` (with the buffer), `CpuShape` and `CpuTiles` with the place class meaning.
- Minecraft, GameTests: forming and reasons in the world, unform with buffer, chains on one station, busy producer with and without buffer, reload with buffer, cancel with buffer, terminal rows. Manual: textures, tabs, action bar.

### D13. Archive order and PLAN.md

This change archives after `m0-port-minecraft-26-1-2`, because both modify the "Order slots" requirement of `storage/crafting-cpu` and this delta's wording builds on the port's. It also archives after `m11-drop-crates-pipe-access`, which introduces `ServusFoundationCapabilities.PACKET_INLET` (D6), removes `WarehouseNetwork.insert` and every instant put-away (D9 relies on that) and removes the "Pushed outputs count" requirement of `storage/crafting-cpu`, which this delta does not touch. `m11-controller-wireless` and `m11-warehouse-power` archive after this change. The change settles no open decision of PLAN.md section 5; it changes the decided "Crafting CPU (Storage, 3x3x3 multiblock on the cable)" in section 2.4, so `PLAN.md` 2.4 and the docs site are updated in this change, and the Purpose line of `openspec/specs/storage/crafting-cpu/spec.md` ("a 3x3x3 multiblock") is edited at archive.

### APIs used (checked in the 26.1.2 worktree)

- `ServusFoundationCapabilities.PACKET_INLET` and `PacketInlets.handler` from `m11-drop-crates-pipe-access` (D1, D2 there); the CPU's registration goes into `storage/ServusStorageEvents.java`.
- `ResourceHandler<ItemResource>` through `foundation/transfer/LegacyItemHandler` and `ItemView`.
- `DataPackets.move` (both forms) and `Delivery.onEnd`: `foundation/data/packet/DataPackets.java`; target check `PacketIo.accepts`.
- `ProductionLedger.open`, `delivered`, `end`, `transfer`: `storage/order/ProductionLedger.java`.
- `DataPackets.send(ServerLevel, BlockPos, BlockPos, ItemStack)`, `Shipment.settle` and `PacketIo.settle`: `foundation/data/packet/`.
- `Block.affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)`, `Level.invalidateCapabilities`, `Containers.dropItemStack`, `ItemStack.getMaxStackSize`, `Player.sendOverlayMessage`: used in `CpuBlock`, `PacketIo` and the screens of this worktree.
- `BlockEntityType` with several blocks: `storage/registry/ServusStorageBlockEntities.java` (rails).

## Risks / Trade-offs

- [Balance] A 2x2x2 of CPU blocks runs 8 orders where a 3x3x3 cube ran 2, with a recipe that gives 4 blocks. → Values live in `cpu.json`; the CPU block recipe and values are part of the open M9 balance pass. Start values: `orders_per_cpu_block` 1, `stacks_per_buffer_block` 1.
- [Small buffer] One stack per Buffer block is little for big chains. → Overflow falls back to today's behaviour and the terminal shows it, so players see when to add Buffer blocks.
- [One order fills the buffer] First come first served lets one big order take all room. → Accepted; oldest orders run first anyway. A per order quota can follow if play tests ask for it.
- [Depends on the packet inlet] The CPU buffer needs `PACKET_INLET` from `m11-drop-crates-pipe-access`. → Apply and archive that change first; until then this change cannot be applied.
- [Buffer drops at the Packing Station] On unform, what the Packing Station's To storage buffer cannot take drops beside that station, not at the broken CPU block. → Accepted: nothing is lost and the items lie where storage would have taken them.
- [Flood fill cost] A click on a huge wall of CPU blocks. → Bounded by `max_blocks`, only on a click.
- [Member lookup] `memberPos` walked 27 blocks and would walk up to `max_blocks` now. → Cache the member block on the master and look again only when it is no longer a member.
- [Ledger forgets an arrival] A transfer into the buffer the ledger forgot counts as collected for the step but the buffer holds only what arrived. → The later step feeds the rest from storage, as for handoffs today.
- [GameTests] Many GameTests build a 3x3x3 cube and expect 2 slots. → A shared GameTest helper builds boxes; tests that check slots build 2 CPU blocks plus Buffer blocks.

## Migration Plan

Nothing is released, so there is no migration. `order_slots` is removed from `cpu.json`. Side note for dev worlds: formed CPUs saved before this change have no master offset in their block entities and act unformed; break one block and form them again with the carrot.

## Answered Questions

Answered by the user on 2026-10-12:
- Buffer block recipe: like the CPU recipe with a chest instead of the crafting table, 4 per craft.
- `stacks_per_buffer_block` starts at 1; a full buffer taking the detour through storage is wanted.
- No item pipe connection to the CPU (D6).
- The buffer is shared first come first served, no quota per order.

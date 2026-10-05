# Design

## Context

The power cable exists as a one off block. Machines already push outputs into any adjacent item handler. See proposal.md and the delta spec.

APIs: same as the cable change (`BlockCapabilityCache` with `Capabilities.ItemHandler.BLOCK`, `IItemHandler`), verified earlier.

## Goals / Non-Goals

**Goals:**
- Reuse: one connectable block base and one multipart generator for cables and ducts.
- Zero buffering and zero scanning: routing happens inside the insert call, endpoints come from caches.

**Non-Goals:**
- Pull mode, filters, rendering of items.

## Decisions

### D1: ConnectableBlock
`ConnectableBlock(Properties, radius)` holds the six boolean properties, computes centre and arm shapes from the radius, and implements placement, `updateShape`, `getShape`, wrench skip and sneak dismantle. Subclasses implement `connectsTo` and `newBlockEntity`. Cable radius 3 (6 by 6 core), duct radius 4 (8 by 8 core).

### D2: Routing on insert
`ItemDuctBlockEntity.handlerFor(side)` returns one `ItemDuctHandler` per side (and one for a null side). `insertItem` asks the duct's network to `route(stack, originPos, simulate)`. The network keeps six `BlockCapabilityCache<IItemHandler>` per duct, flattened into a list; `route` walks the list from a round robin cursor, skips the origin and other ducts' handlers, tries slot 0 then every slot, and inserts the accepted amount. The cursor advances past the inventory that took items. `simulate` calls never move the cursor. The per network budget is reset every tick in `onTick` from the `items_per_tick` stat.
Alternative: ducts with a buffer and periodic transfer. Rejected: more state, more ticks, and the plan calls this duct "simple".

### D3: Why never into another duct
Two ducts at different places on the same network are the same network, so routing into a duct would be a loop. Ducts of different networks cannot be adjacent (adjacent ducts always connect).

### D4: Data
Stats file `item_duct.json` with `items_per_tick` 16. Crafting: three iron ingots over two glass over three iron ingots gives eight ducts.

## Pure Java vs Minecraft

All Minecraft. Verified by server runs over RCON (extractor, ducts, smelter, ducts, chest chain; a full chest case) and the client launch. The round robin scenario is checked with two chests.

## PLAN.md decisions settled

None.

## Risks / Trade-offs

- [Machines push every 8 ticks and the duct budget is per tick] -> Budget only matters for very dense setups; retunable in data.
- [Hoppers feeding ducts from above also count as origin] -> Correct: a hopper never gets its own items back.

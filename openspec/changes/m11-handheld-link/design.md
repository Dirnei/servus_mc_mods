# Design

## Context

- Shuttles deliver to any `DeliveryTarget` (`deliveries()` as `LockedSlots`): `WarehouseNetwork.planAndStart(destination, key, amount, requestId)` plans picks from unlocked rack stock, locks the sources OUT and queues `PickJob`s; on arrival `readyToSend` locks room IN at the target and `deliverTo` inserts with `insertAs(jobId)`. The Outbound and Packing Stations and, since `m6-handheld-relocate`, rack bays are targets.
- Put-away from a buffer into the racks goes through `PutAwaySource` (`putAwayBuffer()`, `setBlocked`, `clearBlocked`), used by the Inbound Station and the Field Head.
- Stations join a warehouse by the rail in front of them and get numbers and names through `NamedMachine` (`machine-numbers`, `station-numbers` specs). Multi-block items placed as one piece exist already: the Fluid Inbound and Outbound (two high, one item, facing rule towards a rail).
- The Handheld (`shuttles/handheld`) has the STOCK screen with the warehouse basket (`HANDHELD_BASKET`, pure `Basket`), the amount cap from the listed stock, the device GUI `HandheldScreen`, the panel `HandheldHud`, and a binding to a warehouse controller (`HandheldBinding`).
- Sites: `site/SiteRecord` keeps the chunks a warehouse's site covers (`SiteChunks` keys).

## Goals / Non-Goals

**Goals:** order the STOCK basket from the Handheld to a Parcel Locker of the bound warehouse, collect it there, and return what is not collected.

**Non-Goals:** HH-2/HH-3 range tiers and sleeping sites (`m11-handheld-tiers`), Direct Delivery (`m11-direct-delivery`), autocrafting for orders, terminal orders to lockers.

## Decisions

### One block, ten positions
`ParcelLockerBlock` with a `PART` property (enum of the ten positions: `S_COLUMN_LOW`, `S_COLUMN_HIGH`, `MIXED_LOW`, `MIXED_HIGH`, `HEAD_DOCK`, `HEAD_SCREEN`, `M_COLUMN_LOW`, `M_COLUMN_HIGH`, `L_LOW`, `L_HIGH`) and `FACING`. The item places all ten when free, like the Fluid Inbound places two; every part points at the dock part, which holds the only block entity. Breaking any part removes all ten (`playerWillDestroy` on the head's set) and the dock drops the item and the box contents. *Alternative:* separate column blocks. Rejected: the layout is fixed by decision.

Box geometry lives in pure `LockerLayout` (box id, size, column, row) shared by model datagen, renderer and logic.

### Boxes and orders in the dock entity
`ParcelLockerBlockEntity` (at the dock) implements `DeliveryTarget`, `PutAwaySource`, `NamedMachine` and `MenuProvider`. It keeps one `LockedSlots` for all boxes (S 3, M 6, L 9 slots each by `warehouse.json` keys `locker_box_s`, `locker_box_m`, `locker_box_l`; slot ranges per box from `LockerLayout`), and a list of `LockerOrder(orderId, owner UUID, boxes, state RESERVED/READY, readySince game time)`, saved with the entity.

Pure `BoxAssignment.assign(freeBoxes, stacksNeeded)` returns the set with the least spare room, then the fewest boxes (exhaustive over at most 10 boxes, 1024 subsets). `stacksNeeded` is the sum of each entry rounded up to whole stacks by its stack size.

### Placing an order
`HandheldRequestPayload(lockerPos)` from the STOCK screen. Server:
1. Checks binding, range (pure `OrderRange.inRange(siteChunks, playerPos, margin)`: the player's block column within the site's chunk area widened by `handheld_order_margin`, same dimension), the locker belongs to the bound warehouse.
2. Clamps each basket entry to the unlocked stock now (`stockOf`), reports shortfalls.
3. Asks the locker for boxes (`BoxAssignment`); none, refuse.
4. Reserves the boxes with a new order id (`ProductionLedger.nextRequestId()`), then `planAndStart(lockerDockPos, key, amount, orderId)` per entry, so picks deliver into the dock entity's `deliveries()`. `DeliveryTarget.lockDelivery(jobId, requestId, key, amount)` (default: empty slots, as before) lets the locker lock IN room only in the boxes of the pick's order, so two orders never share slots; the boxes are counted slots, so picks of one item share a slot. `insertAs` puts each stack into the order's boxes; nothing else inserts into them. The order logic lives in `locker/LockerOrders`.
5. Empties `HANDHELD_BASKET`, sends the action bar note.

The picker data comes from `HandheldMenuStatePayload`: per locker its label, position, free boxes and a "fits" flag computed for the current basket on the server when the picker opens (`HandheldLockersPayload`). Distance is computed on the client from the positions and the player.

### Ready and notify
When an order's planned stacks are all in, it turns READY with `readySince = gameTime`; the dock plays a chime, the box lamps go green, and the owner gets `HandheldParcelPayload(locker label)` which the panel shows until the next order or collection. An order whose picks fail (stock gone, shuttle lost) turns READY with what arrived, or is dropped and its boxes freed when nothing arrived.

### Collecting
`ParcelLockerMenu` shows the opening player's boxes as take-only slots (a filtered view of `LockedSlots`, no insert), others' boxes as taken cells. Opened by right clicking the screen part with the Handheld or an empty hand (`useWithoutItem`/`useItemOn` on the screen part). A box whose slots are all empty and whose order is READY is freed; when all boxes of an order are free the order is removed.

### Returns
Every `fluid_check_interval`-like `locker_check_interval` ticks the warehouse checks READY orders older than `locker_return_ticks` (example 24000, `warehouse.json`). Those boxes join the put-away buffer (`PutAwaySource.drains(slot)`, true by default, is true only for the slots of expired orders), so the existing put-away planning takes them by zone rules. Stuck items stay and `setBlocked` shows the reason on the screen and in the potato line.

### Look
Datagen models per part from `LockerLayout`: yellow (Handheld bumper colour) box doors with a stamped id, grey steel frame, a handle and a small lamp per box (dark, orange, green). The lamps are block state properties of the parts (`LAMP_TOP`, `LAMP_BOTTOM`, the dock's own lamp on `LAMP_TOP`) that the dock entity sets from the order states, drawn by emissive multipart models, so no block entity renderer is needed. The screen part shows "SCAN TO OPEN" on an emissive LCD plate. The dock has a hatch and a lamp like the stations. Plane rules from CLAUDE.md apply; lamps and handles stand in front of the door faces without a back face. Textures come from `tools/locker-textures/gen.py`.

### Numbers, potato, recipe
`NamedMachine` kind `parcel_locker`, label "Parcel Locker #n". Potato line via the station report pattern. Recipe example: yellow dye, iron ingots, chests, a Data Cable and an Outbound Station.

### NeoForge APIs
`BlockEntityType`, `MenuType` with `IMenuTypeExtension.create`, `CustomPacketPayload` with `PayloadRegistrar.playToServer/playToClient` (already used in `ServusShuttlesPayloads`), `BlockEntityRenderer`, `Block.playerWillDestroy` and `onRemove`/`affectNeighborsAfterRemoval` as used by the Fluid Inbound for multi-part removal, `ValueInput`/`ValueOutput` for saving. No new capability exposure: the locker offers no item handler to pipes.

### Pure and Minecraft parts
Pure (JUnit): `BoxAssignment`, `LockerLayout` (counts, slot ranges), `OrderRange`, return timing (`LockerReturns.expired(readySince, now, returnTicks)`). Minecraft (GameTest): placement and removal of the ten parts, order reserves boxes and delivers, refusal without room, shortfall clamp, collecting frees boxes and is limited to the owner, returns put away, breaking drops contents and unlocks reserved stock. Look and GUI: play check.

### PLAN.md
Removes the open point "Handheld orders to a Parcel Locker" at archive and keeps "Direct Delivery upgrade"; adds "Handheld HH-2 and HH-3 range tiers (`m11-handheld-tiers`)".

## Risks / Trade-offs

- A big order holds many picks in the queue like a large terminal request; it is bounded by the locker's 45 stacks.
- An order in flight while its locker is broken: picks are cancelled like a terminal request whose destination is gone; loads in transit go back to the racks.
- The "fits" flag in the picker can be stale if another order lands meanwhile; SEND checks again on the server and refuses with a note.

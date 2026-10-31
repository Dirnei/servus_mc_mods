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
Two ways in, both on the screen part:
- **Handheld scan** (`useItemOn` with a `HandheldItem`): the entity opens the player's READY boxes in the world. It keeps per box an `openUntil` game time (`locker_door_open_ticks`, example 200) and sends the open boxes in its update tag; the screen state goes to HELLO (or NONE). The client staggers the swing by box order (about 8 ticks apart) and plays a ding per door; the scan itself plays the Handheld's chirp.
- **Empty hand** (`useWithoutItem`): `ParcelLockerMenu` as before (`NamedMachineScreen` with the name pencil), own boxes take-only, others' boxes taken.

Taking from an open box: right click (any hand) on a box part. The box comes from the part and the hit height inside the block through `LockerLayout.boxAt(part, hitY)` (S boxes are half a block). The owner's open box moves its stacks into the inventory (`Inventory.add`, the rest stays); an empty READY box is freed and closes. Another player's box sets the screen to DENY. The Data Potato still passes to its report.

### Screen states
`ParcelLockerBlock` gets `SCREEN` (IDLE, HELLO, NONE, LOAD, DENY) on the HEAD_SCREEN part, emissive LCD models per state. IDLE uses an animated texture (`.mcmeta`, QR with SCAN blinking). HELLO, NONE and DENY fall back after a few seconds through a scheduled tick; LOAD shows while picks of an order are arriving and returns to IDLE with the last one. HELLO shows the count of opened boxes in the renderer, drawn over the LCD as text, so the texture carries only the frame of that state.

### Doors, lamps and items: renderer
The doors leave the static model. `ParcelLockerRenderer` (block entity renderer on the dock, `shouldRenderOffScreen`, a render box over all ten parts) draws per box from `LockerLayout`: the door as a 1 px slab hinged on its left edge, turning to about 108 degrees with an ease out over 10 ticks when opened and back when closed; the door's lamp full bright in its colour (dark free, ochre someone else's, amber reserved, green ready); and in an open box up to three of its stacks as items with their counts. The update tag carries per box its state, open flag and owner UUID, and the first three stacks of open boxes; the client compares the owner with the local player for the lamp colour. The `LAMP_TOP`/`LAMP_BOTTOM` properties and lamp multipart models are removed.

### Dock lamp and roof
The dock lamp is a separate emissive element with an animated amber texture shown while the screen is LOAD (block state on the dock part `LOADING`). The roof is a model element 3 px tall on the top of the five high parts with a 1 px overhang at the front, back and both ends; its faces end at the part edges so neighbours never share a plane.

### Returns
Every `fluid_check_interval`-like `locker_check_interval` ticks the warehouse checks READY orders older than `locker_return_ticks` (example 24000, `warehouse.json`). Those boxes join the put-away buffer (`PutAwaySource.drains(slot)`, true by default, is true only for the slots of expired orders), so the existing put-away planning takes them by zone rules. Stuck items stay and `setBlocked` shows the reason on the screen and in the potato line.

### Look
From `temp/parcel-locker-look.html`. Textures by `tools/locker-textures/gen.py`: the doors at 32 px per block (small `S-01` ids, lamp spot, handle, sheen), door insides, the column sides and carcass (dark interior, 1 px ochre gaps right and bottom of each box), the roof, the dock on the Inbound Station side with a hatch, and the screen states at 32 px (steel bezel with yellow edge, red scan window, green LCD). Static models keep the carcass, frame, roof, screen and dock; the renderer draws doors, lamps and items. Plane rules from CLAUDE.md apply: closed doors stand 0.05 px in front of the carcass face.

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

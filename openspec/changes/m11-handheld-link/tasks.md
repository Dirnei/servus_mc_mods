# Tasks

## 1. Pure logic

- [x] 1.1 `shuttles/.../locker/LockerLayout.java`: the ten parts, box ids, sizes, columns and slot ranges from the box sizes. JUnit `LockerLayoutTest`: `sixSmallThreeMediumOneLarge`, `slotRangesDoNotOverlap`, `totalStacksFromSizes`. Run `:shuttles:test`.
- [x] 1.2 `shuttles/.../locker/BoxAssignment.java`: least spare room, then fewest boxes. JUnit `BoxAssignmentTest`: `smallOrderTakesOneSmallBox`, `twelveStacksTakeLargeAndSmall`, `sevenStacksWithoutLarge`, `tooBigIsRefused`, `stacksRoundedPerEntry`. Run `:shuttles:test`.
- [x] 1.3 `shuttles/.../locker/OrderRange.java` and `LockerReturns.java`: site area plus margin, same dimension; expiry by game time. JUnit `OrderRangeTest` (`insideSite`, `withinMargin`, `beyondMargin`, `otherDimension`), `LockerReturnsTest` (`expiresAfterReturnTime`, `reservedNeverExpires`). Run `:shuttles:test`.

## 2. Block and entity

- [x] 2.1 `locker/ParcelLockerBlock.java` with `PART` and `FACING`, `ParcelLockerItem` placing all ten parts by the stations' facing rule, removal of the whole piece, registrations in `registry/ServusShuttlesBlocks`, `ServusShuttlesItems`, `ServusShuttlesBlockEntities`, creative tab. Run `:shuttles:compileJava`.
- [x] 2.2 `locker/ParcelLockerBlockEntity.java` at the dock: `LockedSlots` for the boxes, `LockerOrder` list, save and load, `DeliveryTarget`, `PutAwaySource`, `NamedMachine`, drops on break; `warehouse.json` keys `locker_box_s`, `locker_box_m`, `locker_box_l`, `locker_return_ticks`, `locker_check_interval`, `handheld_order_margin`. Run `:shuttles:build`.

## 3. Orders

- [x] 3.1 `network/WarehouseNetwork.java`: locker membership by rail, order placement (clamp to unlocked stock, reserve boxes, `planAndStart` per entry into the dock), READY when all planned stacks are in, failed picks, returns of expired orders through put-away, cancel on removal. Run `:shuttles:build`.
  - Note: the order logic lives in `locker/LockerOrders.java`; `WarehouseNetwork` offers `pickFor` (planAndStart tagged with the order id), `reachRefusal`, `picksPending`, `parcelLockers` and the locker check on `locker_check_interval`. Picks lock room only in their order's boxes through the new `DeliveryTarget.lockDelivery`, and put-away takes only returning parcels through the new `PutAwaySource.drains(slot)`. Order ids come from `ProductionLedger.nextRequestId()`.
- [x] 3.2 `handheld/HandheldRequestPayload.java`, `HandheldLockersPayload.java`, `HandheldParcelPayload.java` registered in `terminal/ServusShuttlesPayloads`; server handling in `handheld/HandheldOrders.java` (binding, range, locker of the bound warehouse, empty the STOCK basket, action bar). Run `:shuttles:build`.
  - Note: REQUEST sends `HandheldPickerPayload`; the server answers with `HandheldLockersPayload`.

## 4. Client and GUI

- [x] 4.1 `client/HandheldScreen.java`: REQUEST button on STOCK, locker picker popup (sorted by distance, nearest preselected, greyed lockers, reason line, SEND and CANCEL); `client/HandheldHud.java`: parcel ready line. Run `:shuttles:build`.
- [x] 4.2 `menu/ParcelLockerMenu.java` and `client/ParcelLockerScreen.java`: own boxes take-only, others taken; opening by Handheld or empty hand on the screen part. Run `:shuttles:build`.
- [x] 4.3 `client/ParcelLockerRenderer.java` (box lamps), datagen models and textures per part from `LockerLayout`, blockstates, loot, recipe, lang through `datagen/ServusShuttlesLanguageProvider`; potato line. List overlapping boxes and shared planes in the task note. Run `:shuttles:runData` and `:shuttles:build`.
  - Note: no renderer. The lamps are block state properties (`LAMP_TOP`, `LAMP_BOTTOM`) with one emissive multipart model per box and lamp state, so no block entity renderer is needed; no loot table either, the dock's block entity drops the one item like the fluid stations. Textures from `tools/locker-textures/gen.py`.
  - Planes (models face north, front z 0): box part body z 1 to 16; doors z 0 to 1 inset 1 px from the block edges and each other, no back face (body front at z 1 is covered only where no door face is), an L door split over two blocks has no UP face below and no DOWN face above the seam; handle x 2 to 3, z -1 to 0, lamp x 4 to 6, z -0.3 to 0, both without back face (the door front is at z 0), 1 px apart so they never touch. Dock: body z 2 to 16 (hatch on its front), side bars x 0 to 3 and 13 to 16 full height z 0 to 2, top bar x 3 to 13 y 12 to 16 and bottom bar x 3 to 13 y 0 to 3 without end faces where they meet the side bars and without back faces; dock lamp x 6 to 10, z -0.5 to 0 without back face. Screen: body z 1 to 16 without front face, bezel of the same shape z 0 to 1, LCD plate x and y 2 to 14 at z 0.5 to 1 with only its front face. Faces between parts are on block boundaries and culled; the outer side faces of the dock and screen frames keep no cullface so the grooves of the box parts next to them never look into a culled face. No two faces share a plane where they overlap.

## 5. GameTests

- [x] 5.1 `gametest/ParcelLockerGameTests.java`: placement needs ten free spaces, break removes all and drops contents, numbered by its warehouse, order reserves boxes and delivers, too big refused before picking, shortfall clamped, collecting only own boxes frees them, expired parcel put away, break during an order unlocks stock, STOCK basket emptied by SEND and kept by cancel, out of range refused. Run `:shuttles:runGameTestServer`.

## 6. Verify

- [ ] 6.1 Full `gradlew build`, all tests and GameTests green, the game launches with all mods; play check: place a locker at a rail, order from the Handheld at several distances, watch the shuttle deliver and the lamps, collect, let a parcel expire, break a full locker, look for flicker on every part from several angles.

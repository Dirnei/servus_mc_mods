# Tasks

## 1. Pure logic

- [ ] 1.1 `shuttles/.../locker/LockerLayout.java`: the ten parts, box ids, sizes, columns and slot ranges from the box sizes. JUnit `LockerLayoutTest`: `sixSmallThreeMediumOneLarge`, `slotRangesDoNotOverlap`, `totalStacksFromSizes`. Run `:shuttles:test`.
- [ ] 1.2 `shuttles/.../locker/BoxAssignment.java`: least spare room, then fewest boxes. JUnit `BoxAssignmentTest`: `smallOrderTakesOneSmallBox`, `twelveStacksTakeLargeAndSmall`, `sevenStacksWithoutLarge`, `tooBigIsRefused`, `stacksRoundedPerEntry`. Run `:shuttles:test`.
- [ ] 1.3 `shuttles/.../locker/OrderRange.java` and `LockerReturns.java`: site area plus margin, same dimension; expiry by game time. JUnit `OrderRangeTest` (`insideSite`, `withinMargin`, `beyondMargin`, `otherDimension`), `LockerReturnsTest` (`expiresAfterReturnTime`, `reservedNeverExpires`). Run `:shuttles:test`.

## 2. Block and entity

- [ ] 2.1 `locker/ParcelLockerBlock.java` with `PART` and `FACING`, `ParcelLockerItem` placing all ten parts by the stations' facing rule, removal of the whole piece, registrations in `registry/ServusShuttlesBlocks`, `ServusShuttlesItems`, `ServusShuttlesBlockEntities`, creative tab. Run `:shuttles:compileJava`.
- [ ] 2.2 `locker/ParcelLockerBlockEntity.java` at the dock: `LockedSlots` for the boxes, `LockerOrder` list, save and load, `DeliveryTarget`, `PutAwaySource`, `NamedMachine`, drops on break; `warehouse.json` keys `locker_box_s`, `locker_box_m`, `locker_box_l`, `locker_return_ticks`, `locker_check_interval`, `handheld_order_margin`. Run `:shuttles:build`.

## 3. Orders

- [ ] 3.1 `network/WarehouseNetwork.java`: locker membership by rail, order placement (clamp to unlocked stock, reserve boxes, `planAndStart` per entry into the dock), READY when all planned stacks are in, failed picks, returns of expired orders through put-away, cancel on removal. Run `:shuttles:build`.
- [ ] 3.2 `handheld/HandheldRequestPayload.java`, `HandheldLockersPayload.java`, `HandheldParcelPayload.java` registered in `terminal/ServusShuttlesPayloads`; server handling in `handheld/HandheldOrders.java` (binding, range, locker of the bound warehouse, empty the STOCK basket, action bar). Run `:shuttles:build`.

## 4. Client and GUI

- [ ] 4.1 `client/HandheldScreen.java`: REQUEST button on STOCK, locker picker popup (sorted by distance, nearest preselected, greyed lockers, reason line, SEND and CANCEL); `client/HandheldHud.java`: parcel ready line. Run `:shuttles:build`.
- [ ] 4.2 `menu/ParcelLockerMenu.java` and `client/ParcelLockerScreen.java`: own boxes take-only, others taken; opening by Handheld or empty hand on the screen part. Run `:shuttles:build`.
- [ ] 4.3 `client/ParcelLockerRenderer.java` (box lamps), datagen models and textures per part from `LockerLayout`, blockstates, loot, recipe, lang through `datagen/ServusShuttlesLanguageProvider`; potato line. List overlapping boxes and shared planes in the task note. Run `:shuttles:runData` and `:shuttles:build`.

## 5. GameTests

- [ ] 5.1 `gametest/ParcelLockerGameTests.java`: placement needs ten free spaces, break removes all and drops contents, numbered by its warehouse, order reserves boxes and delivers, too big refused before picking, shortfall clamped, collecting only own boxes frees them, expired parcel put away, break during an order unlocks stock, STOCK basket emptied by SEND and kept by cancel, out of range refused. Run `:shuttles:runGameTestServer`.

## 6. Verify

- [ ] 6.1 Full `gradlew build`, all tests and GameTests green, the game launches with all mods; play check: place a locker at a rail, order from the Handheld at several distances, watch the shuttle deliver and the lamps, collect, let a parcel expire, break a full locker, look for flicker on every part from several angles.

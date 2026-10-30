# Tasks

## 1. Pure logic

- [x] 1.1 `Basket` in `shuttles/handheld`: entries of (key, amount), add merges an existing key, set amount, remove, clear, at most `MAX_ENTRIES` (9), adding a new key to a full basket refused. Verify: JUnit `BasketTest` (add, merge, set, remove, clear, full refuses, amount below 1 removes), `gradlew :shuttles:test`.
- [x] 1.2 `BasketFit` in `shuttles/lock`: whether (key, count) pairs fit into `SlotFill.Slot` states with a stack limit per key, partial stacks first, locked slots skipped. Verify: JUnit `BasketFitTest` (two items share two empty slots, one more does not fit, partial stack top up, locked slot ignored, exact fit), `gradlew :shuttles:test`.
  - Note: `BasketFit.place` places the planned picks one after another, each pick closing the slots it lands in as its IN lock would, and returns the slots per pick; the network locks exactly those slots, so check and locks agree.

## 2. Basket on the handheld

- [x] 2.1 Components `HANDHELD_BASKET` (list of item and amount, codec and stream codec) and `HANDHELD_PICK_TARGET` (boolean) in `registry/ServusShuttlesComponents`; `HandheldActions.setMode` clears the pick target flag. Verify: `gradlew :shuttles:build`, GameTest: a mode switch clears pick target and keeps the basket.
- [x] 2.2 `HandheldBasketPayload` (add, set, remove, clear) and `HandheldRelocatePayload` in `handheld`, registered in `terminal/ServusShuttlesPayloads`; the server edits the handheld of the open `HandheldMenu`; Relocate needs a bound handheld and a non empty basket, sets the flag and closes the menu. Verify: `gradlew :shuttles:build`, GameTests: payloads build a basket, Relocate with an empty basket changes nothing.

## 3. Relocation

- [x] 3.1 `HeavyRackBlockEntity` implements `network/DeliveryTarget` with its `items()`. Verify: `gradlew :shuttles:build`, existing shuttles GameTests green.
- [x] 3.2 `WarehouseNetwork.relocate(shelf, entries)`: refuse unless every bay is a Heavy Duty Rack of this warehouse; refuse entries the shelf's zone filter does not take; stock outside the shelf only; fit check with `BasketFit` over the shelf's slots; on fit, plan per entry with `RequestPlan`, lock OUT at sources and IN on the shelf bays, queue `PickJob`s with the bay as destination; return moved, short and refused per entry. Verify: `gradlew :shuttles:build`, GameTests: shuttles carry the basket into an empty unzoned shelf; zone filter refuses bread and moves raw copper; no room moves nothing and leaves no locks; 40 of 128 bread moves and 88 are reported short; stock on the target shelf is not used; IN locks on the shelf are released when a pick comes up short.
  - Note: `relocate(shelf, Map<ItemKey, Integer>)` returns a `network/Relocation` (outcome, and per item moved, short, refused); the zone rule is `WarehouseZones.takes`, the put-away rule for one block. A pick whose slots lie in two bays becomes one pick per bay.
- [x] 3.3 Target scan in `HandheldActions.scan`: in FIND with the flag set, relocate onto the scanned bay's shelf; green flash, beep, action bar with code, refusals and shortfall, flag cleared on success; red flash, buzz and flag kept on no room, a Fluid Rack, a rack of another warehouse. Lang keys through `datagen/ServusShuttlesLanguageProvider`. Verify: `gradlew :shuttles:runData` and `:shuttles:build`, GameTests: scan in pick target mode starts the move and clears the flag; Fluid Rack scan keeps the flag.
  - Note: Server logic lives in the new `handheld/HandheldRelocation` (basket edits, Relocate, target scan); `HandheldActions` only calls it. When nothing moves at all (everything refused or short) the scan buzzes red and pick target mode stays. A right click on a block that is no rack bay in pick target mode buzzes instead of opening the GUI; a click into the air still opens it.

## 4. Client

- [x] 4.1 FIND screen in `client/HandheldScreen`: basket row under the stock list with icons and short counts, amount popup on the Foundation `NumberStepper` for a stock item or a basket cell, shift click adds a stack, right click removes, Clear and Relocate buttons (Relocate disabled without entries or binding), basket full note. Verify: `gradlew :shuttles:build`, in game: build, edit and clear a basket.
  - Note: left click on a stock item adds it to the basket (popup, shift click a stack), right click lights up its shelves; the popup is the shuttles `BasketAmountPopup`, because the Foundation `AmountPopup` is package private and has Clear and Save only.
- [x] 4.2 Panel in `client/HandheldHud`: in pick target mode shows `PICK TARGET`, the entry count and the aimed shelf code. Verify: `gradlew :shuttles:build`, in game: panel after Relocate and after a mode switch.
- [x] 4.4 Play check fixes on main: the menu also syncs the player inventory so basket edits show at once (`menu/HandheldMenu.broadcastChanges`); amounts capped at the listed stock (`client/BasketAmountPopup`, `client/HandheldScreen`); ALL button back to the whole warehouse (`HandheldMenu.clickMenuButton`, lang). Verify: `gradlew :shuttles:build` and GameTests green.

## 5. Two baskets and a stashed relocation

- [x] 5.1 Components `HANDHELD_SHELF_BASKET` (bay pos plus basket) and `HANDHELD_RELOCATION` (source bay pos plus entries) in `registry/ServusShuttlesComponents` with codecs and equals/hashCode (no `ItemStack` as component type); `HandheldActions.setMode` clears the shelf basket, the relocation and the flag. Verify: `gradlew :shuttles:build`.
- [x] 5.2 `handheld/HandheldRelocation` and `menu/HandheldMenu`: basket edits go to the basket of the shown list; ALL, a FIND scan of another shelf and opening the GUI on another shelf empty the shelf basket; Relocate only from the shelf view, stashes into `HANDHELD_RELOCATION` and empties the shelf basket; the target scan uses the stash and clears it on success. Verify: `gradlew :shuttles:build`.
  - Note: both components use one record `handheld/ShelfBasket` (shelf key, the leftmost bay, plus entries), so a scan of another bay of the same shelf keeps the shelf basket. Opening the GUI in the air shows the warehouse basket and keeps the shelf basket. The FIND buttons are Clear, ALL, Relocate; Relocate is hidden in the warehouse view.
- [x] 5.3 `network/WarehouseNetwork.relocate`: picks only from the source shelf's bays; the source shelf as target is refused; shortfall from the source shelf. Verify: `gradlew :shuttles:build`.
- [x] 5.4 `client/HandheldScreen`: the basket row shows the basket of the shown list; Relocate only in the shelf view; panel entry count from the stash (`client/HandheldHud`). Verify: `gradlew :shuttles:build`.
- [x] 5.5 GameTests in `gametest/HandheldRelocateGameTests`: warehouse basket kept over ALL and mode switch, shelf basket emptied by ALL, by another shelf scan and by a mode switch, kept over a GUI close; Relocate empties the shelf basket; relocation takes only from the source shelf; source shelf refused as target; shortfall from the source; mode switch drops the relocation. Adapt the tests of the old behaviour. Verify: `gradlew :shuttles:runGameTestServer` green.

## 7. FIND and INFO merged, STOCK added

- [x] 7.1 `handheld/HandheldMode`: ZONE, INFO, STOCK, codec reads "find" as INFO; `HandheldActions` scan per mode (INFO scans and opens the GUI, STOCK switches to INFO and scans, target scan first in INFO), `setMode` as before. Verify: `gradlew :shuttles:build`.
- [x] 7.2 `menu/HandheldMenu` and `handheld/HandheldRelocation`: shelf view from the last INFO scan, warehouse view in STOCK, baskets by screen, Relocate only from INFO on a Heavy Duty Rack shelf; remove the ALL button and `clickMenuButton`. Verify: `gradlew :shuttles:build`.
- [x] 7.3 `client/HandheldScreen`, `client/HandheldHud`, `client/ClientHandheld`: INFO screen with readout lines, item grid, shelf basket, Clear and Relocate (fluid shelf readout only); STOCK screen with list, warehouse basket and Clear; right click finds in both; softkeys ZONE, INFO, STOCK; panel per mode. Lang via `datagen/ServusShuttlesLanguageProvider` (remove FIND and ALL keys). Verify: `gradlew :shuttles:runData` and `:shuttles:build`.
- [x] 7.4 GameTests in `gametest/HandheldGameTests` and `gametest/HandheldRelocateGameTests`: mode order and wrap, "find" loads as INFO, STOCK scan switches to INFO, INFO scan opens the shelf view, find from the INFO list, Relocate refused in STOCK; adapt every test that used FIND. Verify: `gradlew :shuttles:runGameTestServer` green.
  - Note: the menu always carries the warehouse stock; the INFO screen draws the shelf from the last INFO readout, and the shelf view is the last scanned bay (`HANDHELD_PIN`) while the handheld is in INFO. GameTests run with mock players, which open no GUI and get no packets, so the find from a list is covered by `HandheldActions.found` for an item of the INFO readout.

## 6. Verify

- [ ] 6.1 Full gradle build, tests green, game launches with all mods; play check: build a basket, Relocate, scan a target shelf, shuttles fill it, zone refusal, no room refusal, modes ZONE, INFO, STOCK, INFO basket emptied by another scan and a mode switch, STOCK basket kept, items only from the source shelf. Verify: `gradlew build` and `gradlew test` green, client starts from main.

## Workflow follow-up

- Apply only after `m6-warehouse-handheld` is archived.
- At archive: Added line in `shuttles/CHANGELOG.md`, groom the PLAN.md open point, keep the Request action as the next open point.

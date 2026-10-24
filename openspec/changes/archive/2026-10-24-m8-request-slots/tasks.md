# Tasks

## 1. Slot choice

- [x] 1.1 Add pure `shuttles/terminal/RequestSlots` (same item with room first, then leftmost empty unowned slot, else -1) with `RequestSlotsTest`: `sameItemFirst`, `ownedSlotSkipped`, `endedItemBlocksOtherItems`, `fullRowGivesNone`, `sameItemWithoutRoomSkipped`; run `gradlew :shuttles:test`.

## 2. Requests own a slot

- [x] 2.1 `StorageTerminalBlockEntity`: `Request.slot`, saved as `Slot`, assigned on load when missing; `newRequest` takes the slot; `RoutingHandler` puts packets only into the slot of the open request waiting for that item; `credit` books on the slot's owner; `TerminalRequests.fits` checks the request's slot. Run `gradlew :shuttles:build`.
- [x] 2.2 `TerminalRequests.request`: grow an open request of this site for the key without an open work order, else choose a slot or refuse `output_full`; `craft`, `craftOneOff` and `RemoteTransfers` take a slot or refuse `output_full`. Add the `output_full` refusal text in `ServusShuttlesLanguageProvider` and run `:shuttles:runData`. Run `gradlew :shuttles:build`.
- [x] 2.3 GameTests in `TerminalRequestGameTests`: one item request lands in its slot; three requests of the same item make one request; ten different items refuse the tenth with `output_full`; a 94 request keeps one slot and waits for room until the slot is emptied; packets never land in another request's slot; cancel keeps delivered items and the slot stays taken until emptied; slot survives a save and load. Run `:shuttles:runGameTestServer`.

## 3. Screen

- [x] 3.1 `RequestRow` carries the slot; `StorageTerminalScreen.clickGrid`: left click requests 1, shift a stack, right click opens the amount prompt, Craft entries and middle or Ctrl on craftables open the craft dialog; grid no longer holds request cells (`Cell.isRequest` and request drawing removed). Run `gradlew :shuttles:build`.
- [x] 3.2 `StorageTerminalScreen`: draw requests on the Output row in `extractSlots` (ghost item when empty, progress bar, state mark) and their tooltip in `extractTooltip` with the cancel hint; middle click or Ctrl click on an owned output slot sends `CancelRequestPayload` before the vanilla slot click. Lang keys for the tooltip lines via `ServusShuttlesLanguageProvider` and `:shuttles:runData`. Run `gradlew :shuttles:build`.
- [x] 3.3 Update `StorageTerminalRowsGameTests` and any GameTest that read requests from the grid rows; add the new M8 point to PLAN.md "Open: Shuttles". Run `:shuttles:runGameTestServer`.

## 4. Verification

- [x] 4.1 Full `gradlew build`, `gradlew test` and `:shuttles:runGameTestServer` green; the game launches with all mods.
- [x] 4.2 Play check in game by the user: click requests 1 and repeated clicks add up in one slot; shift click a stack; right click opens the prompt; requests show on the Output row with ghost, bar and tooltip; middle and Ctrl click cancel; a request over a stack refills its slot; a tenth request is refused with "Output row full, take your items"; nothing overlaps on the Output row.

## Workflow follow-up

- Archive after the play check; add a Shuttles CHANGELOG line under Unreleased and groom PLAN.md.

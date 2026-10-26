# Tasks

Prerequisites: `m8-station-numbers`, `m8-terminal-tabs` and `m8-rule-cap` are applied.

## 1. Pure export logic

- [x] 1.1 Add `shuttles/.../order/ExportPlan` (`surplus`, `bounded`, `validExport`) with JUnit `ExportPlanTest`: below, at and above the threshold, queued amounts subtracted, one stack per free slot bound, absent Fill up to passes, Export above not over Fill up to fails; `gradlew :shuttles:test` green
- [x] 1.2 Add generic `shuttles/.../order/ExportBook<K, P>` (holder, claim moves, occupied slot refused, releaseSlot, releaseStation, clear) with JUnit `ExportBookTest`; `gradlew :shuttles:test` green

## 2. Rules carry the export

- [x] 2.1 Extend `order/StockRule` with the optional export threshold and `ExportTarget`, `order/StockRules` with an `ExportBook` and save and load of `Export`, `ExportStation`, `ExportSlot` (old saves load without export); clearing the threshold or removing the rule clears the claim; `gradlew build` green
- [x] 2.2 Validate Export above against Fill up to on save in `network/StockRuleKeeper.set` and the Rule tab Save of `client/StorageTerminalScreen` (refusal message in the shuttles lang datagen); GameTest in a new `gametest/StockRuleExportGameTests` (holding this change's GameTests) that a rule with Export above not over Fill up to is refused; `gradlew build` and GameTests green

## 3. Deliveries and release

- [x] 3.1 In `network/WarehouseNetwork.restock` plan exports after keep slots with `ExportPlan` and the shared free slot budget, record the per export outcome and set the station's short stack with reason `export_full` (lang string); GameTests: surplus above the threshold is shipped, nothing below it, no second request for surplus on its way, full station turns the lamp red; `gradlew build` and GameTests green
- [x] 3.2 Release claims when the network drops a station member (`WarehouseNetwork` rebuild path) and keep them for unloaded stations; GameTests: release on break, on carrot dismantle and on rail removal, rail placed again does not restore it; `gradlew build` and GameTests green

- [x] 3.3 Incoming surplus skips the racks: `ExportPlan.direct` (JUnit in `ExportPlanTest`: below, across and above the threshold), `network/WarehouseNetwork.drain` sends the part above the threshold as a put-away targeting the Outbound Station that holds the export (IN lock in its buffer, joins an open put-away to it), `unloadInto` fills that station, `pendingTo` and `capRoom` count those put-aways, `shuttle/ShuttleFleet` gets `putAwaysWaiting`, `putAwayCargoTo` and a target filter on `openPutAway`; GameTests: terminal input above the threshold goes straight to the station, a full station falls back to storing; `gradlew build` and GameTests green

## 4. Station claim UI

- [x] 4.1 `menu/OutboundStationMenu`: refuse left clicks and scrolls on export slots, right click releases, `outbound/OutboundStationBlockEntity.setKeep` refuses export slots; new `outbound/ClaimExportPayload` registered in `terminal/ServusShuttlesPayloads` with server checks (online, slot free, rule has an export); GameTests: claim an unassigned export, claim moves from another station, claim into an occupied slot refused; `gradlew build` and GameTests green
- [x] 4.2 New `outbound/ExportListPayload` sent through `PayloadSync` from `OutboundStationMenu` (this station's export slots, claimable exports with holder labels from `m8-station-numbers`); `gradlew build` green
- [x] 4.3 `client/OutboundStationScreen`: export marker and threshold on export slots with tooltip, left click on an empty keep slot with an empty hand opens the Claim an export picker (unassigned list, held elsewhere list, search `EditBox` over item name and label, "nothing to claim" line); lang strings in the shuttles datagen; launch the client and check claim, move, search and release in game

## 5. Terminal

- [x] 5.1 Extend `terminal/RuleRow` and its sync with the export threshold, holder label and export state; fill them in `network/StockRuleKeeper.rows`; show them in the Rules tab Export and Station columns, the Rule tab "Export goes to" or "No station" line and the grid tooltip in `client/StorageTerminalScreen`; launch the client and check no station, ok, exporting and station full

## 6. Wrap up

- [x] 6.1 Full `gradlew build`, `gradlew test` and GameTests green, and the game launches with all mods; play check a wheat export into a Sink through an Outbound Station

## Workflow follow-up

- On archive: groom PLAN.md (remove "warehouse excess on stock rules"), add an Added line to `shuttles/CHANGELOG.md` under Unreleased.

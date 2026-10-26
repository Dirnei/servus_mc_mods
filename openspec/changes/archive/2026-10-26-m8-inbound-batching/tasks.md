# Tasks: m8-inbound-batching

Prerequisites: `m8-rule-cap`, `m5-duct-endpoint-gui` and `m5-duct-keep-amounts` are archived. Re-read `InboundStationBlockEntity.intake`, `ItemChannelNetwork.pull` and `route` after they land; the line references in design.md are from main `8cecf03d`.

## 1. Stacked inserts

- [x] 1.1 `foundation/.../logistics/network/ItemChannelNetwork`: replace `insertAnySlot` in `route` and `capacity` with `ItemView.insertItemStacked`; add GameTests in new `foundation/.../logistics/gametest/DuctStackingGameTests` for the scenarios "Chest with a partial stack" and "Partial stack full"; `gradlew build` and the Foundation GameTests green
- [x] 1.2 `shuttles/.../station/InboundStationBlockEntity.pullOneStack`: simulate and insert with `buffer.insertItemStacked`; GameTest "Pull fills the partial slot first" in new `shuttles/.../gametest/InboundBatchingGameTests` (with an unlocked partial slot for now); `gradlew build` and GameTests green

## 2. Top up a planned slot (root cause)

- [x] 2.1 Pure: add `SlotFill.unreserved(count, reservedOut)` and new `shuttles/.../lock/TopUp.join(jobTotal, surplus, stackSize)` (how much surplus joins a job capped at a stack); JUnit `SlotFillTest` (unreserved with none, part and all reserved) and new `TopUpTest` (room in the job, job full, surplus bigger than the room, stack size 16); `gradlew :shuttles:test` green
- [x] 2.2 `shuttles/.../lock/LockedSlots`: add a top up mode (constructor flag) in which a slot holding only OUT locks takes inserts of its own item up to its limit and still refuses other items and every extract not by its job; `isValid` follows; switch it on for the Inbound buffer in `InboundStationBlockEntity`; GameTests in `SlotLockGameTests`: "Pull side tops up a planned slot", "Other items stay out", and an extract through the intake and the GUI still refused; replace the old "Pull side cannot top up" test; `gradlew build` and GameTests green
- [x] 2.3 `shuttles/.../network/WarehouseNetwork.drain`: stop skipping locked slots; per slot take the unreserved surplus and grow the OUT lock of the open put-away already on the slot (new `LockedSlots.growOut(slot, jobId, more)` plus `extendIn` on the rack, through `TopUp`), else `joinOpenPutAway`, else plan a new put-away with a second OUT lock on the slot; keep `put_away_per_tick` as the budget; GameTests in `InboundBatchingGameTests`: "Hopper trickle fills one slot" (64 cobblestone in one slot, one `PutAwayJob`), "Topped up before loading" (one trip of 32), "Arriving after loading" (a new job for the 8), "Inbound slot grows" and "Job full" from `shuttles/slot-locks`; `gradlew build` and GameTests green

## 3. Shared grant helper

- [x] 3.1 New `shuttles/.../station/BufferGrants` holding a `GrantBook` over a `LockedSlots`: `offer` with a slot planner, `receive` through `fillAs`, `close` releasing the IN locks, `holdsGrant`, `left(key)`, `dropGrantLocks` and the sweep; move `PackingStationBlockEntity`'s grant code onto it without behaviour change; `WarehouseNetwork.grantLock` asks any `BufferGrants` holder; `gradlew build` and the existing `PackingStationGameTests` green

## 4. Inbound Station answers offers

- [x] 4.1 Pure: a one-slot grant plan for the Inbound Station (`SlotGrant.plan` given at most one free slot, amount capped at the stack and the cap room); extend `SlotGrantTest` (or add it) for 200 offered, 20 offered, no free slot, cap room 26; `gradlew :shuttles:test` green
- [x] 4.2 `InboundStationBlockEntity implements PacketEndpoint` through `BufferGrants`: refuse offline, no storage (`putAwayTarget`), no free slot and no cap room; one empty slot IN locked under `nextJobId()`; grants not saved, IN locks without a grant dropped on load; sweep in `serverTick`; register `ServusFoundationCapabilities.PACKET_ENDPOINT` in `ServusShuttlesEvents` for sides that are not Off and not the front; GameTests in `InboundBatchingGameTests` for "A chest of cobblestone", "Less than a stack", "Buffer full", "Offline" (offered by hand through the capability), "Restart while a pipe fills a slot" (reload drops the lock) and that a data line next to the station gains no member; `gradlew build` and GameTests green
- [x] 4.3 Cap: `InboundStationBlockEntity.held(key)` adds the open grants' `left()`; GameTests in `StockRuleCapGameTests` for "Grant at the cap", "Open grant blocks a hopper" and "Grant ends short", plus the existing cap tests still green; `gradlew build` and GameTests green

## 5. The pipe announces its pulls

- [x] 5.1 Pure: new `foundation/.../logistics/duct/PipeOffer.amount(movable, stackSize, receiveAllowance)` (no allowance means none); JUnit `PipeOfferTest` (200 movable gives 64, 34 after keep, stack size 16, fill up allowance 10, nothing movable gives 0); `gradlew :foundation:test` green
- [x] 5.2 `ItemChannelNetwork`: endpoint record gains a `BlockCapabilityCache` for `PACKET_ENDPOINT`; `route` takes a nullable pull context (source endpoint, item); during a pull `capacity` offers to a reached endpoint without a grant for (endpoint, item) via `PipeOffer` and uses `grant.left()` when granted, today's capacity when refused; the real pass sends into a granted target with `Grant.deliver`; pushes stay as they are; GameTests in new `shuttles/.../gametest/DuctGrantGameTests` with the Inbound Station as the endpoint (Foundation has no endpoint block): "Station told about a stack" (offer before the first item, rate unchanged), "Keep amount respected", "Refused offer", "Pushed items are not announced"; `gradlew build` and GameTests green
- [x] 5.3 `ItemChannelNetwork` grant lifetime: grants per (target endpoint, item) with their source endpoints; after each pull cycle close filled grants, grants whose sources give no more of the item and grants whose endpoints left (`endpointsChanged`); `onDisposed` closes all; GameTests "Next stack" and "Source dry" in `DuctGrantGameTests`; `gradlew build` and GameTests green
- [x] 5.4 The user's case in `InboundBatchingGameTests`: a chest with 64 cobblestone behind a duct Pull side, the duct beside an online Inbound Station with one shuttle: one buffer slot used, no shuttle sent before all 64 arrived, one put-away trip of 64; a chest with 192 cobblestone: at most two slots in use at any time, three trips of 64; "Chest runs dry" (40 logs, one trip), "Pipe broken" (30 put away); the `m8-rule-cap` scenario "Duct falls back" still passes; `gradlew build` and GameTests green

## 6. GUI

- [x] 6.1 `shuttles/.../network/SlotLocksPayload.Entry` gains a `pipe` flag set when the lock id belongs to an Inbound pipe grant, amount = lock amount minus the slot's count; `client/LockOverlay.line` reads "Reserved: pipe brings {left} more {item}" with lang key `gui.servus_shuttles.lock.in_pipe` in `datagen/ServusShuttlesLanguageProvider`; `gradlew build` and `runData` clean; the tooltip "Half filled grant slot" is checked in the client with the play check of 7.1

## 7. Integration

- [x] 7.1 Full `gradlew build`, `gradlew test` and all GameTests green, the game launches with all mods, and a play check: a chest of 3 stacks of cobblestone on a duct Pull side into an Inbound Station fills one slot at a time, each slot shows the pipe tooltip while filling, and each stack goes into storage in one shuttle trip; a hopper trickle fills one slot; the station never looks full from one stack

## Workflow follow-up

- Archive after the play check sign-off; add under `## Unreleased` in `shuttles/CHANGELOG.md` (Fixed: a pipe or hopper feeding an Inbound Station fills one slot per stack and one shuttle takes the whole stack) and in `foundation/CHANGELOG.md` (Changed: Item Pipes fill partial stacks first and tell warehouse stations how much they bring).
- No PLAN.md open point closes; when the play check asks for more inspection, add the Inbound Station to the Data Potato open point.

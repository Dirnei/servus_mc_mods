## 1. Pure logic

- [ ] 1.1 Terminal slot chooser in `shuttles/.../terminal/` returns several slots: same item with room first, then leftmost free empty slots, up to `ceil(amount / stackSize)`, at least one, else none. Extend its test: 130 cobblestone gets slots 0 to 2; two free slots for 256; same item first; owned slots skipped. Check: `gradlew :shuttles:test`.
- [ ] 1.2 `shuttles/.../terminal/RequestRoom.java`: grant room from slot counts, stack size and held grants. Add `RequestRoomTest`. Check: `gradlew :shuttles:test`.
- [ ] 1.3 `shuttles/.../packing/PickAdmission.java`: admit a pick by granted, admitted, job amount, endpoint present and buffer room. Add `PickAdmissionTest`: within grant, beyond grant waits, no endpoint falls back to room, zero grant admits nothing. Check: `gradlew :shuttles:test`.

## 2. Foundation

- [ ] 2.1 `foundation/.../data/packet/PacketLimits` and `PacketLimitsManager`: `stall_ticks` next to the existing `grant_hold_ticks`; `resources/data/servus_foundation/data/packets.json` with 600. Check: `gradlew :foundation:build`.
- [ ] 2.2 `Shipment`: stall counter for deliveries without a grant, fail with `DeliveryReason.STALLED`; `GrantBook.sweep` already ends unbound grants after `grantHoldTicks`. GameTests (in shuttles, Foundation has no block with an endpoint): a full target without endpoint stalls after the ticks and reports what arrived; a target that frees room in time finishes; an unused held grant ends. Check: `gradlew :shuttles:runGameTestServer`.

## 3. Storage Terminal

- [ ] 3.1 `terminal/StorageTerminalBlockEntity`: `Request.slots` list with save and load (old `slot` key read as a list of one), endpoint for request slots and the Input row with `RequestRoom`, registered in `ServusShuttlesEvents`; `terminal/TerminalRequests` takes several slots on create and on growth. Check: `gradlew :shuttles:build`.
- [ ] 3.2 `menu/StorageTerminalMenu` and `client/StorageTerminalScreen`: a request shown on each of its slots, waiting for room per slot. Check: `gradlew :shuttles:build`, every payload type registered.

## 4. Packing Station egress

- [ ] 4.1 `packing/PackingStationBlockEntity`: per shipment grant, `admitted`, waiting reason; `ship` offers per push interval, starts a delivery per shipment with the held grant once its admitted items are there, runs several deliveries, always calls `returnOrphans`; stalled deliveries keep the shipment open; status line shows the waiting target in the alert colour. Check: `gradlew :shuttles:build`.
- [ ] 4.2 `network/WarehouseNetwork.readyToSend` asks the station's `PickAdmission` for Packing Station destinations. Check: `gradlew :shuttles:build`.
- [ ] 4.3 `cpu/CpuCollector`: a delivery to a terminal that fails at once opens no ledger transfer and is retried next round. Check: `gradlew :shuttles:build`.
- [ ] 4.4 Lang for the waiting target status and `output_full` refusal (`datagen/ServusShuttlesLanguageProvider`). Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 5. GameTests

- [ ] 5.1 `gametest/TerminalRequestGameTests`, `PackingStationGameTests`, `StorageTerminalRowsGameTests`: 128 copper with free slots fills two slots; with one free slot the second stack waits in the rack (no copper in the To network buffer) and arrives after the slot is emptied; a 16 iron request behind a full copper slot arrives; requestable count stays without the waiting copper; a request with slots 4 and 5 survives a save and load; a stalled delivery to a full Machine Adapter target does not block another shipment; orphans return to To storage while a shipment waits. Check: `gradlew :shuttles:runGameTestServer`.

## 6. Plan and integration

- [ ] 6.1 `PLAN.md`: keep "Deeper inspection with the Data Potato" open and mention Packing Station deliveries there. Check: `openspec validate m8-wtp-egress --strict`.
- [ ] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :shuttles:runGameTestServer` green; then the user's play check of all three changes: hand in mixed stacks (whole slots, one shuttle per stack), request 128 copper with and without free slots, request something else behind a full slot, two warehouses on one line show the conflict.

## Workflow follow-up

- Archive last, after `m8-one-warehouse-per-line` and `m8-wtp-ingress`, once the user's play check passed; `foundation/CHANGELOG.md` and `shuttles/CHANGELOG.md` lines under Unreleased at archive.

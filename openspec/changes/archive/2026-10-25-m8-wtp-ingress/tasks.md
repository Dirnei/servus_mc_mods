## 1. Pure logic

- [x] 1.1 `shuttles/.../packing/SlotGrant.java`: plan whole slot grants from free slot indices, stack size and amount. Add `SlotGrantTest`: 200 at 64 with two free slots is 128 over two slots; 16 ender pearls in one slot; exact fit; no free slot is 0; amount smaller than a stack takes one slot. Check: `gradlew :shuttles:test`.
- [x] 1.2 `shuttles/.../packing/IngressRanking.java`: order candidates by cost, unreachable last, lowest position on a tie. Add `IngressRankingTest`. Check: `gradlew :shuttles:test`.

## 2. Foundation handshake

- [x] 2.1 `foundation/.../data/packet/PacketEndpoint.java`, `Grant.java`; `production/ServusFoundationCapabilities.PACKET_ENDPOINT`; `DeliveryReason.REFUSED` and a refusal key on `Delivery`. Check: `gradlew :foundation:build`.
- [x] 2.2 `data/packet/DataPackets`: offer on start when the target face has an endpoint, fail refused at once, shrink the delivery to the grant, overload taking a held `Grant`, running delivery registry with `running(id)`; `GrantBook` (open grants and their sweep) with `grant_hold_ticks` in `PacketLimits` and `packets.json` (example 1200). `Shipment`: insert through `receive`, accept up to the grant's rest, close the grant once on every end path (done, fail, settle). Check: `gradlew :foundation:build`.
- [x] 2.3 Handshake GameTests in `shuttles/.../gametest/PacketEndpointGameTests` (Foundation has no block with an endpoint, so the Packing Station serves as the endpoint and the Crafting Station as a target without one): partial grant moves only the granted amount and leaves the rest at the source; a refusal fails at once with the reason and takes nothing; a second delivery cannot fill reserved room; a delivery ending short closes its grant with the arrived count; a target without endpoint works as before. Check: `gradlew :shuttles:runGameTestServer` (after 3.1).

## 3. Packing Station endpoint

- [x] 3.1 `packing/PackingStationBlockEntity`: implement `PacketEndpoint` for the To storage buffer (`SlotGrant` over empty unlocked slots, IN locks with ids from `WarehouseNetwork.nextJobId`, refusals `offline`, `no_storage`, `to_storage_full`), `receive` with `insertAs`, `close` releasing locks and marking the station pending, sweep every 20 ticks with `DataPackets.running`, `onLoad` drops IN locks; register the capability in `ServusShuttlesEvents`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `network/WarehouseNetwork`: `sweepLocks` skips the station's grant locks; `offerIngress(sender, key, amount)` with `IngressRanking` over stations reaching the sender and the trip cost to `putAwayTarget`. Check: `gradlew :shuttles:build`.
- [x] 3.3 Wireless arrivals without a terminal ask the receiving station for a grant and move only into it (`PackingStationBlockEntity` wireless path, `intakeRoom` removed where it served this). Check: `gradlew :shuttles:build`.
- [x] 3.4 `resources/data/servus_shuttles/shuttles/packing_station.json` `items_per_second` 10. Check: `gradlew :shuttles:build`.

## 4. Senders

- [x] 4.1 `terminal/StorageTerminalBlockEntity.storeInput` through `offerIngress` with the refusal as `inputRefusal`; remove `nearestPackingStation`. Check: `gradlew :shuttles:build`.
- [x] 4.2 `cpu/CpuCollector`, `cpu/CpuBlockEntity.fromBuffer` and `emptyBuffer` through `offerIngress`; remove `collectionPoint`. Check: `gradlew :shuttles:build`.
- [x] 4.3 Lang for the refusal keys on the terminal message line and the Packing Station status (`datagen/ServusShuttlesLanguageProvider`). Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 5. GameTests

- [x] 5.1 `gametest/PackingStationGameTests`, `StorageTerminalRowsGameTests`: three Input stacks of different items take three slots and each arrives whole; no put-away job exists until a slot is full, then one shuttle takes 64; two free slots grant 128 of 200; logs with no storage stay in the Input row with `no_storage`; Input row goes to the station with the cheaper trip, and to the other one when it is full; a grant ending short puts the rest away; a reload drops grants and puts the buffer away; timing tests updated to the rate from data. Check: `gradlew :shuttles:runGameTestServer`.
- [x] 5.2 CPU tests that collect outputs into storage still pass through the ingress choice (`CpuBufferGameTests`, `PatternResultGameTests`), wireless into storage without a terminal reserves a slot (`ControllerWirelessGameTests`). Check: `gradlew :shuttles:runGameTestServer`.

## 6. Plan and integration

- [x] 6.1 `PLAN.md` "Open: Shuttles" keeps the handshake open point for `m8-wtp-egress`; the architecture line names the packet endpoint; `docs/` Shuttles and Foundation pages describe grants and the rate of 10. Check: `openspec validate m8-wtp-ingress --strict`.
- [x] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :shuttles:runGameTestServer` green; launching the game with all mods is left to the user's play check, which covers all three changes.

## 7. Play check fixes

- [x] 7.1 An input slot whose move runs takes nothing more (`terminal/StorageTerminalBlockEntity` input slot limit), so a stack shift clicked in meanwhile gets its own slot and grant instead of topping up the leaving slot and leaving a short remainder for a third To storage slot. GameTest `StorageTerminalRowsGameTests.stackHandedInMeanwhileTakesOwnSlot`. Check: `gradlew :shuttles:runGameTestServer`.

## Workflow follow-up

- Archive after `m8-one-warehouse-per-line` and before `m8-wtp-egress`, after the user's play check; `foundation/CHANGELOG.md` and `shuttles/CHANGELOG.md` lines under Unreleased at archive.

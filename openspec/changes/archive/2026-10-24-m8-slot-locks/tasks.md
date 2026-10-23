## 1. Pure logic

- [x] 1.1 `shuttles/.../lock/SlotFill.java`: IN fill (partial top ups, then empty), IN into empty slots only, OUT take (smallest first), room, free slots, available, all over unlocked slots. JUnit `SlotFillTest`: top up before empty, locked slots skipped, empty only ignores partials, out takes smallest first and stops at the amount, room and available leave out locked slots. Check: `gradlew :shuttles:test`.
- [x] 1.2 `shuttles/.../zone/PutAwayRank.java`: tier, holding (partial first, then cost), others by free slot bucket then cost, join order last, no room dropped. JUnit `PutAwayRankTest`: tier beats everything, holder beats emptier, partial beats cheaper holder, bucket beats cost, cost within a bucket, join order on exact ties, quarter buckets for 27 slots. Check: `gradlew :shuttles:test`.
- [x] 1.3 `shuttles/.../shuttle/RailCosts.java` (Dijkstra over previous and node, multi start, cost to node, cheapest goal, path) and `shuttles/.../shuttle/TravelWeights.java` (rail, corner, lift block, handover plus lift wait on ride entry). JUnit `RailCostsTest`, `TravelWeightsTest`: straight beats corners, flat beats a slow lift, busy lift avoided, multi start, unreachable is infinite, path ends on the cheapest goal. Check: `gradlew :shuttles:test`.
- [x] 1.4 `RequestPlan`: stock carries a cost, cheapest first, input order on ties. Extend `RequestPlanTest`. Check: `gradlew :shuttles:test`.

## 2. Lockable slots

- [x] 2.1 `shuttles/.../lock/SlotLock.java`, `LockedSlots.java`, `LockHolder.java`: locks per slot, owner bypass, insert and extract as a job, release by job, save and load `Locks`. GameTest-free unit coverage through `SlotFill`; check: `gradlew :shuttles:build`.
- [x] 2.2 Heavy Rack bay, Inbound Station buffer, Packing Station To network, Outbound Station buffer use `LockedSlots`; `DeliveryTarget.deliveries()` returns it; claims become OUT locks (`isClaimed`, `claimedBy`, `claim`, `release` removed or mapped); Outbound `pushInto` and Packing `returnOrphans` skip locked slots. Check: `gradlew :shuttles:build`.

## 3. Travel cost

- [x] 3.1 `warehouse.json` `travel_costs` and `free_slot_bucket`; `ShuttlesStatsManager.travelWeights()`, `freeSlotBucket()`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `ShuttleFleet`: lift load snapshot, `costsFrom(starts)`, `path(shuttle, from, goals)` for every driven leg (warehouse legs, home, restore, `FieldRuns` outside the field), `nearest` by cost with one search per free shuttle; `refreshHome` uses `equals`. Check: `gradlew :shuttles:build`.

## 4. Planning with locks

- [x] 4.1 `WarehouseNetwork.drain`: rank targets with `PutAwayRank` and cost from the station, lock IN in the bay and OUT on the station slot, job amount limited to the bay's room; `putAwayTarget` (field runs) uses the same ranking without locking. Check: `gradlew :shuttles:build`.
- [x] 4.2 Picks: unlocked stock per bay with cost from the destination, OUT locks at planning, destination IN lock when free, `Host.readyToSend` in `assignJobs`; outbound restock asks only for free slots; `available` leaves out OUT locks; Packing Station choice by cost. Check: `gradlew :shuttles:build`.
- [x] 4.3 Load and unload as the job (`loadPutAway`, `loadPick`, `unloadInto`, `deliverTo`), `releaseLocks` on refuse, request removal, dropped cargo; `sweepLocks` over every lock holder; broken bay drops and replans its picks. Check: `gradlew :shuttles:build`.

## 5. Leftovers and the parked state

- [x] 5.1 `Load.stops`, leg `STRANDED`, `Shuttle.strandReason`; re-route leftovers by the put-away rule with new locks, pick cargo for a gone destination stored and counted missing; no TO_STATION or WAITING_ROOM for items; strand at the garage, retry on `roomChanged()` and every 100 ticks; restore parks stranded instead of dropping; `FleetSaves` `Stops` and `Strand`. Check: `gradlew :shuttles:build`.
- [x] 5.2 Garage: `ACTIVITY` on `ShuttleGarageBlock`, blockstate datagen with `shuttle_garage_front_blocked.png` (red lamp recolour), lamp set from the fleet; `GarageStrandedPayload` via `ShuttleGarageMenu`, line, tooltip and red slot wash in `ShuttleGarageScreen`. Check: `gradlew :shuttles:build` and `gradlew :shuttles:runData`.

## 6. GUI

- [x] 6.1 `SlotLocksPayload` (registered in `ServusShuttlesPayloads`), `MenuLocks` (also copies locks onto the client inventories), `LockedMenu`; rack section, Inbound, Outbound and Packing Station menus sync their locks; `WarehouseNetwork.shuttleOnJob`. Check: `gradlew :shuttles:build`, every payload type registered.
- [x] 6.2 `client/LockOverlay`: IN and OUT wash, faded ghost for empty IN slots, tooltip; wired into the four screens; lang entries in `ServusShuttlesLanguageProvider`. Check: `gradlew :shuttles:build` and `gradlew :shuttles:runData`.

## 7. Small fixes

- [x] 7.1 `InboundStationBlockEntity.pullOneStack` moves inside one transaction and respects locks; `PackingStationBlockEntity.shipWireless` never loses the put back rest. Check: `gradlew :shuttles:build`.

## 8. Tests

- [x] 8.1 GameTests `SlotLockGameTests`: locked rack slot refuses hand pickup, shift click and a hopper; Pull side does not top up a locked slot; put-away locks station and bay and releases both; pick locks bay and destination and releases both; two picks never plan the same items; orphan lock released; locks survive a reload; broken source bay replans the pick. Check: `gradlew :shuttles:runGameTestServer`.
- [x] 8.2 GameTests `LeftoverGameTests`: the reported spill (nearly full rack, chest on a Pull side pulling fast) drops nothing and every item ends in a rack or the buffer; a leftover re-routes to another bay; with no room the shuttle parks in its garage, the lamp is red, the GUI line names it, and it delivers once a rack joins; parked cargo survives a garage reload; a pick whose destination is broken stores its cargo. Check: `gradlew :shuttles:runGameTestServer`.
- [x] 8.3 GameTests for the target rule and cost: partial stack first, emptier bay first, cheaper bay among equals, cheapest pick source, straight way over corners. Fix existing fixed tick GameTests honestly where travel timing or paths changed. Check: `gradlew build`, `gradlew :shuttles:test`, `gradlew :shuttles:runGameTestServer` green.

## 9. Plan and changelog

- [x] 9.1 `PLAN.md`: note what this change cut or decided, keep the series lines; `shuttles/CHANGELOG.md` line goes in at archive. Check: `openspec validate m8-slot-locks --strict`.

## 10. Play check

- [x] 10.1 User play check: an Inbound Station with a chest and a backpack on Pull sides into a nearly full warehouse drops nothing; locked slots show their wash, ghost and tooltip in the rack, station and Packing Station GUIs and refuse hands; a full warehouse parks a shuttle in its garage with a red lamp and the GUI line, and it drives out when a rack is added; shuttles prefer straight aisles and free lifts.

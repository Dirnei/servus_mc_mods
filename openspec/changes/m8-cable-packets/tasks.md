# Tasks

## 1. Pure packet logic

- [x] 1.1 Add `PacketRoutes`, `PacketFlight`, `PacketLimits`, `Delivery` and `DeliveryReason` in `foundation/src/main/java/.../foundation/data/packet/` (no Minecraft imports) with JUnit tests `PacketRoutesTest` (`straightLine`, `shorterBranch`, `noRoute`, `startIsGoal`), `PacketFlightTest` (`sevenBlocksAtFourPerSecondTake35Ticks`, `currentNodeFollowsProgress`, `rerouteKeepsPosition`) and `DeliveryTest` (`countsDelivered`, `doneOnce`, `failedWithReason`, `listenersRunOnce`). Verify with `gradlew :foundation:test`.

## 2. Limits and stash

- [x] 2.1 Add `PacketLimitsManager` (reload listener for `data/<ns>/data/packets.json`, defaults when missing), register it in a new `DataPacketEvents`, add `foundation/src/main/resources/data/servus_foundation/data/packets.json` (8, 4.0, 20). Add `PacketStash` (`SavedData` with stacks and positions, drops when a position is loaded, ticked from `LevelTickEvent.Post` only while not empty). Verify with `gradlew build`.

## 3. Shipments on the network

- [x] 3.1 Add `Shipment` and `DataPackets` (`send`, `move`, `delivery`, `settleAll`), shipment ticking, re-routing, settling and absorb/dispose handling in `DataNetwork`, and the `ServerStoppingEvent` hook. Verify with `gradlew build`.
- [x] 3.2 Add `storage/src/main/java/.../storage/gametest/DataPacketGameTests.java`: `packetMoveControllerToStation`, `packetTravelTime`, `packetSendSplits`, `packetTargetFullRetries`, `packetSourceShortEndsDone`, `packetNoRouteKeepsItems`, `packetDetourArrives`, `packetBrokenRouteKeepsItems`. Verify with `:storage:runGameTestServer` and `:foundation:runGameTestServer` green.

## 4. Command

- [ ] 4.1 Add `foundation/command/ServusFoundationCommands` with `/servus_foundation data move <from> <to> <item> <count>` on `RegisterCommandsEvent`, and a GameTest or dev server check that it starts a delivery (`packetCommandStartsMove`). Verify with `gradlew build` and the Storage GameTests.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, Foundation, Manufacturing and Storage GameTests green, `runClientAll` reaches the title screen; update the Foundation docs page with a short "Packets" section and `packets.json` values, docs tests and build green.

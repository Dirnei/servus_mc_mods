# Tasks

## 1. Side modes and the Outbound Station

- [ ] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/station/SideModes.java`; give `InboundStationBlockEntity` and `InboundStationBlock` side modes, the capability per side and the carrot cycle instead of rotation. Add `outbound/OutboundStationBlockEntity.java` and `OutboundStationBlock.java` (buffer, keep slots, side modes, push, `DeliveryTarget`, `producer()`), registrations, `push_interval` and `outbound_check_interval` in `StorageStatsManager.java` and `warehouse.json`. Verify `gradlew :storage:build` is green.

## 2. Network, orders, removal

- [ ] 2.1 In `WarehouseNetwork.java`: producers and rows keyed by outbound station, `tickOutbound` restocking with pending counts (fleet and held load queries get a key filter), `feedStation`, outbound activity lamp, `drain` credits collected outputs from every inbound station. In `OrderBook.java` and `OrderStep.java`: station naming, `collected` matching by touching machine, new step state names. Rename commands to stations. Delete the `dock` package, menu, screen, registrations, datagen, textures and generated assets. Reword `PLAN.md` 2.4 and the M7 item. Verify `gradlew :storage:build` is green.

## 3. GUIs

- [ ] 3.1 Add `menu/OutboundStationMenu.java`, `client/OutboundStationScreen.java`, `KeepSlotPayload`, `CycleSidePayload`, the shortage in `StationStatePayload`, six side buttons in `InboundStationScreen.java`, lang, block and GUI textures, datagen (blockstate with lamps, loot, tag, recipe). Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 4. Tests

- [ ] 4.1 Replace the dock GameTests with the outbound and inbound side tests listed in design.md and rewrite the order, rule and board tests for the station pair in `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 5. Verification

- [ ] 5.1 Run `openspec validate m7-outbound-station --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 5.2 Play check: keep slots restock, hopper on an extract side, carrot cycles sides on both stations, Assembler with an Outbound Station in front and an Inbound Station beside it runs a work order end to end.

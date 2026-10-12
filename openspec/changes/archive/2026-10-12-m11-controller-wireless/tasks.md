# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/site/WirelessReach.java` (capable = upgrade and powered, FE needed) per design.md; JUnit `WirelessReachTest` under `storage/src/test/java/io/github/dirnei/mc/servus/storage/site/` (upgrade and 500 FE capable; upgrade and 2 FE at 4 per item not capable, needs 4; no upgrade never capable; fractions round up; at least 1 FE at cost 0). Keep `WirelessCostTest` green. Run `gradlew :storage:test`.
- [x] 1.2 `storage/src/main/java/io/github/dirnei/mc/servus/storage/packing/ReceiverChoice.java` (fewest open incoming transfers, lowest position on a tie, empty list gives none); JUnit `ReceiverChoiceTest` under `storage/src/test/java/io/github/dirnei/mc/servus/storage/packing/` (two idle stations pick the lower; a busier station is skipped; tie by position; no station). Run `gradlew :storage:test`.

## 2. Controller: energy, upgrade, data

- [x] 2.1 `storage/src/main/resources/data/servus_storage/storage/power.json` (`controller_energy_capacity`, `wireless_fe_per_item`, `wireless_fe_per_item_per_block`), `stats/StorageStatsManager.java` (read it, drop the `wireless_packing_station` fields, keep `setWirelessCost` for GameTests). Run `gradlew build`.
- [x] 2.2 `controller/ControllerEnergy.java`, `controller/WirelessUpgradeItem.java`, `controller/WirelessUpgradeSlots.java`, `WarehouseControllerBlockEntity.java` (energy saved, upgrade slot saved and dropped, `capable()`, flip on energy and slot change), registries (item, creative tab), `ServusStorageEvents.java` (`Capabilities.Energy.BLOCK` for the controller). Run `gradlew build`.
- [x] 2.3 Datagen: Wireless Upgrade item model, recipe, lang (item, tooltip, controller hint and FE lines, new refusal and status keys without the word team); `ServusStorageLanguageProvider`, recipe provider. Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Remove the Wireless Packing Station

- [x] 3.1 Delete `packing/WirelessPackingStationBlock.java`, `WirelessPackingStationBlockEntity.java`, `storage/wireless_packing_station.json`, their entries in `ServusStorageBlocks`, `ServusStorageItems`, `ServusStorageBlockEntities`, the creative tab, wood registration, `ServusStorageEvents`, datagen (block state, model, loot, recipe, mineable tag, lang) and textures; `gametest/WirelessPackingStationGameTests.java` is removed here and its cases come back in 5.1. Grep the storage tree for `WirelessPackingStation` and `wireless_packing_station`: no hits outside docs and PLAN.md. Run `gradlew :storage:runData`, then `gradlew build`.

## 4. Transfers through Packing Stations

- [x] 4.1 `packing/PackingStationBlockEntity.java`: wireless shipments (flag and site key saved), per tick sending with `WirelessCost.movable`, FE from the receiving controller, shared rate budgets, receiver checks, waiting reasons, status lines (no data line not an alert while capable). Run `gradlew build`.
- [x] 4.2 `network/WarehouseNetwork.java` (`shipWireless` over all Packing Stations, refusals, incoming counts for `ReceiverChoice`, dirty flag for upgrade, power and Packing Station presence), `site/SiteRecord.java` and `site/SiteRegistry.java` (`upgrade`, `powered`, `packing`; `updateWireless`). Run `gradlew build`.
- [x] 4.3 `terminal/RemoteTransfers.java` (sites only while capable, receiver by `ReceiverChoice`, new refusal keys), `terminal/SiteRow.java` (`trade`), `terminal/TerminalRequests.java` (waiting for power from either controller), `terminal/StockSyncPayload.java`, `menu/StorageTerminalMenu.java` (view back to this site when no rows), `client/StorageTerminalScreen.java` (selector only with rows, no power line). Run `gradlew :storage:runData`, then `gradlew build`.
- [x] 4.4 `controller/SiteStatusPayload.java` (`capable`, hint), `menu/WarehouseControllerMenu.java` (wireless slot, FE), `client/WarehouseControllerScreen.java` (slot, FE line, sites panel or hint, orange names, hover name and state only). Run `gradlew build`.

## 5. GameTests

- [x] 5.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/gametest/ControllerWirelessGameTests.java`: `controllerTakesFeFromPowerLine`, `controllerGivesNoFe`, `wirelessSlotTakesOnlyTheUpgrade`, `upgradeDropsWhenDismantled`, `transferMovesItemsAndCostsReceiverFe` (two sites built with `MiniWarehouse`, rack to rack by shuttle, 10 items at 500 blocks cost 140 FE at the receiver, sender unchanged), `transferThroughStationWithoutDataLine`, `transferWithoutShuttleRefused`, `noUpgradeNoSites` (terminal rows empty, panel hint), `unpoweredSiteRefused` (both directions, reasons), `noPackingStationRefused` (both directions), `transferWaitsForPowerAndResumes`, `upgradeRemovedPausesTransfer`, `viewFallsBackWhenPowerLost`, `inputLockedWhileViewingOtherSite`, `sleepingSiteWithoutUpgradeRefused`, `wakeableSiteHeldForRequest`, `receiversSpreadOverStations`, `recordKeepsTradeStateWhileAsleep`. Update `PackingStationGameTests.java` cases that used the removed block (`inputStackReachesRackBesideWirelessStation` becomes beside a Packing Station). Run `gradlew :storage:runGameTestServer`; all storage GameTests green.

## 6. Docs and plan

- [x] 6.1 `docs/src/content/docs/mods/storage.md` (controller as antenna, Wireless Upgrade, controller FE, Packing Stations for wireless, no Wireless Packing Station) and `PLAN.md` (section 2.3 item 6 and the topology bullet, M11 item 3 note). Run `gradlew :manufacturing:runData` for the docs gamedata, the docs tests, and `openspec validate m11-controller-wireless`.

## 6b. Transfers land in the terminal's output (play check decision)

- [x] 6b.1 `packing/PackingStationBlockEntity.java` (wireless `Shipment` carries the requesting terminal position, saved; on arrival a terminal bound transfer goes into the receiver's To network buffer and opens a data line shipment to the terminal; room checked against To network), `terminal/RemoteTransfers.java` (receiver only among stations reaching the terminal by data line; the request tracks the second leg), `network/WarehouseNetwork.shipWireless` (passes the terminal), `terminal/StorageTerminalBlockEntity.java` and `TerminalRequests.java` (request ends when the items are in the output) (design.md, decision after the play check). Update GameTests in `ControllerWirelessGameTests.java`: the online outpost transfer ends in the terminal's output slots and the base's rack count stays the same; new `transferWaitsWhileOutputFull`; operator and test shipments without a terminal still land in To storage. Run `gradlew build`, `gradlew test` and `gradlew :storage:runGameTestServer`.
- [x] 6b.2 `docs/src/content/docs/mods/storage.md`: a request from another site lands in the terminal's output. Run `cd docs && npm test`.

## 7. Verification

- [x] 7.1 Full `gradlew build`, `gradlew test` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: at the base and at a far outpost put a Wireless Upgrade into each controller and feed each controller from a Coal Generator through a power duct (the GUI shows FE rising); each site has a Heavy Duty Rack, a Shuttle Garage with a shuttle and a Packing Station on its rails (the outpost's without any duct). At the base terminal click the title, pick the outpost, request iron: a shuttle picks it into the outpost's Packing Station, arrives in the base's storage at about the Packing Stations' rate, and the base controller's FE drops (the outpost's does not). Take the Wireless Upgrade out of the base controller: the terminal falls back to this site, the controller panel shows "Insert a Wireless Upgrade to trade with other sites". Put it back, drain the outpost controller: the outpost shows in orange, hover shows name and state only, a request is refused with "That site has no power for wireless". While the outpost is shown the Input row is greyed. A sleeping outpost with a Chunk Loader Upgrade still wakes for a transfer and sleeps after the grace time.

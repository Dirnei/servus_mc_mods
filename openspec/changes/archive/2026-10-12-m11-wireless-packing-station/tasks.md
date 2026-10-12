# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/site/WirelessCost.java` (cost per item, cost of n rounded up, items a buffer pays for, items movable this tick) and `RemoteSite.java` (online, sleeping, unreachable) per design.md; JUnit tests `WirelessCostTest` (500 blocks: 14 per item, 10 items 140; fractions round up; empty buffer pays nothing; movable is the smallest limit; nothing negative) and `RemoteSiteTest` under `storage/src/test/java/io/github/dirnei/mc/servus/storage/site/`. Run `gradlew :storage:test`.

## 2. Block and data

- [x] 2.1 `storage/.../packing/WirelessPackingStationBlock.java`, `WirelessPackingStationBlockEntity.java` (team, FE buffer, rate from data, status), `PackingStationBlockEntity.java` (protected shipping hook and helpers), `PackingStationMenu.java` (valid for both blocks), registries (block, item, block entity, creative tab, wood registration), `ServusStorageEvents` (item handler and energy capabilities), `StorageStatsManager` and `storage/src/main/resources/data/servus_storage/storage/wireless_packing_station.json`, textures. Run `gradlew build`.
- [x] 2.2 Datagen: block state (station lamps), loot (wooden), mineable tag, recipe, lang (block, tooltip, status lines). Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Transfers

- [x] 3.1 `PackingStationBlockEntity`/`WirelessPackingStationBlockEntity`: wireless sending per tick with cost, rate, room and ledger; `WarehouseNetwork.java`: `shipThrough`, `shipWireless`, `wirelessTeams`, record update; `SiteRecord`/`SiteRegistry`: wireless teams. Run `gradlew build`.

## 4. Terminal and controller

- [x] 4.1 `storage/.../terminal/RemoteTransfers.java`, `StorageTerminalBlockEntity.java` (remote field saved, credit and wait skip remote, hold release on removal), `TerminalRequests.java` (remote booking, retry, cancel, rows, `WAKING`), `SiteRow.java`, `SelectSitePayload.java`, `StockSyncPayload.java`, `StorageTerminalMenu.java` (view, remote stock, remote request), `ServusStoragePayloads.java`; grep that every payload type is registered. Run `gradlew build`.
- [x] 4.2 `StorageTerminalScreen.java` (title selector, greyed entries, view line), `ClientStock.java`, `SiteStatusPayload.java` and `WarehouseControllerMenu.java` (site rows), `WarehouseControllerScreen.java` (site panel), lang keys. Run `gradlew :storage:runData`, then `gradlew build`.

## 5. GameTests

- [x] 5.1 `storage/.../gametest/WirelessPackingStationGameTests.java`: `wirelessStationIsNoDataMember`, `transferMovesItemsAndCostsEnergy` (two sites in one area, same team: crate to crate, FE taken by the receiver, ledger done), `noPowerNoTransfer`, `otherTeamRefused`, `terminalListsSitesAndRemoteStock` (site rows, live stock of the other site, greyed last known stock of a recorded sleeping site), `remoteRequestArrivesInStorage` (terminal request on the other site: listed, items put away at this site, row gone), `sleepingSiteWithoutUpgradeRefused`, `wakeableSiteHeldForRequest` (hold while open, released on cancel), `dataShipmentIgnoresWirelessStation`. Run `gradlew :storage:runGameTestServer`; all storage GameTests stay green.

## 6. Docs and plan

- [x] 6.1 `docs/src/content/docs/mods/storage.md` (Wireless Packing Station, sites in the terminal and the controller) and `PLAN.md` (section 2.3 item 6, M11 item 3 with a Built note). Run `gradlew :manufacturing:runData` for the docs gamedata, the docs tests, and `openspec validate m11-wireless-packing-station`.

## 7. Play test fixes (2026-10-12)

- [x] 7.1 Powered rule, pure logic: `WirelessCost.minimumCharge` and `powered`; JUnit tests in `WirelessCostTest` (one item at the smallest cost, fractions round up, at least 1 FE, empty buffer unpowered). Run `gradlew :storage:test`.
- [x] 7.2 `WirelessPackingStationBlockEntity` (powered state on energy change, lamp, no power status, sending stops while unpowered, `waitsForPower`), `WarehouseNetwork` (`wirelessTeams` powered only, `wirelessStationTeams`, `shipWireless` refuses `no_power_there`, `pushLamps`), `SiteRecord`/`SiteRegistry` (unpowered teams), `SiteRow` (`station`), `RemoteTransfers` (`no_power_here`, `no_power_there`, sites with unpowered stations listed), `TerminalRequests` and `StorageTerminalBlockEntity` (`NO_POWER` state), `WarehouseControllerMenu`/`WarehouseControllerScreen` (orange, no power hover), `StorageTerminalScreen` (no power view line), lang. Run `gradlew :storage:runData`, then `gradlew build`.
- [x] 7.3 GameTests in `WirelessPackingStationGameTests`: `unpoweredStationRefusesRequest` (refused, lamp dark, FE brings the site back and the request arrives), `unpoweredReceiverRefusesRequest`, `transferWaitsForPower`; existing tests power both stations. Run `gradlew :storage:runGameTestServer`.
- [x] 7.4 Input row closed while another site is shown: `StorageTerminalMenu` (`inputOpen`, slots, shift click, carried stack, grid clear, `clientView`), `ClientStock`, `StorageTerminalScreen` (greyed row, tooltip), lang; GameTest `inputLockedWhileViewingOtherSite`. Run `gradlew :storage:runData`, `gradlew build`, `gradlew :storage:runGameTestServer`.
- [x] 7.5 Terminal rows never stock: `storage/.../network/NoStock.java`, `WarehouseNetwork` (`addSource`, `poll`), `StorageTerminalBlockEntity` and `CraftingStationBlockEntity` implement it; GameTests in `PackingStationGameTests`: `terminalBesideControllerIsNoStock` (the user's case, fails without the fix: 4 of 64 logs reach the rack), `terminalBesideCrateIsNoStock`, `inputStackReachesRack`, `inputStackReachesRackBesideWirelessStation`; `StorageTerminalRowsGameTests.inputWithoutRoomShowsBlocked`. Run `gradlew build` and `gradlew :storage:runGameTestServer`.
- [x] 7.6 Delta specs (`wireless-packing-station`, `storage-terminal` with Input row modified, `warehouse-controller` with sources modified, `site-wake`) and this design. Run `openspec validate m11-wireless-packing-station`.

## 8. Verification

- [x] 8.1 Play check by the user: build a second site far away with a controller, a Chunk Loader Upgrade, a crate with iron and a Wireless Packing Station on a rail; at the base a Wireless Packing Station on a rail fed by a Coal Generator through a power duct. At the base terminal click the title to switch to the outpost: its stock shows (greyed while it sleeps), request iron: the outpost wakes (its controller lamp orange if you look), the transfer is listed, the iron lands in the base's storage, the FE drops, and the outpost sleeps again after about 30 s. The base controller GUI lists the outpost with its state. Then take the FE out of the outpost's station (break and replace it): its lamp goes dark, the base controller shows the outpost in orange with "Wireless Packing Station has no power", and a request is refused with that reason; power it again and the request works. While the terminal shows the outpost the Input row is greyed and takes nothing. At the site where the terminal touches the controller, the Input and Output rows no longer show in the stock and 64 logs handed in all get stored.

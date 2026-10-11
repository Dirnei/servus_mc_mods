# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/site/WirelessCost.java` (cost per item, cost of n rounded up, items a buffer pays for, items movable this tick) and `RemoteSite.java` (online, sleeping, unreachable) per design.md; JUnit tests `WirelessCostTest` (500 blocks: 14 per item, 10 items 140; fractions round up; empty buffer pays nothing; movable is the smallest limit; nothing negative) and `RemoteSiteTest` under `storage/src/test/java/io/github/dirnei/mc/servus/storage/site/`. Run `gradlew :storage:test`.

## 2. Block and data

- [x] 2.1 `storage/.../packing/WirelessPackingStationBlock.java`, `WirelessPackingStationBlockEntity.java` (team, FE buffer, rate from data, status), `PackingStationBlockEntity.java` (protected shipping hook and helpers), `PackingStationMenu.java` (valid for both blocks), registries (block, item, block entity, creative tab, wood registration), `ServusStorageEvents` (item handler and energy capabilities), `StorageStatsManager` and `storage/src/main/resources/data/servus_storage/storage/wireless_packing_station.json`, textures. Run `gradlew build`.
- [ ] 2.2 Datagen: block state (station lamps), loot (wooden), mineable tag, recipe, lang (block, tooltip, status lines). Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Transfers

- [ ] 3.1 `PackingStationBlockEntity`/`WirelessPackingStationBlockEntity`: wireless sending per tick with cost, rate, room and ledger; `WarehouseNetwork.java`: `shipThrough`, `shipWireless`, `wirelessTeams`, record update; `SiteRecord`/`SiteRegistry`: wireless teams. Run `gradlew build`.

## 4. Terminal and controller

- [ ] 4.1 `storage/.../terminal/RemoteTransfers.java`, `StorageTerminalBlockEntity.java` (remote field saved, credit and wait skip remote, hold release on removal), `TerminalRequests.java` (remote booking, retry, cancel, rows, `WAKING`), `SiteRow.java`, `SelectSitePayload.java`, `StockSyncPayload.java`, `StorageTerminalMenu.java` (view, remote stock, remote request), `ServusStoragePayloads.java`; grep that every payload type is registered. Run `gradlew build`.
- [ ] 4.2 `StorageTerminalScreen.java` (title selector, greyed entries, view line), `ClientStock.java`, `SiteStatusPayload.java` and `WarehouseControllerMenu.java` (site rows), `WarehouseControllerScreen.java` (site panel), lang keys. Run `gradlew :storage:runData`, then `gradlew build`.

## 5. GameTests

- [ ] 5.1 `storage/.../gametest/WirelessPackingStationGameTests.java`: `wirelessStationIsNoDataMember`, `transferMovesItemsAndCostsEnergy` (two sites in one area, same team: crate to crate, FE taken by the receiver, ledger done), `noPowerNoTransfer`, `otherTeamRefused`, `terminalListsSitesAndRemoteStock` (site rows, live stock of the other site, greyed last known stock of a recorded sleeping site), `remoteRequestArrivesInStorage` (terminal request on the other site: listed, items put away at this site, row gone), `sleepingSiteWithoutUpgradeRefused`, `wakeableSiteHeldForRequest` (hold while open, released on cancel), `dataShipmentIgnoresWirelessStation`. Run `gradlew :storage:runGameTestServer`; all storage GameTests stay green.

## 6. Docs and plan

- [ ] 6.1 `docs/src/content/docs/mods/storage.md` (Wireless Packing Station, sites in the terminal and the controller) and `PLAN.md` (section 2.3 item 6, M11 item 3 with a Built note). Run `gradlew :manufacturing:runData` for the docs gamedata, the docs tests, and `openspec validate m11-wireless-packing-station`.

## 7. Verification

- [ ] 7.1 Play check by the user: build a second site far away with a controller, a Chunk Loader Upgrade, a crate with iron and a Wireless Packing Station on a rail; at the base a Wireless Packing Station on a rail fed by a Coal Generator through a power duct. At the base terminal click the title to switch to the outpost: its stock shows (greyed while it sleeps), request iron: the outpost wakes (its controller lamp orange if you look), the transfer is listed, the iron lands in the base's storage, the FE drops, and the outpost sleeps again after about 30 s. The base controller GUI lists the outpost with its state.

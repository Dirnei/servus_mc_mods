# Tasks

## 1. Pure logic

- [x] 1.1 Add `shuttles/src/main/java/.../shuttles/station/StationCounters.java`: last number per kind (Inbound, Outbound), `take(kind)`, `needsNumber(storedWarehouse, number, currentWarehouse)`, tag save and load. Add JUnit `shuttles/src/test/java/.../shuttles/station/StationCountersTest.java`: counts per kind from 1, never reuses after a take, kinds are independent, save round trip, needs a number when unnumbered or numbered by another warehouse and not when numbered by this one. Run `gradlew :shuttles:test`.
- [x] 1.2 Add `station/StationLabel.java`: label text from kind word, number and name ("Outbound #3", "Compost Dock (#3)", "Outbound Station", name only when unnumbered); name cleaned with `site/SiteName.clean`. Add JUnit `station/StationLabelTest.java` for every form and a name that cleans to empty. Run `gradlew :shuttles:test`.

## 2. Warehouse identity and counters

- [x] 2.1 `registry/ServusShuttlesComponents.java`: add `WAREHOUSE_ID` (UUID), `STATION_COUNTERS` (record with codec and stream codec), `STATION_NUMBER` (Integer), `STATION_NAME` (String), `STATION_WAREHOUSE` (UUID). Run `gradlew :shuttles:build`.
- [x] 2.2 `controller/WarehouseControllerBlockEntity.java`: warehouse id created when missing, a `StationCounters` instance, save and load, carry both in `collectImplicitComponents`/`applyImplicitComponents` and strip them in `removeComponentsFromTag`. Public `warehouseId()` and `takeStationNumber(kind)`. Run `gradlew :shuttles:build`.

## 3. Station number and name

- [x] 3.1 `station/StationBlockEntity.java`: number, name and numbering warehouse id; save in `saveStation`, load in `loadStation`, add to `getUpdateTag`; components in and out like the wood; `label()` returning the label `Component` (kind word from lang); `getDisplayName()` returns `label()`; `rename(String)` cleaning the name; `assignNumber(UUID, int)`. Abstract kind per subclass in `InboundStationBlockEntity.java` and `outbound/OutboundStationBlockEntity.java`. Run `gradlew :shuttles:build`.
- [x] 3.2 `network/WarehouseNetwork.java`: `stationsToNumber` set filled in the `NodeKind.STATION` join branch and with all stations when the status becomes online; drained in the server tick while online, loaded stations only, in ascending position order, numbering those that `needsNumber`. Run `gradlew :shuttles:build`.
- [x] 3.3 GameTests in `gametest/StationNumberGameTests.java`: two Outbound and one Inbound numbered #1, #2 and #1; a broken #2 is not reused; numbers survive a reload of the block entity data; no number while offline; several stations at once numbered by position; carrot dismantle and place in the same warehouse keeps the number; place in another warehouse gives the next number there and keeps the name; controller carrot dismantle keeps the count. Run `gradlew :shuttles:runGameTestServer`.

## 4. GUI name field

- [x] 4.1 Add `station/RenameStationPayload.java` (codec `stringUtf8(64)`) and register it with `toMenu` for `menu/InboundStationMenu` and `menu/OutboundStationMenu` in `terminal/ServusShuttlesPayloads.java`; both menus get `rename(String)` guarded by `stillValid(player)` and expose the current name and label to the screen. Run `gradlew :shuttles:build`.
- [x] 4.2 `client/InboundStationScreen.java`, `client/OutboundStationScreen.java`: borderless `EditBox` in the title row with the label as hint, max `SiteName.MAX_LENGTH`, sent on focus loss and in `removed()`, keys consumed while focused, as in `client/WarehouseControllerScreen.java`. Lang keys for the field and hint in `datagen/ServusShuttlesLanguageProvider.java`; run `gradlew :shuttles:runData`. GameTest in `StationNumberGameTests`: rename through the menu cleans the name, empty removes it, label follows. Run `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.

## 5. Data Potato report

- [x] 5.1 Add `station/StationReport.java` (label, site display name, online or offline reason) and implement `PotatoInspectable` on `station/InboundStationBlock.java` and `outbound/OutboundStationBlock.java`, letting the potato through in `useItemOn` like `rack/HeavyRackBlock`. Lang keys in `datagen/ServusShuttlesLanguageProvider.java`, `gradlew :shuttles:runData`. GameTest in `gametest/PotatoStorageGameTests.java`: online and offline station report, no GUI opens. Run `gradlew :shuttles:runGameTestServer`.

## 6. Packing Stations (play check fix)

- [x] 6.1 `station/StationCounters.java`: add `Kind.PACKING` and its counter, `counterOf(saved, kind)` (a save without a counter name was numbered by Inbound for a Packing Station, by its own kind otherwise); `station/CarriedCounters.java`: optional `packing` field. JUnit in `StationCountersTest`: Packing counts on its own, round trip and copy with three counters, saved counter name kept, a number saved without a counter came from Inbound for Packing. Run `gradlew :shuttles:test`.
- [x] 6.2 `packing/PackingStationBlockEntity.java`: `stationKind()` returns `PACKING`. `station/StationBlockEntity.java`: save the counter with the number (tag, update tag, new `STATION_COUNTER` component in `registry/ServusShuttlesComponents.java`) and drop a loaded number whose counter is not its own. `packing/PackingStationBlock.java`: the carrot dismantle keeps the block entity's components. Lang key `station.servus_shuttles.label.packing` in `datagen/ServusShuttlesLanguageProvider.java` and the generated `en_us.json`. Run `gradlew build`.
- [x] 6.3 `menu/PackingStationMenu.java` implements `StationNameMenu`; `client/PackingStationScreen.java` gets the `StationNameField` title row with the number at the right. Run `gradlew build`.
- [x] 6.4 GameTests in `gametest/StationNumberGameTests.java`: Inbound #1, Packing #1 and Inbound #2 on one warehouse; renaming through the Packing Station menu, not from far away; carrot dismantle of a named Packing Station keeps number and name; a Packing Station saved without a counter loads unnumbered while an Inbound Station keeps its number. Run `gradlew :shuttles:runGameTestServer`.
- [x] 6.5 Play check: a Packing Station on a warehouse with Inbound Stations reads "Packing #1" in its title, the Data Potato report and any Rules tab state; rename it in its GUI and see "#1" at the right; an old world's Packing Station that showed "Inbound #2" shows a Packing number after loading.

## 7. Docs and finish

- [x] 7.1 `PLAN.md` "Open: Shuttles": add numbers for the Fluid Inbound and Fluid Outbound stations once M13 builds them. `shuttles/CHANGELOG.md` `## Unreleased`: Added, stations get numbers and optional names.
- [x] 7.2 Full `gradlew build`, `gradlew test` and GameTests green; launch the client with all mods, link two Outbound Stations and an Inbound Station, check titles "Outbound #1", "Outbound #2", "Inbound #1", rename one, dismantle and place it again, and use the Data Potato on a station.

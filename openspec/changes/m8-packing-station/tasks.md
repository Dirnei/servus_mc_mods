# Tasks

## 1. Rename

- [x] 1.1 Rename Access Point to Packing Station with a script and `git mv` (classes `AccessPoint*` to `PackingStation*`, package `storage.access` to `storage.packing`, ids, lang keys, textures, the docs icon, specs text, PLAN.md, README, docs pages), move `openspec/specs/storage/access-point` to `storage/packing-station`, regenerate data (`:storage:runData`, `:manufacturing:runData`). Verify with `gradlew build test`, Storage GameTests and a grep that finds no `access_point`, `AccessPoint` or "Access Point" outside archives; `openspec validate m8-packing-station --strict` shows no "target spec does not exist" notice.

## 2. Instant deliveries and gates (Foundation)

- [x] 2.1 Add pure `foundation.data.packet.RateBudget` and JUnit `RateBudgetTest` (`eightPerSecondOverTwentyTicks`, `fractionalCarry`, `capAtOneSecond`, `directionsIndependent`). Verify with `gradlew :foundation:test`.
- [x] 2.2 Add the `servus_foundation:transfer_gate` capability (`TransferGate`, `Flow`) to `ServusFoundationCapabilities`; make `Shipment` move items per tick without travel, bounded by source, target and gate allowances; remove `PacketFlight` and the travel part of `PacketRoutes`; shrink `PacketLimits`/`PacketLimitsManager` and `packets.json` to `retry_interval`; update `DataPackets` docs and the `/servus_foundation data move` command. JUnit for the per tick amount. Verify with `gradlew build test` and `:foundation:runGameTestServer`, `:storage:runGameTestServer` (rewrite packet tests that waited fixed ticks).

## 3. Packing Station

- [x] 3.1 `PackingStationBlockEntity` implements `TransferGate` with two budgets from `items_per_second` in new `data/servus_storage/storage/packing_station.json` (via `StorageStatsManager`); IN accepts only allowance and To storage room; OUT sends from To network at the allowance; `WarehouseNetwork` releases picks into To network only while it has room. GameTests `drainsAtEightPerSecond`, `twoStationsDoubleThroughput`, `bothDirectionsAtOnce`. Verify with build and Storage GameTests.
- [ ] 3.2 Read-only GUI: `PackingStationMenu` (read-only slot subclass, no shift click), `PackingStationScreen` with "To storage" and "To network" rows and tooltips, status line payload (current delivery or wait reason), item tooltip, lang. GameTest `guiRefusesExtraction` (menu click leaves the slot unchanged). Verify with build and Storage GameTests; grep that every payload TYPE is registered.

## 4. Terminal input

- [ ] 4.1 `StorageTerminalBlockEntity.storeInput` starts one move per input slot to the nearest Packing Station with an extraction view of the Input row; slots with a running move are skipped; message "No Packing Station on this data line". GameTests `inputDrainsGradually`, `fullToStorageKeepsInput`, existing terminal input tests updated. Verify with build and Storage GameTests.

## 5. Docs and plan

- [ ] 5.1 Update the Storage and Foundation docs pages (Packing Station, instant network, rates), PLAN.md 2.4 data line text and M8 list (add this item, replace "Later: High-Speed Data Cable" with `m8-data-bandwidth`). Verify with `npm test` in `docs/` and a read of PLAN.md.

## 6. Integration

- [ ] 6.1 Full `gradlew build` and `gradlew test` green, Foundation, Manufacturing and Storage GameTests green, and the game launches with all three mods.

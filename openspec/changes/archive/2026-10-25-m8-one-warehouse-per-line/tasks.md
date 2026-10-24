## 1. Pure logic

- [x] 1.1 `shuttles/.../terminal/LineClassification.java`: classify warehouse identities found among members into none, one or conflict (identity comparison). Add `LineClassificationTest`: no members, one warehouse seen through controller and two Packing Stations, two warehouses, three warehouses. Check: `gradlew :shuttles:test`.

## 2. The line's warehouse

- [x] 2.1 `terminal/TerminalWarehouses`: `line(level, pos)` returning `LineWarehouse` (`NONE`, `ONE`, `CONFLICT`, `online()`), counting warehouses of every status; `stock(warehouse)` with a per warehouse cache; remove `reachable`, the list cache and `merge`. Check: `gradlew :shuttles:build` (callers fixed in 2.2 to 2.4).
- [x] 2.2 `terminal/TerminalRequests` (request refusal `line_conflict`, retries wait, lamp), `terminal/RemoteTransfers`, `terminal/StorageTerminalBlockEntity` (`storeInput` with `inputRefusal = "line_conflict"`, `nearestPackingStation` limited to the line's warehouse). Check: `gradlew :shuttles:build`.
- [x] 2.3 `menu/StorageTerminalMenu` (stock sync and Storage tab data from the one warehouse, conflict flag), `production/ProductionView` (lamp and Storage tab), `client` screens drawing the conflict line in the alert colour on the Storage tab. Check: `gradlew :shuttles:build`, every payload type registered.
- [x] 2.4 `cpu/CpuBlockEntity` (host stock from the one warehouse, empty on conflict, summary names the conflict), `crafting/CraftingStationBlockEntity` payer, `packing/PackingStationBlockEntity` (no data delivery starts on conflict, status line), `packing/WarehouseSupplier` (0 on conflict). Check: `gradlew :shuttles:build`.
- [x] 2.5 Lang `line_conflict` keys ("Two warehouses on this data line") for terminal, Input row, Production Terminal, CPU and Packing Station in `datagen/ServusShuttlesLanguageProvider`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 3. GameTests

- [x] 3.1 Replace the two warehouse test in `gametest/TerminalCableGameTests` and any two header Storage tab test with conflict tests: a line with two controllers refuses a request with `line_conflict` and its lamp is dark; the Input row keeps its logs; a Packing Station on the line starts no delivery while the other warehouse's Inbound Station put-away still runs; breaking the joining duct lets a waiting request finish; an offline second warehouse still conflicts; the supplier answers 0. Check: `gradlew :shuttles:runGameTestServer`.

## 4. Plan and integration

- [x] 4.1 `PLAN.md`: note in "Open: Shuttles" that the delivery handshake follows (`m8-wtp-ingress`, `m8-wtp-egress`) if not yet listed. Check: `openspec validate m8-one-warehouse-per-line --strict`.
- [x] 4.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green; launching the game with all mods is left to the user's play check, which covers all three changes.

## Workflow follow-up

- Archive in order after the user's play check: `m8-one-warehouse-per-line`, `m8-wtp-ingress`, `m8-wtp-egress`; add the `shuttles/CHANGELOG.md` line under Unreleased at archive.

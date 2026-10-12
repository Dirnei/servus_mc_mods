# Tasks

## 1. Pure logic

- [ ] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/power/PowerBudget.java` (all or nothing draw, last refusal, power state, use in the last second) per design.md; JUnit `PowerBudgetTest` under `storage/src/test/java/io/github/dirnei/mc/servus/storage/power/` (draw larger than stored refused and nothing taken; exact draw paid; empty buffer is no power; refusal holds no power for 20 ticks then clears after a paid draw; use sums the last 20 ticks and forgets older ones; no negative values). Run `gradlew :storage:test`.
- [ ] 1.2 `power/WakeCost.java` (cost per tick, FE to start = one second) and `power/StepPower.java` (segment cost horizontal and lift ride, gate decision, craft progress advances only when paid); `site/WakeAllowance.java` gains `NO_POWER` with stored FE and FE to start. JUnit `WakeCostTest` (9 chunks at 2 FE: 18 per tick, 360 to start, zero chunks costs nothing), `StepPowerTest` (one block 20, ride of 4 is 80, unpaid gate refuses, unpaid tick keeps progress, paid tick advances), extend `WakeAllowanceTest` (no power reason, order of reasons with no upgrade and over capacity). Run `gradlew :storage:test`.

## 2. Controller power

- [ ] 2.1 `storage/src/main/resources/data/servus_storage/storage/power.json` (`shuttle_fe_per_block`, `wake_fe_per_chunk_per_tick`, `crafting_station_fe_per_tick`), `crate/StorageStatsManager.java` (read with defaults, GameTest setters). Run `gradlew build`.
- [ ] 2.2 `controller/ControllerEnergy.java` (`draw`, `noPower`, use per second through `PowerBudget`), `WarehouseControllerBlockEntity.java` (lamp red on no power, flip refreshes lamp and status), wireless spend through `draw`, `network/WarehouseNetwork.java` (`power()` access for consumers, summary line). Run `gradlew build`.
- [ ] 2.3 `controller/SiteStatusPayload.java` (use, no power), `client/WarehouseControllerScreen.java` (use next to FE, red no power line, which red applies), lang via `datagen/ServusStorageLanguageProvider.java`. Run `gradlew :storage:runData`, then `gradlew build`.
- [ ] 2.4 `gametest/PowerFixtures.java` (fill a controller's buffer) and every existing Storage GameTest that runs shuttles, wakes or Crafting Stations calls it (grep `gametest/` for garages, `SiteWakes` and Crafting Stations). Run `gradlew :storage:runGameTestServer`; all storage GameTests green before the consumers start paying.

## 3. Consumers

- [ ] 3.1 `shuttle/ShuttleFleet.java`: pay per block in the `mayStart` gate, pay a ride in `handover`, no `assignJobs` and no field run start while no power. Run `gradlew build`.
- [ ] 3.2 `crafting/CraftingStationBlockEntity.java` (pay per working tick from the first reachable warehouse, cached per second; `waitingForPower`, status, front), `cpu/CpuOrders.java` and the Production Terminal step text (waiting for power). Run `gradlew :storage:runData`, then `gradlew build`.
- [ ] 3.3 `site/SiteWakes.java` (charge per tick, release on refusal, allowance with live or recorded FE), `site/SiteRecord.java` and `site/SiteRegistry.java` (`energy` on unload and snapshot), `WarehouseControllerBlockEntity.onChunkUnloaded`. Run `gradlew build`.
- [ ] 3.4 `terminal/StockSyncPayload.java` (`noPower`), `terminal/TerminalRequests.java` (`NO_POWER` row state for requests waiting on shuttles or stations), `client/StorageTerminalScreen.java` (alert line), lang. Run `gradlew :storage:runData`, then `gradlew build`.

## 4. GameTests

- [ ] 4.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/gametest/WarehousePowerGameTests.java`: `idleWarehouseDrawsNothing`, `shuttleTripCostsPerBlock` (six rails out and back is 240 FE), `liftRideCostsPerBlockOfHeight`, `shuttleStopsAndResumesWithCargo`, `noNewJobsWithoutPower`, `fieldRunWaitsForPower`, `craftCostsPerWorkingTick` (40 ticks at 5 FE is 200), `craftPausesAndResumesProgress`, `waitingForInputsIsFree`, `cpuStepWaitsForPowerAndFinishes`, `adapterStepKeepsRunning`, `wakeCostsPerChunkPerTick`, `wakeRefusedWithoutStoredFe`, `wakeEndsWhenDry`, `recordKeepsEnergyWhileAsleep`, `lampRedWithoutPower`, `terminalShowsNoPowerAndKeepsStock`, `rackUsableByHandWithoutPower`, `useReadoutCountsLastSecond`. Run `gradlew :storage:runGameTestServer`; all storage GameTests green.

## 5. Docs and plan

- [ ] 5.1 `docs/src/content/docs/mods/storage.md` (warehouse power: one connection, costs, no power state, what keeps working) and `PLAN.md` (M11 item, section 2.3, M11 item 2 note). Run `gradlew :manufacturing:runData` for the docs gamedata, the docs tests, and `openspec validate m11-warehouse-power`.
- [ ] 5.2 Optional: Balance lab page "Warehouse power" under `docs/src/content/docs/lab/` with `storage/power.json` and the Coal Generator output exported into `docs/src/data/gamedata.json` by `manufacturing/.../datagen/gamedata/GameDataExportProvider.java` (JUnit in `GameDataJsonTest` for the new fields). Run `gradlew :manufacturing:test`, `gradlew :manufacturing:runData` and the docs tests.

## 6. Verification

- [ ] 6.1 Full `gradlew build`, `gradlew test` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: a warehouse with racks, a garage with two shuttles, a Crafting Station and a CPU, its controller fed by one Coal Generator through a power duct. Request iron from a rack and craft sticks: the controller GUI shows the FE use while shuttles drive and the station crafts, and nothing while idle. Take the coal out of the generator and let the buffer empty during a trip: the lamp turns red, the GUI and the terminal say no power, the shuttle stops on the rail with its cargo, the station's craft stops part way, the Production Terminal shows the step waiting for power; the rack still opens by hand and the terminal still shows the stock. Put coal back: everything goes on by itself. At an outpost with a Chunk Loader Upgrade and a Wireless Upgrade, let its controller sleep almost empty: a transfer request waits with no power; power it on a visit, leave, request again: it wakes, its FE drops per chunk while awake, and it sleeps again after the grace time.

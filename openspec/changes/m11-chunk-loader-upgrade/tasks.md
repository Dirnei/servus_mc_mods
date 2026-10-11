# Tasks

## 1. Pure logic

- [ ] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/site/WakeMachine.java` and `WakeAllowance.java` per design.md; JUnit tests `WakeMachineTest` (asleep stays asleep unwanted, wanted and allowed loads, wanted and refused waits with the reason then loads once allowed, waiting unwanted falls asleep, awake while busy stays, grace counts down and releases, new work in grace returns to awake, zero grace releases at once, over capacity while awake releases) and `WakeAllowanceTest` (order of reasons, registered site never hits the cap, cap reached, allowed) under `storage/src/test/java/io/github/dirnei/mc/servus/storage/site/`. Run `gradlew :storage:test`.

## 2. Registry

- [ ] 2.1 `storage/.../site/SiteRecord.java`, `SiteRegistry.java` (SavedData, key, update, stock, remove, list), `storage/.../crate/StorageStatsManager.java` (`wake_grace_seconds`, `site_snapshot_interval`, setters), `storage/src/main/resources/data/servus_storage/storage/warehouse.json`; `WarehouseControllerBlockEntity` writes its record on changes and load, removes it when broken (unload flag), `WarehouseNetwork` snapshots the stock and offers `siteBusy()`. Run `gradlew build`.

## 3. Wakes

- [ ] 3.1 `storage/.../site/SiteTickets.java` (ticket controller with the validation callback, registration), `SiteWakes.java` (request, hold, release, tick, load and release of chunks and the team slot, state queries), `SiteEvents.java` (server tick, stopping, started). Run `gradlew build`.
- [ ] 3.2 Controller lamp and wake line from `SiteWakes`, lang keys in `ServusStorageLanguageProvider`; `ServusStorageCommands` (`wake <pos>`, `sites`). Run `gradlew :storage:runData`, then `gradlew build`.

## 4. GameTests

- [ ] 4.1 `storage/.../gametest/SiteWakeGameTests.java`: `recordKeepsNameSpanAndStock`, `wakeLoadsAndReleasesAfterGrace` (forced chunks equal the span, the team counts one loader, after the grace none and nothing forced; lamp orange while awake), `overCapacityNeverWakes`, `noUpgradeWaits`, `teamCapWaitsThenWakes` (four fake loaders of the test team: waits with team limit, one freed: wakes), `queuedJobKeepsSiteAwake`, `brokenControllerDropsRecordAndWake`, `wakeCommandReports`. Each test uses its own team id. Run `gradlew :storage:runGameTestServer`; all storage GameTests stay green.

## 5. Docs and plan

- [ ] 5.1 `docs/src/content/docs/mods/storage.md` (waking a site, hard limit, team cap, FTB Chunks, grace time, commands) and `PLAN.md` (M11 item 2 with a Built note). Run `openspec validate m11-chunk-loader-upgrade`.

## 6. Verification

- [ ] 6.1 Play check by the user: build an outpost with a controller and a Chunk Loader Upgrade, fly far away, run `/servus_warehouse sites` (the outpost is listed with its stock) and `/servus_warehouse wake <pos>`; the outpost's chunks load (a hopper or crafting there keeps running, `/forceload query` does not list them since they are tickets, F3 shows the chunks loaded), the controller lamp is orange, and after about 30 s they are released; with a site larger than its upgrades the wake says over capacity; with four active Chunk Loaders the wake says team limit reached.

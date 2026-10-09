# Tasks

## 1. Pure handoff split

- [x] 1.1 Add `storage/order/Handoff.java` (no Minecraft imports): split a movable count of one output over dependants (owed, delivering, same producer), hold or store the rest by the wait rule. Add `storage/src/test/.../order/HandoffTest.java` with `allToDeliveringDependant`, `surplusGoesToStorage`, `holdsWhileDependantWaits`, `holdExpires`, `sameProducerGoesToStorage`, `severalDependantsInOrder`. Verify with `gradlew :storage:test`.

## 2. CPU handoff

- [x] 2.1 `OrderStep`: mutable producer, collecting with an optional target (saved), transient hold time, the first in-flight handoff for wording. `OrderBook`: move waiting steps to a free producer with the same recipe; `feed` ignores missing inputs an earlier unfinished step makes. `CpuBlockEntity.collect`: terminal first, then handoffs via `Handoff`, then hold or Access Point; `handoff_wait_ticks` in `storage/cpu.json` via `StorageStatsManager`. Verify with `gradlew build` and `:storage:runGameTestServer` (existing tests green).
- [x] 2.2 Production Terminal wording: `JobStatus.Step` sending fields, `JobStatus.of` status `sending`, `ProductionView` fills them, lang `gui.servus_storage.production.job.sending`; extend `JobStatusTest` with `sendingNamesTarget`. Verify with `gradlew :storage:test` and the data run.
- [x] 2.3 Add `storage/.../gametest/ChainHandoffGameTests.java`: `planksHandedToSecondStation`, `stoneHandedFromFurnaceToStation`, `busyNextProducerFallsBack`, `reloadMidHandoffCompletes`. Verify with `:storage:runGameTestServer`, all green.

## 3. Docs

- [x] 3.1 Storage docs page: intermediates go machine to machine. Verify with `npx vitest run` in `docs/`.

## 4. Integration

- [ ] 4.1 Full `gradlew build test`, Storage, Foundation and Manufacturing GameTests green; `openspec validate m8-chain-handoff --strict`.

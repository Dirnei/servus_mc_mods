# Tasks

Module roots: `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/`, `manufacturing/src/main/java/io/github/dirnei/mc/servus/manufacturing/`, `storage/src/main/java/io/github/dirnei/mc/servus/storage/`, and the matching `src/test/java` trees. Each task follows design.md D1 and D2 and commits per package or small group.

## 1. Flaky tests

- [x] 1.1 Analyse `shaftBreakDropsRiderCargo`, `FieldGameTests.openPlotsCounted`, `FieldGameTests.hydrationReachesFourBlocks` and the `*sendsonopen` tests under `runGameTestServerAll`; record cause and fix per test in design.md "Decisions made during apply". No code yet.
- [x] 1.2 Fix each at its cause (test or production code as the analysis says). Run the affected GameTests at least 10 times without a failure, then `gradlew :storage:runGameTestServer` and `gradlew runGameTestServerAll`.

## 2. Foundation

- [x] 2.1 Foundation group (a) packages (design.md D3). Run `gradlew :foundation:build :foundation:test` and the Foundation GameTests.
- [x] 2.2 Foundation group (b) packages (design.md D3), main and test sources. Run `gradlew :foundation:build :foundation:test` and the Foundation GameTests.
- [x] 2.3 Merge both onto main; `gradlew build test` and all three `runGameTestServer` tasks green (Manufacturing and Storage still compile against the cleaned Foundation).

## 3. Manufacturing and Storage

- [x] 3.1 Manufacturing, all packages, main and test sources. Run `gradlew :manufacturing:build :manufacturing:test` and the Manufacturing GameTests.
- [x] 3.2 Storage `gametest`. Run the Storage GameTests.
- [x] 3.3 Storage `client`, `menu`, `terminal`, `picker`. Run `gradlew :storage:build :storage:test` and the Storage GameTests.
- [x] 3.4 Storage `shuttle`, `field`, `rail`, `garage`, `rack`, `zone`, `index`, `station`, `outbound`, `packing` and their tests. Run `gradlew :storage:build :storage:test` and the Storage GameTests.
- [x] 3.5 Storage `cpu`, `network`, `order`, `production`, `pattern`, `controller`, `crafting`, `stats`, `command`, `registry`, `integration`, `power`, `site`, `datagen` and their tests. Run `gradlew :storage:build :storage:test` and the Storage GameTests.
- [x] 3.6 Merge all onto main; `gradlew build test` green.

## 4. Review and verification

- [x] 4.1 Review pass over the whole diff since the start of this change: behaviour changes, leftover reference comments (`grep` for `D[0-9]`, `design`, `spec`, `PLAN`, `play check`, `task`), broken Javadoc, duplicated helpers created twice by parallel agents. Fix what it finds. Run `gradlew build test`.
- [ ] 4.2 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :manufacturing:runGameTestServer`, `gradlew :storage:runGameTestServer`, `gradlew runGameTestServerAll` and `cd docs && npm test` green, the game launches with all three mods. Then the play check by the user from the main checkout: the main flows of all three mods still work (ducts and pipes, machines and pumps, the alchemy builds, warehouse with shuttles, terminal requests, crafting orders, fields, sites and the Ender Anchor). Apply status 2026-10-13: build (JUnit Foundation 309, Manufacturing 81, Storage 536), runData for all three projects unchanged, GameTests Foundation 129, Manufacturing 211, Storage 587, runGameTestServerAll 669, docs 50, all green in the worktree that main now equals; client launch and play check pending.

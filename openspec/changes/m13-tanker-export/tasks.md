# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Needs `m13-tanker-import` applied first.

## 1. Pure logic

- [ ] 1.1 `gantry/FluidRestockPlan.java` (design.md D3). JUnit `gantry/FluidRestockPlanTest.java`: `missingCountsIncoming`, `noMissingNoRun`, `belowMinLoadWaitsUnlessItFills`, `limitedByShuttleAndSource`, `sourceWithMostFreeFirst`, `nearerWinsOnTie`. Run `gradlew :shuttles:test`.

## 2. Keep levels

- [ ] 2.1 `gantry/FluidGantryBlockEntity.java`, `gantry/GantryContents.java`, `menu/FluidGantryMenu.java`, `client/FluidGantryScreen.java`, lang (design.md D1). Run `gradlew :shuttles:runData`, `gradlew build`.
- [ ] 2.2 GameTests in `gametest/FluidGantryGameTests.java`: `keepLevelSetAndClamped`, `keepLevelRidesTheDroppedItem`, `keepLevelIgnoredOnAny`. Run the Shuttles GameTests.

## 3. Restock runs

- [ ] 3.1 `shuttle/ShuttleJob.java` (`FluidJob.Purpose`), `shuttle/ShuttleFleet.java` (restock leftover rule), `network/WarehouseNetwork.java` (`tickFluids` restock and missing stock lamp) (design.md D2, D3, D4). Run `gradlew build`.
- [ ] 3.2 GameTests in `gametest/TankerGameTests.java`: `exportRestockedFromStorage`, `restockCountsRunsOnTheWay`, `restockBelowMinLoadWaits`, `missingStockRedLamp`, `stockAppearsRestockStarts`, `restockPartialPick`, `restockLeftoverBackToStorage`, `importIsNoRestockSource`. Run the Shuttles GameTests.

## 4. Plan and docs

- [ ] 4.1 `PLAN.md` M13 checked for anything this change found wrong; regenerate `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` if data changed and run the docs tests. Run `gradlew build`.

## 5. Verify

- [ ] 5.1 Full `gradlew build`, `gradlew test`, the Foundation, Alchemy and Shuttles GameTests green; the game launches with all three mods.
- [ ] 5.2 Play check by the user: an Export gantry with a keep level is restocked by a tanker while a pipe drains it, shows red when storage has none of the fluid, and the keep buttons work.

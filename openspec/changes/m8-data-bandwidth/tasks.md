# Tasks

## 1. Pure logic

- [x] 1.1 Add pure `foundation.data.packet.DuctBudget` and `PathPick` and `foundation.upgrade.UpgradeMath` with JUnit `DuctBudgetTest` (`simplexSharesBothDirections`, `duplexSeparate`, `fractionalCarry`, `loadWindow`), `PathPickTest` (`shortestWins`, `highestBottleneckAmongShortest`), `UpgradeMathTest` (`twoDoublesGiveFour`, `noUpgradesIsOne`). Verify with `gradlew :foundation:test`.

## 2. Tiers and replacement

- [x] 2.1 Tier line items (Duplex Data Cable, Fast Data Cable) in Foundation registries, tier id saved on the duct block entity, `cable_tiers.json` with a reload listener, tint per tier in `DuctBlockColor`, recipes, lang, item models via datagen, drops by tier. Verify with build, `:foundation:runData` and Foundation GameTests.
- [x] 2.2 Replacement in `LineItem.useOn`: higher tier replaces in place and drops the old item, lower or equal unchanged, sneak unchanged. GameTests `upgradeCableDropsOld`, `noDowngrade`. Verify with build and Foundation GameTests.

## 3. Bandwidth

- [x] 3.1 `DataNetwork` per tick duct budgets and load window; `Shipment` path choice with `PathPick` and per tick amount bounded by the path budgets. GameTests `slowestDuctDecides`, `sharedDuctSplits`, `simplexOppositeShares`. Verify with build, Foundation and Storage GameTests.
- [x] 3.2 Summary with "N of M items/s"; goggles load payload and overlay tint. GameTest `summaryShowsLoad`. Verify with build and Foundation GameTests; grep that every payload TYPE is registered.

## 4. Upgrades

- [x] 4.1 `foundation.upgrade` (`UpgradeItem`, `UpgradeSlots`, `upgrades.json`, `servus_foundation:upgrades` tag) and the Speed Upgrade item with recipe and lang. Verify with build and `:foundation:runData`.
- [x] 4.2 Packing Station `upgrade_slots` in `packing_station.json`, upgrade row in its GUI, rate from multiplier, rate shown. GameTest `speedUpgradeDoubles`. Verify with build and Storage GameTests.

## 5. Production Terminal

- [x] 5.1 `ProductionView`/`ProductionRows` Packing Station rows with use per direction and the busiest duct; Machines tab drawing; lang. GameTests `stationRowShowsUse`, `busiestDuctNamed`. Verify with build, JUnit and Storage GameTests.

## 6. Docs and plan

- [x] 6.1 Foundation and Storage docs pages (tiers, simplex and duplex, replacement, upgrades, load), `:manufacturing:runData` for gamedata, PLAN.md M8 list item and section 3. Verify with `npm test` in `docs/`.

## 7. Integration

- [ ] 7.1 Full `gradlew build` and `gradlew test` green, Foundation, Manufacturing and Storage GameTests green, and the game launches with all three mods.

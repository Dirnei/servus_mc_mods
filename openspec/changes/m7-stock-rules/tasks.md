# Tasks

## 1. Rules and the check

- [ ] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/order/StockRule.java` and `StockRules.java`, hold and persist them on `WarehouseControllerBlockEntity.java` (carrot dismantle keeps them in the item), add `openAmountFor` and rule origin orders to `OrderBook.java`, track the controller position and run `tickStockRules` in `WarehouseNetwork.java`, read `stock_rule_interval` in `StorageStatsManager.java` and add it to `warehouse.json`. Verify `gradlew :storage:build` is green.

## 2. Terminal and commands

- [ ] 2.1 Add `SetStockRulePayload`, rule rows in `StockSyncPayload`, `setRule` in `CounterTerminalMenu.java`, the Rule mode, grid marker and tooltip in `CounterTerminalScreen.java`, lang keys, and the `rules`, `rule` and `unrule` commands in `ServusStorageCommands.java`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 3. Tests

- [ ] 3.1 Add GameTests `stockRuleOrdersWhenLow`, `stockRuleIgnoresWhenEnough`, `stockRuleWithoutProducerShowsReason` and `stockRulesPersist` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [ ] 4.1 Run `openspec validate m7-stock-rules --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.

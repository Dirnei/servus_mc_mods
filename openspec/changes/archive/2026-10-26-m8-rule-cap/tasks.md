# Tasks: m8-rule-cap

Prerequisites: `m8-terminal-tabs` (Rule tab, Rules tab) and `m8-station-numbers` (station labels) are applied.

## 1. Rule model

- [x] 1.1 Add pure `shuttles/.../order/RuleThresholds` (validate a Make pair and a cap, return `ok`, `empty`, `make_order` or `cap_below_max`) and `order/CapRoom` (`room(cap, held, offered)`, no cap means all); add JUnit `RuleThresholdsTest` (cap only, Make only, both, nothing set, min not below max, cap below max, cap equal to max) and `CapRoomTest` (below, at and above the cap, offer larger than room, no cap); `gradlew :shuttles:test` green
- [x] 1.2 Change `order/StockRule` to `(key, Make make, int cap)` with nullable `Make(min, max)` and update `StockRules.save`/`load` (write `Min`/`Max` only with Make, `Cap` only when set, read Make when `Max` is present); fix every caller (`StockRuleKeeper`, `WarehouseNetwork.setStockRule`, `StorageTerminalMenu.setRule`, `ServusShuttlesCommands`, `CraftingCpuGameTests`, `ControllerSiteGameTests`); `gradlew build` green and existing rule GameTests pass
- [x] 1.3 `network/StockRuleKeeper`: check only rules with Make, keep the state order refusing, ordering, no CPU / no producer, ok; add a GameTest in new `gametest/StockRuleCapGameTests` that a cap only rule orders nothing and that an old `Min`/`Max` tag loads as Make only; `gradlew build` and GameTests green

## 2. Cap at the Inbound Station

- [x] 2.1 `network/WarehouseNetwork.capRoom(ItemKey, int offered)`: index count plus Inbound buffers plus shuttle cargo on put-aways of the item, through `CapRoom`; offered unchanged without a capped rule; GameTest "waiting stacks count" in `StockRuleCapGameTests`; `gradlew build` green
- [x] 2.2 `station/InboundStationBlockEntity`: clamp `intake.insertItem` and `pullOneStack` by `capRoom`, leave GUI and shift click on `buffer`; GameTests hopper at the cap (partial accept), Pull side at the cap, hand insert ignores the cap, other items pass, offline station takes as before; `gradlew build` and GameTests green
- [x] 2.3 GameTest duct fallback: cobblestone at its cap, a duct with the station at priority 1 and a Sink at priority 0, the Sink receives the cobblestone; `gradlew build` and GameTests green

- [x] 2.4 Count open work orders in the held count: `order/OrderStep.toCollect`, `order/WorkOrder.toCollect`, `order/OrderBook.incomingFor`, `cpu/CpuOrders.incomingFor`, used by `network/WarehouseNetwork.capRoom`; GameTest `openOrderCountsTowardsTheCap` (a duct from a chest moves only the room an open order leaves); `gradlew build`, `gradlew test` and GameTests green

## 3. Refusing state

- [x] 3.1 Post `CapRefused(key, stationPos)` from non simulated clamped inserts and pulls to the network mailbox; `StockRuleKeeper` records the first station per item and shows `refusing` with the station label at the next check, clearing after a check without refusals; `RuleRow` gains `cap` and `detail`; GameTests "quarry at the cap" (reads "refused at Inbound #1") and "below the cap again" (back to ok); `gradlew build` and GameTests green

## 4. Terminal and commands

- [x] 4.1 `terminal/SetStockRulePayload` to `(item, min, max, cap, remove)`, validated server side with `RuleThresholds`, refusal sent to the terminal message line; `menu/StorageTerminalMenu.setRule` updated; `StockSyncPayload` change hash includes the cap; `gradlew build` green
- [x] 4.2 `client/StorageTerminalScreen`: Limit group in the Rule tab with the "Refuse at Inbound above" field (off below 1) in the left column, Save sends the cap, popup message on a refused rule, grid tooltip "refuse above N"; Rules tab Refuse column and the refusing state text; lang strings in `datagen/ServusShuttlesLanguageProvider`; `gradlew build` and `runData` clean, then play check the Rule tab and Rules tab in the client
- [x] 4.3 `command/ServusShuttlesCommands`: `cap <pos> <item> <amount>`, `uncap <pos> <item>`, `rule` keeps the cap, `rules` prints Make, cap and state, refusals name the reason; GameTest or manual check of the scenarios "Op sets a cap", "Uncap a cap only rule" and "Cap below the maximum"; `gradlew build` green

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test` and GameTests green, the game launches with all mods, and a play check confirms a quarry hopper stops at the cap with the surplus going to a Sink on a duct

## Workflow follow-up

- Archive after the play check sign-off; add a Shuttles `CHANGELOG.md` line under Unreleased (Added: stock rules can refuse an item at Inbound Stations above a cap).

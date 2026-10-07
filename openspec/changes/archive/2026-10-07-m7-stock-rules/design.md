# Design

## Context

See proposal.md for motivation. `WarehouseNetwork` owns the `OrderBook`; `OrderBook.create(key, amount, requestId)` plans an order and answers `no_producer` when nothing makes the item; orders carry `requestId` (0 for manual). `WarehouseControllerBlockEntity` is the one persistent block per warehouse and already had a per warehouse rule holder once (zone rules, since moved to labels). The terminal prompt is drawn by `CounterTerminalScreen` with an `EditBox` and buttons; `StockSyncPayload` carries entries, requests and a message.

## Goals / Non-Goals

**Goals:**
- Rules are warehouse policy, so they live on the controller and are edited wherever the stock is seen.
- The check is cheap and runs on the network tick like everything else.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Rule holder on the controller
`StockRule(ItemKey key, int min, int max)` and `StockRules` (list keyed by `ItemKey`, `set`, `remove`, `get`, `all`, NBT save and load as a list of `{Item, Min, Max}`) in `io.github.dirnei.mc.servus.storage.order`. `WarehouseControllerBlockEntity` holds `StockRules rules`, saves them in `"StockRules"`, and its carrot dismantle uses `saveToItem` so the rules ride along (legacy shuttle items drop first as today). `WarehouseNetwork.stockRules()` reaches them through the controller position the fleet does not know any more, so the network keeps `controllerPos` from `onNodeAdded`.

### The check
`WarehouseNetwork.tickStockRules()` every `stock_rule_interval` ticks: for each rule, `have = index.count(key) + orders.openAmountFor(key, rule origin)`; when `have < min`, call `orders.create(key, max - have, RULE_ORIGIN)` where rule orders carry `requestId = -1`. `OrderBook.openAmountFor(key)` sums amounts of open orders for the key. Rule state is computed on demand: `NO_PRODUCER` when the last create said so (kept in a transient map on the network), `ORDERING` when an open order exists, else `OK`.

### Terminal
`StockSyncPayload` gains `List<RuleRow>` with `RuleRow(ItemStack stack, int min, int max, String state)`. `SetStockRulePayload(ItemStack item, int min, int max)` sets a rule, and `min < 0` removes it. `CounterTerminalMenu.setRule` forwards to the network. The screen's prompt gets a Rule toggle button; in Rule mode the amount box becomes the minimum box, a second box holds the maximum, and the buttons are Save, Remove and Cancel. Grid entries with a rule get a two pixel marker in the corner; the tooltip adds "keep min to max, state".

### Commands
`rules`, `rule` and `unrule` beside the existing subcommands.

### Minecraft side verification
GameTests with the Crafting Station: `stockRuleOrdersWhenLow` (rule 8 to 16 sticks, 2 sticks and planks in crates, station with sticks: within the interval an order for 14 exists and later the sticks are in storage, rule state ok afterwards), `stockRuleIgnoresWhenEnough`, `stockRuleWithoutProducerShowsReason`, `stockRulesPersist` (save and load the controller's tag).

## Risks / Trade-offs

- [A rule and a request both order the same item] → both go through the book; the request's order counts as on order for the rule, so the rule orders less or nothing.
- [Rule orders are never cancelled] → removing the rule leaves running orders to finish; their output is stock anyway.

## Open Questions

None.

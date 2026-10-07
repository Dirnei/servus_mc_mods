# Proposal

## Why

Work orders only start when someone asks. A factory wants its warehouse to keep itself stocked: "keep 500 to 1,000 iron gears" should order gears whenever the count drops below 500 and stop at 1,000. This change implements the M7 item "Min/max stock rules" from `PLAN.md`.

## What Changes

- **Stock rules on the warehouse.** A rule names an item, a minimum and a maximum. Rules live on the Warehouse Controller, persist with it, and survive a carrot dismantle in the dropped item.
- **Rules create orders.** Every few seconds the warehouse checks each rule: when the indexed count plus what is already on order falls below the minimum, it creates a work order for the difference up to the maximum. A rule whose item no producer can make shows that as its state instead of ordering.
- **Rules in the terminal.** The amount prompt of the Counter Terminal gets a Rule mode: minimum and maximum fields with Save and Remove. Items with a rule are marked in the stock grid and their tooltip shows the rule and its state.
- **Commands.** `/servus_warehouse rules <pos>` lists rules; `/servus_warehouse rule <pos> <item> <min> <max>` sets one; `/servus_warehouse unrule <pos> <item>` removes one.

## Capabilities

### New Capabilities
- `storage/stock-rules`: rule definition and persistence, the ordering check, rule states, GUI editing and commands.

### Modified Capabilities
- `storage/counter-terminal`: the prompt's Rule mode, grid marker and tooltip for ruled items.
- `storage/warehouse-controller`: the controller holds the stock rules; the rule commands.

## Non-goals

Deferred to later changes:
- Rules per zone or per block; a rule counts the whole warehouse.
- Rules that consume items (discard above the maximum).
- Scheduling or batching several rules into one order.

## Impact

- `storage`: `StockRule` and `StockRules` in the `order` package, a field and NBT on `WarehouseControllerBlockEntity`, the periodic check in `WarehouseNetwork`, `WorkOrder` marks rule orders, a payload `SetStockRulePayload`, `StockSyncPayload` gains rule rows, screen and menu changes, three commands, lang.
- Datapack: `warehouse.json` gains `stock_rule_interval` (example 100 ticks).
- `PLAN.md` needs no change.

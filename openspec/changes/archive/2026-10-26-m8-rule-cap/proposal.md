# Proposal: m8-rule-cap

## Why

Nothing limits what comes into a warehouse: a quarry or mob farm piped into an Inbound Station fills every rack until the station turns red. Players need a way to say "enough cobblestone" at the door, so the surplus stays upstream where a duct can send it on, for example into a Sink. The stock rule is the one place per item that already says how much the warehouse wants, so the limit belongs there.

PLAN.md milestone M8 (autocrafting and stock rules) lists no open point for this; it came out of the 2026-10-26 exploration of warehouse imports and excess. This is the second of three stacked rule changes: it applies after `m8-terminal-tabs` (item popup with a Rule tab, terminal Rules tab) and uses the station labels of `m8-station-numbers`. `m8-rule-export` follows.

## What Changes

- A stock rule's thresholds become optional. A rule holds a Make pair (order when below, fill up to) and a new cap, "Refuse at Inbound above". At least one of the two SHALL be set; a rule with only a cap orders nothing.
- Inbound Stations refuse an item whose warehouse count reached its rule's cap. A hopper, duct or machine insert is taken only up to the cap, the rest stays with the sender; a Pull side pulls at most up to the cap. The count includes stacks waiting in Inbound buffers and put-aways on their way.
- Items the player puts into the station GUI by hand or shift click are not capped. Work order output, the Field Head harvest buffer and the Packing Station are not capped either.
- A rule whose cap refused items since its last check shows the state "refusing" with the station that refused, for example "refused at Inbound #1".
- The Rule tab of the item popup gains a Limit group with the cap field, beside room for the export field of `m8-rule-export`. The terminal Rules tab gains a Refuse column.
- The rule commands set and clear the cap: `/servus_warehouse cap <pos> <item> <amount>` and `/servus_warehouse uncap <pos> <item>`; `rules` lists the cap.
- Saved rules load unchanged as rules with a Make pair and no cap.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/stock-rules`: rules with optional Make and cap thresholds, ordering only for rules with Make, the refusing state, cap editing in the terminal, the Refuse column, cap commands.
- `shuttles/inbound-station`: intake refuses above a rule's cap on Input and Pull sides.

## Impact

- `shuttles/.../order/StockRule`, `StockRules` (optional make, cap, save format), new pure `order/RuleThresholds` (validation) and `order/CapRoom` (room calculation).
- `shuttles/.../network/StockRuleKeeper` (skips cap only rules, refusing state), `WarehouseNetwork` (cap room per item from index, Inbound buffers and in-flight put-aways, refusal mailbox).
- `shuttles/.../station/InboundStationBlockEntity` (intake clamp, `pullOneStack` clamp, refusal report).
- `shuttles/.../terminal/SetStockRulePayload`, `RuleRow`, `menu/StorageTerminalMenu`, `client/StorageTerminalScreen` (Limit group, Refuse column), `command/ServusShuttlesCommands`.
- Lang strings in the Shuttles datagen language provider.
- JUnit `RuleThresholdsTest`, `CapRoomTest`; new `gametest/StockRuleCapGameTests` for the hopper, duct fallback, Pull cap, refusing state and old rule loading.

## Non-goals

- Export of surplus to an Outbound Station (`m8-rule-export`).
- Capping any other way into the warehouse than Inbound Stations.
- Fluid caps (fluids in stock rules are an open M13 point).
- A refusal line or lamp on the Inbound Station itself; the rule state names the station.

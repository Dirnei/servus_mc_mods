# Design: m8-rule-cap

## Context

A stock rule today is `StockRule(key, min, max)` stored in `StockRules` on the Warehouse Controller and checked by `network/StockRuleKeeper` every `stock_rule_interval` ticks. The terminal sends `SetStockRulePayload(item, min, max)` (a negative min removes) and receives `RuleRow(stack, min, max, state)` in `StockSyncPayload`. After `m8-terminal-tabs` the rule is edited in the Rule tab of the item popup and listed in the terminal Rules tab; after `m8-station-numbers` every station has a label such as "Inbound #1".

The Inbound Station exposes one insert only handler, `intake`, on every side that is not Off; Pull sides call `pullOneStack`, which simulates the insert into the buffer and then extracts that much in a transaction. The GUI slots and shift click write into `buffer` directly, not through `intake`. A buffered stack becomes a `PutAwayJob`; `WarehouseNetwork.loadPutAway` moves it from the buffer onto the shuttle, and the shuttle unloads it into the rack (or parks it in its garage). Ducts insert slot by slot through `insertAnySlot` and route what a target refuses to the next priority group.

## Goals / Non-Goals

**Goals:**
- One cap check that every intake path of an Inbound Station shares, exact across several inserts in the same tick.
- A rule model where Make and cap are independent and the export threshold of `m8-rule-export` fits in without another format change.

**Non-Goals:**
- Caching the held count across ticks or maintaining it incrementally.
- Any cap on other PutAwaySources (Field Head harvest buffer, Packing Station).

## Decisions

### Rule model: optional parts in one record
`StockRule(key, OptionalInt min, OptionalInt max, OptionalInt cap)` would spread presence checks everywhere. Instead `StockRule(ItemKey key, Make make, int cap)` with `Make(int min, int max)` nullable and `cap` 0 meaning off (a cap of 0 is meaningless, a rule cap of at least 1 is enforced). Pure `order/RuleThresholds` validates a candidate and returns a reason key: `empty` (neither set), `make_order` (min not below max), `cap_below_max`. The terminal, the payload handler and the commands all call it, so the messages match. `m8-rule-export` adds its threshold to the same record and the same validator.

Alternative considered: keep `min`/`max` ints with `-1` for off. Rejected because `StockRule`'s compact constructor already clamps negatives, and a typed `Make` makes "cap only" obvious in `StockRuleKeeper`.

### Save format stays readable by old code paths
`StockRules.save` writes `Min`/`Max` only when a Make pair exists and `Cap` only when set. `load` reads a Make pair when `Max` is present, so every old tag (always `Min` and `Max`) loads as Make only. No data version or migration step.

### Cap room as a pure function
`order/CapRoom.room(cap, held, offered)` returns how many of `offered` may enter: `clamp(cap - held, 0, offered)`, with no cap meaning all. JUnit covers below, at, above the cap, an offer larger than the room, and no cap.

### Held count computed on demand
`WarehouseNetwork.capRoom(ItemKey key, int offered)` returns `offered` when the item has no capped rule or the network is offline. Otherwise held = `index().count(key)` + the item in the buffers of all Inbound Stations of the network + the cargo of shuttles currently carrying a put-away of that item (from the fleet's shuttle views) + what open CPU orders still make for storage (see "Decisions made while applying"). Buffers are summed live on every call, so a duct filling slot after slot in one tick sees its own earlier inserts and the total stays capped. The station list and shuttle views are small (tens of entries), so no cache is needed; the index lookup is a map get.

Alternative considered: an incoming tally updated on `loadPutAway` and on unload. Rejected: two more hooks that can drift (parked cargo, recall, garage drop) for a saving the sizes do not need.

Only Inbound Station buffers count, not other PutAwaySources, because the cap is a door rule; a harvest buffer waiting for a shuttle must not close the door for the same crop, and it is also not capped itself.

### Clamping at the station
`intake.insertItem(slot, stack, simulate)` asks `capRoom` for the stack's key and inserts at most that many, returning the rest. `pullOneStack` takes `min(buffer room, capRoom)`. The GUI and shift click keep writing to `buffer`, so they bypass the cap without a special case. An offline station has no network, so `capRoom` is not consulted and intake works as before.

### Refusal reporting through the mailbox
When `intake` or `pullOneStack` takes less than offered because of the cap (not the buffer), and the call is not simulated, the station posts `CapRefused(key, stationPos)` to the network's mailbox. Simulated inserts do not report, since ducts simulate before they commit. `StockRuleKeeper` drains these into a map key to first station position during the tick and, at each check, turns it into the state `refusing` with the station label (looked up through `m8-station-numbers`) and clears the map. `RuleRow` gains `cap` and a `detail` string carrying the label, so the state column and the command can say "refused at Inbound #1".

State precedence in `StockRuleKeeper.state`: refusing, then ordering, then the stored `no_cpu` / `no_producer`, then ok.

### Payload and terminal
`SetStockRulePayload` becomes `(ItemStack item, int min, int max, int cap, boolean remove)`; `min`/`max` of `-1` mean no Make pair and `cap` of `0` means none. The server validates with `RuleThresholds` and answers a refusal through the existing terminal message line. The Rule tab's Limit group gets one Foundation number field "Refuse at Inbound above" with off below 1, laid out in the left column of the Limit row so `m8-rule-export` adds its field in the other column (see the mockup board "Item popup"). The Rules tab adds a Refuse column between Make and the state.

### Commands
`cap <pos> <item> <amount>` and `uncap <pos> <item>` join `rule`/`unrule` in `ServusShuttlesCommands`; `rules` prints "keep 8 to 16, refuse above 4096". `rule` keeps an existing cap, `cap` keeps an existing Make pair.

### APIs
No new NeoForge APIs. The intake stays a NeoForge transfer `ResourceHandler<ItemResource>` (via the project's `LegacyItemHandler` adapter) exposed with `Capabilities.Item.BLOCK` as today; a partial accept is a remainder from `insertItem`, which ducts, hoppers and other mods already handle. Transactions for Pull use the project's `Transfer.open()` wrapper as `pullOneStack` does now.

### Pure versus Minecraft
Pure and unit tested: `RuleThresholds` and `CapRoom`. `StockRules` saves through `CompoundTag` and registries, so its round trip is a GameTest. Minecraft side, GameTests: hopper partial accept, duct fallback to a lower priority Sink, Pull cap, waiting stacks counted, hand insert bypass, refusing state, old rule tag loading. Terminal screens: manual play check.

## Risks / Trade-offs

- [A machine pushing one item at a time keeps posting refusals every tick] → the keeper keeps only the first station per key per check, so the mailbox drain stays a map put.
- [Held count misses cargo the instant between `loadPutAway` and the shuttle view update] → the load and the view change happen in the same fleet step on the server thread; the GameTest "waiting stacks count" covers the buffer side.
- [Players expect the cap on every way in] → the Rule tab label says "Refuse at Inbound above", and the non-goal is stated in the proposal.
- [Cap without Make on an item a CPU makes from the warehouse] → crafting output is not capped by design; documented in the spec scenarios.

## Migration Plan

None beyond the save format note above. Old rules load as Make only. Rolling back to a build without caps reads `Min`/`Max` and ignores `Cap`; a cap only rule (no `Max`) would load as 0 to 0 in an old build, which is acceptable for an unreleased feature.

## Decisions made while applying

- The intake clamps in its transfer API `insert(index, resource, amount, transaction)`, not in `insertItem`: hoppers and ducts reach it through transactions, and the slot style insert runs again only when the root transaction commits. What the open transactions already accepted on any slot counts towards the cap through a new protected `pendingInserts(resource)` of Foundation's `LegacyItemHandler`, so a duct filling slot after slot stays capped. The commit itself is not clamped, so nothing a simulation accepted is lost.
- A simulated insert is an aborted transaction there and cannot be told from a real one, so refusals are reported from every clamped insert. The refusing state only names a station, so a simulation that reports changes nothing else.
- `RuleRow.detail` is a `Component` (sent with `ComponentSerialization.TRUSTED_STREAM_CODEC`), so the station label is resolved in the player's language; the rules command appends it to its line the same way.
- Following the mockup the cap field "Refuse at Inbound above" sits in the right column of the Limit row; `m8-rule-export` puts Export above into the left one.
- Fill up to and the cap show 0 as "off" through a new `InlineStepper.offAtZero(hint)` in Foundation. The Rule tab has no Make pair when Fill up to is off and Order when below is 0; Fill up to off with Order when below above 0 is refused like a minimum not below the maximum. `terminal/RuleInput` stays as the terminal's adapter from the fields to a rule and checks it with `RuleThresholds`.
- The Rules tab's add slot row carries the column headings (Make, Refuse, State); the add hint moved into the add slot's tooltip, and a rule row's tooltip lists the whole rule. The Refuse column abbreviates large caps (4.1k).
- The duct fallback GameTest uses a chest as the lower priority target instead of a Sink multiblock; the duct's priority routing is the same.
- Play check: with keep 3 to 64, a cap of 80 and an export above 100, a chest piped into an Inbound Station pushed oak stairs far past 80. The duct and the intake clamp were fine (a copy of the play world held the cap exactly while no order ran). The cause was the held count: it left out work orders. Stock below the Make minimum started a CPU order for 62, the pipe filled the warehouse up to the cap meanwhile, and the crafted stairs landed on top (122, then the export shipped it back to 100). Held now also counts what open CPU orders on the warehouse's data line still make for storage: per order, the outputs of its item its steps have not yet brought to a Packing Station (`WorkOrder.toCollect`, `OrderBook.incomingFor`, `CpuOrders.incomingFor`). Collected outputs are already counted in the Packing Station buffer, on shuttles or in the index. Orders for a terminal request are left out, since their result leaves for the terminal. GameTest `openOrderCountsTowardsTheCap`.
- The rule commands are covered by the GameTest `capCommands` (cap, rules, uncap, a cap below the maximum, `rule` keeping the cap).

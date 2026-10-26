# Design: m8-rule-export

## Context

Stock rules live in `StockRules` on the Warehouse Controller, one `StockRule` per `ItemKey`; `StockRuleKeeper` checks them every `stock_rule_interval` and builds the `RuleRow`s the terminal shows. After `m8-rule-cap` every threshold of a rule is optional and the Rule tab (from `m8-terminal-tabs`) has a Make and a Limit group. `m8-station-numbers` gives every station a label ("Outbound #3" or "Compost Dock (#3)").

The Outbound Station restock already does almost what an export needs: `WarehouseNetwork.tickOutbound` runs every `outbound_check_interval`, and `restock` compares each keep slot with the buffer plus `pendingTo(pos, key)`, bounds the request by `buffer().freeSlots()` and calls `planAndStart(pos, key, missing, 0)`, the same delivery path as terminal requests. `setRestockState` drives the activity lamp and the GUI state line. Keep slots are an `ItemSlots` of ghost stacks edited through `OutboundStationMenu.clicked` (left click sets the carried stack, right click clears).

## Goals / Non-Goals

**Goals:**
- One source of truth for who exports what: the rule.
- Reuse the keep restock path for deliveries, so exports respect slot locks, free slots and shuttles like any request.
- Pure, unit tested logic for the surplus amount and for the claim bookkeeping.

**Non-Goals:**
- Changing how keep slots restock, or how the station pushes.
- A new network event; exports ride on the existing outbound check.

## Decisions

### The assignment is stored on the rule
`StockRule` gains an optional export threshold and an optional `ExportTarget(BlockPos station, int slot)`. `StockRules` saves both (`Export`, `ExportStation`, `ExportSlot`) and so the carrot dismantle keeps them with the rules. The station holds no export state of its own; it asks the network which rules target its position.
- Alternative: an export ghost in the station's keep `ItemSlots`. Rejected: exclusivity would need a warehouse wide scan of stations, two places would have to agree, and a dismantled station would carry a claim into another warehouse.

### Claim bookkeeping as a pure class
A generic `ExportBook<K, P>` (no Minecraft imports) keeps `K -> (P, slot)` and answers `holder(key)`, `claim(key, station, slot)` (moves an existing claim, refuses an occupied slot), `releaseSlot(station, slot)`, `releaseStation(station)` and `clear(key)`. `StockRules` delegates to it with `ItemKey` and `BlockPos`. JUnit `ExportBookTest` covers claim, move, occupied slot refusal, release by slot and by station, and clearing on rule removal.

### Surplus formula
`ExportPlan.surplus(indexed, threshold, queued)` returns `max(0, indexed - threshold - queued)`, where `indexed` is the rack count from the index and `queued` is the part of `pendingTo` still waiting in racks (`fleet.queuedAmount`). Riding and held loads already left the racks and are not in `indexed`, so subtracting them again would under ship. `ExportPlan.bounded(surplus, freeSlots, maxStackSize)` applies the same one stack per free slot bound as the keep check. `ExportPlan.validExport(fillUpTo, exportAbove)` is the save check (an absent Fill up to always passes). JUnit `ExportPlanTest` covers below, at and above the threshold, queued amounts, the slot bound and the validation.

### Deliveries ride on `restock`
`restock(pos, station)` runs the keep slots as today, then the exports of that station in slot order, sharing the same free slot budget, calling `planAndStart` for the bounded surplus. It records per export key one of planned, nothing to do, or full (surplus left but no free unlocked slot) for the rule state, and a full export sets the station's short stack with reason `export_full` so the lamp turns red and the state line names it. Keep slots go first so a station that both keeps and exports never starves its machine for surplus.
- Alternative: a separate export tick in `StockRuleKeeper`. Rejected: it would duplicate the free slot and pending logic and race the keep check for the same buffer.

### Release on leaving the warehouse
When the network drops a station from its member set (break, carrot dismantle, rail removed), it calls `StockRules.releaseStation(pos)`. Unloading does not change membership, so an unloaded station keeps its exports. A rule removed or saved without an export clears its claim. Rejoining does not restore a claim.

### Keep slot clicks
`OutboundStationScreen` intercepts a left click on an empty keep slot with an empty carried stack and opens the picker instead of sending the click. `OutboundStationMenu.clicked` refuses left clicks on an export slot and turns a right click on an export slot into a release; `setKeep` refuses export slots. Claiming sends `ClaimExportPayload(slot, item)`; the server checks the station is online, the slot holds neither a keep nor an export and the rule has an export, then claims through `StockRules`.

### Syncing the picker and the slots
`OutboundStationMenu` gets a `PayloadSync` of `ExportListPayload`: this station's export slots (slot, item, threshold, state) and the claimable exports of the warehouse (item, threshold, holder label or none). The screen filters it by the search box client side. `RuleRow` gains the export threshold, the holder label and the export state for the Rules tab and the Rule tab line.

### NeoForge APIs
- Payloads: `CustomPacketPayload` with `StreamCodec`, registered in `ServusShuttlesPayloads` through `PayloadRegistrar` like `KeepSlotPayload` (https://docs.neoforged.net/docs/networking/payload).
- Menus: `AbstractContainerMenu.clicked` and `broadcastChanges` with the existing `PayloadSync` helper; the search box is a vanilla `EditBox`.
- No new events or capabilities; delivered items leave through the station's existing `Capabilities.Item.BLOCK` handler.

### What is pure and what is tested in game
Pure and JUnit: `ExportBook`, `ExportPlan`. GameTests: claiming and moving between two stations, release on break and on rail removal, surplus shipped above the threshold, nothing shipped below it, full station state, no second request for surplus on its way. Manual: the picker, its search, the export marker and the Rules tab columns.

## Risks / Trade-offs

- [Export and Make fight over the same item] → the save check requires Export above over Fill up to, and Make never orders above Fill up to.
- [Cap below the export threshold means Inbound never fills to it] → intended: then the export only catches surplus from harvests, leftovers and the player.
- [A station that pushes nowhere fills with surplus and stays red] → the full state names the item in the station GUI and the Rules tab.
- [A network rebuild while the station chunk is unloaded could drop it as a member] → release only for positions the rebuild saw as loaded and not an Outbound Station; an unloaded position keeps its claim.

## Migration Plan

Rules saved before this change have no `Export` key and load without an export. Stations need no migration. Rollback leaves the extra keys unread.

## Decisions made while applying

- The surplus is the racks' unlocked stock (`WarehouseNetwork.available`, read from the rack slots with every pick lock taken off) above the threshold, not the index count less `fleet.queuedAmount`. A pick locks its items when it is planned, and a pick a shuttle has taken is no longer queued while its items are still in the rack, so index less queued counted the same items twice and shipped the whole stock (the `surplusShipped` GameTest found it). `ExportPlan.surplus` keeps its third parameter for requests still counted in the stock; the restock passes 0.
- `StockRule` gets only the threshold (`export`, -1 for none). The station and slot live in `StockRules` through an `ExportBook<ItemKey, BlockPos>`, saved as `ExportStation` and `ExportSlot` beside `Export`; there is no `ExportTarget` field on the record.
- Claims are released at every outbound check instead of in the network's member removal, since a broken rail rebuilds the networks rather than removing one member: a claim whose station is loaded, not a member of this warehouse and not waiting to join a network (a station with no network yet) is freed. An unloaded station keeps its claims. There is no GameTest for the unloaded case.
- Export above is off at -1, since 0 is a useful threshold (export everything). `InlineStepper.offAtZero` became `offAt(value, hint)`, so Fill up to and the cap are off at 0 and Export above at -1.
- Following the mockup, the Limit row has Export above on the left and Refuse at Inbound above on the right. The Rule tab's second note line reads "Export goes to Compost Dock (#3)" or, in the alert colour, "No station: open an Outbound Station and claim this export".
- The Rules tab columns are Make, Export, Refuse and a State column that shows, for a rule with an export, its station, "no station" or "Outbound #2 full", as the mockup's Station / state column; the refusing state still comes first. Numbers are abbreviated (2k) and the row tooltip shows the whole rule.
- The station's full state uses the short reason `export_full` ("Wheat: the station is full, the export waits"); the keep slot of an export shows the item, an orange corner and the threshold with a plus ("2k+").
- Incoming surplus skips the racks (play check): `drain` asks `ExportPlan.direct(stock, threshold, count)` before ranking racks, with the stock as the racks' unlocked count plus put-aways on their way into racks. That part becomes a `PutAwayJob` whose target is the Outbound Station holding the export, with an IN lock in its buffer (empty slots only, like a pick) and an OUT lock on the source slot beside the lock of the stored rest; later items join an open put-away to the same station like a top up, and waiting put-aways count by the OUT locks they hold at their source. `unloadInto` fills the station's locked slots, and what it does not take is routed to racks again like any put-away leftover. It covers every put-away source (Packing Station, Inbound Station, Field Head) since they share `drain`. A put-away stop that is a station, not a rack, takes its cargo out of storage: `pendingTo` counts such put-aways for the station, so the export does not request them again, and `capRoom` leaves them out of the held count. When no station holds the export, its station is unreachable or has no free slot, everything is stored and the export check ships it later.
- `RuleRow` carries the holder's label and `ExportListPayload` the labels of other stations as components, so labels show in the player's language. The rules command lists "export above N".

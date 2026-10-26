# Proposal: m8-rule-export

## Why

A warehouse only ever fills up: stock rules order what runs low, but nothing ever leaves on its own, so harvests, crafting leftovers and hand deposits pile up until the racks are full. Players want the surplus of an item shipped out to a Sink, a composter or a furnace line once the warehouse holds enough of it.

PLAN.md milestone M8 (autocrafting and stock rules) and the open point "warehouse excess on stock rules" that `m5-duct-endpoint-gui` adds under "Open: Foundation". This is the last of four stacked changes: it needs `m8-station-numbers` (station labels), `m8-terminal-tabs` (the Rule tab and the Rules tab) and `m8-rule-cap` (optional thresholds and the Limit group of the Rule tab).

## What Changes

- A stock rule gets a fourth, optional threshold: **Export above**. It is set in the Limit group of the Rule tab next to the cap. With Make set, Export above must be over Fill up to; saving a rule that breaks this is refused.
- An export needs a target. A rule with an export and no station shows "no station" in the Rule tab and the Rules tab.
- At any Outbound Station of the same warehouse, clicking an empty keep slot with an empty hand opens a **Claim an export** picker: exports without a station first, then exports held by other stations (claiming moves them). A search box filters by item name or station label. One export belongs to one station.
- A claimed export occupies that keep slot and shows an export marker; its threshold comes from the rule and is not edited at the station. Right clicking the slot with an empty hand releases it.
- Every outbound check, the warehouse ships the surplus (stock minus the threshold minus what is already on its way) into the claiming station's buffer by shuttle, limited by the buffer's free unlocked slots. The station pushes it out like any delivery.
- The assignment lives on the rule. Breaking or dismantling the station, or the station leaving the warehouse, releases the export; removing the export from the rule frees the slot.
- Export states (exporting, no station, station full) show in the Rules tab, in the Rule tab's export line ("Export goes to Compost Dock (#3)") and in the station's GUI and activity lamp.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/stock-rules`: the export threshold, its validation, its states and where an export's target is kept and released.
- `shuttles/outbound-station`: claiming and releasing exports in keep slots, the claim picker, export deliveries and the full state.

## Impact

- `shuttles/.../order/StockRule`, `StockRules` (export threshold, assigned station and slot, save and load), `network/StockRuleKeeper` (export state), `terminal/RuleRow` and the rule sync payload (export, station label, export state).
- `shuttles/.../network/WarehouseNetwork` (`restock` also plans exports, release on station removal), new pure `order/ExportPlan` and `order/ExportBook`.
- `shuttles/.../outbound/OutboundStationBlockEntity` (export slots read from the rules, full state), `menu/OutboundStationMenu` (claim and release clicks, export list sync), `client/OutboundStationScreen` (export marker, claim picker with search), new payloads `ClaimExportPayload`, `ExportListPayload`.
- `client/StorageTerminalScreen` Rule tab and Rules tab (Export field and Station column, both laid out by `m8-terminal-tabs` and `m8-rule-cap`).
- Lang strings in the shuttles datagen; JUnit `ExportPlanTest` and `ExportBookTest`; GameTests for claiming, moving, releasing and delivering.

## Non-goals

- Several stations sharing one export, or splitting its load.
- Exports of fluids (fluids in stock rules are an M13 open point).
- Exports to other sites (SEND and KEEP rules between sites stay open).
- Export thresholds in `/servus_warehouse rule` commands.
- A number plate on the station block; labels show in the UI only.

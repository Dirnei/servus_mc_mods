# Proposal

## Why

A play check follow up to M10 field work, not an item in PLAN.md. Fields harvest and fertilize whenever crops are ripe, so a farm fills the warehouse without end and spends bone meal even when the produce is plentiful. That goes against the family's idea of production on demand: nothing should run without a need.

## What Changes

- The Field Head learns its **produce** from its harvests: the item a harvest brings home the most of, leaving out the seed when something else comes too (wheat for a wheat field, carrots for carrots).
- Two levels per head, counted in the produce in stock:
  - a **harvest limit** at which the field stops harvesting (ripe crops wait on the field)
  - a **fertilize level** below which the field fertilizes at all

  Both are 0 (off) by default, so existing fields keep working as before.
- An **anti-corrosion item** chosen on the Water tab with a picker like the fertilizer picker (honeycomb until chosen, or Off to let the copper age), and an **anti-corrosion reserve**: like the fertilizer reserve, the items at or below it stay in the warehouse. 0 by default (`anti_corrosion_reserve`).
- A new **Stock** tab in a larger Field Head GUI (play test: the tabs were too crowded) holds the produce with its stock, the two levels and the two Keep in stock reserves (fertilizer and anti-corrosion) as typed number fields, and the head's buffer (seeds, fertilizer, anti-corrosion). The Field and Fertilizer pickers span the page again; the Field, Water and Fertilizer tabs lose the level rows, the reserve and the buffer counts. The statuses say "paused" or "not needed" without a red marker. The field command prints the produce and both levels.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/field-head`: new requirements for the produce, the levels, how they stop harvesting and fertilizing, and their GUI. Modifies "Field Head GUI" (as `m10-field-head-buffer` leaves it) for the Stock tab and the larger panel, and "Fertilize settings per head" because the reserve is typed instead of stepped. A new requirement for the anti-corrosion item and reserve; the Water tab gets the anti-corrosion picker.
- `storage/field-supply` (from `m10-field-head-buffer`, archived first): "Supply run" brings the chosen anti-corrosion item above its reserve.

## Impact

- `FieldHeadBlockEntity` (produce, levels, save and load), `FieldRuns` (count harvest drops, leave ripe plots while capped), `WarehouseNetwork.tickFields` and `FieldDue`.
- `FieldHeadMenu`, `FieldHeadScreen`, a payload for the typed amounts (levels and reserve), lang, the field command.
- `storage/field.json` (`harvest_limit`, `fertilize_below`, `anti_corrosion_reserve`), `SupplyRun` and `WarehouseNetwork.supplyDue`, `StorageStatsManager`, docs, GameTests, pure logic with JUnit.
- Lands after `m10-field-head-buffer`, which touches the same head, GUI and runs; apply that one first.

## Non-goals

- Limits across several fields of the same produce (each head counts the whole warehouse on its own).
- Choosing the produce by hand.
- Stock rules or CPU orders driving fields.

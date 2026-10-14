# Proposal

## Why

A play check follow up to M10 field work, not an item in PLAN.md. Fields harvest and fertilize whenever crops are ripe, so a farm fills the warehouse without end and spends bone meal even when the produce is plentiful. That goes against the family's idea of production on demand: nothing should run without a need.

## What Changes

- The Field Head learns its **produce** from its harvests: the item a harvest brings home the most of, leaving out the seed when something else comes too (wheat for a wheat field, carrots for carrots).
- Two levels per head, counted in the produce in stock:
  - a **harvest limit** at which the field stops harvesting (ripe crops wait on the field)
  - a **fertilize level** below which the field fertilizes at all

  Both are 0 (off) by default, so existing fields keep working as before.
- The GUI shows the produce and both levels with step buttons, and the statuses say "paused" or "not needed" without a red marker. The field command prints them.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/field-head`: new requirements for the produce, the levels, how they stop harvesting and fertilizing, and their GUI. The existing GUI and work status requirements are not modified here (`m10-field-head-buffer` modifies them); the new requirements add to them.

## Impact

- `FieldHeadBlockEntity` (produce, levels, save and load), `FieldRuns` (count harvest drops, leave ripe plots while capped), `WarehouseNetwork.tickFields` and `FieldDue`.
- `FieldHeadMenu`, `FieldHeadScreen`, a payload for the level buttons, lang, the field command.
- `storage/field.json` (`harvest_limit`, `fertilize_below`), `StorageStatsManager`, docs, GameTests, pure logic with JUnit.
- Lands after `m10-field-head-buffer`, which touches the same head, GUI and runs; apply that one first.

## Non-goals

- Limits across several fields of the same produce (each head counts the whole warehouse on its own).
- Choosing the produce by hand.
- Stock rules or CPU orders driving fields.

# Proposal: m5-duct-keep-amounts

## Why

Players want an overflow valve: keep a tank at a level and send the surplus to a Sink, keep 64 saplings in a chest and send the rest to a composter. Ducts today either take everything from a block or nothing. The same number on a receiving side gives a regulator ("fill this furnace with at most 16 coal"), which stops a duct from flooding one machine with all of a resource.

PLAN.md milestone M5 Logistics, open point added to "Open: Foundation" by `m5-duct-endpoint-gui`. Apply after `m5-duct-endpoint-gui` and `m5-duct-fluid-filter`, whose item and fluid filter slots carry the amounts.

## What Changes

- Every item and fluid filter slot on a duct side gets an amount, default none.
- On a side that gives (an item side with Pull, a fluid side with only In, or a fluid side with In and Out acting as a source) the amount is a keep amount: the duct only takes what the block holds above it of that entry. "Sapling, keep 64" leaves 64 saplings in the chest; "Water, keep 8,000 mB" leaves the tank at 8,000 mB.
- On a side that receives (a side with Out; a fluid side with In and Out only while it acts as a sink) the amount is a fill up to amount: the block only receives that entry until it holds the amount.
- The amount counts everything of that entry the block shows to that side, over all its slots or tanks.
- The GUI shows the amount on each filter slot (like a stack count, mB shortened for fluids) and edits it in a small amount popup with a number field and step buttons. The label says "Keep" on giving sides, "Up to" on receiving sides and "Keep / up to" on sides that do both.
- The Data Potato's side report names how many entries have an amount.
- PLAN.md: the overflow valve open point is done on the duct; the Overflow Valve block and warehouse excess stay open.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: new requirements for amounts on filter entries, keep amounts, fill up to amounts, their editing and the pure allowance logic.
- `foundation/data-potato`: the duct inspect report includes the amount entries.

## Impact

- New pure `foundation/.../logistics/duct/KeepAmounts` (allowance to give and to receive), `KeepAmountsTest`.
- `logistics/duct/UtilityDuctBlockEntity` (amount per item filter slot and per fluid filter slot, save, sync, dismantle), `FluidFilter` gains amounts.
- `logistics/network/ItemChannelNetwork` (`pull` caps by keep, `capacity` caps by up to), `logistics/network/FluidChannelNetwork` (offers capped by keep, sinks capped by up to, pushed fluid too).
- `logistics/menu/UtilityDuctMenu` and a new `SetDuctFilterAmountPayload`, `logistics/client/UtilityDuctScreen` (amount overlay, amount popup on the Foundation `NumberStepper`).
- `duct/DuctReport`, lang in `logistics/datagen/LogisticsDataParts`, `PLAN.md`.

## Non-goals

- An Overflow Valve block; it stays an open point in PLAN.md.
- Warehouse excess (a stock rule with only a maximum that ships the surplus out); it stays an open point.
- Amounts without a filter entry, and amounts on the power line.
- Ratios or percentages instead of absolute amounts.

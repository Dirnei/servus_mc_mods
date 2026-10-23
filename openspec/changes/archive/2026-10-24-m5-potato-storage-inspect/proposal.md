# Proposal

## Why

A player cannot tell how full one rack section or one gantry is without opening its GUI and counting slots or reading tank bars. The Data Potato already reports what ducts and tanks are doing, so it is the natural tool to ask a storage block how full it is. This implements the storage part of the PLAN.md open point under "Open: Foundation": "Deeper inspection with the Data Potato: throughput per channel, why items are stuck, machine and storage reports".

## What Changes

- Using the Data Potato on a Heavy Duty Rack prints a chat report for its whole section: the zone, the number of bays, the slots in use (holding items or locked by a job, as the storage fill counts them) and all slots, a full marker when no slot is free, and the items it holds most with their counts.
- Using the Data Potato on any part of a Fluid Gantry prints its mode and, per tank, the fluid, amount and capacity in mB and the lock.
- Neither report opens a GUI or changes anything; before, the potato on a rack front or on a gantry opened their GUI.
- Foundation gets a small hook so a block of any Servus mod can answer the potato with its own report. The duct and the Fluid Tank move onto the same hook; their reports stay as they are.
- The potato's tooltip names storage as something it reports on.

## Non-goals

- Fill levels for the whole warehouse, per zone or per site (the controller lines and the Production Terminal Storage tab are `m8-storage-fill`).
- Reports for stations, the controller, machines or throughput per channel; they stay open under the same PLAN.md point.

## Capabilities

### New Capabilities

### Modified Capabilities
- `foundation/data-potato`: blocks of other Servus mods can give their own potato report; a block without one is unaffected.
- `shuttles/heavy-duty-rack`: rack section inspect with the potato.
- `shuttles/fluid-gantry`: gantry inspect with the potato.

## Impact

- Foundation: `item/DataPotatoItem`, a new block interface next to `item/Wrenchable`, `UtilityDuctBlock` and `FluidTankBlock` implement it, lang datagen.
- Shuttles: `rack/HeavyRackBlock` and `gantry/FluidGantryBlock` implement it and let the potato through instead of opening their GUI, new report classes in `rack/` and `gantry/`, lang datagen, GameTests.
- No new dependency; Foundation still knows nothing of Shuttles.

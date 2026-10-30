# Proposal

## Why

A Fluid Outbound feeding a machine drains a little at a time, and today a tanker sets off as soon as the tank lacks anything below its keep amount, bringing odd amounts like 237 mB. The play check showed tankers driving back and forth for scraps. Restocks should move whole buckets, and only once the tank has dropped to a level the player chooses.

This refines PLAN.md M13 Fluid logistics (Fluid Racks to Fluid Outbounds by Tanker shuttle), which is built and archived; it adds no new open point.

## What Changes

- Restock runs carry whole buckets only: a run's amount is rounded down to a multiple of 1,000 mB, and no run starts for less than one bucket.
- The Fluid Outbound's keep gets a restock level, from 0 up to the keep amount. A restock starts when the tank's have (amount plus what runs on the way bring) falls below the restock level, and runs keep coming until less than one bucket is missing to the keep amount.
- The GUI gets a second amount, "Restock below", with its own -1 B, +1 B, -10 B, +10 B buttons and a second mark on the gauge.
- The Data Potato line names the restock level.
- A new keep and an old saved station start with the restock level at the keep amount, so they behave like today apart from whole buckets.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/fluid-outbound`: the keep entry gets a restock level; the GUI keep row, the lamp and the inspect line follow it.
- `shuttles/tanker`: export restock runs start below the restock level and carry whole buckets.

## Non-goals

- Whole buckets for fluid put-away runs from Fluid Inbounds to Fluid Racks.
- Moving the keep and restock steps onto the Foundation amount widgets (still open in PLAN.md).
- A restock level for the item Outbound Station.

## Impact

- `shuttles/.../fluidstation/FluidKeep.java` (restock level), `FluidOutboundBlockEntity.java` (save, steps, restocking flag), `FluidStationReport.java`.
- `shuttles/.../fluid/FluidRestockPlan.java` (whole buckets, start rule), `network/WarehouseNetwork.java` (`restockFluids`).
- `shuttles/.../menu/FluidOutboundMenu.java`, `client/FluidOutboundScreen.java`, lang provider.
- Unit tests `FluidKeepTest`, `FluidRestockPlanTest`; GameTests in `TankerRestockGameTests`, `FluidOutboundGameTests`.

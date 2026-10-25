# Proposal: m5-duct-endpoint-gui

## Why

A duct side set to disabled loses its arm, and the endpoint GUI only opens from an arm, so a disabled side can never be switched back on without breaking the duct. Players also want to pause a side without disconnecting it, and to switch it with redstone. This is the first of three duct changes toward the overflow valve (keep amounts), and it gives the endpoint GUI the side picker and layout the next two build on.

PLAN.md milestone M5 Logistics is done and lists no open point for this. This change adds the duct overflow work to "Open: Foundation" and implements its first part.

## What Changes

- The Configuration Carrot opens the endpoint GUI from the centre or any arm of a duct, not only from an arm that ends at a block.
- The GUI gets a side picker: six buttons, one per side, each showing the item of the block on that side. Sides toward a block are selectable, also when disabled. Sides toward another duct show that duct greyed and are not selectable. Sides with nothing beside them show an empty frame and are not selectable. Picking a side switches the page to that side without closing the GUI.
- Every item, power and fluid line on every side gets a Control setting: Always, While powered, While unpowered, Off. Off and an unmet redstone condition keep the arm and the connection and only stop that side from moving anything. The disabled mode still disconnects the side.
- A duct counts as powered when it receives a redstone signal from any neighbour. It follows signal changes as they happen, without scanning every tick.
- The data line keeps its on and off button and gets no Control.
- The Data Potato's duct report names the Control of each line and whether it currently blocks the side.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: the Duct GUI requirement (open from anywhere, side picker), a new Control requirement with redstone, and the line behaviour requirements skip sides whose Control blocks them.
- `foundation/data-potato`: the duct inspect report includes each line's Control.

## Impact

- `foundation/.../logistics/duct/UtilityDuctBlock` (`onWrench` opens from centre and every arm, `neighborChanged` refreshes the power flag), `UtilityDuctBlockEntity` (control per line per side, powered flag, save and sync), new pure `DuctControl`.
- `foundation/.../logistics/menu/UtilityDuctMenu` (mutable side, side select and control buttons), `logistics/client/UtilityDuctScreen` (side picker row, Control button, layout below it).
- `foundation/.../logistics/network/` (`ItemChannelNetwork`, `FluidChannelNetwork`, `EnergyChannelNetwork` skip blocked endpoints), `duct/DuctReport`.
- Lang strings in `logistics/datagen/LogisticsDataParts`, the GUI texture height if the page grows.
- JUnit `DuctControlTest`; GameTests for redstone control and reopening a disabled side.
- `PLAN.md` "Open: Foundation": the duct overflow work (this change, fluid filter, keep amounts), a possible Overflow Valve block, and warehouse excess on stock rules.

## Non-goals

- Fluid filter slots and fluid priority (`m5-duct-fluid-filter`).
- Keep amounts and fill up to on duct sides (`m5-duct-keep-amounts`).
- An Overflow Valve block and warehouse excess export; they stay open points in PLAN.md.
- Control on the data line, and redstone read per side instead of per duct.

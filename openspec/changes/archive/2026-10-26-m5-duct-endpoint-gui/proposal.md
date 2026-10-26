# Proposal: m5-duct-endpoint-gui

## Why

A duct side set to disabled loses its arm, and the endpoint GUI only opens from an arm, so a disabled side can never be switched back on without breaking the duct. Players also want to pause a side without disconnecting it, and to switch it with redstone. This is the first of three duct changes toward the overflow valve (keep amounts), and it gives the endpoint GUI the side picker and layout the next two build on.

PLAN.md milestone M5 Logistics is done and lists no open point for this. This change adds the duct overflow work to "Open: Foundation" and implements its first part.

## What Changes

- The Configuration Carrot opens the endpoint GUI from the centre or any arm of a duct, not only from an arm that ends at a block.
- The GUI gets side tabs on top of the page, one fixed place per side, each tab showing the item of the block on that side. Sides toward a block are selectable, also when no line connects there. Sides toward another duct show a greyed tab that cannot be selected. Sides with nothing beside them show no tab. Picking a tab switches the page to that side without closing the GUI.
- The single mode cycle (in and out, pull in, push out, disabled) becomes three small icon toggles per line per side: In, Out and Pull (Item Pipe only, needs In). In and Out is the default; In only takes what the block pushes in and never pulls; neither In nor Out disconnects the side. Older worlds load with the matching flags.
- Every item, power and fluid line on every side gets a redstone setting (Ignore redstone, While powered, While unpowered) on one small icon button and a separate On/Off button. A side runs when it is on and its redstone setting is met; a side that does not run keeps its arm and connection. Switching off keeps the redstone setting.
- The remove line button becomes a small trash can in the page's bottom right corner, apart from the settings, with a tooltip that it removes the line from the duct and drops it.
- A duct counts as powered when it receives a redstone signal from any neighbour. It follows signal changes as they happen, without scanning every tick.
- The data line keeps its on and off button and gets no redstone setting.
- The Data Potato's duct report names each line's flags, its redstone setting and whether the side is switched off or paused.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: the modes become In, Out and Pull flags (per side modes, item channel behaviour, in and out sides for fluids), the Duct GUI requirement (open from anywhere, side tabs, icon buttons, trash can), a new redstone setting and On/Off requirement, and the line behaviour requirements skip sides that are not active.
- `foundation/data-potato`: the duct inspect report names flags, redstone setting and on or off.

## Impact

- `foundation/.../logistics/duct/UtilityDuctBlock` (`onWrench` opens from centre and every arm, `neighborChanged` refreshes the power flag), `UtilityDuctBlockEntity` (flags, redstone setting and On/Off per line per side, powered flag, save, older save loading and sync), new pure `DuctMode` and `DuctRedstone`.
- `foundation/.../logistics/menu/UtilityDuctMenu` (mutable side, side select, flag, redstone and On/Off buttons), `logistics/client/UtilityDuctScreen`, `DuctSideTabs`, `DuctIconButton` (side tabs, icon row, trash can).
- `foundation/.../logistics/network/` (`ItemChannelNetwork`, `FluidChannelNetwork`, `EnergyChannelNetwork` read the flags and skip inactive endpoints, `EndpointTally`), `duct/DuctReport`.
- Lang strings in `logistics/datagen/LogisticsDataParts`.
- JUnit `DuctModeTest`, `DuctRedstoneTest`; GameTests for flags, On/Off, redstone, older saves and reopening an unconnected side. Duct GameTests in Shuttles and Alchemy move to the flags.
- `PLAN.md` "Open: Foundation": the duct overflow work (this change, fluid filter, keep amounts), a possible Overflow Valve block, and warehouse excess on stock rules.

## Non-goals

- Fluid filter slots and fluid priority (`m5-duct-fluid-filter`).
- Keep amounts and fill up to on duct sides (`m5-duct-keep-amounts`).
- An Overflow Valve block and warehouse excess export; they stay open points in PLAN.md.
- Redstone setting on the data line, and redstone read per side instead of per duct.
- Pull on the Power Cable and the Fluid Pipe: In already drains a block there.

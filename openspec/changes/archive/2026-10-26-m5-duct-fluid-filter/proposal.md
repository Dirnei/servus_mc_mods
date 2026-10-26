# Proposal: m5-duct-fluid-filter

## Why

The Fluid Pipe moves any fluid to any sink on its channel and shares it evenly, so a player cannot send only lava to one machine or fill a main tank before an overflow tank. The item line has had filters and priorities since M5. This is the second of three duct changes toward the overflow valve: the keep amounts in `m5-duct-keep-amounts` sit on fluid filter slots, so the fluid line needs them first.

PLAN.md milestone M5 Logistics, open point added to "Open: Foundation" by `m5-duct-endpoint-gui`. Apply after `m5-duct-endpoint-gui`, whose side tabs and page layout this change extends.

## What Changes

- Each side of a duct's fluid line gets a whitelist of nine fluid slots. A slot holds a fluid, not an item.
- A slot is set by clicking it with any item that holds fluid (a water bucket sets water, a filled Fluid Tank item or another mod's container sets its fluid), by shift clicking such an item in the inventory, by dragging a fluid or a filled container from JEI, or by picking a fluid in the search popup. Right click clears a slot. The player keeps the item.
- An empty fluid filter passes every fluid. A side with entries only drains and only fills the listed fluids, also from a block with several tanks.
- Each side of the fluid line gets a priority from -9 to 9, independent of the item priority. Sinks of the highest priority are filled first, sharing fairly among themselves, and lower priorities only get what they cannot take.
- The Fluid Pipe page shows the filter slots and the priority like the Item Pipe page.
- The Data Potato's fluid side report names the priority and the filter.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: new fluid filter and fluid priority requirements; the fluid line behaviour honours them; the Duct GUI requirement shows them on the Fluid Pipe page.
- `foundation/data-potato`: the duct inspect report includes the fluid priority and filter.

## Impact

- `foundation/.../fluid/FluidRouting` (priority tiers, pure), `FluidRoutingTest`.
- `foundation/.../logistics/duct/UtilityDuctBlockEntity` (fluid filters and fluid priorities per side, save, sync, dismantle), new pure `FluidFilter` over fluid ids.
- `foundation/.../logistics/network/FluidChannelNetwork` (filtered offers per source, filtered sinks, priorities).
- `foundation/.../logistics/menu/UtilityDuctMenu` (fluid filter slots as fake slots, payload, priority buttons per line), new `SetDuctFluidFilterPayload`.
- `foundation/.../logistics/client/UtilityDuctScreen` (Fluid Pipe page filter and priority, fluid picker), `client/ItemPicker` or a fluid picker variant, `logistics/client/FilterPickerEntries`.
- `foundation/.../integration/jei/DuctFilterGhostHandler` (fluid ingredients and filled containers).
- `duct/DuctReport`, lang in `logistics/datagen/LogisticsDataParts`.

## Non-goals

- Keep amounts and fill up to (`m5-duct-keep-amounts`).
- Blacklist mode for filters, on items or fluids.
- Matching fluid components (a filter entry matches the fluid type, whatever its components).
- Filters or priorities on the power line.

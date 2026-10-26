# Design

## Context

Builds on `m5-duct-endpoint-gui` (side tabs, mutable menu side, page layout, In, Out and Pull flags, redstone setting and On/Off). Apply after it.

`FluidRouting` (pure) routes per fluid key: each source offers one key (`Offer`), each sink takes the first offered key it accepts, short supply of a key is water filled over its sinks (`WaterFilling.allocate`), and what a sink could not take as its share goes to the sinks in order afterwards. `FluidRoutingTest` covers it.

`FluidChannelNetwork.tickChannel` builds the sources from `handler.drain(Integer.MAX_VALUE, true)`, so a block with several tanks always offers whatever its handler drains first. `fill` (fluid pushed into a duct through `DuctFluidHandler`) builds sinks on the channel and routes one pushed source.

Item filters are nine `GhostSlot`s per side backed by `ItemSlots`, set through slot clicks, `SetDuctFilterPayload(slot, Optional<Identifier>)`, the `ItemPicker` popup with `FilterPickerEntries`, and `DuctFilterGhostHandler` for JEI. The item priority is `priorities[side]`, one per side, used only by the item line.

`Transfer` already reads an item's fluid with `ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM)`. `client/FluidSprites` draws a fluid's still sprite in its tint.

## Goals / Non-Goals

**Goals:**
- Fluid filter and priority with the same feel as the item ones.
- Multi-tank blocks give only the filtered fluid.
- Routing tiers stay pure and tested.

**Non-Goals:**
- Keep amounts (next change), blacklists, component matching.

## Decisions

### D1 Fluid filter is data on the block entity, not menu slots
Fluid entries are not items, so they are not `Slot`s. `UtilityDuctBlockEntity` gets `fluidFilters[side]`, a pure `FluidFilter` of nine nullable fluid ids (`Identifier`), with `allows(id)` (empty filter passes all), `set`, `clear`, `firstFree`, `entries()`. Saved under `"FluidFilters"` (side name to list of ids), synced in the update tag, kept on dismantling like the item filter. An id that no longer resolves loads as an empty slot.

The screen draws the nine fluid slots at the item filter position on the Fluid Pipe page from the client copy of the block entity, which the update tag refreshes.

### D2 Setting slots through one payload
New `SetDuctFluidFilterPayload(slot, Optional<Identifier> fluid)`, handled like `SetDuctFilterPayload` through the open `UtilityDuctMenu` (checks `stillValid`, the Fluid Pipe, the slot range and that the id is a registered fluid other than empty). The client resolves every way of setting into a fluid id:
- click with a carried item: the item's fluid through `ItemAccess.forStack(carried).getCapability(Capabilities.Fluid.ITEM)`, first non-empty resource; nothing sent when it holds none;
- shift right click: empty id (right click opens the amount popup, see m5-duct-keep-amounts);
- shift click of an inventory slot on the Fluid Pipe page: the screen intercepts `QUICK_MOVE` there, resolves the fluid and sends it for `firstFree`; on other pages shift click keeps setting item filters;
- click with an empty cursor: opens the picker;
- JEI: see D4.

The carried item is never sent or changed, so a bucket stays full.

### D3 Fluid picker
`ItemPicker` gets a fluid entry kind: an entry carries either an item id or a fluid id, and the fluid kind draws the still sprite through `FluidSprites` with the fluid's tint. `FilterPickerEntries` gains `fluids()`: every registered fluid whose default state is a source, minus `Fluids.EMPTY`, named by `FluidType.getDescription()`, sorted by name, with "none" first. Flowing variants are left out so water shows once.

### D4 JEI
`DuctFilterGhostHandler` offers the fluid slot targets while the Fluid Pipe page shows: for `NeoForgeTypes.FLUID_STACK` (checked in `jei-26.1.2-neoforge-api-29.43.0.107.jar`: `IIngredientTypeWithSubtypes<Fluid, FluidStack> FLUID_STACK`) through `ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)`, and for an item stack that holds fluid through the same item capability as D2. The item filter targets stay for item ingredients on the Item Pipe page.

### D5 Filtered sources and sinks
In `FluidChannelNetwork`:
- A source with an empty filter offers `drain(MAX, true)` as now. A source with entries offers the first entry the handler can drain: `drain(new FluidStack(fluid, MAX), true)` per entry in slot order. So a Calciner-like block with Sulfur and Mercury tanks gives Mercury when only Mercury is listed.
- A sink's `fill` returns 0 for a key whose fluid its filter does not allow; the undecided sides with In and Out use the same check in `accepts`.
- `fill` (pushed in) refuses a fluid the pushing side's filter rejects, and skips sinks whose filter rejects it.

### D6 Fluid priority as routing tiers
`UtilityDuctBlockEntity` gets `fluidPriorities[side]` (-9 to 9, default 0, saved under `"FluidPriorities"`), separate from the item `priorities`. `FluidRouting.Sink` gets `default int priority() { return 0; }`. `routeSupply` groups the key's sinks by priority, highest first, and runs the existing water filling and leftover pass per group with what is left of the supply. With every priority at 0 the result equals today's.

The menu's priority buttons become per line: `BUTTON_PRIORITY_DOWN`/`UP` act on the shown page's line, so the screen sends the page with them (`BUTTON_PRIORITY_DOWN + 2 * line.ordinal()` style ids, or two new ids for the fluid line). A second data slot carries the fluid priority.

### D7 Potato
`DuctReport` adds priority and filter count to the fluid side line and the filter count to the item side line.

## Settled PLAN.md decisions

None open in PLAN.md. Settled here: a fluid filter entry matches the fluid type only; fluid priority is separate from item priority; flowing fluids are not listed in the picker.

## APIs

- `ItemAccess.forStack(ItemStack).getCapability(Capabilities.Fluid.ITEM)` returning `ResourceHandler<FluidResource>`, as used in `transfer/Transfer`.
- `NeoForgeTypes.FLUID_STACK` and `ITypedIngredient.getIngredient(IIngredientType)` from the JEI 29.43 NeoForge API.
- `BuiltInRegistries.FLUID`, `Fluid.isSource(FluidState)`, `Fluid.getFluidType().getDescription()`.

## Pure vs Minecraft

- Pure, JUnit: `FluidFilter` (empty passes, set, clear, first free, unknown entries), `FluidRouting` tiers in `FluidRoutingTest` (two tiers, fair share inside a tier, leftover to the lower tier, all zero equals the old result).
- Minecraft, GameTest: lava only to a filtered sink, multi-fluid source with a filter, priority 5 tank filled before priority 0, pushed water refused by a lava-only side, filters and priorities survive reload.
- Manual: slot drawing, bucket click, shift click of a tank item, picker, JEI drag.

## Risks / Trade-offs

- [Filled containers report several fluids] -> the first non-empty resource wins; documented in the tooltip only if play tests show confusion.
- [Shift click behaviour depends on the client page] -> the client decides and sends a fluid id; the server never guesses the page.
- [Fluids of other mods without a source state] -> rare; they can still be set with a bucket or JEI.

## Decisions made while applying

- `FluidFilter` holds fluid ids as strings, not `Identifier`, so it stays free of Minecraft classes; the block entity converts.
- A duct drops only its line items when it is broken, and the item filter is not kept either, so "kept on dismantling" means nothing to do for fluid filters.
- The several tanks GameTest drives `FluidChannelNetwork.offerOf` with an in-memory two tank `FluidStacksResourceHandler`, because Foundation has no block with two fluid tanks to drain.
- The fluid filter and priority GameTests live in a new `logistics/gametest/DuctFluidFilterGameTests`.
- The fluid priority buttons got their own ids, `BUTTON_FLUID_PRIORITY_DOWN` (32) and `BUTTON_FLUID_PRIORITY_UP` (33). The Fluid Pipe page reuses the "Priority %s" label of the Item Pipe page, because "Fluid priority" does not fit between the buttons.
- The potato reports the item and the fluid side line with one key, `message.servus_foundation.potato.filtered_side` (it replaces `item_side`), which adds the number of filter entries after the priority. The power line keeps `side`.
- Shift clicking an inventory item on the Fluid Pipe page sends nothing when the item holds no fluid or every fluid slot is set; it never falls through to the item filter.
- `ItemPicker.Entry` gained a fluid component (with the old three argument constructor kept), and fluid entries draw the still texture in the fluid's colour through the new `FluidSprites.drawIcon`.
- After the endpoint GUI rework (`m5-duct-endpoint-gui` design D4a and D6) the Fluid Pipe page shows In, Out, the redstone button and On/Off without Pull, the fluid filter row at y 73 and the fluid priority at y 93, and the trash can in the bottom right corner. The Duct GUI and Duct inspect copies in this change's delta specs follow it.

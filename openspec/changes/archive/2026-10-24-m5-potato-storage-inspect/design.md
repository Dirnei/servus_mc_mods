# Design

## Context

`DataPotatoItem.useOn` checks the clicked block entity against `UtilityDuctBlockEntity` and `FluidTankBlockEntity` and builds `DuctReport` or `TankReport`. Foundation cannot know Shuttles classes, so this switch cannot grow to racks and gantries. Blocks already hand clicks to Foundation tools through block interfaces: `Wrenchable` lets a block decide what the Configuration Carrot does on it.

A gantry is five blocks; only the `LEFT_LOW` master has a block entity, found with `FluidGantryBlock.master(level, pos)`. A rack section is found with `HeavyRackBlock.section(level, pos)`, a list of bay positions from left to right; each bay's `HeavyRackBlockEntity.items()` is a `LockedSlots` whose `states()` gives its slots with their job locks as `SlotFill.Slot`, and `zone()` gives its zone name. `SlotFill.inUse` counts a slot as in use when it holds items or a job locked it; `m8-storage-fill` uses it for the controller and the Storage tab. `TopCounts.pick` already picks the largest keys of a count map, ties in insertion order, for the rack front sample.

## Goals / Non-Goals

**Goals:**
- One block interface in Foundation that any Servus block implements to answer the potato.
- Rack and gantry reports with plain chat lines, built on the server only.

**Non-Goals:**
- Warehouse wide or per zone totals (`m8-storage-fill`).
- Listing the locks themselves; a locked slot only counts as used.

## Decisions

### A block interface, not a block entity interface
`PotatoInspectable` lives in `foundation/item` next to `Wrenchable`: `List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit)`, called on the server only. It sits on the block because a gantry's legs and beam have no block entity; the block resolves its master itself. An empty list means nothing to report and the potato passes. `UtilityDuctBlock` (uses `hit` for the arm) and `FluidTankBlock` implement it, so `DataPotatoItem` no longer names any concrete block.

Alternative: a NeoForge block capability (`BlockCapability.createVoid`). It would also work across mods, but a capability fits services that other mods query; the potato is our own tool and the interface matches how `Wrenchable` already works.

### Blocks let the potato through
`Block.useItemOn` runs before `Item.useOn`. Returning `InteractionResult.PASS` skips `useWithoutItem` and lets the item act, which is how `UtilityDuctBlock` already lets the potato and the carrot through (vanilla `ServerPlayerGameMode.useItemOn`, NeoForge 26.1.2 sources). `HeavyRackBlock.useItemOn` and `FluidGantryBlock.useItemOn` add `DataPotatoItem` to their pass checks; without that they return `TRY_WITH_EMPTY_HAND` and open their GUI.

### Section fill is pure
`rack/SectionFill` (pure Java, generic key) takes the `SlotFill.Slot` states of a section's bays in order and gives the slots in use via `SlotFill.inUse` (so the potato and the warehouse fill count alike), total slots, full, and the top items with totals via `TopCounts.pick`. `rack/RackReport` turns a section into chat lines: zone line (distinct zone names of the bays in bay order, or "no zone"), bays and slots line with the full mark, then one line per top item, or the empty line. The number of items listed is `potato_top_items` in `rack.json` (example 5), read through `ShuttlesStatsManager` like `display_count`.

### Gantry report reuses the tank wording
`gantry/GantryReport` prints the mode line, then per tank the side name, `FluidTankBlock.describe(content, capacity)` from Foundation for the fluid part, and the lock (fluid name or Any). Reusing `describe` keeps the tank and gantry wording the same.

### Lang
New keys under `message.servus_shuttles.potato.` in the Shuttles language provider; the potato tooltip in `ServusFoundationLanguageProvider` changes to name storage.

## Pure and Minecraft parts

- Pure, JUnit: `SectionFill` (totals over several slots, largest first, ties in slot order, the limit, used and full counts, a locked empty slot counting as used).
- Minecraft, GameTests: rack report on a section of three with a zone and a locked empty slot, full single bay, empty unzoned bay, no GUI opens; gantry report from a click on the beam and on a leg, nothing changes; existing duct and tank potato GameTests keep passing after the move to the interface.

## Risks / Trade-offs

- [A rack bay of the section is unloaded at a chunk edge] → `HeavyRackBlock.section` walks loaded blocks only; the report counts the bays it finds, like the section GUI.
- [Item names in chat] → use `ItemStack.getHoverName()` of a one item stack, so names are translated on the client.

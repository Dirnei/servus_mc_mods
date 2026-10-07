# Proposal

## Why

PLAN.md promises "a fluid line later" for the Utility Duct, and the user asked for a water pipe. With the Fluid Tank in place (`m5-fluid-tank`), fluids need a way to travel between tanks, pumps and other mods' machines.

Milestone: adds the item "Fluid Pipe line" to M5 (Logistics first release). PLAN.md is updated in the same change. Second of three fluid changes: `m5-fluid-tank`, `m5-fluid-pipe`, `m3-pumps`.

## What Changes

- **Fluid Pipe**: a third Utility Duct line next to the Item Pipe and the Power Cable, placed as its own item. It moves any fluid between blocks that expose `IFluidHandler`, with the same per side modes and eight channels per side as the other lines.
- No rate limit on the pipe: per tick it moves what the sources give and the sinks take, shared fairly among the sinks.
- On an in and out side, a block that accepts the channel's fluid is filled and a block that only gives fluid is drained, so pump to pipe to tank works with default settings and tanks never trade fluid back and forth. Pull in empties a tank into the pipe.
- The duct accepts fluid that other mods push into it and passes it straight on.
- The endpoint GUI gets a Fluid Pipe tab with mode, channels and remove (no filters or priorities).
- The duct grows to a third size for three lines and shows the fluid line's own color as a band; every combination of lines gets its parts.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `logistics/utility-duct`: the Fluid Pipe line, its behaviour, fluid pushed into a duct, crafting, GUI tab and channels.
- `logistics/duct-appearance`: size, colors and rings for three lines.

## Non-goals

- Fluid filters and priorities per side, a duct internal buffer, fluid visible inside the duct.
- Pumps (`m3-pumps`).

## Impact

- `lib`: pure `FluidRouting` next to `EnergyDistributor`, sharing its water filling, with JUnit tests.
- `logistics`: `Line.FLUID`, `DuctParts` and `DuctShapes` for seven line sets, part model datagen, `FluidChannelNetwork`, `DuctFluidHandler`, Fluid Pipe item and recipe, endpoint GUI tab, GameTests.
- `PLAN.md`: section 2.2 and M5.
- Built in parallel with `m5-fluid-tank` and `m3-pumps`; GameTests use vanilla cauldrons, the tank check follows the merge.

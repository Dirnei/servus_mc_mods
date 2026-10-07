# Design

## Context

- The Utility Duct (`logistics/duct`) knows two lines through the `Line` enum (`ITEM`, `ENERGY`). Per line state lives in arrays indexed by `Line.VALUES` in `UtilityDuctBlockEntity`, so a third value grows storage, NBT, menu button ids and GUI tabs mostly by itself. Hard coded two line spots: `DuctParts` (masks `ITEM`, `ENERGY`, `BOTH`, suffixes), `DuctShapes.radius`, shape and arm counting in `UtilityDuctBlock`, `DuctPartModels` (datagen), `UtilityDuctBlockEntity.networkType`, `ServusLogisticsItems.item(Line)` and the screen's Item Pipe checks.
- `EnergyChannelNetwork` is the template for a buffer less per channel line network: endpoints per duct side with a `BlockCapabilityCache`, rebuilt on `DuctEvent.ConfigChanged`, distributed every tick with the pure `EnergyDistributor`. `DuctItemHandler` is the template for a capability the duct exposes on its sides.
- NeoForge 21.1.255 (sources checked): `Capabilities.FluidHandler.BLOCK`, `IFluidHandler.fill/drain` with `FluidAction.SIMULATE` and `EXECUTE`, `FluidStack.isSameFluidSameComponents`.
- `m5-fluid-tank` is built in parallel in its own worktree. NeoForge registers `CauldronWrapper` as the fluid handler of vanilla cauldrons (`CauldronFluidContent`), so GameTests use water and lava cauldrons as 1,000 mB fixtures; tank based checks run after both are merged.

## Goals / Non-Goals

**Goals:**
- A third line that reuses all per line machinery, and a pure distributor for several fluids on one channel.

**Non-Goals:**
- Changing the item or energy line behaviour.

## Decisions

### D1. `Line.FLUID` and line set masks
`Line` gains `FLUID("fluid")` as ordinal 2. `DuctParts` uses masks `1 << ordinal` for all seven line sets; the part suffix joins the line ids in ordinal order with `_` (`item`, `energy`, `fluid`, `item_energy`, `item_fluid`, `energy_fluid`, `item_energy_fluid`). The current `both` becomes `item_energy`; nothing is released, so no migration. The axis centres (`_x`, `_y`, default along Z) and the uniform `_junction` centre that `m5-duct-center-bands` added for the two line set exist for every set with two or more lines, so `center_both_junction` becomes `center_item_energy_junction` and so on. `DuctShapes.radius` is 2, 3, 4 pixels for one, two, three lines; arm counting iterates `Line.VALUES`. Ring tints `RING_TINT_BASE + line * 6 + side` already cover a third line. Datagen splits each face of a multi line part into equal strips in line order.
Alternative: draw the fluid line as a sleeve around the two line duct. Rejected, it breaks "size tells the line count".

### D2. Pure `FluidRouting` in Lib
`lib/fluid/FluidRouting` works on opaque fluid keys: sources report what they can give (key, amount), sinks report how much of a given key they accept; a sink holding a fluid accepts only that key, an empty sink takes the first key offered to it in source order. Allocation per key reuses the water filling of `EnergyDistributor.allocate`, moved into the public helper `lib/flow/WaterFilling` (energy and fluid live in different packages, so package private cannot be shared; `EnergyDistributor.allocate` delegates to it). What a sink cannot take as its fair share, for example a cauldron that only fills in whole buckets, is offered to the sinks in order afterwards. No rate parameter: the amounts are whatever the handlers report.
Alternative: one fluid per channel per tick. Rejected, the "Mixed fluids" scenario would starve the water tank whenever lava came first.

### D3. `FluidChannelNetwork`, no rate, roles per tick
`logistics/network/FluidChannelNetwork` copies the energy network's endpoint caching and per channel tick. Sources are queried with `drain(Integer.MAX_VALUE, SIMULATE)`, sinks with `fill(stack with Integer.MAX_VALUE amount, SIMULATE)` per offered key; then execute. Endpoint role per tick: pull in is a source, push out a sink, in and out a sink when it accepts one of the channel's offered keys on a simulated fill and otherwise a source when a simulated drain returns fluid. So tank to tank never ping pongs and pump to tank works by default. The user decided against a pipe limit, so `utility_duct.json` gets no fluid key.
`DuctFluidHandler` (like `DuctItemHandler`) is `Capabilities.FluidHandler.BLOCK` on duct sides whose fluid mode allows input; `fill` routes straight to that channel's sinks (excluding the block on its own side), `drain` returns empty, it reports one tank of capacity 0.

### D4. GUI and items
`UtilityDuctMenu` and `UtilityDuctScreen` get a Fluid Pipe page like the Power Cable page (mode, channels, remove). Fluid Pipe item `servus_logistics:fluid_pipe`, recipe iron ingots, glass and a bucket for eight, line color blue (`0x3F76E4`, the vanilla water tint) in `DuctBlockColor`.

### Settled PLAN.md items
- 2.2 "a fluid line later" becomes the Fluid Pipe; M5 gains "Fluid Pipe line".

### Pure Java vs Minecraft
- Pure, JUnit: `FluidRouting`, `DuctParts` masks and suffixes, `DuctShapes.radius`.
- GameTest (cauldron fixtures): water cauldron on pull in to an empty cauldron on default side; a full and an empty water cauldron on default sides move nothing back; channel isolation; the full 1,000 mB moves in one tick; water and lava cauldrons reach only matching sinks; fill into a duct side reaches a cauldron and is refused when full; three line duct drops three items.
- Manual: three line look, bands and rings, GUI tab.

## Risks / Trade-offs

- [Seven line sets multiply the part models] → All generated; the dynamic model picks by mask.
- [No pipe limit makes tanks equalize instantly over long runs] → Wanted by the user; the pumps are the throughput limit.

## Migration Plan

None, nothing is released; the `both` part name becomes `item_energy`.

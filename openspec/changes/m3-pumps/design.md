# Design

## Context

- Machines (`manufacturing/machine`) share `MachineBlockEntity` (item handler per side, `SideConfig` with locked front, `pushOutputs`), `BurnerMachineBlockEntity` (fuel only when `canWork()`), `PoweredMachineBlockEntity` (FE buffer exposed on every face), `MachineStats` from `data/<ns>/machine/<name>.json` with optional fields over code defaults, and the generic `MachineScreen`.
- Lib `SideMode`: Auto allows insert, extract and push; Output and Both allow extract; Push extracts and pushes; Input and Pull never give out.
- Research gating: an assembling recipe with a `tech` field, listed in the tech JSON's `unlocks` (see `tech/electric_extractor.json`).
- NeoForge 21.1.255 (sources checked): `Capabilities.FluidHandler.BLOCK`, `IFluidHandler`, `FluidStack`, `net.neoforged.neoforge.fluids.capability.templates.FluidTank` with a validator predicate; client `IClientFluidTypeExtensions.of(fluid).getStillTexture(stack)` and `getTintColor(stack)`. Vanilla `FluidState.isSource()` and `FluidTags.WATER`.

## Goals / Non-Goals

**Goals:**
- Pumps that are ordinary machines: side config, markers, fuel and energy handling reused.

**Non-Goals:**
- A shared fluid machine base for later fluid consumers; extract it when the second fluid machine arrives.

## Decisions

### D1. Two machines, one helper
`machine/pump/PumpLogic` holds the source check, the water `FluidTank` (validator: water only), the drain only side wrapper and the push. `BurnerPumpBlockEntity extends BurnerMachineBlockEntity` (one fuel slot) and `ElectricPumpBlockEntity extends PoweredMachineBlockEntity` (no item slots) delegate to it. `canWork()` is true when the cached source check passed and the buffer has room for `fluid_per_cycle`; `finishWork()` fills `fluid_per_cycle` mB.
Alternative: a `PumpBlockEntity` base class. Rejected, burner and powered already are two different bases.

### D2. Source check without polling
The check reads the block below and its four horizontal neighbours: below must be a water source (`FluidState.isSource()` and `is(FluidTags.WATER)`), and at least `min_source_neighbours` of the four must be too. It runs when progress is 0 at the start of a cycle and in the pump block's `neighborChanged`, and the result is cached and saved. Five block reads once per cycle, never every tick. Nothing is ever written to the water.

### D3. Sides
The buffer is offered through a drain only `IFluidHandler` (fill returns 0) on sides whose `SideMode.allowsExtract()`; every 10 ticks the pump fills `Capabilities.FluidHandler.BLOCK` neighbours on sides whose `SideMode.pushes()`. Front and bottom are locked Off with `SideConfig.lock`. Fuel insertion keeps the machine rules (Auto and Input sides). The Electric Pump's FE capability stays on every face like the other powered machines.

### D4. Stats
`MachineStats` gains `fluidPerCycle`, `fluidCapacity`, `minSourceNeighbours` (JSON `fluid_per_cycle`, `fluid_capacity`, `min_source_neighbours`), defaulting to 0 for other machines. Code defaults: Burner Pump 20 work ticks, 250 mB per cycle; Electric Pump 20 work ticks, 500 mB per cycle, 20 FE per tick, 8,000 FE buffer; both 4,000 mB buffer and 2 neighbours. The values also go into `burner_pump.json` and `electric_pump.json`.

### D5. GUI
`BurnerPumpMenu` and `ElectricPumpMenu` over `MachineMenu` sync fluid amount and fluid id (registry id index) and the source flag in container data. `MachineScreen` gains an optional fluid gauge and a status line ("Needs an infinite water source below"). The gauge is Lib `client/FluidGauge` (still sprite tiled and tinted, amount and capacity tooltip) so later machines and a tank GUI can reuse it.

### D6. Recipes and research
Burner Pump: shaped, iron ingots, cobblestone, a bucket and a furnace. Electric Pump: assembling recipe from a Burner Pump, 4 iron gears and 4 copper wire with `tech: electric_extractor`, and the recipe id added to that tech's `unlocks`.

### Settled PLAN.md items
- 2.1 gains the pumps; M3 gains "Burner Pump and Electric Pump".

### Pure Java vs Minecraft
- Pure, JUnit: `PumpSource` (count of source flags against the minimum) is small but kept pure; JUnit for 0 to 4 neighbours and missing source below.
- GameTest (the tank is built in parallel, so an empty vanilla cauldron, filled through NeoForge's `CauldronWrapper`, stands in): pump on a 2 by 2 pool fills an adjacent cauldron; lone source burns no fuel; source removed stops the next cycle; Output side does not push; pump refuses a fill; Electric Pump without energy idles; pond blocks unchanged after running.
- Manual: GUI gauge and status line; cross mod check over RCON on `runServerAll`: pump, Fluid Pipe run, tank.

## Risks / Trade-offs

- [A removed neighbour two blocks away is not a neighbour of the pump] → Checked again at every cycle start, so at most one cycle runs on a lost source.
- [Infinite water from a 2 by 2 pool is cheap] → Intended, like Factorio's offshore pump; the pump rate and fuel cost are the balance levers in data.

## Migration Plan

None, nothing is released.

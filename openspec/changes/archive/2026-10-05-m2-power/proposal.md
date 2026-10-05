# Proposal

## Why

Everything beyond the burner tier runs on FE: the Assembler and the Research Lab need power, and the plan ships its own generator and cable so the mod is playable alone. PLAN.md milestone M2, items "Coal Generator (FE)" and "Power Cable (simple)", plus the powered machine base those two machines need.

## What Changes

- Pure Java `EnergyDistributor` in Servus Lib: moves energy from sources to sinks under a per endpoint rate, testable without Minecraft. Logistics energy channels will reuse it.
- **Coal Generator** in Manufacturing: burns furnace fuel four times faster than a furnace, produces 40 FE/t into an internal buffer, offers it to neighbours through the energy capability and pushes it into adjacent consumers.
- **Power Cable**: a thin connectable block. Cables form networks through the Lib network manager; each tick a network moves energy from adjacent sources to adjacent sinks. Cable connections update their look when neighbours change.
- `PoweredMachineBlockEntity`: machine base with an FE buffer that accepts energy and spends it per tick while working. No concrete powered machine yet; the Assembler is next.
- Generator GUI with a fuel slot and an energy bar; the shared machine screen learns to draw an energy bar.
- Machine stats gain `energy_per_tick`, `energy_capacity` and `fuel_divisor`.

## Capabilities

### New Capabilities
- `lib/energy-distribution`: how energy is split between sources and sinks each tick.
- `manufacturing/coal-generator`: fuel, generation and delivery of the generator.
- `manufacturing/power-cable`: how cables connect, form networks and move energy.

### Modified Capabilities
- `manufacturing/machines`: adds the powered machine rule (energy buffer, consumption while working, pause without power).

## Non-goals

Deferred to later changes:
- Assembler and Research Lab (next changes) are the first consumers.
- Tiered cables, per side modes and the Logistics Utility Duct (M5).
- Facades and goggles.
- Energy loss, cable burnout or similar realism.

## Impact

- Lib gets a new pure package `energy`.
- Manufacturing registers its first `NetworkType` with Lib.
- New blocks, block entity types, menu type, models and recipes in Manufacturing.

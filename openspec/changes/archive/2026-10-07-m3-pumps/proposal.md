# Proposal

## Why

With the Fluid Tank and the Fluid Pipe in place, there is no way to produce fluid besides buckets. The user asked for a pump station in a burner and an electric version. Water is the first fluid later tiers (steam, washing, cooling) will need.

Milestone: adds the item "Burner Pump and Electric Pump" to M3 (Manufacturing Tier 1 completion), proposed after M5 at the user's request. PLAN.md is updated in the same change. Third of three fluid changes: `m5-fluid-tank`, `m5-fluid-pipe`, `m3-pumps`.

## What Changes

- **Burner Pump**: burns furnace fuel and pumps water while it stands on an infinite water source (a water source block with at least two water source neighbours, like vanilla's infinite water rule). The water is never used up. Hand crafted.
- **Electric Pump**: the same on FE and faster, made in the assembler, unlocked by the existing `electric_extractor` tech.
- Both fill an internal buffer and push water out through their sides by the usual machine side modes; front and bottom are fixed Off. They never accept fluid.
- Pump GUI with a fluid gauge and a line telling whether the source below is valid.
- `fluid_per_cycle`, `fluid_capacity` and `min_source_neighbours` in the machine JSON (examples: Burner Pump 250 mB per second, Electric Pump 500 mB per second, 4,000 mB buffer, 2 neighbours).

## Capabilities

### New Capabilities
- `manufacturing/pumps`: Burner Pump and Electric Pump: water source rule, fuel and energy, buffer, side output, GUI, crafting and research.

### Modified Capabilities
None.

## Non-goals

- Pumping lava or other fluids, draining finite water.
- Machines that consume fluids.
- Pump upgrades or tiers beyond the two.

## Impact

- `manufacturing`: `machine/pump` package, `MachineStats` gains three optional fields, menus and `MachineScreen` fluid gauge, `burner_pump.json` and `electric_pump.json`, recipes, `electric_extractor` tech unlock, GameTests.
- `lib`: client `FluidGauge` widget.
- `PLAN.md`: section 2.1 and M3.
- Built in parallel with `m5-fluid-tank` and `m5-fluid-pipe`; GameTests use a vanilla cauldron, the cross mod check follows the merge.

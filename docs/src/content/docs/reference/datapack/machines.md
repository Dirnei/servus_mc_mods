---
title: Machine stats
description: The machine stats JSON format of Servus Manufacturing, every field, the code defaults and the timing rules.
sidebar:
  order: 1
---

Every Servus Manufacturing machine reads its balance numbers from a machine stats file. All numbers on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/machine/<machine>.json
```

The file name is the machine id. The built in machines are in the `servus_manufacturing` namespace, so to retune the Burner Smelter a datapack provides `data/servus_manufacturing/machine/burner_smelter.json`. Files are reloaded with `/reload`.

## Fields

Every field is optional. A field the file leaves out, or a missing file, falls back to the machine's code default (see the table below).

| Field | Type | Default | Meaning |
|---|---|---|---|
| `work_ticks` | int | per machine | Ticks per work cycle. Used by extractors, the Burner Smelter and the Assembler for plain crafting recipes. |
| `energy_per_tick` | int | per machine | FE a powered machine consumes per working tick, or FE a generator produces per burning tick. |
| `energy_capacity` | int | per machine | Size of the FE buffer. 0 for burner machines. |
| `fuel_divisor` | int | 1 | Furnace burn time of a fuel item is divided by this. Values below 1 count as 1. |
| `items_per_tick` | int | 0 | Reserved for item throughput. No Manufacturing machine uses it yet. |
| `recipe_slots` | int | 1 | How many recipes a crafter can have configured. Values below 1 count as 1. |
| `fuel_reserve` | int | 0 | How many of its own yield a burner extractor keeps in its fuel slot when the yield is a furnace fuel. Surplus goes to the output slot. 0 disables the reserve. Values below 0 count as 0. |
| `fluid_per_cycle` | int | 0 | mB of water a pump adds to its buffer per work cycle. |
| `fluid_capacity` | int | 0 | Size of a pump's water buffer in mB. |
| `min_source_neighbours` | int | 0 | How many of the four horizontal neighbours of the water source below a pump must be water sources too. |

## Example

The shipped `data/servus_manufacturing/machine/coal_generator.json`:

```json
{
  "work_ticks": 1,
  "energy_per_tick": 40,
  "energy_capacity": 16000,
  "fuel_divisor": 4
}
```

A partial file is fine. This one only changes the smelter's speed and keeps everything else at the defaults:

```json
{
  "work_ticks": 40
}
```

## Machines and code defaults

These are the defaults built into the code, used for any field a file leaves out.

| Machine | Kind | Runs | `work_ticks` | `energy_per_tick` | `energy_capacity` | `fuel_divisor` | `recipe_slots` |
|---|---|---|---|---|---|---|---|
| `burner_extractor` | burner | deposit extraction | 80 | 0 | 0 | 1 | 1 |
| `burner_smelter` | burner | `minecraft:smelting` | 80 | 0 | 0 | 1 | 1 |
| `electric_extractor` | powered | deposit extraction | 40 | 20 | 8000 | 1 | 1 |
| `crusher` | powered | `servus_manufacturing:crushing` | 60 | 20 | 8000 | 1 | 1 |
| `assembler` | crafter | `servus_manufacturing:assembling`, `minecraft:crafting` | 20 | 20 | 8000 | 1 | 2 |
| `coal_generator` | generator | burns fuel | 1 | 40 | 16000 | 4 | 1 |
| `research_lab` | powered | research packs | 1 | 10 | 4000 | 1 | 1 |
| `chunk_loader` | powered | nothing, upkeep only | 1 | 10 | 4000 | 1 | 1 |
| `burner_pump` | burner | water | 20 | 0 | 0 | 1 | 1 |
| `electric_pump` | powered | water | 20 | 20 | 8000 | 1 | 1 |

`items_per_tick` is 0 for every machine. `fuel_reserve` is 4 for the `burner_extractor` and 0 for every other machine: on a coal deposit the extractor keeps four coal in its fuel slot and pushes only the surplus. The pumps add `fluid_per_cycle` 250 (Burner Pump) and 500 (Electric Pump), `fluid_capacity` 4000 and `min_source_neighbours` 2; these are 0 for every other machine.

The shipped stats files repeat these defaults, with one difference: `assembler.json` sets `work_ticks` to 1, so plain crafting recipes take one tick in the Assembler.

## Timing rules

Which number decides how long a cycle takes depends on the machine:

| Machine | Cycle time |
|---|---|
| Burner Extractor, Electric Extractor | `work_ticks` per extracted item |
| Burner Smelter | `work_ticks` per smelted item. The smelting recipe's own `cookingtime` is ignored. |
| Crusher | The crushing recipe's `time`. `work_ticks` is not used. |
| Assembler | The assembling recipe's `time`. For a plain crafting recipe, `work_ticks`. |
| Research Lab | The current tech's `ticks_per_pack` per consumed pack. `work_ticks` is not used. |
| Coal Generator | Produces `energy_per_tick` for as long as one fuel item burns. |
| Burner Pump, Electric Pump | `work_ticks` per `fluid_per_cycle` mB of water. |

Fuel and energy:

- **Burner machines** burn fuel only while they have work. One fuel item lasts its furnace burn time divided by `fuel_divisor`. Coal burns 1600 ticks, so with the default divisor of 1 one coal runs a burner machine for 1600 ticks, 20 cycles at 80 ticks.
- **Generators** use the same rule: with `fuel_divisor` 4, one coal burns 400 ticks (20 seconds) at 40 FE per tick, 16000 FE in total.
- **Powered machines** draw `energy_per_tick` only on ticks they work. With less than that in the buffer, progress pauses and is kept. A cycle costs `energy_per_tick` times the cycle time, for example 20 FE x 100 ticks = 2000 FE for one Red Pack.
- **Chunk Loader** spends `energy_per_tick` every tick while it is active and releases its chunk when the buffer runs dry.

## Notes

- Ticks are game ticks, 20 per second.
- Stats apply to every machine of that type on the server; there is no per block override.
- Try changes in the [Balance lab](/servus_mc_mods/lab/) before shipping them.

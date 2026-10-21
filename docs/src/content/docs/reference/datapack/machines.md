---
title: Machine stats
description: The machine stats JSON format of the Servus machines, every field, the code defaults and the timing rules.
sidebar:
  order: 1
---

Every Servus machine reads its balance numbers from a machine stats file. All numbers on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/machine/<machine>.json
```

The file name is the machine id and the namespace is the mod that owns the machine: the Coal Generator, the pumps, the Ender Anchor and the Electric Motor are `servus_foundation`, the other machines `servus_alchemy`. To retune the Calciner a datapack provides `data/servus_alchemy/machine/calciner.json`. Files are reloaded with `/reload`.

## Fields

Every field is optional. A field the file leaves out, or a missing file, falls back to the machine's code default (see the table below).

| Field | Type | Default | Meaning |
|---|---|---|---|
| `work_ticks` | int | per machine | Ticks per work cycle. Used by the pumps, as the Principle Dropper's drip interval and as the Salamander Forge's time per item; the Calciner and Coagulator take the recipe's time. |
| `energy_per_tick` | int | per machine | FE a powered machine consumes per working tick, or FE a generator produces per burning tick. |
| `energy_capacity` | int | per machine | Size of the FE buffer. 0 for burner machines. |
| `fuel_divisor` | int | 1 | Furnace burn time of a fuel item is divided by this. Values below 1 count as 1. |
| `items_per_tick` | int | 0 | Reserved for item throughput. No Alchemy machine uses it yet. |
| `fluid_per_cycle` | int | 0 | mB of water a pump adds to its buffer per work cycle, or mB of each principle one drip of the Principle Dropper carries. |
| `fluid_capacity` | int | 0 | Size of a pump's water buffer, or of each principle tank of the Calciner, Coagulator, Gnome Hollow and Principle Dropper, in mB. |
| `item_capacity` | int | 0 | Items a machine's own store holds: the Calciner's Ash Pan and the Gnome Hollow's store of dug ore. |
| `min_source_neighbours` | int | 0 | How many of the four horizontal neighbours of the water source below a pump must be water sources too. |
| `upkeep_item` | item id | per machine | The item a machine keeps stocked in its fuel slot from the data line. `minecraft:coal` for the Burner Pump, the Coal Generator and the Calciner, empty for the others. |
| `upkeep_level` | int | per machine | How many of the upkeep item the fuel slot is kept at. 16 for the Burner Pump and the Coal Generator, 0 for the others. 0 switches upkeep off. |
| `restart_below` | number | per machine | Restart threshold of the Coal Generator and the pumps, as a fraction of their buffer. Once the buffer is full the machine stays off until the buffer holds less than this share of its capacity, then runs until it is full again. 0.5 for the Coal Generator and both pumps; 1 restarts as soon as there is room. Values outside 0 to 1 are clamped. Other machines ignore it. |

## Example

The shipped `data/servus_foundation/machine/coal_generator.json`:

```json
{
  "work_ticks": 1,
  "energy_per_tick": 40,
  "energy_capacity": 16000,
  "fuel_divisor": 4,
  "restart_below": 0.5
}
```

A partial file is fine. This one only gives the Calciner bigger tanks and keeps everything else at the defaults:

```json
{
  "fluid_capacity": 8000
}
```

## Machines and code defaults

These are the defaults built into the code, used for any field a file leaves out.

| Machine | Kind | Runs | `work_ticks` | `energy_per_tick` | `energy_capacity` | `fuel_divisor` | `fluid_capacity` |
|---|---|---|---|---|---|---|---|
| `calciner` | burner | `servus_alchemy:calcination` | 1 | 0 | 0 | 2 | 4000 |
| `coagulator` | crafter | `servus_alchemy:coagulation` | 1 | 0 | 0 | 1 | 4000 |
| `athanor` | crafter | `servus_alchemy:athanor` | 1 | 0 | 0 | 1 | 4000 |
| `gnome_hollow` | crafter | vein yield | 1 | 0 | 0 | 1 | 4000 |
| `principle_dropper` | crafter | drips into the Gnome Hollow below | 20 | 0 | 0 | 1 | 1000 |
| `salamander_forge` | crafter | `minecraft:smelting`, without fuel | 200 | 0 | 0 | 1 | 0 |
| `prelum` | crafter | `servus_alchemy:pressing`, without fuel; each recipe's `time` decides | 600 | 0 | 0 | 1 | 0 |
| `servus_foundation:coal_generator` | generator | burns fuel | 1 | 40 | 16000 | 4 | 0 |
| `servus_foundation:ender_anchor` | powered | nothing, upkeep only | 1 | 10 per chunk | 20000 | 1 | 0 |
| `servus_foundation:burner_pump` | burner | water | 20 | 0 | 0 | 1 | 4000 |
| `servus_foundation:electric_pump` | powered | water | 20 | 20 | 8000 | 1 | 4000 |
| `servus_foundation:electric_motor` | powered | drives the block its shaft faces | 1 | 20 | 8000 | 1 | 0 |

`items_per_tick` is 0 for every machine. `item_capacity` is 64 for the Calciner and the Gnome Hollow and 0 for the others. The pumps add `fluid_per_cycle` 250 (Burner Pump) and 500 (Electric Pump) and `min_source_neighbours` 2, and the Principle Dropper `fluid_per_cycle` 25; these are 0 for every other machine. `upkeep_item` is `minecraft:coal` and `upkeep_level` 16 for the Burner Pump, the Coal Generator and the Calciner; every other machine keeps no upkeep. `restart_below` is 0.5 for the Coal Generator and both pumps and 1 for every other machine.

The shipped stats files repeat these defaults.

## Timing rules

Which number decides how long a cycle takes depends on the machine:

| Machine | Cycle time |
|---|---|
| Calciner | The calcination recipe's `time`. `work_ticks` is not used. |
| Coagulator | The coagulation recipe's `time`. `work_ticks` is not used. |
| Research Lab | The current tech's `ticks_per_pack` per consumed pack. `work_ticks` is not used. |
| Coal Generator | Produces `energy_per_tick` for as long as one fuel item burns. |
| Burner Pump, Electric Pump | `work_ticks` per `fluid_per_cycle` mB of water. |
| Electric Motor | One work tick per tick while the driven block wants work; the Mill finishes when the recipe's `work` is collected. |

Fuel and energy:

- **Burner machines** burn fuel only while they have work. A burn counts down only on ticks the machine works: without work the fire is banked, keeps its remaining time and shows as not lit. One fuel item lasts its furnace burn time divided by `fuel_divisor`. Coal burns 1600 ticks, so with the default divisor of 1 one coal runs a burner machine for 1600 ticks, 20 cycles at 80 ticks.
- **Generators** use the same rule: with `fuel_divisor` 4, one coal burns 400 ticks (20 seconds) at 40 FE per tick, 16000 FE in total. A full buffer banks the fire, and the generator waits until its buffer drops below `restart_below`.
- **Powered machines** draw `energy_per_tick` only on ticks they work. With less than that in the buffer, progress pauses and is kept. A cycle costs `energy_per_tick` times the cycle time, for example 20 FE x 80 ticks = 1600 FE to calcine one cobblestone.
- **Ender Anchor** spends `energy_per_tick` for every chunk of its square every tick while it is active (1, 9 or 25 chunks with 0, 1 or 2 Anchor Upgrades) and releases its whole square when the buffer runs dry.

## Drive

The Hand Crank and the Electric Motor read how much work they give from a drive file:

```
data/<namespace>/drive/<driver>.json
```

Every field is optional and falls back to the driver's code default.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `work_per_turn` | int | 25 (Hand Crank) | Work a full turn of the crank gives the driven block. |
| `exhaustion_per_turn` | float | 0.1 (Hand Crank) | Food exhaustion a full turn costs the player. 4 exhaustion is one hunger point or saturation point. |
| `work_per_tick` | int | 5 (Electric Motor) | Work the motor gives per working tick, before upgrades. |

The motor's FE comes from its machine stats above: it spends `energy_per_tick` on every tick it gives work, and nothing while the driven block has nothing to do. Upgrades multiply its work and its FE per tick alike. The Mill has no stats file; each milling recipe's `work` decides (see [Recipes](/servus_mc_mods/reference/datapack/recipes/)).

```json
{
  "work_per_turn": 25,
  "exhaustion_per_turn": 0.1
}
```

```json
{
  "work_per_tick": 5
}
```

## Notes

- Ticks are game ticks, 20 per second.
- Stats apply to every machine of that type on the server; there is no per block override.
- Try changes in the [Balance lab](/servus_mc_mods/lab/) before shipping them.

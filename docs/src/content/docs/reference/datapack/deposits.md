---
title: Deposits
description: The deposit definition JSON format that tells extractors what a deposit block yields.
sidebar:
  order: 2
---

A deposit definition links a block to the item stack an extractor gets from it per cycle. The Burner Extractor and the Electric Extractor read it for the block directly below them. Yields on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/deposit/<name>.json
```

Definitions are reloaded with `/reload`. A datapack can add new files, override a built in one by using the same path (for example `data/servus_manufacturing/deposit/iron.json`) or remove one with a datapack filter.

## Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `block` | block id | required | The deposit block. Any block works, not only the Servus deposit blocks. |
| `yield` | item stack | required | What one extraction produces: `id` (item id) and `count` (default 1). |

## Example

The shipped `data/servus_manufacturing/deposit/iron.json`:

```json
{
  "block": "servus_manufacturing:iron_deposit",
  "yield": {
    "id": "minecraft:raw_iron",
    "count": 1
  }
}
```

## Built in deposits

| File | Block | Yield |
|---|---|---|
| `coal.json` | `servus_manufacturing:coal_deposit` | 1 `minecraft:coal` |
| `copper.json` | `servus_manufacturing:copper_deposit` | 1 `minecraft:raw_copper` |
| `iron.json` | `servus_manufacturing:iron_deposit` | 1 `minecraft:raw_iron` |
| `stone.json` | `servus_manufacturing:stone_deposit` | 1 `minecraft:cobblestone` |

## Notes

- How often an extractor yields is set by its `work_ticks` in the [machine stats](/servus_mc_mods/reference/datapack/machines/), not by the deposit.
- A block without a definition yields nothing, and an extractor standing on it burns no fuel and spends no energy.
- Deposit blocks are unbreakable in survival and never run out.
- Where deposits generate is regular worldgen JSON, not part of this format. Each deposit has a configured feature (a `minecraft:disk` of radius 3 to 5 that replaces soil, stone, sand and gravel) in `worldgen/configured_feature/`, a placed feature (rarity chance 20, on the surface) in `worldgen/placed_feature/`, and a NeoForge biome modifier in `neoforge/biome_modifier/` that adds it to every overworld biome.

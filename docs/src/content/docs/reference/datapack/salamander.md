---
title: Salamander
description: How the Servus Alchemy salamander takes to gold and where it spawns.
sidebar:
  order: 9
---

A salamander takes Gold Ingots: a wild one may be befriended by them, a befriended one digests them into Sal Ammoniac. Numbers on this page are current values; they live in datapack JSON and modpacks can change them.

```
data/<namespace>/servus_alchemy/salamander_temper/<name>.json
```

The file id names the entity type: `data/servus_alchemy/servus_alchemy/salamander_temper/salamander.json` is the temper of `servus_alchemy:salamander`. Without a file the shipped numbers below are used.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `befriend_chance` | number, 0 to 1 | required | Chance that one Gold Ingot befriends a wild salamander. The ingot is used up either way. |
| `digest_ticks` | int, at least 1 | required | Ticks from feeding a Gold Ingot to the drop of one Sal Ammoniac. |
| `spawn` | object | see below | Where and how many wild salamanders spawn. |

The `spawn` object and each of its fields may be left out; the default is the shipped value.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `home_radius` | int, 0 to 4 | 4 | A ground spawn needs a block of the tag `servus_alchemy:salamander_home` within this many blocks on each axis (a box of at most 9 by 9 by 9). |
| `crowd_radius` | int, at least 0 | 32 | Wild salamanders within this many blocks of the spot count against `crowd_max`. |
| `crowd_max` | int, at least 0 | 3 | No wild salamander spawns where this many wild ones already live within `crowd_radius`. Befriended ones never count; 0 stops all spawns. |
| `biome_chance` | map of biome id to number, 0 to 1 | `{"minecraft:warped_forest": 0.5, "minecraft:nether_wastes": 0.2, "minecraft:basalt_deltas": 0.2}` | Chance that a spawn try in that biome goes on. A biome without an entry always goes on. |

Shipped temper:

```json
{
  "befriend_chance": 0.33,
  "digest_ticks": 1200,
  "spawn": {
    "home_radius": 4,
    "crowd_radius": 32,
    "crowd_max": 3,
    "biome_chance": { "minecraft:warped_forest": 0.5, "minecraft:nether_wastes": 0.2, "minecraft:basalt_deltas": 0.2 }
  }
}
```

| Entity | Befriend chance | Digest time |
|---|---|---|
| `salamander` | 33 % | 1,200 ticks (60 s) |

## Spawning

Salamanders spawn in the ambient group, like bats, in two places:

- **On the ground** near a block of the block tag `servus_alchemy:salamander_home` (within `home_radius`). The tag holds `#minecraft:crimson_stems`, `#minecraft:warped_stems`, `minecraft:nether_wart_block`, `minecraft:warped_wart_block` and `minecraft:nether_wart`.
- **On lava** whose column is topped by air, and only in a biome of the biome tag `servus_alchemy:salamander_lava` (the Nether Wastes and the Basalt Deltas).

Both then need the biome's `biome_chance` and room in the crowd. The biomes, weights and group sizes come from two NeoForge biome modifiers in `data/servus_alchemy/neoforge/biome_modifier/`. `salamander_forest.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": ["minecraft:crimson_forest", "minecraft:warped_forest"],
  "spawners": [
    {"type": "servus_alchemy:salamander", "weight": 40, "minCount": 1, "maxCount": 2}
  ]
}
```

`salamander_lava.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": ["minecraft:nether_wastes", "minecraft:basalt_deltas"],
  "spawners": [
    {"type": "servus_alchemy:salamander", "weight": 5, "minCount": 1, "maxCount": 1}
  ]
}
```

Striders are creatures and never despawn, so in the Nether they fill the creature cap; in the ambient group salamanders have a cap of their own, and spawning is tried every tick. No other ambient mob spawns in these biomes, so the weights only matter against spawns another pack adds. How often salamanders show comes from `biome_chance` (fewer in the Warped Forest, rare on lava) and the crowd limit. A pack can override these files or the tags, or remove the spawns with `neoforge:remove_spawns`.

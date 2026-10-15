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
| `biome_chance` | map of biome id to number, 0 to 1 | `{"minecraft:warped_forest": 0.5}` | Chance that a spawn try in that biome goes on. A biome without an entry always goes on. |

Shipped temper:

```json
{
  "befriend_chance": 0.33,
  "digest_ticks": 1200,
  "spawn": {
    "home_radius": 4,
    "crowd_radius": 32,
    "crowd_max": 3,
    "biome_chance": { "minecraft:warped_forest": 0.5 }
  }
}
```

| Entity | Befriend chance | Digest time |
|---|---|---|
| `salamander` | 33 % | 1,200 ticks (60 s) |

## Spawning

Salamanders spawn as creatures in two places:

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

In the Crimson Forest most of the Strider's weight 60 picks fail on nylium, so weight 40 makes salamanders about as common as Striders on a lava lake. The Warped Forest has no other creature, so there every creature pick is a salamander; it has fewer because of its `biome_chance` of 0.5, not because of the weight. On lava, weight 5 against the Strider's 60 keeps them rare. A pack can override these files or the tags, or remove the spawns with `neoforge:remove_spawns`.

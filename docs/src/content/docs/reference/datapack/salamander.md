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

Shipped temper:

| Entity | Befriend chance | Digest time |
|---|---|---|
| `salamander` | 33 % | 1,200 ticks (60 s) |

## Spawning

Salamanders spawn as creatures on lava whose column is topped by air. The biomes, weight and group size come from a NeoForge biome modifier, `data/servus_alchemy/neoforge/biome_modifier/salamander.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": ["minecraft:nether_wastes", "minecraft:basalt_deltas"],
  "spawners": [
    {"type": "servus_alchemy:salamander", "weight": 5, "minCount": 1, "maxCount": 1}
  ]
}
```

The Strider has weight 60 in the same biomes, so salamanders are rare. A pack can override this file or remove the spawn with `neoforge:remove_spawns`.

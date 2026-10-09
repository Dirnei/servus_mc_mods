---
title: Recipes
description: The calcination and coagulation recipe formats of Servus Manufacturing.
sidebar:
  order: 4
---

Servus Manufacturing adds two recipe types: `servus_manufacturing:calcination` for the Calciner and `servus_manufacturing:coagulation` for the Coagulator. Times and amounts on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/recipe/<path>.json
```

These are ordinary Minecraft recipes, so they live in the normal `recipe` folder and are reloaded with `/reload`. The built in ones use the subfolders `calcination/` and `coagulation/`, which gives ids like `servus_manufacturing:coagulation/salt_crystal`. Overriding a built in recipe works by using the same path.

## Principles

Both recipe types write principle amounts the same way: an object with `salt`, `sulfur` and `mercury` in mB. A missing key is 0.

```json
{"salt": 100, "mercury": 10}
```

## Calcination

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_manufacturing:calcination` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `output` | principles | required | What one cycle puts into the Calciner's tanks. |
| `time` | int | 80 | Ticks per cycle. |

An item without a calcination recipe is refused by the Calciner.

Built in calcination recipes (80 ticks each):

| Recipe | Input | Salt | Sulfur | Mercury |
|---|---|---|---|---|
| `cobblestone` | `#c:cobblestones` | 100 | 0 | 10 |
| `stone` | `#c:stones` | 100 | 0 | 10 |
| `dirt` | `#minecraft:dirt` | 50 | 20 | 20 |
| `sand` | `#c:sands` | 80 | 0 | 10 |
| `gravel` | `#c:gravels` | 80 | 0 | 10 |
| `logs` | `#minecraft:logs` | 20 | 100 | 10 |
| `planks` | `#minecraft:planks` | 5 | 25 | 0 |
| `leaves` | `#minecraft:leaves` | 0 | 40 | 20 |
| `saplings` | `#minecraft:saplings` | 0 | 40 | 20 |
| `crops` | `#c:crops` | 10 | 30 | 30 |
| `seeds` | `#c:seeds` | 10 | 30 | 30 |
| `coal` | `#minecraft:coals` | 0 | 200 | 0 |
| `raw_materials` | `#c:raw_materials` | 150 | 0 | 50 |
| `redstone` | `#c:dusts/redstone` | 0 | 0 | 100 |
| `bones` | `#c:bones` | 100 | 0 | 0 |

## Coagulation

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_manufacturing:coagulation` |
| `input` | principles | required | What one cycle takes from the Coagulator's tanks. |
| `ingredient` | sized ingredient | none | An item the cycle takes as well. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | 40 | Ticks per cycle. |
| `chapter` | chapter id | none | A [journal chapter](/servus_mc_mods/reference/datapack/journal/) the team must have finished before a Coagulator may select this recipe. |

The Coagulator runs only the recipe picked in its GUI.

Built in coagulation recipes (40 ticks each, no item ingredient):

| Recipe | Salt | Sulfur | Mercury | Result |
|---|---|---|---|---|
| `salt_crystal` | 250 | 0 | 0 | 1 Salt Crystal |
| `sulfur` | 0 | 250 | 0 | 1 Sulfur |
| `quicksilver` | 0 | 0 | 250 | 1 Quicksilver |
| `clay_ball` | 100 | 0 | 50 | 1 clay ball |
| `flint` | 150 | 0 | 0 | 1 flint |
| `gunpowder` | 50 | 150 | 0 | 1 gunpowder |
| `redstone` | 50 | 0 | 150 | 1 redstone |
| `glowstone_dust` | 0 | 100 | 100 | 1 glowstone dust |
| `bone_meal` | 60 | 20 | 0 | 1 bone meal |
| `slime_ball` | 0 | 50 | 150 | 1 slime ball |

---
title: Recipes
description: The assembling and crushing recipe formats of Servus Manufacturing, and how the Burner Smelter uses vanilla smelting.
sidebar:
  order: 4
---

Servus Manufacturing adds two recipe types: `servus_manufacturing:assembling` for the Assembler and `servus_manufacturing:crushing` for the Crusher. Times and amounts on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/recipe/<path>.json
```

These are ordinary Minecraft recipes, so they live in the normal `recipe` folder and are reloaded with `/reload`. The built in ones use the subfolders `assembling/` and `crushing/`, which gives ids like `servus_manufacturing:assembling/red_pack`. Overriding a built in recipe works by using the same path.

## Assembling

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_manufacturing:assembling` |
| `ingredients` | list of sized ingredients | required | Each entry is an `item` or a `tag` with a `count` (default 1). |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | 20 | Ticks per craft. |
| `tech` | tech id | none | When set, a team must have completed this [tech](/servus_mc_mods/reference/datapack/techs/) before the recipe can be added to an Assembler. |

Example, the shipped `data/servus_manufacturing/recipe/assembling/crusher.json`:

```json
{
  "type": "servus_manufacturing:assembling",
  "ingredients": [
    { "count": 1, "item": "servus_manufacturing:electric_extractor" },
    { "count": 4, "item": "servus_manufacturing:iron_gear" },
    { "count": 6, "item": "servus_manufacturing:copper_wire" }
  ],
  "result": {
    "count": 1,
    "id": "servus_manufacturing:crusher"
  },
  "tech": "servus_manufacturing:crusher",
  "time": 200
}
```

Built in assembling recipes:

| Recipe | Inputs | Result | Time | Tech |
|---|---|---|---|---|
| `iron_gear` | 2 iron ingot | 1 Iron Gear | 20 (default) | none |
| `copper_wire` | 1 copper ingot | 2 Copper Wire | 10 | none |
| `red_pack` | 1 Iron Gear, 2 Copper Wire | 1 Red Pack | 100 | none |
| `electric_extractor` | 1 Burner Extractor, 4 Iron Gear, 6 Copper Wire | 1 Electric Extractor | 200 | `electric_extractor` |
| `crusher` | 1 Electric Extractor, 4 Iron Gear, 6 Copper Wire | 1 Crusher | 200 | `crusher` |
| `stone` | 2 cobblestone | 2 stone | 20 (default) | `stone_processing` |
| `stone_bricks` | 2 stone | 2 stone bricks | 20 (default) | `stone_processing` |
| `glass` | 1 sand | 1 glass | 40 | `stone_processing` |

The Assembler also runs any `minecraft:crafting` recipe, from any mod. Those take the Assembler's `work_ticks` from the [machine stats](/servus_mc_mods/reference/datapack/machines/) instead of a recipe time, and are never locked by research.

## Crushing

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_manufacturing:crushing` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | 60 | Ticks per crush. |

Example, the shipped `data/servus_manufacturing/recipe/crushing/raw_iron.json`:

```json
{
  "type": "servus_manufacturing:crushing",
  "input": {
    "count": 1,
    "item": "minecraft:raw_iron"
  },
  "result": {
    "count": 2,
    "id": "servus_manufacturing:iron_grit"
  }
}
```

Built in crushing recipes:

| Recipe | Input | Result | Time |
|---|---|---|---|
| `raw_iron` | 1 raw iron | 2 Iron Grit | 60 (default) |
| `raw_copper` | 1 raw copper | 2 Copper Grit | 60 (default) |
| `cobblestone` | 1 cobblestone | 1 gravel | 40 |
| `gravel` | 1 gravel | 1 sand | 40 |

Iron Grit and Copper Grit have vanilla smelting and blasting recipes that give one ingot each.

## Burner Smelter

The Burner Smelter has no recipe type of its own. It runs every `minecraft:smelting` recipe, from vanilla and from other mods, and finishes one item per its own `work_ticks` (80 by default). The recipe's `cookingtime` and `experience` are ignored. To make the smelter accept a new item, add a normal smelting recipe.

## Notes

- Recipe times are game ticks, 20 per second. A powered machine spends its `energy_per_tick` on every tick of the recipe time.
- With JEI installed, assembling and crushing recipes appear in their own categories with times in seconds.
- Use the [Balance lab](/servus_mc_mods/lab/) to see how a changed recipe time moves the ratios of a production chain.

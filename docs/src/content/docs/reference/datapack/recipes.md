---
title: Recipes
description: The calcination, purification, coagulation and pressing recipe formats of Servus Alchemy and the milling recipe format of Servus Foundation.
sidebar:
  order: 4
---

Servus Alchemy 0.3.0-alpha adds `servus_alchemy:calcination` for the Calciner, `servus_alchemy:purification` for the Purifier, `servus_alchemy:coagulation` for the Coagulator and `servus_alchemy:pressing` for the Prelum. The Athanor's `servus_alchemy:athanor` recipes are on the [Athanor](/servus_mc_mods/reference/datapack/athanor/) page. Servus Foundation 0.4.0 adds `servus_foundation:milling` for the Mill. Times and amounts on this page are the shipped values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/recipe/<path>.json
```

These are ordinary Minecraft recipes, so they live in the normal `recipe` folder and are reloaded with `/reload`. The built in ones use subfolders named after their type, such as `calcination/` and `coagulation/`, which gives ids like `servus_alchemy:coagulation/salt_crystal`. Overriding a built in recipe works by using the same path.

## Principles

Recipes write principle amounts the same way: an object with `salt`, `sulfur` and `mercury` in mB. A missing key is 0.

```json
{"salt": 100, "mercury": 10}
```

## Calcination

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:calcination` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `output` | principles | required | What one cycle makes. Sulfur and Mercury go into the Calciner's Fluid Tanks; Salt collects in the Ash Pan as Ash. |
| `time` | int | 80 | Ticks per cycle. |

An item without a calcination recipe is refused by the Calciner. The Ash Pan holds Salt in mB and gives it out as Ash: one Ash for the Salt that one Ash makes in the first purification recipe that takes Ash (100 mB with the shipped recipe, and 100 mB when no recipe takes Ash).

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

## Purification

The Purifier washes an item with water into Salt and may leave a residue. It needs no power and no fuel.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:purification` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `water` | int | required | mB of water one cycle takes. |
| `salt` | int | required | mB of Salt one cycle makes. |
| `residue` | item stack | none | `id` and `count` (default 1) of what is left in the Residue Crock. |
| `time` | int | 40 | Ticks per cycle. |

Built in purification recipe:

| Recipe | Input | Water | Salt | Residue | Time |
|---|---|---|---|---|---|
| `ash` | 4 `servus_alchemy:ash` | 400 | 400 | 1 Caput Mortuum | 160 |

## Coagulation

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:coagulation` |
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

## Pressing

The Prelum runs `servus_alchemy:pressing` recipes. It needs no fuel and no principles.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:pressing` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). The Prelum presses once its input slot holds the count. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | required | Ticks per pressing. |

An item without a pressing recipe is refused by the Prelum.

Built in pressing recipe:

| Recipe | Input | Result | Time |
|---|---|---|---|
| `netherite_scrap` | 16 `servus_alchemy:caput_mortuum` | 1 Netherite Scrap | 600 |

## Milling

The Mill runs `servus_foundation:milling` recipes. It has no power of its own: a Hand Crank or an Electric Motor gives it work, and a milling is done once it has collected the recipe's `work`.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_foundation:milling` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). The Mill grinds once its input slot holds the count. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `work` | int | required | Work units one milling takes. A Hand Crank gives 25 per turn, an Electric Motor 5 per tick (see [Machine stats](/servus_mc_mods/reference/datapack/machines/)). |

An item without a milling recipe is refused by the Mill. Any mod or datapack can add milling recipes under `data/<namespace>/recipe/`.

Built in milling recipe:

| Recipe | Input | Result | Work |
|---|---|---|---|
| `servus_foundation:milling/sand` | Gravel | 1 Sand | 200 |

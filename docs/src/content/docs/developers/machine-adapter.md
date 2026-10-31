---
title: Machine Adapter
description: Put a machine of vanilla or another mod on the Servus data line with a Machine Adapter and a datapack machine profile.
---

Some machines come from mods that do nothing for the data line. The **Machine Adapter** of Servus: Foundation %foundation_version% makes them producers anyway. A player places the adapter against the machine, and a datapack **machine profile** tells the adapter how the machine works. No code is needed.

## In game

The adapter faces the block it was placed against. When a profile knows that block, the adapter joins the data line as a producer and an upkeep member:

- It reports the machine's recipes, so a Crafting CPU can plan with them.
- Items the line sends into the adapter go into the machine's ingredient side. Expected upkeep goes into its upkeep side.
- The adapter gives out what lies in the machine's output side, so the CPU can collect the results.
- It keeps an upkeep item stocked in the machine's upkeep side. The first profile it meets sets the default (coal for the shipped profiles). Right click the adapter with an empty hand to change the upkeep item and the level (1 to 64).

Without a profile for the faced block the adapter offers no recipes.

On a Shuttles warehouse's data line the adapter gets a number, and it reads as the machine it serves: "Furnace (Adapter #1)". A name typed into the title row of its screen replaces the machine's name.

## Machine profiles

```
data/<namespace>/machine_profiles/<name>.json
```

Profiles reload with `/reload`. Foundation ships `furnace.json`, `smoker.json` and `blast_furnace.json`:

```json
{
  "blocks": ["minecraft:furnace"],
  "recipe_type": "minecraft:smelting",
  "sides": {
    "top": "ingredients",
    "bottom": "output",
    "back": "upkeep",
    "left": "upkeep",
    "right": "upkeep",
    "front": "off"
  },
  "upkeep": {"item": "minecraft:coal", "level": 16}
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `blocks` | list of block ids or `#tags` | required | The blocks this profile covers. |
| `recipe_type` | recipe type id | required | The recipe type the machine runs, for example `minecraft:smelting`. |
| `sides` | object | all `off` | The role of each side of the machine: `ingredients`, `output`, `upkeep` or `off`. Keys are `front`, `back`, `left`, `right`, `top` and `bottom`, relative to the machine's facing. |
| `upkeep` | object | no upkeep | The default upkeep `item` and `level` (16 when left out), and the `batch` (8 when left out): how many must be missing before the adapter asks again. Players can change item and level on the adapter. |

The adapter talks to the machine through its standard item capability on the side the profile names. When several sides share a role, the first of top, bottom, back, left, right and front is used. The machine's facing comes from its `facing` block state property; a machine without one counts as facing north.

## Limits

- **One ingredient only.** The adapter turns every recipe of the type that has exactly one ingredient into one production recipe per accepted item. Recipes with two or more ingredients are left out.
- **Time.** The time of a recipe is its cooking time for cooking recipes (smelting, blasting, smoking, campfire), and 200 ticks for any other type.
- **Items only.** Fluids, energy and machine settings are not part of a profile.
- **One machine per adapter**, the block it faces.

A mod that wants more than this should join the data line itself, see [Join the data line](/servus_mc_mods/developers/join-the-data-line/).

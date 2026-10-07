---
title: Techs
description: The tech JSON format of the Servus Manufacturing research tree.
sidebar:
  order: 3
---

A tech is one node of the research tree. A team starts an available tech in the Research Lab's tech tree screen, and its labs consume packs until the cost is paid. Costs and times on this page are current values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/servus_manufacturing/tech/<name>.json
```

Note the extra `servus_manufacturing` folder. The tech id is `<namespace>:<name>`, so `data/servus_manufacturing/servus_manufacturing/tech/crusher.json` defines `servus_manufacturing:crusher`. Techs reach the client with the datapack; a new tech appears in the tech tree screen after a world reload.

## Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `icon` | item stack | required | Item shown for the tech in the tree: `id` and optional `count`. |
| `parents` | list of tech ids | `[]` | Techs that must be completed before this one is available. A tech without parents is a root. |
| `cost` | list of item stacks | required | Packs to consume, each with `id` and `count`. The lab only accepts items that appear in some tech's cost. |
| `ticks_per_pack` | int | 100 | Lab time per consumed pack. |
| `unlocks` | list of recipe ids | `[]` | Recipes this tech unlocks, for display and tools. |

## Example

The shipped `data/servus_manufacturing/servus_manufacturing/tech/crusher.json`:

```json
{
  "icon": {
    "id": "servus_manufacturing:crusher"
  },
  "parents": [
    "servus_manufacturing:electric_extractor"
  ],
  "cost": [
    {
      "id": "servus_manufacturing:red_pack",
      "count": 15
    }
  ],
  "ticks_per_pack": 100,
  "unlocks": [
    "servus_manufacturing:assembling/crusher"
  ]
}
```

## Built in techs

| Tech | Parents | Cost | Unlocks |
|---|---|---|---|
| `electric_extractor` | none | 10 Red Packs | `assembling/electric_extractor` |
| `stone_processing` | none | 10 Red Packs | `assembling/stone`, `assembling/stone_bricks`, `assembling/glass` |
| `crusher` | `electric_extractor` | 15 Red Packs | `assembling/crusher` |
| `logistics` | `crusher` | 25 Red Packs | `assembling/green_pack` |

All use 100 ticks per pack.

## Notes

- **Research time**: one lab needs `count x ticks_per_pack` ticks for a tech. The Crusher tech takes 15 x 100 = 1500 ticks (75 seconds) with one lab. Several labs of the same team work in parallel, so two labs take about half as long.
- **Energy**: a lab spends its `energy_per_tick` from the [machine stats](/servus_mc_mods/reference/datapack/machines/) while it works (10 FE per tick by default), so one pack at 100 ticks costs 1000 FE.
- **What actually locks a recipe** is the `tech` field of an [assembling recipe](/servus_mc_mods/reference/datapack/recipes/). The `unlocks` list does not gate anything on its own. Keep both in sync when you add or move recipes.
- Progress is per team and survives restarts. Packs consumed for a tech still count after switching to another tech and back.
- The display name comes from the language key `tech.<namespace>.<name>`.

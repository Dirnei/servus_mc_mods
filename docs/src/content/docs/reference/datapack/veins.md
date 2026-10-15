---
title: Veins
description: The ore vein format of the Servus Alchemy Gnome Hollow.
sidebar:
  order: 6
---

A vein is what a Gnome Hollow grows. Numbers on this page are current values; they live in datapack JSON and modpacks can change them.

```
data/<namespace>/servus_alchemy/vein/<name>.json
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `yield` | item | required | `id` of what the gnome digs once the vein is ripe. |
| `color` | `#rrggbb` | the yield block's map colour, else grey | The colour of the vein's material in the cracks under the sight, and of its Seed. |
| `steps` | int | required | Ripening steps until the vein is ripe. |
| `step_ticks` | int | required | Ticks per step while the principles are there. |
| `step_cost` | principles | none | `salt`, `sulfur`, `mercury` in mB, taken at each step. |
| `yield_ticks` | int | required | Ticks per yielded item once the gnome works the ripe vein. |

A vein is planted with its Seed, one `servus_alchemy:seed` item whose `servus_alchemy:vein` component names the vein. Seeds are made on the Athanor; the recipe for each shipped Seed is an `servus_alchemy:athanor` recipe in `data/servus_alchemy/recipe/athanor/seed_of_<vein>.json`. A vein's name comes from the lang key `vein.<namespace>.<name>`.

Shipped veins:

| Vein | Yield | Ripening | Yield | Seed from | Athanor |
|---|---|---|---|---|---|
| `coal` | coal | 15 steps of 100 ticks, 60 Salt and 20 Sulfur each | one per 100 ticks | Coal, 150 Salt, 150 Sulfur | Albedo |
| `iron` | raw iron | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 200 ticks | `c:raw_materials/iron`, 250 Salt, 100 Mercury | Albedo |
| `copper` | raw copper | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 200 ticks | `c:raw_materials/copper`, 250 Salt, 100 Mercury | Albedo |
| `gold` | raw gold | 30 steps of 100 ticks, 150 Salt and 50 Mercury each | one per 300 ticks | `c:raw_materials/gold`, 250 of each | Citrinitas |
| `redstone` | redstone dust | 30 steps of 100 ticks, 100 Salt and 100 Mercury each | one per 200 ticks | `c:dusts/redstone`, 250 Sulfur, 250 Mercury | Citrinitas |
| `quartz` | nether quartz | 30 steps of 100 ticks, 150 Salt and 50 Mercury each | one per 200 ticks | `c:gems/quartz`, 250 Salt, 250 Mercury | Citrinitas |
| `diamond` | diamond | 40 steps of 100 ticks, 200 Salt, 50 Sulfur and 100 Mercury each | one per 1,200 ticks | `c:gems/diamond`, 500 of each | Rubedo |
| `emerald` | emerald | 40 steps of 100 ticks, 200 Salt, 50 Sulfur and 100 Mercury each | one per 1,200 ticks | `c:gems/emerald`, 500 of each | Rubedo |

The Seed of Coal takes Coal only, not Charcoal. A pack adds a vein with a vein file and an Athanor recipe whose result is a Seed naming it; the Seed then shows in the creative tab and in JEI.

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
| `samples` | list of item tags | required | Items of these tags seed the vein. The first vein by id whose tags hold the item wins. |
| `yield` | item | required | `id` of what a bound gnome digs. |
| `chapter` | id | none | A journal chapter the Hollow's team must have finished to seed the vein. |
| `steps` | int | required | Ripening steps until a gnome can be bound. |
| `step_ticks` | int | required | Ticks per step while the principles are there. |
| `step_cost` | principles | none | `salt`, `sulfur`, `mercury` in mB, taken at each step. |
| `yield_ticks` | int | required | Ticks per yielded item once a gnome works the vein. |

Shipped veins:

| Vein | Samples | Yield | Chapter | Ripening | Yield |
|---|---|---|---|---|---|
| `iron` | `c:ores/iron`, `c:raw_materials/iron` | raw iron | Villach | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 200 ticks |
| `copper` | `c:ores/copper`, `c:raw_materials/copper` | raw copper | Villach | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 200 ticks |
| `gold` | `c:ores/gold`, `c:raw_materials/gold` | raw gold | Schwaz | 30 steps of 100 ticks, 150 Salt and 50 Mercury each | one per 300 ticks |
| `redstone` | `c:ores/redstone`, `c:dusts/redstone` | redstone dust | Schwaz | 30 steps of 100 ticks, 100 Salt and 100 Mercury each | one per 200 ticks |
| `diamond` | `c:ores/diamond`, `c:gems/diamond` | diamond | Schwaz | 40 steps of 100 ticks, 200 Salt, 50 Sulfur and 100 Mercury each | one per 1,200 ticks |

Because samples are matched by `c:` tags, ores of other mods can be grown by adding a vein file for their tags.

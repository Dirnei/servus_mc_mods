---
title: Veins
description: The ore vein format of the Servus Alchemy Gnome Hollow.
sidebar:
  order: 6
---

A vein is what a Gnome Hollow grows. Numbers on this page are the shipped values of Alchemy %alchemy_version%; they live in datapack JSON and modpacks can change them.

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
| `yield_ticks` | int | required | Ticks per yielded item once the gnome works the ripe vein, while it is not fed. |
| `feed` | map of principles | none | For `salt`, `sulfur` and `mercury`: the `amount` in mB one dug item uses and the `bonus` it gives. See [Feed](#feed). |

A vein is planted with its Seed, one `servus_alchemy:seed` item whose `servus_alchemy:vein` component names the vein. Seeds are made on the Athanor; the recipe for each shipped Seed is an `servus_alchemy:athanor` recipe in `data/servus_alchemy/recipe/athanor/seed_of_<vein>.json`. A vein's name comes from the lang key `vein.<namespace>.<name>`.

Shipped veins:

| Vein | Yield | Ripening | Unfed | Seed from | Athanor |
|---|---|---|---|---|---|
| `coal` | coal | 15 steps of 100 ticks, 60 Salt and 20 Sulfur each | one per 600 ticks | Coal, 150 Salt, 150 Sulfur | Albedo |
| `iron` | raw iron | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 1,200 ticks | `c:raw_materials/iron`, 250 Salt, 100 Mercury | Albedo |
| `copper` | raw copper | 20 steps of 100 ticks, 100 Salt and 20 Mercury each | one per 1,200 ticks | `c:raw_materials/copper`, 250 Salt, 100 Mercury | Albedo |
| `gold` | raw gold | 30 steps of 100 ticks, 150 Salt and 50 Mercury each | one per 2,400 ticks | `c:raw_materials/gold`, 250 of each | Citrinitas |
| `redstone` | redstone dust | 30 steps of 100 ticks, 100 Salt and 100 Mercury each | one per 1,800 ticks | `c:dusts/redstone`, 250 Sulfur, 250 Mercury | Citrinitas |
| `quartz` | nether quartz | 30 steps of 100 ticks, 150 Salt and 50 Mercury each | one per 1,800 ticks | `c:gems/quartz`, 250 Salt, 250 Mercury | Citrinitas |
| `diamond` | diamond | 40 steps of 100 ticks, 200 Salt, 50 Sulfur and 100 Mercury each | one per 4,800 ticks | `c:gems/diamond`, 500 of each | Rubedo |
| `emerald` | emerald | 40 steps of 100 ticks, 200 Salt, 50 Sulfur and 100 Mercury each | one per 4,800 ticks | `c:gems/emerald`, 500 of each | Rubedo |

The Seed of Coal takes Coal only, not Charcoal. A pack adds a vein with a vein file and an Athanor recipe whose result is a Seed naming it; the Seed then shows in the creative tab and in JEI.

## Feed

A ripe vein with a bound gnome is fed from the Hollow's principle tanks, poured in by bucket or dripped in by a Principle Dropper. A principle of the feed whose tank holds at least its `amount` counts as fed. The gnome digs at the unfed rate times one plus the bonuses of the fed principles, and each dug item uses the `amount` of every fed principle. Feeding is never needed: unfed, the gnome digs at `yield_ticks` and uses nothing. A principle the feed does not name neither speeds the gnome up nor is used, and an unripe vein takes its principles for ripening only. A gift multiplies the fed rate (see [Gnome gifts](/servus_mc_mods/reference/datapack/gnome-gifts/)).

```json
"feed": {
  "salt": {"amount": 10, "bonus": 1.0},
  "mercury": {"amount": 4, "bonus": 0.5},
  "sulfur": {"amount": 2, "bonus": 0.5}
}
```

With this feed and a `yield_ticks` of 1,200, Salt alone gives one item per 600 ticks, and all three give one per 400 ticks.

Shipped feeds, per dug item (bonus in brackets). With all three fed, every shipped vein digs three times as fast as unfed.

| Vein | Salt | Sulfur | Mercury |
|---|---|---|---|
| `coal` | 4 mB (1) | 4 mB (0.75) | 1 mB (0.25) |
| `iron`, `copper` | 10 mB (1) | 2 mB (0.5) | 4 mB (0.5) |
| `gold` | 10 mB (0.5) | 15 mB (1) | 5 mB (0.5) |
| `redstone` | 5 mB (0.5) | 10 mB (0.5) | 15 mB (1) |
| `quartz` | 10 mB (0.5) | 5 mB (0.5) | 15 mB (1) |
| `diamond`, `emerald` | 80 mB (0.75) | 30 mB (0.5) | 50 mB (0.75) |

---
title: Athanor
description: The Athanor craft, tier and ritual formats of Servus Alchemy.
sidebar:
  order: 5
---

The Athanor crafts with the item lying on it and the principles in its tanks, as far as its stage allows. Its stage comes from blocks around its build, and rituals turn those blocks into the next stage. Numbers on this page are the shipped values of Alchemy 0.4.0-alpha; they live in datapack JSON and modpacks can change them.

## Crafts

Ordinary recipes in `data/<namespace>/recipe/`, type `servus_alchemy:athanor`. The item on the Athanor picks the craft.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `principles` | principles | none | `salt`, `sulfur`, `mercury` in mB, taken when the craft completes. |
| `tier` | int 0 to 3 | 0 | The lowest stage that allows the craft. |
| `result` | item stack | required | `id`, `count` (default 1) and optional `components`. |
| `time` | int | 100 | Ticks per craft. |

Built in crafts:

| Recipe | Item | Principles | Tier | Result | Ticks |
|---|---|---|---|---|---|
| `athanor/blaze_powder` | gunpowder | 200 Sulfur | 0 (Nigredo) | Blaze Powder | 100 |
| `athanor/salt_crystal_block` | Salt Crystal | 1,000 Salt | 0 (Nigredo) | Salt Crystal Block | 100 |
| `athanor/experience_bottle` | glass bottle | 50 Salt, 100 Mercury | 1 (Albedo) | Bottle o' Enchanting | 100 |
| `athanor/elixir_of_sight` | Quicksilver | 100 Salt, 100 Sulfur, 250 Mercury | 1 (Albedo) | Elixir of Sight | 200 |
| `athanor/gnome_hollow` | cracked stone bricks | 500 Salt, 250 Mercury | 1 (Albedo) | Gnome Hollow | 200 |
| `athanor/ignis_perpetuus` | Sal Ammoniac | 500 Sulfur | 2 (Citrinitas) | Ignis Perpetuus | 200 |
| `athanor/principle_dropper` | Principle Vat | 250 of each | 2 (Citrinitas) | Principle Dropper | 200 |
| `athanor/sealed_flask` | Crying Obsidian | 250 Salt, 500 Mercury | 3 (Rubedo) | Sealed Flask | 200 |
| `athanor/prelum_screw` | Dragon's Breath | 500 Sulfur, 500 Mercury | 3 (Rubedo) | Prelum Screw | 200 |
| `athanor/seed_of_<vein>` | see [Veins](/servus_mc_mods/reference/datapack/veins/) | | 1 to 3 | Seed | 200 |

## Tiers

```
data/<namespace>/servus_alchemy/athanor_tier/<name>.json
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `tier` | int 1 to 3 | required | The tier number. A tier counts when its blocks, and those of every lower tier, are in place. |
| `color` | string | `#FFFFFF` | The flame colour of the stage. |
| `blocks` | list | required | Each entry has an `offset` `[x, y, z]` from the Athanor, a `block` id and optional `properties`: block state values the block must have, each one value or a list of accepted values. |

Offsets are given for an Athanor whose front faces north (z +1 is behind it), and turn with the Athanor. y 0 is the Athanor's level, y -1 the hearth level and y -2 the layer under the hearth. Tier 0, Nigredo, is the formed Athanor alone.

Shipped tiers:

| File | Tier | Colour | Blocks |
|---|---|---|---|
| `albedo.json` | 1 | `#F2F0EA` | sixteen `servus_alchemy:salt_crystal_block` at y -2, the ring of a 5 by 5 square under the hearth |
| `citrinitas.json` | 2 | `#F2C531` | three `servus_alchemy:principle_vat` with `crystal` `citrine` or `rubedo` at y -1, beside each tank (offsets `[0, -1, 3]`, `[-3, -1, 0]`, `[3, -1, 0]`) |
| `rubedo.json` | 3 | `#B3261E` | the three vats with `crystal` `rubedo`, and four `servus_alchemy:rubedo_obelisk`, each a `lower` half at y -1 and an `upper` half at y 0, at `[1, 2]`, `[-1, 2]`, `[2, 1]` and `[-2, 1]` (x and z) |

The Athanor checks its stage when it is placed or used, when a craft starts and every few seconds. Jade shows the stage by name.

## Rituals

A ritual turns blocks of the build into the next stage. A player offers the ritual's item to a formed Athanor of the ritual's tier.

```
data/<namespace>/servus_alchemy/athanor_ritual/<name>.json
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `offering` | item id | required | The item the player uses on the Athanor. It is taken from the hand. |
| `tier` | int 0 to 3 | required | The stage the Athanor must have. |
| `color` | string | required | The colour of the offering's glow, the flame during the ritual and the streams as they leave. |
| `result_color` | string | required | The colour the streams turn into and the grown blocks glow in. |
| `duration` | int 1 to 1200 | required | Ticks the ritual takes. The blocks turn when it ends. |
| `phases` | object | required | `burst`, `fill`, `grow` and `settle`: the ticks after the offering at which each part of the show starts. They must rise in that order and end before `duration`. |
| `sounds` | object | none | Optional `offering`, `burst`, `land`, `grow` and `end` sounds, each with `id` and optional `volume` and `pitch` (default 1). |
| `turns` | list | required | The blocks to turn. Each has an `offset`, a `from` block (with optional `properties`) that must be in place, a `to` block it becomes, and `stream` (default true): whether a stream of light runs to it. |

A ritual starts only when no craft and no other ritual runs and every `from` block is in place; otherwise the player keeps the offering and is told what is missing. When the build later loses a block, the Athanor falls back to the highest stage still complete, and blocks that rituals grew for higher stages turn back.

Shipped rituals:

| File | Offering | Tier | Turns |
|---|---|---|---|
| `citrinitas.json` | Amethyst Block | 1 (Albedo) | the three empty Principle Vats beside the tanks into vats with citrine (160 ticks) |
| `rubedo.json` | Ghast Tear | 2 (Citrinitas) | Obsidian on the four obelisk cells (with air above) into Rubedo Obelisks, and the citrine vats into orange (`rubedo`) vats (160 ticks) |

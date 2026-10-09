---
title: Athanor
description: The Athanor craft and tier formats of Servus Manufacturing.
sidebar:
  order: 5
---

The Athanor crafts with the item lying on it and the principles in its tanks, as far as the structure around it allows. Numbers on this page are current values; they live in datapack JSON and modpacks can change them.

## Crafts

Ordinary recipes in `data/<namespace>/recipe/`, type `servus_manufacturing:athanor`. The item on the Athanor picks the craft.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `principles` | principles | none | `salt`, `sulfur`, `mercury` in mB, taken when the craft completes. |
| `tier` | int | 0 | The lowest structure tier that allows the craft. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | 100 | Ticks per craft. |

Built in crafts (100 ticks each):

| Recipe | Item | Principles | Tier | Result |
|---|---|---|---|---|
| `athanor/blaze_powder` | gunpowder | 200 Sulfur | 0 (Nigredo) | Blaze Powder |
| `athanor/experience_bottle` | glass bottle | 50 Salt, 100 Mercury | 1 (Albedo) | Bottle o' Enchanting |
| `athanor/elixir_of_sight` | Quicksilver | 100 Salt, 100 Sulfur, 250 Mercury | 1 (Albedo) | Elixir of Sight (200 ticks) |

## Tiers

```
data/<namespace>/servus_manufacturing/athanor_tier/<name>.json
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `tier` | int 1 to 3 | required | The tier number. A tier counts when its blocks and those of every lower tier are in place. |
| `color` | string | `#FFFFFF` | The flame colour of the stage. |
| `blocks` | list | required | Each entry has an `offset` `[x, y, z]` from the Athanor and a `block` id. |

Tier 0, Nigredo, is the Athanor alone. The shipped `albedo.json` is tier 1: eight `servus_manufacturing:salt_crystal_block` around the Athanor on its own level. The Athanor checks its tier when it is placed or used, when a craft starts and every 5 seconds.

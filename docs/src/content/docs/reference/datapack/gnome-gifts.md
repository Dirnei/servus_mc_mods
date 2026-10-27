---
title: Gnome gifts
description: What the gnomes of the Servus Alchemy Gnome Hollow like as gifts, and how much faster they dig.
sidebar:
  order: 7
---

A gnome at work in a Gnome Hollow digs faster for a while after a gift it likes. Gifts are optional: without one it digs at its vein's `yield_ticks`. Numbers on this page are the shipped values of Alchemy 0.3.0-alpha; they live in datapack JSON and modpacks can change them.

```
data/<namespace>/servus_alchemy/gnome_gift/<name>.json
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `items` | list of item ids | none | Items the gnomes like. |
| `tags` | list of item tags | none | Item tags whose items the gnomes like. |
| `factor` | number, 1 to 100 | required | How many times faster the gnome digs while the gift lasts. |
| `duration_ticks` | int | required | How long the gift lasts, in ticks while the Hollow is loaded. |

An item uses the first gift file by id that names it or one of its tags; items no file names are refused. A new gift replaces a running one: it restarts with its own factor and full duration, gifts never add up. Gifts speed up digging only, not ripening, and only a Hollow with a bound gnome takes them.

Shipped gifts:

| Gift | Items | Factor | Duration |
|---|---|---|---|
| `treats` | cookie, bread, sweet berries, glow berries | x2 | 12,000 ticks (10 minutes) |

For now gifts are given by hand under Elemental Sight; one item is used per gift.

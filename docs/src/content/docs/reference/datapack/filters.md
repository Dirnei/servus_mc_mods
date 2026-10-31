---
title: Filters
description: The filter data map and the Carbon Filter recipes of Servus Foundation.
sidebar:
  order: 2
---

Servus Foundation 0.6.0 adds the Carbon Filter, a filter that wears down as machines use it. Which items are filters, and how long they last, is datapack data. Numbers on this page are the shipped values; modpacks can change them.

## The filter data map

```
data/<namespace>/data_maps/item/filter.json
```

The data map `servus_foundation:filter` makes items filters. It reloads with `/reload` and is sent to the clients.

```json
{
  "values": {
    "servus_foundation:carbon_filter": {
      "points": 512,
      "spent": "servus_foundation:spent_filter"
    }
  }
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `points` | int | required | Cleaning points one filter has. At least 1. |
| `spent` | item id | required | What a filter turns into when its last point is spent. |

A machine spends points from a filter as it works. A used filter shows a bar and its points left, and no longer stacks with fresh ones. Foundation's own machines use no filter; machines of other mods may.

## Making the Carbon Filter

| Step | Recipe | Shipped values |
|---|---|---|
| Charcoal Powder | `servus_foundation:milling/charcoal_powder` in the Mill | 1 Charcoal into 2 Charcoal Powder, 100 work |
| Activated Charcoal | `servus_foundation:activated_charcoal_from_blasting`, a vanilla blasting recipe | 1 Charcoal Powder into 1 Activated Charcoal, 100 ticks |
| Carbon Filter | `servus_foundation:carbon_filter`, shapeless crafting | 2 Activated Charcoal, 1 binder and 1 Paper |

A binder is any item in the item tag `servus_foundation:filter_binders`. It holds `#c:crops/wheat` and, when a mod adds it, `#c:flours`. The Spent Filter composts with a chance of 0.65.

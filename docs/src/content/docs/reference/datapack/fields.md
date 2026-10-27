---
title: Fields and tree fields
description: The tree field, Lumber shuttle and Field Head buffer fields of the Servus Shuttles field stats file.
sidebar:
  order: 11
---

Servus Shuttles reads the numbers of its fields from one stats file. This page covers the fields that Shuttles 0.5.0 added for tree fields, the Lumber shuttle and the Field Head's To storage buffer. Values are the shipped ones; they live in datapack JSON and modpacks can change them.

## File location

```
data/servus_shuttles/shuttles/field.json
```

The file reloads with `/reload`. Every field is optional, and a field the file leaves out falls back to the default below. The shipped file sets more fields (water, corrosion, fertilizer, seed buffer and others); they keep their meaning from earlier versions.

## Tree fields and the Lumber shuttle

| Field | Default | Meaning |
|---|---|---|
| `unfold_ticks` | 30 | Ticks a Lumber shuttle takes to unfold at a rail before its first tree, and again to fold before it leaves. Handling Upgrades do not shorten it. |
| `fell_ticks` | 40 | Ticks the saw part of felling one tree takes. Handling Upgrades shorten it like a stop, to at least one tick. |
| `topple_ticks` | 80 | Ticks the tree takes to topple and break apart after the saw part. Handling Upgrades do not shorten it. |
| `max_tree_logs` | 200 | A tree with more logs than this is not felled. |
| `max_tree_blocks` | 1000 | A tree with more blocks in all (logs, leaves and hanging parts) than this is not felled. |

The Field Head shows a warning for a tree that is too large, and does not count that plot as ready to fell until a block on it changes. Planting a sapling takes `work_ticks`, like a harvester's plot.

## To storage buffer

| Field | Default | Meaning |
|---|---|---|
| `harvest_buffer_slots` | 9 | Slots of the Field Head's To storage buffer. Harvesters, Spreaders and Lumber shuttles unload into it, and plain shuttles put it away into the racks. |

## Packing Station

The Packing Station's own file, `data/servus_shuttles/shuttles/packing_station.json`, ships `items_per_second` 10 in Shuttles 0.5.0 (8 before) with `upgrade_slots` 2.

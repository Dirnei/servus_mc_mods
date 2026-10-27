---
title: Old gnome
description: When Paracelsus' old gnome wakes in the deepslate, and how his Ancient Hollow gives hints.
sidebar:
  order: 10
---

Paracelsus' old gnome wakes while a team mines deep. Nothing is generated with the world: each Deepslate block a player breaks may wake him, and the mod then turns a nearby wall block into his Ancient Hollow. Numbers on this page are the shipped values of Alchemy 0.3.0-alpha; they live in datapack JSON and modpacks can change them.

```
data/<namespace>/servus_alchemy/old_gnome/old_gnome.json
```

Only the file `servus_alchemy:old_gnome` is read. Every field is optional; a missing field, or a missing file, uses the value in the Default column.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `wake_chance` | number, 0 to 1 | 0.003 | Chance per broken block of the tag `servus_alchemy:wakes_old_gnome` (Deepslate) to wake him. |
| `wake_chapter` | string | `servus_alchemy:schwaz` | The journal chapter the breaker's team must have finished. |
| `max_y` | int | 0 | Only blocks below this height wake him. |
| `knock_range` | int | 24 | A player within this many blocks hears the Ancient Hollow knock. |
| `knock_interval` | int | 100 | Ticks between two series of three knocks. |
| `dust_range` | int | 6 | A player within this many blocks sees dust fall from it. |
| `trace_range` | int | 8 | Blocks of the tag broken by his team within this many blocks may drop a trace. |
| `trace_chance` | number, 0 to 1 | 0.15 | Chance per such block to drop a trace. |

The Ancient Hollow is the first block of the tag with air on a horizontal side, out of a fixed list of positions two to four blocks from the broken block. Traces are the items in the tag `servus_alchemy:hint_traces`. Each team has one old gnome; once found he never wakes again.

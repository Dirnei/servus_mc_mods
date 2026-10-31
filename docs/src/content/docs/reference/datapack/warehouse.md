---
title: Racks and warehouse
description: The rack, Fluid Rack, fluid station, Handheld and Parcel Locker fields of the Servus Shuttles stats files.
sidebar:
  order: 12
---

Servus Shuttles reads the numbers of its warehouse from stats files. This page covers the fields that Shuttles 0.6.0 added or renamed for shelves, the Fluid Rack, the fluid stations, the Handheld HH-1 and the Parcel Locker. Values are the shipped ones; they live in datapack JSON and modpacks can change them.

## File location

```
data/servus_shuttles/shuttles/rack.json
data/servus_shuttles/shuttles/fluid_rack.json
data/servus_shuttles/shuttles/warehouse.json
```

The files reload with `/reload`. Every field is optional, and a field a file leaves out falls back to the default below. The shipped files set more fields; they keep their meaning from earlier versions.

## Shelves

Bays of a rack in a row join into shelves. Joins are decided when a bay is placed, so a new value changes only shelves built after it.

| File | Field | Default | Meaning |
|---|---|---|---|
| `rack.json` | `bays_per_shelf` | 3 | Bays a Heavy Duty Rack shelf may hold. |
| `fluid_rack.json` | `bays_per_shelf` | 3 | Bays a Fluid Rack shelf may hold. |

`bays_per_shelf` was `bays_per_section` before Shuttles 0.6.0. The old name still works when a file has no `bays_per_shelf`.

## Fluid Rack

`fluid_rack.json`:

| Field | Default | Meaning |
|---|---|---|
| `capacity_per_bay` | 16000 | mB each bay adds to its shelf's tank. A full shelf of three bays holds 48,000 mB. |
| `level_steps` | 16 | Steps in which players see the fill of a shelf or a fluid station tank. |

## Fluid stations and the Tanker

`warehouse.json`:

| Field | Default | Meaning |
|---|---|---|
| `fluid_station_tank_capacity` | 16000 | mB the tank of a Fluid Inbound or a Fluid Outbound holds. |
| `fluid_check_interval` | 40 | Ticks between two looks of a warehouse at its fluid stations. |
| `tanker_min_load` | 1000 | mB a Fluid Inbound must hold, unless its tank is full, before a Tanker comes for it. A restock run smaller than this waits, unless it brings every whole bucket missing. |
| `tanker_pump_ticks` | 40 | Ticks a Tanker stops at a rack port or a station to take or give fluid. Handling Upgrades shorten it like other stops. |

## Handheld HH-1

`warehouse.json`:

| Field | Default | Meaning |
|---|---|---|
| `handheld_scan_range` | 16 | How far the Handheld scans a shelf, in blocks. At least 1. |
| `handheld_find_glow_seconds` | 8 | How long the shelves holding a picked item glow, in seconds. |
| `handheld_order_margin` | 64 | Blocks around the chunks of a warehouse site from which ORDER still sends a basket. |

## Parcel Locker

`warehouse.json`:

| Field | Default | Meaning |
|---|---|---|
| `locker_box_s` | 3 | Stacks one S box holds. |
| `locker_box_m` | 6 | Stacks one M box holds. |
| `locker_box_l` | 9 | Stacks the L box holds. |
| `locker_return_ticks` | 24000 | Ticks a ready parcel waits before shuttles put it back into the racks. 24000 is one in-game day. |
| `locker_door_open_ticks` | 200 | Ticks the doors a scan opened stay open. |
| `locker_check_interval` | 20 | Ticks between two looks of a warehouse at its Parcel Lockers. |

## Packing Station shipments

`warehouse.json`:

| Field | Default | Meaning |
|---|---|---|
| `refused_shipment_timeout` | 1200 | Ticks a Packing Station shipment may stay cut from its target, or keep being refused by a machine, before it ends and its items go back into storage. Never below `push_interval`. |

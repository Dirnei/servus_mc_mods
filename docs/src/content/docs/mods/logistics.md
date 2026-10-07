---
title: "Servus: Logistics"
description: The Utility Duct, one block that carries items and energy on eight colored channels, with filters, priorities and facades.
---

Servus: Logistics (`servus_logistics`) adds the Utility Duct: one block that can carry several lines. Today those are the Item Pipe and the Power Cable; a fluid line is planned. It depends only on Servus Lib and works with any block that offers the standard item or energy capability.

## Items

- **Item Pipe**: places a duct with an item line, or adds the item line to a duct that lacks it. Eight from iron ingots, glass and a chest.
- **Power Cable**: places a duct with an energy line, or adds it to a duct. Eight from copper ingots, redstone and glass.
- **Facade**: any full block without a block entity plus one iron ingot gives six facades of that block. Use one on a duct to make it look like that block.

## How a duct works

- Each line forms its own network: an item network only spans ducts with an Item Pipe, an energy network only ducts with a Power Cable.
- Every side of every line has a mode: in and out (default), pull in, push out or disabled. A disabled side does not connect.
- Every side of every line sits on one of eight channels, each with a fixed color. Items and energy only move between sides on the same channel. Item and energy channels on one side are independent.
- **Items** inserted into a duct go round robin to outputs on the same channel, never back to where they came from. Pull in sides extract items from their inventory at a fixed interval.
- **Filters**: each item side has a whitelist of up to nine ghost items. An empty list passes everything.
- **Priorities**: each item side has a priority from -9 to 9. Higher priorities are filled first.
- **Energy** is shared evenly between receivers on a channel, up to a per tick limit.

## Endpoint GUI

Use the Configuration Carrot on an arm that touches an inventory or machine. The GUI shows, per line, the mode button, the channel selector and a remove button, and for the item line also the filter slots and the priority. The carrot on the center or on an arm to another duct opens nothing, and a bare hand never does. Removing the last line removes the duct.

## Appearance

A duct grows thicker with its lines, and each line has its own color, so you can tell from a distance what a duct carries. An arm that ends at an inventory or machine carries a ring in its channel color. A duct with a facade renders as the facade block. While you wear Engineer Goggles or hold the carrot, facaded ducts within 32 blocks show as a translucent overlay. Sneak with the carrot to take the facade off.

## Datapack values

Transfer rates live in `data/servus_logistics/duct/utility_duct.json`. Current values, which modpacks can change:

```json
{
  "items_per_tick": 16,
  "items_per_pull": 8,
  "pull_interval": 8,
  "energy_per_tick": 512
}
```

## How it connects

- [Servus Lib](/servus_mc_mods/mods/lib/): network graph, energy distribution, the Configuration Carrot and the goggles overlay.
- [Manufacturing](/servus_mc_mods/mods/manufacturing/) requires Logistics and uses its ducts as its pipes and cables.
- [Storage](/servus_mc_mods/mods/storage/) does not depend on Logistics, but ducts can feed and drain any of its blocks that expose an item handler.

---
title: "Servus: Foundation"
description: The base every Servus mod requires. The Utility Duct, the Fluid Tank, facades, the Configuration Carrot, Engineer Goggles, side configuration and the production API.
---

Servus: Foundation (`servus_foundation`) is the base of the mod family. Manufacturing and Storage both require it and never depend on each other. It has two halves: shared code that makes the family look and behave the same, and the transport blocks every base needs.

Foundation loads on its own, and each content mod loads with only Foundation installed.

## Items

- **Configuration Carrot**: the wrench of the family. A carrot with a metal bar and a red tip instead of leaves. Use it on a face to cycle that side's mode, sneak use it to dismantle a block and keep its configuration. On blocks with a facing and no own handling it rotates the block.
- **Engineer Goggles**: worn in the head slot like a helmet without armor. While worn, or while a Configuration Carrot is held, hidden things show up, such as side mode markers and ducts behind facades.
- **Item Pipe**: places a duct with an item line, or adds the item line to a duct that lacks it. Eight from iron ingots, glass and a chest.
- **Power Cable**: places a duct with an energy line, or adds it to a duct. Eight from copper ingots, redstone and glass.
- **Fluid Pipe**: places a duct with a fluid line, or adds it to a duct. Eight from iron ingots, glass and a bucket.
- **Facade**: any full block without a block entity plus one iron ingot gives six facades of that block. Use one on a duct to make it look like that block.
- **Fluid Tank**: a glass block that holds one fluid. Iron ingots and glass.

All of them are listed in the Servus creative tab.

## Utility Duct

One block that carries up to three lines: Item Pipe, Power Cable and Fluid Pipe. It works with any block that offers the standard item, energy or fluid capability.

- Each line forms its own network: an item network only spans ducts with an Item Pipe, and so on.
- Every side of every line has a mode: in and out (default), pull in, push out or disabled. A disabled side does not connect.
- Every side of every line sits on one of eight channels, each with a fixed color. Items, energy and fluids only move between sides on the same channel.
- **Items** inserted into a duct go round robin to outputs on the same channel, never back to where they came from. Pull in sides extract items from their inventory at a fixed interval.
- **Filters**: each item side has a whitelist of up to nine ghost items. An empty list passes everything.
- **Priorities**: each item side has a priority from -9 to 9. Higher priorities are filled first.
- **Energy** is shared evenly between receivers on a channel, up to a per tick limit.
- **Fluids** move between blocks with no rate limit of their own; pumps set the throughput.

### Endpoint GUI

Use the Configuration Carrot on an arm that touches an inventory or machine. The GUI has a tab per line with the mode button, the channel selector and a remove button, and for the item line also the filter slots and the priority. The carrot on the center or on an arm to another duct opens nothing, and a bare hand never does. Removing the last line removes the duct.

### Appearance

A duct grows thicker with its lines, and each line has its own color, so you can tell from a distance what a duct carries. An arm that ends at an inventory or machine carries a ring in its channel color. A duct with a facade renders as the facade block. While you wear Engineer Goggles or hold the carrot, facaded ducts within 32 blocks show as a translucent overlay. Sneak with the carrot to take the facade off.

## Fluid Tank

Touching tank blocks that fill a complete rectangular box form one tank with a shared content. A group that is not a full box works as single block tanks until it is. Buckets work on every face, and an empty hand shows what the tank holds. A broken block keeps its share of the fluid in its item.

## Side configuration

Every machine and station uses the same side setup. A side has one of these modes:

| Mode | Behavior |
|---|---|
| Auto | Insert and extract, and the block pushes its output. The default. |
| Input | Insertion only. |
| Output | Extraction only, nothing is pushed. |
| Both | Insertion and extraction. |
| Pull | Insertion, and the block pulls from the neighbor. |
| Push | Extraction, and the block pushes to the neighbor. |
| Off | Nothing. |

Sides are relative (front, back, left, right, top, bottom), so a configuration survives rotation. Blocks may support only some modes. The GUI shows a side panel left of the container, and the block draws a colored frame on every face that is not in its default mode. With the carrot or goggles, every face shows its frame and an arrow glyph.

## Shared systems

- **Network graph and manager**: ducts, tanks, rails and warehouses are connected graphs that merge and split on placement and removal. Nothing scans blocks every tick.
- **Tick mailbox**: network events are queued and handled once per server tick, in order.
- **Energy distribution**: fair sharing of FE between sources and sinks, used by the Power Cable.
- **Teams**: every player has a team id. Without a team mod it is the player's own id. Research and chunk loader limits are per team.
- **Production and stock API**: the capabilities `servus_foundation:production`, `servus_foundation:stock_provider` and `servus_foundation:stock_sink`, plus a pure order planner. This is how a Storage warehouse learns what a Manufacturing machine can make without the two mods knowing each other.

## Datapack values

Transfer rates live in `data/servus_foundation/duct/utility_duct.json` and the tank size in `data/servus_foundation/tank/fluid_tank.json`. Current values, which modpacks can change:

```json
{
  "items_per_tick": 16,
  "items_per_pull": 8,
  "pull_interval": 8,
  "energy_per_tick": 512
}
```

```json
{
  "capacity_per_block": 16000
}
```

## Config

The common config `config/servus_foundation-common.toml` has `wrenchRotatesBlocks` (default true), which decides whether the carrot rotates foreign blocks that have a facing.

## How it connects

- [Manufacturing](/servus_mc_mods/mods/manufacturing/) machines use the side panel and the carrot, report their recipes through the production capability, and use ducts as their pipes and cables.
- [Storage](/servus_mc_mods/mods/storage/) warehouses use the network graph, the stock capabilities and the order planner. Ducts can move items in and out of any storage block that exposes an item handler.

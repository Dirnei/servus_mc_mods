---
title: Servus Lib
description: The shared library every Servus mod requires, with the carrot, goggles, side configuration and the production API.
---

Servus Lib (`servus_lib`) is the base of the mod family. It adds only a few items itself; most of it is shared code that the content mods build on, so they look and behave the same and can work together without depending on each other.

Lib loads on its own, and every content mod loads with only Lib installed. The one exception is Manufacturing, which also requires Logistics.

## Items

- **Configuration Carrot**: the wrench of the family. A carrot with a metal bar and a red tip instead of leaves. Use it on a face to cycle that side's mode, sneak use it to dismantle a block and keep its configuration. On blocks with a facing and no own handling it rotates the block.
- **Engineer Goggles**: worn in the head slot like a helmet without armor. While worn, or while a Configuration Carrot is held, hidden things show up, such as side mode markers and ducts behind facades.

Both are craftable from vanilla materials and listed in the Servus creative tab, which exists whenever Lib is installed.

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

- **Network graph and manager**: ducts, rails and warehouses are connected graphs that merge and split on placement and removal. Nothing scans blocks every tick.
- **Tick mailbox**: network events are queued and handled once per server tick, in order.
- **Energy distribution**: fair sharing of FE between sources and sinks, used by the Power Cable.
- **Teams**: every player has a team id. Without a team mod it is the player's own id. Research and chunk loader limits are per team.
- **Production and stock API**: the capabilities `servus_lib:production`, `servus_lib:stock_provider` and `servus_lib:stock_sink`, plus a pure order planner. This is how a Storage warehouse learns what a Manufacturing machine can make without the two mods knowing each other.

## Config

The common config `config/servus_lib-common.toml` has `wrenchRotatesBlocks` (default true), which decides whether the carrot rotates foreign blocks that have a facing.

## How it connects

- [Manufacturing](/servus_mc_mods/mods/manufacturing/) machines use the side panel, the carrot and report their recipes through the production capability.
- [Logistics](/servus_mc_mods/mods/logistics/) ducts use the network graph, the energy distributor and the goggles overlay.
- [Storage](/servus_mc_mods/mods/storage/) warehouses use the network graph, the stock capabilities and the order planner.

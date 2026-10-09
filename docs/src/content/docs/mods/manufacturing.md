---
title: "Servus: Manufacturing"
description: Alchemy after Paracelsus. Matter is split into Salt, Sulfur and Mercury and made again from them.
---

Servus: Manufacturing (`servus_manufacturing`) is alchemy after Paracelsus. Every material is made of three principles, Salt, Sulfur and Mercury. The Calciner splits matter into them and the Coagulator joins them again into new things. Nothing forces you to build a factory: a few blocks and buckets get you going, and pipes and ratios reward the planner.

It requires Servus: Foundation, whose Utility Duct is its pipe and cable. JEI is optional.

Numbers below are the current defaults. Machine stats and recipes live in datapack JSON and modpacks can change them (see the [datapack reference](/servus_mc_mods/reference/datapack/machines/)).

:::note
Manufacturing is being rebuilt as alchemy (milestone M9). The Athanor, the Elixir of Sight and the gnome grown ore veins follow in the next changes.
:::

## Paracelsus' Journal

The journal is the tech tree. Every player gets it at their first login, and it can be crafted from a book and a piece of coal. It is a Modonomicon book, so Modonomicon is required.

Each chapter of the journal tells part of Paracelsus' life and lists tasks: do something a number of times, or make something for the first time. Your team shares the tasks. When all tasks of a chapter are done, the chapter is finished for the whole team, each member gets its advancement, and the next chapter opens in the book. `/servus_manufacturing journal` shows the progress.

- **Villach** (Nigredo): calcine 100 things, gather 1,000 mB of Salt, coagulate your first Salt Crystal.
- **Schwaz** (Albedo): opens after Villach; its tasks arrive with the gnomes.

## The three principles

Salt, Sulfur and Mercury are fluids. They flow through the Fluid Pipe, wait in Fluid Tanks, go into buckets and can be voided in the Sink like any other fluid. Poured into the world they flow like water but never make new sources.

## Machines

- **Calciner**: burns one item into its principles, for example one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds. It takes common matter: cobblestone, stone, dirt, sand, gravel, logs, planks, leaves, saplings, crops, seeds, coal, raw ores, redstone and bones. It holds 4,000 mB of each principle and pushes them out through its Auto and Push sides. 20 FE per tick while working. Bricks, iron, a furnace and a cauldron.
- **Coagulator**: makes the recipe you pick in its GUI from a fixed amount of each principle, for example a Salt Crystal from 250 mB Salt in 2 seconds. Each principle goes only into its own tank. 20 FE per tick while working. Iron, glass, a cauldron and redstone.

Power, water, voiding surplus and chunk loading come from [Servus: Foundation](/servus_mc_mods/mods/foundation/#machines): the Coal Generator, the Burner and Electric Pump, the Sink and the Chunk Loader.

All machines face the player on placement, open a GUI on right click, push output into adjacent inventories and drop their contents when broken. Powered machines only draw energy on ticks they work and keep their progress when power runs out.

## Coagulator products

Salt Crystal, Sulfur and Quicksilver are the first alchemical materials. The Coagulator also makes clay, flint, gunpowder, redstone, glowstone dust, bone meal and slime balls from the principles. No metal comes from principles in this first stage; metals are grown by the gnomes later.

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): side configuration, the Configuration Carrot, the Fluid Pipe and Fluid Tank, Power Cables and Item Pipes.
- [Storage](/servus_mc_mods/mods/storage/): a Coagulator on the data line is a producer of its selected recipe, so a Crafting CPU can order Salt Crystals or gunpowder. Its principles must reach it by pipe; the data line only carries items. The Calciner is no producer.

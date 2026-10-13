---
title: "Servus: Manufacturing"
description: Alchemy after Paracelsus. Matter is split into Salt, Sulfur and Mercury and made again from them.
---

Servus: Manufacturing (`servus_manufacturing`) is alchemy after Paracelsus. Every material is made of three principles, Salt, Sulfur and Mercury. The Calciner splits matter into them and the Coagulator joins them again into new things. Nothing forces you to build a factory: a few blocks and buckets get you going, and pipes and ratios reward the planner.

It requires Servus: Foundation, whose Utility Duct is its pipe and cable. JEI is optional.

Numbers below are the current defaults. Machine stats and recipes live in datapack JSON and modpacks can change them (see the [datapack reference](/servus_mc_mods/reference/datapack/machines/)).

## Paracelsus' Journal

The journal is the tech tree. Every player gets it at their first login, and it can be crafted from a book and a piece of coal. The book runs on Modonomicon, so Modonomicon is required, but the journal itself is a Servus item. Besides the story, its Workshop tab shows how to build and use every machine, with the crafting recipes and a list of every calcination, coagulation and Athanor recipe.

Each chapter of the journal tells part of Paracelsus' life and lists tasks: do something a number of times, or make something for the first time. Your team shares the tasks. When all tasks of a chapter are done, the chapter is finished for the whole team, each member gets its advancement, and the next chapter opens in the book. `/servus_manufacturing journal` shows the progress.

- **Villach** (Nigredo): calcine 100 things, gather 1,000 mB of Salt, coagulate your first Salt Crystal.
- **Schwaz** (Albedo): opens after Villach; finish one craft on an Albedo Athanor, drink the Elixir of Sight and bind a gnome.

## The three principles

Salt, Sulfur and Mercury are fluids. They flow through the Fluid Pipe, wait in Fluid Tanks, go into buckets and can be voided in the Sink like any other fluid. Poured into the world they flow like water but never make new sources. To fill a bucket with one principle, open the Calciner and click its gauge with the empty bucket; to pour a principle bucket into the Coagulator, Athanor or Gnome Hollow, use it on the block (in the Coagulator you can also click a gauge with it).

## Machines

- **Calciner**: burns one item into its principles, for example one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds. It takes common matter: cobblestone, stone, dirt, sand, gravel, logs, planks, leaves, saplings, crops, seeds, coal, raw ores, redstone and bones. It holds 4,000 mB of each principle and pushes them out through its sides. In the Fluids tab of its side panel each side can be set to one principle, so Salt leaves on one side and Mercury on another, each into its own pipe. Calcination is fire: the Calciner burns any furnace fuel in its fuel slot, one coal for 10 items, and needs no FE. On the data line it asks for coal to keep its fuel stocked. Bricks, iron, a furnace and a cauldron.
- **Coagulator**: makes the recipe you pick in its GUI from a fixed amount of each principle, for example a Salt Crystal from 250 mB Salt in 2 seconds. Each principle goes only into its own tank, and a side set to one principle in the Fluids tab takes only that one. It needs no power: the joined principles only take the recipe's time to set. Iron, glass, a cauldron and redstone.

- **Athanor**: the alchemist's furnace and the heart of the lab. Lay an item on it, feed it principles by bucket or pipe, and it burns them into the craft of that item, for example gunpowder and 200 mB Sulfur into Blaze Powder. It needs no FE. The blocks built around it set its stage, shown by its flame: alone it is Nigredo (black); twelve Salt Crystal Blocks around its diamond brick hearth make it Albedo (white), which some crafts need. Bricks, a blast furnace and a cauldron.
- **Salt Crystal Block**: grown on the Athanor from one Salt Crystal and 1,000 mB Salt; it cannot be crafted or split at a table.
- **Gnome Hollow**: grows an ore vein, see [Gnomes](#gnomes). Made on an Albedo Athanor from cracked stone bricks, 500 mB Salt and 250 mB Mercury.

Nigredo runs on fire and time, no FE needed. Power for later machines, water, voiding surplus and chunk loading come from [Servus: Foundation](/servus_mc_mods/mods/foundation/#machines): the Coal Generator, the Burner and Electric Pump, the Sink and the Ender Anchor.

All machines face the player on placement, open a GUI on right click, push output into adjacent inventories and drop their contents when broken. Powered machines only draw energy on ticks they work and keep their progress when power runs out.

## Elemental Sight

Paracelsus' elementals live inside their element the way we live in air, and common eyes do not see them. The **Elixir of Sight**, made on an Albedo Athanor from Quicksilver with 100 mB Salt, 100 mB Sulfur and 250 mB Mercury, is drunk like a potion and gives **Elemental Sight** for three minutes. While it lasts you see what is hidden from others, such as the gnomes at work in the rock.

## Gnomes

There is no ore generation to plan around: the miners of Paracelsus' time believed metals grow in the mountain, and here they do. A **Gnome Hollow** looks like plain stone.

1. **Seed** it with a sample of ore: put raw iron or any iron ore (or copper) into its sample slot. One item is used. The vein must belong to a journal chapter your team has finished; iron and copper open with Villach.
2. **Ripen** it: every 5 seconds the vein grows a step and takes 100 mB Salt and 20 mB Mercury from the Hollow's tanks, which fill by pipe or bucket. Twenty steps make it ripe; without principles it waits.
3. **Bind** a gnome: drink the Elixir of Sight and use a Salt Crystal on the ripe Hollow. Without the sight nothing happens.
4. The gnome **works** forever: one raw ore every 10 seconds into the output slot, pushed out like any machine output. It needs nothing more.

Under Elemental Sight you see the vein's ore budding on the Hollow, more and larger as it ripens, and the red cap of its gnome. Without the sight it is a stone block. On the data line a working Hollow is a producer of its ore.

## Coagulator products

Salt Crystal, Sulfur and Quicksilver are the first alchemical materials. The Coagulator also makes clay, flint, gunpowder, redstone, glowstone dust, bone meal and slime balls from the principles. No metal comes from principles; metals are grown by the gnomes.

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): side configuration, the Configuration Carrot, the Fluid Pipe and Fluid Tank, Power Cables and Item Pipes.
- [Storage](/servus_mc_mods/mods/storage/): a Coagulator on the data line is a producer of its selected recipe, so a Crafting CPU can order Salt Crystals or gunpowder. Its principles must reach it by pipe; the data line only carries items. The Calciner is no producer. A working Gnome Hollow is a producer of its ore with no inputs.

---
title: "Servus: Alchemy"
description: Alchemy after Paracelsus. Matter is split into Salt, Sulfur and Mercury and made again from them.
---

Servus: Alchemy (`servus_alchemy`) is alchemy after Paracelsus. Every material is made of three principles, Salt, Sulfur and Mercury. The Calciner splits matter into them and the Coagulator joins them again into new things. Nothing forces you to build a factory: a few blocks and buckets get you going, and pipes and ratios reward the planner.

It requires Servus: Foundation, whose Utility Duct is its pipe and cable. JEI is optional.

Numbers below are the current defaults. Machine stats and recipes live in datapack JSON and modpacks can change them (see the [datapack reference](/servus_mc_mods/reference/datapack/machines/)).

## Paracelsus' Journal

The journal is the tech tree. Every player gets it at their first login, and it can be crafted from a book and a piece of coal. The book runs on Modonomicon, so Modonomicon is required, but the journal itself is a Servus item. Besides the story, its Workshop tab shows how to build and use every machine, with the crafting recipes and a list of every calcination, coagulation and Athanor recipe.

Each chapter of the journal tells part of Paracelsus' life and lists tasks: do something a number of times, or make something for the first time. Your team shares the tasks. When all tasks of a chapter are done, the chapter is finished for the whole team, each member gets its advancement, and the next chapter opens in the book. `/servus_alchemy journal` shows the progress.

- **Villach** (Nigredo): calcine 100 things, gather 1,000 mB of Salt, coagulate your first Salt Crystal.
- **Schwaz** (Albedo): opens after Villach; finish one craft on an Albedo Athanor, drink the Elixir of Sight and bind a gnome.
- **Wandering years** (Citrinitas): opens after Schwaz; finish one craft on a Citrinitas Athanor, bind a gnome to a gold vein and bind four gnomes.
- **Basel and Salzburg** (Rubedo): opens after Wandering years; finish one craft on a Rubedo Athanor, bind a gnome to a diamond vein and finish 16 Athanor crafts.

The story pages of Wandering years and Basel and Salzburg are placeholders for now and start with "[storyteller]".

## The three principles

Salt, Sulfur and Mercury are fluids. They flow through the Fluid Pipe, wait in Fluid Tanks, go into buckets and can be voided in the Sink like any other fluid. Poured into the world they flow like water but never make new sources. To fill a bucket with one principle, open the Calciner and click its gauge with the empty bucket; to pour a principle bucket into the Coagulator or Athanor, use it on the block (in the Coagulator you can also click a gauge with it). The Gnome Hollow takes buckets only under Elemental Sight.

## Machines

- **Calciner**: a furnace built 3 wide and 5 tall and formed with the Journal. It burns one item into its principles, for example one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds. It takes common matter: cobblestone, stone, dirt, sand, gravel, logs, planks, leaves, saplings, crops, seeds, coal, raw ores, redstone and bones. It puts Sulfur and Mercury straight into its two Fluid Tanks (Sulfur left, Mercury right), and Mercury drips from its copper condenser into a stone basin above its tank. The Salt stays in the Ash Pan as Ash, up to a stack of 64. When a tank or the Ash Pan is full, it waits. Calcination is fire: the Calciner burns any furnace fuel in its fuel slot, one coal for 10 items, and needs no FE. On the data line it asks for coal to keep its fuel stocked. Bricks, iron, a furnace and a cauldron.
- **Coagulator**: makes the recipe you pick in its GUI from a fixed amount of each principle, for example a Salt Crystal from 250 mB Salt in 2 seconds. Each principle goes only into its own tank, and a side set to one principle in the Fluids tab takes only that one. It needs no power: the joined principles only take the recipe's time to set. Iron, glass, a cauldron and redstone.

- **Athanor**: the alchemist's furnace and the heart of the lab. Lay an item on it, feed it principles by bucket or pipe, and it burns them into the craft of that item, for example gunpowder and 200 mB Sulfur into Blaze Powder. It needs no FE. The blocks built around it set its stage, shown by its flame: alone it is Nigredo (black); twelve Salt Crystal Blocks around its diamond brick hearth make it Albedo (white), which some crafts need. Twelve Amethyst Blocks standing on those Salt Crystal Blocks make it Citrinitas (yellow), and four pillars of two Crying Obsidian on the amethyst corners make it Rubedo (red). Each stage needs the blocks of the stages below it; the Workshop shows each build as a schematic. Bricks, a blast furnace and a cauldron.
- **Salt Crystal Block**: grown on the Athanor from one Salt Crystal and 1,000 mB Salt; it cannot be crafted or split at a table.
- **Gnome Hollow**: grows an ore vein, see [Gnomes](#gnomes). Made on an Albedo Athanor from cracked stone bricks, 500 mB Salt and 250 mB Mercury.
- **Principle Dropper**: a copper drip vessel with a glass bulb that feeds the Gnome Hollow directly below it, see [Gnomes](#gnomes). It has a 1,000 mB tank per principle, fills from Fluid Pipes and principle buckets and never gives anything back. Made on a Citrinitas Athanor from a Principle Vat with 250 mB each of Salt, Sulfur and Mercury.

Nigredo runs on fire and time, no FE needed. Power for later machines, water, voiding surplus and chunk loading come from [Servus: Foundation](/servus_mc_mods/mods/foundation/#machines): the Coal Generator, the Burner and Electric Pump, the Sink and the Ender Anchor.

All machines face the player on placement, open a GUI on right click, push output into adjacent inventories and drop their contents when broken. The Gnome Hollow is the exception: it hides as stone, see [Gnomes](#gnomes). Powered machines only draw energy on ticks they work and keep their progress when power runs out.

## Elemental Sight

Paracelsus' elementals live inside their element the way we live in air, and common eyes do not see them. The **Elixir of Sight**, made on an Albedo Athanor from Quicksilver with 100 mB Salt, 100 mB Sulfur and 250 mB Mercury, is drunk like a potion and gives **Elemental Sight** for three minutes. While it lasts you see what is hidden from others, such as the gnomes at work in the rock.

## Gnomes

There is no ore generation to plan around: the miners of Paracelsus' time believed metals grow in the mountain, and here they do. A **Gnome Hollow** is disguised as stone. Without Elemental Sight it looks like stone, Jade calls it Stone, a right click does nothing and breaking it drops cobblestone (stone with Silk Touch); the vein is lost. It has no screen, and no pipe or duct connects to it. Everything is done by hand under the sight:

1. **Seed** it with a sample of ore: use raw iron or any iron ore (or copper) on it. One item is used. The vein must belong to a journal chapter your team has finished; iron and copper open with Villach. Gold (raw gold or any gold ore), redstone (redstone dust or any redstone ore) and diamond (a diamond or any diamond ore) open with Schwaz; they need more principles and the gnomes dig them slower.
2. **Ripen** it: pour Salt and Mercury buckets into it (the empty bucket comes back; each tank holds 4,000 mB). Every 5 seconds the vein grows a step and takes 100 mB Salt and 20 mB Mercury. Twenty steps make it ripe; without principles it waits. From Citrinitas on, a **Principle Dropper** set directly on top of the Hollow does this for you: fed by pipe, it drips 25 mB of each principle the vein's steps take every second until the vein is ripe, with coloured drops falling from its spout. It drips nothing into a ripe or empty Hollow, nothing that the vein does not take, and nothing into any other block. No pipe touches the Hollow, and Jade on the Dropper shows only its own tanks.
3. **Bind** a gnome: use a Salt Crystal on the ripe Hollow.
4. The gnome **works** forever: one raw ore every 10 seconds into the Hollow's store, which holds 64 items. A full store stops the gnome. Use an empty hand on the Hollow to take the store into your inventory; what does not fit drops.
5. **Gifts** (optional): use a cookie, bread, sweet berries or glow berries on a working Hollow and its gnome digs twice as fast for 10 minutes. A new gift restarts the 10 minutes; gifts do not add up. Without gifts the gnome works at its normal pace, and it refuses anything else. See [Gnome gifts](/servus_mc_mods/reference/datapack/gnome-gifts/).

Under Elemental Sight you see the vein's ore budding on the Hollow, more and larger as it ripens, and the red cap of its gnome, and Jade shows the vein, its ripeness, the store, the principles and a running gift with its time left. A gifted gnome spins faster. A failed action under the sight tells you why on the action bar; an empty hand on an empty store tells you how the vein is doing.

Breaking a Hollow under the sight drops an empty Gnome Hollow and the ore in its store; the vein, its principles and its gnome are lost. Moving a Hollow means growing the vein again.

## Coagulator products

Salt Crystal, Sulfur and Quicksilver are the first alchemical materials. The plain goods of Nigredo come from the principles alone: clay balls and bone meal. The Coagulator also makes flint, gunpowder, redstone, glowstone dust, bone meal and slime balls from the principles. No metal comes from principles; metals are grown by the gnomes.

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): side configuration, the Configuration Carrot, the Fluid Pipe and Fluid Tank, Power Cables and Item Pipes.
- [Storage](/servus_mc_mods/mods/storage/): a Coagulator on the data line is a producer of its selected recipe, so a Crafting CPU can order Salt Crystals or gunpowder. Its principles must reach it by pipe; the data line only carries items. The Calciner is no producer, and neither is the Gnome Hollow.

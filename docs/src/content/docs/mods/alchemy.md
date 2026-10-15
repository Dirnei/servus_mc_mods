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
- **The Old Gnome** (a side chapter): opens after Schwaz; find one of the old gnome's traces and find him. No other chapter waits for it.

The story pages of Wandering years and Basel and Salzburg are placeholders for now and start with "[storyteller]".

## The three principles

Salt, Sulfur and Mercury are fluids. They flow through the Fluid Pipe, wait in Fluid Tanks, go into buckets and can be voided in the Sink like any other fluid. Poured into the world they flow like water but never make new sources. To fill a bucket with one principle, open the Calciner and click its gauge with the empty bucket; to pour a principle bucket into the Coagulator or Athanor, use it on the block (in the Coagulator you can also click a gauge with it). The Gnome Hollow takes buckets only under Elemental Sight.

## Machines

- **Calciner**: a furnace built 3 wide and 5 tall and formed with the Journal. It burns one item into its principles, for example one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds. It takes common matter: cobblestone, stone, dirt, sand, gravel, logs, planks, leaves, saplings, crops, seeds, coal, raw ores, redstone and bones. It puts Sulfur and Mercury straight into its two Fluid Tanks (Sulfur left, Mercury right), and Mercury drips from its copper condenser into a stone basin above its tank. The Salt stays in the Ash Pan as Ash, up to a stack of 64. When a tank or the Ash Pan is full, it waits. Calcination is fire: the Calciner burns any furnace fuel in its fuel slot, one coal for 10 items, and needs no FE. On the data line it asks for coal to keep its fuel stocked. With Ignis Perpetuus set into its Firebox it needs no fuel at all and calcines twice as fast. Bricks, iron, a furnace and a cauldron.
- **Coagulator**: makes the recipe you pick in its GUI from a fixed amount of each principle, for example a Salt Crystal from 250 mB Salt in 2 seconds. Each principle goes only into its own tank, and a side set to one principle in the Fluids tab takes only that one. It needs no power: the joined principles only take the recipe's time to set. Iron, glass, a cauldron and redstone.

- **Athanor**: the alchemist's furnace and the heart of the lab. Lay an item on it, feed it principles by bucket or pipe, and it burns them into the craft of that item, for example gunpowder and 200 mB Sulfur into Blaze Powder. It needs no FE. The blocks built around it set its stage, shown by its flame: alone it is Nigredo (black); twelve Salt Crystal Blocks around its diamond brick hearth make it Albedo (white), which some crafts need. Twelve Amethyst Blocks standing on those Salt Crystal Blocks make it Citrinitas (yellow), and four pillars of two Crying Obsidian on the amethyst corners make it Rubedo (red). Each stage needs the blocks of the stages below it; the Workshop shows each build as a schematic. Bricks, a blast furnace and a cauldron.
- **Salt Crystal Block**: grown on the Athanor from one Salt Crystal and 1,000 mB Salt; it cannot be crafted or split at a table.
- **Gnome Hollow**: grows an ore vein, see [Gnomes](#gnomes). Made on an Albedo Athanor from cracked stone bricks, 500 mB Salt and 250 mB Mercury.
- **Principle Dropper**: a copper drip vessel with a glass bulb that feeds the Gnome Hollow directly below it, see [Gnomes](#gnomes). It has a 1,000 mB tank per principle, fills from Fluid Pipes and principle buckets and never gives anything back. Made on a Citrinitas Athanor from a Principle Vat with 250 mB each of Salt, Sulfur and Mercury.

Nigredo runs on fire and time, no FE needed. Power for later machines, water, voiding surplus and chunk loading come from [Servus: Foundation](/servus_mc_mods/mods/foundation/#machines): the Coal Generator, the Burner and Electric Pump, the Sink and the Ender Anchor.

All machines face the player on placement, open a GUI on right click, push output into adjacent inventories and drop their contents when broken. The Gnome Hollow is the exception: it hides as stone, see [Gnomes](#gnomes). Powered machines only draw energy on ticks they work and keep their progress when power runs out.

## Elemental Sight

Paracelsus' elementals live inside their element the way we live in air, and common eyes do not see them. The **Elixir of Sight**, made on an Albedo Athanor from Quicksilver with 100 mB Salt, 100 mB Sulfur and 250 mB Mercury, is drunk like a potion and gives **Elemental Sight** for three minutes. While it lasts you see what is hidden from others, such as the gnomes at work in the rock.

The dose makes the poison: each drink has a 15 % chance to give you Poison and Nausea for 10 seconds, and 30 % when you drink again while the sight still lasts. You get the sight either way. See [Elixir dose](/servus_mc_mods/reference/datapack/elixir-dose/).

## Gnomes

There is no ore generation to plan around: the miners of Paracelsus' time believed metals grow in the mountain, and here they do. A **Gnome Hollow** is disguised as stone. Without Elemental Sight it looks like stone, Jade calls it Stone, a right click does nothing and breaking it drops cobblestone (stone with Silk Touch); the vein is lost. It has no screen, and no pipe or duct connects to it. Everything is done by hand under the sight:

1. **Seed** it with a sample of ore: use raw iron or any iron ore (or copper) on it. One item is used. The vein must belong to a journal chapter your team has finished; iron and copper open with Villach. Gold (raw gold or any gold ore), redstone (redstone dust or any redstone ore) and diamond (a diamond or any diamond ore) open with Schwaz; they need more principles and the gnomes dig them slower.
2. **Ripen** it: pour Salt and Mercury buckets into it (the empty bucket comes back; each tank holds 4,000 mB). Every 5 seconds the vein grows a step and takes 100 mB Salt and 20 mB Mercury. Twenty steps make it ripe; without principles it waits. From Citrinitas on, a **Principle Dropper** set directly on top of the Hollow does this for you: fed by pipe, it drips 25 mB of each principle the vein's steps take every second until the vein is ripe, with coloured drops falling from its spout. It drips nothing into a ripe or empty Hollow, nothing that the vein does not take, and nothing into any other block. No pipe touches the Hollow, and Jade on the Dropper shows only its own tanks.
3. **Bind** a gnome: use a Salt Crystal on the ripe Hollow.
4. The gnome **works** forever: one raw ore every 10 seconds into the Hollow's store, which holds 64 items. A full store stops the gnome. Use an empty hand on the Hollow to take the store into your inventory; what does not fit drops.
5. **Gifts** (optional): use a cookie, bread, sweet berries or glow berries on a working Hollow and its gnome digs twice as fast for 10 minutes. A new gift restarts the 10 minutes; gifts do not add up. Without gifts the gnome works at its normal pace, and it refuses anything else. See [Gnome gifts](/servus_mc_mods/reference/datapack/gnome-gifts/).

Under Elemental Sight you see the vein's ore budding on the Hollow, more and larger as it ripens, and the red cap of its gnome, and Jade shows the vein, its ripeness, the store, the principles and a running gift with its time left. A gifted gnome spins faster. A failed action under the sight tells you why on the action bar; an empty hand on an empty store tells you how the vein is doing.

Breaking a Hollow under the sight drops an empty Gnome Hollow and the ore in its store; the vein, its principles and its gnome are lost. Moving a Hollow means growing the vein again.

## The Salamander

The salamanders, lizards of glowing coal with a crest of flame, live mostly in the Crimson Forest, on the ground within 4 blocks of crimson trees (stems or wart blocks) or Nether Wart. Fewer live in the Warped Forest near the warped trees, and now and then one spawns on a lava lake in the Nether Wastes or the Basalt Deltas. They never crowd: no wild one spawns where 3 wild salamanders already live within 32 blocks; befriended ones do not count. Without Elemental Sight you see only a flame that flickers now and then; Jade does not name them either. With the sight they show. They walk on lava and on land, fire and lava do not hurt them, and they never attack.

- **Befriending:** use a Gold Ingot on a wild salamander. Every ingot is eaten, and each has a 1 in 3 chance to befriend it (hearts; smoke when it does not). A befriended salamander is yours and shows itself to everyone.
- **Pet:** it follows you and teleports to you when it falls far behind, sits or stands up when you use an empty hand on it, never despawns, and when idle walks into a lit campfire within 8 blocks and lies there. It gives off no light of its own beyond its glow.
- **Sal Ammoniac:** use a Gold Ingot on your own salamander. It digests it for 60 seconds and drops one Sal Ammoniac; it takes the next ingot only when it is done.
- **Ignis Perpetuus:** one Sal Ammoniac with 500 mB Sulfur on a Citrinitas Athanor makes the eternal flame, a building part for later works. It is no furnace fuel, but set into the Firebox of a Calciner by hand it is its eternal flame: the Calciner then burns without fuel or upkeep and calcines twice as fast (any fuel in its slot comes back to you). Sneak with an empty hand at the Firebox to take it back; breaking the Firebox or the Calciner drops it.

The salamander is optional for the journal chapters. See [Salamander](/servus_mc_mods/reference/datapack/salamander/) for the numbers.

## The Salamander Forge

A furnace with the eternal flame inside: crafted from one Ignis Perpetuus in the middle, a Furnace below it and seven Deepslate Bricks around them. It smelts every smelting recipe like a vanilla furnace, one item every 200 ticks, and never takes fuel. Its fire always burns, so it is always lit and gives off light level 13.

- **Slots:** an input and an output, no fuel slot. The screen shows the flame always full.
- **Automation:** a normal Servus machine. Hoppers and Item Pipes reach it on every side but the front; it pushes its output into neighbouring inventories; the side panel and the Configuration Carrot set each side.
- **Data line:** it is a producer, so a Crafting CPU orders smelting from it directly, with no Machine Adapter. It keeps no upkeep.
- **Speed:** `work_ticks` in `servus_alchemy:salamander_forge` (see [Machine stats](/servus_mc_mods/reference/datapack/machines/)).

## The Receptaculum and the Vas Hermeticum

The homunculus is the only machine that takes the ore out of the Gnome Hollows. It lives in a stack of two blocks:

- **Receptaculum:** a Hearth Chest crafted from Ignis Perpetuus between Deepslate Bricks, a Chest above it and three Bricks below. Its fire always burns. Its screen has 27 yield slots (take only), 3 gift slots and 1 Elixir slot. It is a normal Servus machine: Hoppers and Item Pipes take the yield out and bring gifts and Elixir in on every side but the front, and it pushes its yield into neighbours. On a data line it is a producer that lists the yields of the Hollows in reach, so a Crafting CPU orders grown ore from it.
- **Sealed Flask:** made on a Rubedo Athanor from Crying Obsidian with 250 Salt and 500 Mercury. It places the **Vas Hermeticum**, and only on top of a Receptaculum; breaking the Receptaculum breaks the Vas.
- **Growing:** the homunculus grows for 48000 ticks (two in-game days) while it is fed. One Elixir of Sight from the Elixir slot feeds it for 12000 ticks, while it grows and while it works. Without Elixir it pauses.
- **Work:** every 100 ticks a grown, fed homunculus takes the stored ore of every Gnome Hollow in the 5x5 area on the Receptaculum's level into the yield slots, and gives a gift from the gift slots to each gnome whose boost has less than 1200 ticks left.
- **Hidden:** without Elemental Sight the Vas is glass with murky liquid. Under the sight you see the homunculus in it and chains to the Hollows in its reach.
- **Numbers:** `data/<namespace>/servus_alchemy/homunculus/<name>.json` with `grow_ticks`, `work_interval`, `elixir_ticks` and `gift_below_ticks`.

## The Prelum

The Press at the end of the Work. It presses the Caput Mortuum the Purifier leaves behind into Netherite Scrap; four scraps and four Gold Ingots make a Netherite Ingot at a vanilla crafting table.

- **Built once with Dragon's Breath:** one Dragon's Breath with 500 mB Sulfur and 500 mB Mercury on a Rubedo Athanor makes the Prelum Screw. The Prelum is crafted from the screw at the top, a Heavy Weighted Pressure Plate between two Stripped Dark Oak Logs, and three Polished Deepslate below.
- **Pressing:** 16 Caput Mortuum make one Netherite Scrap every 600 ticks (30 seconds). It needs no fuel and no principles. While it presses, its bar turns and its plate goes down onto the bed and up again.
- **Slots:** an input that takes only what a pressing recipe accepts, and an output.
- **Automation:** a normal Servus machine. Hoppers and Item Pipes reach it on every side but the front; it pushes its output into neighbouring inventories; the side panel and the Configuration Carrot set each side.
- **Data line:** it is a producer, so a Crafting CPU orders Netherite Scrap from it. It keeps no upkeep.
- **Journal:** the first pressed Netherite Scrap is a task of the Basel and Salzburg chapter.
- **Numbers:** the `servus_alchemy:pressing` recipes (see [Recipes](/servus_mc_mods/reference/datapack/recipes/)).

## Hints and the old gnome

Everything hidden behind Elemental Sight gives a hint without it, so you know when to drink. The hints come from far to close: **knocking** in the rock (subtitle "Something knocks in the rock"), which you hear from its direction; **dust** that trickles from one block; and **traces**, small items to pick up: a Tiny Pickaxe, a Scrap of Red Felt and a Bowl of Salt. Picking up a trace tells the journal "They are near. Drink, and look."

- **Gnome Hollow:** a Hollow with a bound gnome knocks softly now and then.
- **Salamander:** a wild salamander shows a flame now and then, and sometimes a little spiral of dancing flames.
- **Vas Hermeticum:** until the homunculus is freed, the liquid in the Vas bubbles by itself now and then.
- **The old gnome:** once your team has finished Schwaz, every Deepslate block a team member breaks below Y 0 has a small chance (0.3 %) to wake Paracelsus' old gnome. A Deepslate block in a wall a few blocks away, with air beside it, becomes his **Ancient Hollow**: it looks like Deepslate, Jade calls it Deepslate and it drops Cobbled Deepslate. A trace lies in front of it. While you are within 24 blocks it knocks every few seconds, within 6 blocks dust falls from it, and Deepslate your team breaks within 8 blocks of it sometimes drops another trace. Under the sight he stands in front of it as the House Gnome, with a patched red hat, a grey beard and a Salt Crystal in his hand, looking around; use the journal on him or on the Ancient Hollow to find him, and he goes back in. Each team has one old gnome; if the Ancient Hollow is broken before you find him, he can wake again elsewhere. Taking him home comes later.
- **Numbers:** `data/<namespace>/servus_alchemy/old_gnome/old_gnome.json`, see [Old gnome](/servus_mc_mods/reference/datapack/old-gnome/).

## Coagulator products

Salt Crystal, Sulfur and Quicksilver are the first alchemical materials. The plain goods of Nigredo come from the principles alone: clay balls and bone meal. The Coagulator also makes flint, gunpowder, redstone, glowstone dust, bone meal and slime balls from the principles. No metal comes from principles; metals are grown by the gnomes.

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): side configuration, the Configuration Carrot, the Fluid Pipe and Fluid Tank, Power Cables and Item Pipes.
- [Storage](/servus_mc_mods/mods/storage/): a Coagulator on the data line is a producer of its selected recipe, so a Crafting CPU can order Salt Crystals or gunpowder. Its principles must reach it by pipe; the data line only carries items. The Calciner is no producer, and neither is the Gnome Hollow.

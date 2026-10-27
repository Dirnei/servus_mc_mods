---
title: "Servus: Alchemy"
description: Alchemy after Paracelsus. Split matter into Salt, Sulfur and Mercury, join them into new things, and grow ore with gnomes.
sidebar:
  badge:
    text: Alpha
    variant: caution
---

![Servus: Alchemy icon](../../../assets/alchemy.png)

**Version 0.3.0-alpha.** Mod id `servus_alchemy`. Needs Servus: Foundation 0.4.0 or newer and Modonomicon.

:::caution[Alpha]
Alchemy is an alpha. Builds and recipes can still change between versions, and some story pages of the journal are placeholders. Back up your world before you update.
:::

Alchemy follows Paracelsus: every material is made of three principles, **Salt**, **Sulfur** and **Mercury**. You burn matter to get them, and you join them again into new things. There is no ore generation to plan around. Metals grow in the rock, tended by gnomes that only the alchemist can see.

## Paracelsus' Journal

You get the journal at your first login, or craft it from a book and a piece of coal. It is your guide and your tech tree:

- The **Workshop** tab shows every build as a schematic, with its recipes.
- The **chapters** follow Paracelsus' life: Villach, Schwaz, Wandering years, and Basel and Salzburg, plus a side chapter about an old gnome. Each chapter lists tasks. Your team shares them, and a finished chapter opens the next.
- You also form the multiblock machines with it: build the structure, then right click its main block with the journal. If a block is missing, the journal tells you which and where.

## The principles

Salt, Sulfur and Mercury are fluids. They flow through Foundation Fluid Pipes, wait in Fluid Tanks and fill buckets.

## Machines

- **Calciner**: a furnace 3 blocks wide and 5 tall. It burns common matter (stone, dirt, sand, logs, crops, coal, raw ores and more) on any furnace fuel. Sulfur and Mercury flow into the two Foundation Fluid Tanks built into it (Sulfur left, Mercury right). The Salt stays behind as **Ash** in the Ash Pan.
- **Purifier**: a Leaching Vat over a log, with a Catch Basin on one side and a Residue Crock on the other. It washes Ash with water into Salt and leaves **Caput Mortuum** behind.
- **Coagulator**: a Coagulator between Stone Bricks under three Principle Vats. Pick a recipe and feed it principles. It makes Salt Crystals, Sulfur, Quicksilver and everyday goods such as flint, gunpowder, redstone and slime balls.
- **Athanor**: the alchemist's furnace. It stands on a 3 by 3 brick hearth, with three Fluid Tanks around it, each joined by a Copper Duct. Lay an item on it, give it principles, and it crafts. Its stage decides what it can make:
  - **Nigredo**: the Athanor alone.
  - **Albedo**: a ring of Salt Crystal Blocks under the hearth.
  - **Citrinitas**: place a Principle Vat beside each tank and offer an Amethyst Block. Citrine grows out of the vats.
  - **Rubedo**: place four Obsidian behind the tanks and offer a Ghast Tear. Red obelisks rise.

None of these machines need FE. Fire and time do the work.

## Gnomes and veins

Drink an **Elixir of Sight** (made on an Albedo Athanor) to see what is hidden for a few minutes. Careful: the dose makes the poison.

A **Gnome Hollow** looks like plain stone. Under the sight you call a gnome with a Salt Crystal, plant a **Seed** made on the Athanor, and ripen the vein with principles, poured in by bucket. Then the gnome digs ore for you, forever. Seeds of coal, iron and copper need Albedo, gold, redstone and quartz Citrinitas, and diamond and emerald Rubedo. A **Principle Dropper** on top of a Hollow feeds it for you. Gnomes like a cookie now and then.

## Later in the work

- **Salamander**: a fire lizard of the Nether, only seen under the sight. Befriend it with gold, and it digests gold into Sal Ammoniac.
- **Ignis Perpetuus**: the eternal flame, made on a Citrinitas Athanor. Set into the Calciner, it burns without fuel and twice as fast. It also builds the **Salamander Forge**, a furnace that never needs fuel.
- **Receptaculum** and **Vas Hermeticum**: a homunculus grows in the flask and collects the ore of the Gnome Hollows around it.
- **Prelum**: a press built from a screw made with Dragon's Breath. It presses Caput Mortuum into Netherite Scrap.

## With Shuttles

On a Foundation data line, the Coagulator, the Athanor, the Salamander Forge, the Receptaculum and the Prelum are producers, so a Shuttles Crafting CPU can order from them. The Calciner's Firebox asks the warehouse for coal. Principles never travel on the data line; they need pipes.

## Numbers

Recipes, stages, veins and machine stats live in datapack JSON. See the [datapack reference](/servus_mc_mods/reference/datapack/machines/).

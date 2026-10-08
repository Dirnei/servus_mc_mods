---
title: "Servus: Manufacturing"
description: Factorio style production with infinite deposits, fixed rate machines and a pack driven research tree.
---

Servus: Manufacturing (`servus_manufacturing`) is about building a factory you can plan. Resources come from infinite deposits, machines run at fixed speeds with clean ratios, and progress is gated by research packs that a Research Lab consumes over time.

It requires Servus: Foundation, whose Utility Duct is its pipe and cable. JEI is optional.

Numbers below are the current defaults. Machine stats, recipes, techs and deposits live in datapack JSON and modpacks can change them (see the [datapack reference](/servus_mc_mods/reference/datapack/machines/)).

## Deposits

Iron, copper, coal and stone deposits generate as flat surface patches in the overworld. They cannot be mined and never run out. An extractor on top yields one raw iron, raw copper, coal or cobblestone per cycle.

## Machines

- **Burner Extractor**: burns furnace fuel and mines the deposit below it, one item per 4 seconds.
- **Burner Smelter**: burns fuel and smelts any vanilla smelting recipe at a fixed 4 seconds per item, ignoring the recipe's cooking time.
- **Coal Generator**: burns fuel for 40 FE per tick; one coal lasts 20 seconds. Pushes energy into adjacent consumers.
- **Assembler Mk1**: powered crafter with 18 input and 4 output slots. Configure up to two recipes (assembling recipes or any vanilla or modded crafting recipe) in its recipe editor; it alternates between those it can craft.
- **Research Lab**: powered, consumes packs for the team's current research.
- **Electric Extractor** (research): powered extractor, twice as fast as the burner one.
- **Crusher** (research): powered, turns one raw ore into two grit; grit smelts into ingots, so crushing doubles the ingots per ore.
- **Chunk Loader**: keeps its chunk loaded while powered. At most 4 active loaders per team (config). Stays inactive when FTB Chunks is installed, unless the config says otherwise.

All machines face the player on placement, open a GUI on right click, push output into adjacent inventories and drop their contents when broken. Burner machines only burn fuel while they have work; powered machines only draw energy on ticks they work and keep their progress when power runs out.

## Items

- **Iron Gear**: two iron ingots, assembled.
- **Copper Wire**: one copper ingot gives two wire, assembled.
- **Red Pack**: one gear and two wire, the first research pack.
- **Iron Grit, Copper Grit**: from the Crusher, smelt into ingots.

## Research

Techs form a tree. A team picks an available tech in the lab's tech tree screen, and every lab of that team consumes one matching pack per `ticks_per_pack`. More labs finish faster. Completed techs unlock assembling recipes for the whole team.

Current techs: Electric Extractor, Crusher (after Electric Extractor), Stone Processing (stone, stone bricks and glass in the assembler) and Logistics (after Crusher, the gate to Tier 2).

## Balance target

The reference block (two extractors and two smelters on iron, one of each on copper, one extractor on coal, three assemblers, generators and one lab) makes one Red Pack every 5 seconds. A GameTest keeps that true.

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): side configuration, the Configuration Carrot, teams, the production capability every machine exposes, and the Item Pipes and Power Cables that move items and FE between machines.
- [Storage](/servus_mc_mods/mods/storage/): an Assembler on the data line lets a Crafting CPU plan work orders from the Assembler's configured recipes. A Burner Smelter or Crusher on the data line runs order steps with its smelting or crushing recipes.
- **Upkeep**: the Burner Extractor, Burner Smelter, Burner Pump and Coal Generator keep coal at 16 in their fuel slot as upkeep. On a data line with a warehouse they are refilled on their own, independent of orders; the item and level are the machine stats `upkeep_item` and `upkeep_level`.

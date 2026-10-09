# Proposal: m9-tria-prima

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 2: `m9-tria-prima`.

## Why

PLAN.md 2.1 rebuilds Manufacturing around Paracelsus' Tria Prima: matter is split into Salt, Sulfur and Mercury and products are made again from them. This change lays that core loop down and removes the Factorio content it replaces (deposits, extractors, smelter, crusher, assembler, gears, wire and packs), so the following changes (journal, Athanor, Elixir of Sight, Gnome Hollow) build on alchemy instead of the old tier.

## What Changes

- **Three principles as fluids**: Salt, Sulfur and Mercury, each with a fluid type, a source and a flowing fluid, a liquid block and a bucket. They travel through the Fluid Pipe and wait in Fluid Tanks like any fluid.
- **Calciner**: a powered machine (FE) that burns one input item into fixed amounts of the three principles, from calcination recipes in datapack JSON. It holds one tank per principle and pushes them out through its sides. In Nigredo it takes common materials (cobblestone, stone, dirt, sand, gravel, logs, planks, leaves, crops, coal, raw ores and more by tag), so no ore source is needed but the pickaxe.
- **Coagulator**: a powered machine that makes an item from a fixed ratio of the three principles, optionally with one item ingredient, from coagulation recipes in datapack JSON. The player picks the recipe in its GUI. It accepts each principle only into its own tank.
- **Data cable**: the Calciner and the Coagulator expose the production capability so the Crafting CPU can order their products. Principles arrive by pipe, never as packets.
- **First recipes** (datapack, examples): Salt Crystal, Sulfur and Quicksilver as the first alchemical materials, plus a handful of vanilla goods made from principles (clay, flint, gunpowder, redstone, glowstone, bone meal, slime). No metal is made from principles in Nigredo; metals come from the gnomes later.
- **JEI**: Calcination and Coagulation categories replace Assembling and Crushing.
- **Removed**: deposit blocks and their world generation, Burner and Electric Extractor, Burner Smelter, Crusher, Assembler with its recipe editor payload, Stone Processing recipes, Iron Gear, Copper Wire, Red Pack, Iron and Copper Grit, the assembling and crushing recipe types and the tech JSON files that only unlocked removed recipes. The Research Lab and the tech system stay until `m9-journal` replaces them; with no techs shipped the lab has nothing to research.
- **Docs**: the Manufacturing page describes the Calciner, Coagulator and principles; the Balance lab tests keep their old data as a test fixture until the lab is rebuilt on the new data (M9 item 7).

Nothing is released, so worlds are not migrated: placed deposits and removed machines disappear.

## Capabilities

### New Capabilities
- `manufacturing/principles`: the Salt, Sulfur and Mercury fluids, their buckets and how they move.
- `manufacturing/calciner`: the Calciner and calcination recipes.
- `manufacturing/coagulator`: the Coagulator and coagulation recipes.

### Modified Capabilities
- `manufacturing/data-line`: the Burner Smelter and Crusher producer requirements go; the Calciner and Coagulator become producers; the upkeep requirements move to `foundation/upkeep` because only Foundation burner machines remain.
- `foundation/upkeep`: gains the burner fuel upkeep and upkeep stats requirements for the Burner Pump and Coal Generator.
- `manufacturing/recipe-viewer`: Calcination and Coagulation categories replace Assembling.
- `manufacturing/deposits`, `manufacturing/burner-extractor`, `manufacturing/electric-extractor`, `manufacturing/burner-smelter`, `manufacturing/crusher`, `manufacturing/assembler`, `manufacturing/stone-processing`, `manufacturing/tier1-balance`: every requirement removed.

## Non-goals

- The journal, chapter tasks and the removal of the Research Lab (`m9-journal`).
- The Athanor, its structure tiers and altar crafts (`m9-athanor`); Salt Crystal blocks are only an item here.
- Elemental Sight and gnome veins (`m9-elixir-of-sight`, `m9-gnome-hollow`).
- A converter between principles; surplus goes to the Sink.
- Rebuilding the docs Balance lab on alchemy data (M9 item 7).
- Final balance numbers; every number is a datapack example.

## Impact

- Code: `manufacturing/` loses `deposit`, `machine/{extractor,smelter,crusher,assembler}`, assembling and crushing recipe code, `ConfigureRecipePayload`, `PatternSlot`, their menus, screens, JEI categories and GameTests; gains `alchemy` packages for fluids, Calciner, Coagulator, recipes, JEI categories and GameTests.
- Data: deposit, biome modifier and worldgen JSON, tech JSON and old recipes deleted; calcination and coagulation recipes, machine stats and tags added.
- Docs: `mods/manufacturing.md`, datapack reference pages, `gamedata.json`, icons, and Balance lab tests moved to a fixture.
- Storage and Foundation do not change, apart from the upkeep spec.

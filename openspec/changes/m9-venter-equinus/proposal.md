# Proposal

## Why

Players want Dirt in bulk, for building and for filling the holes their mines leave, and Alchemy has no way to make it. Real soil is ground rock, clay, rotted life and water. The rotted life is the missing piece: the alchemists got it by putrefaction, bedding sealed vessels in warm horse dung, the *venter equinus* ("horse's belly"), and Paracelsus himself putrefied his most secret work there. This change adds that build and the Humus it makes, and lets the Foundation Mixer turn Sand, Clay and Humus into Dirt.

PLAN.md milestone: Alchemy, Nigredo (M9 prefix), the "Venter Equinus" open point decided on 2026-10-30 under "Open: Alchemy". This change must be applied after `m14-mixer`, which adds the Foundation Mixer and the `servus_foundation:mixing` recipe type that two recipes of this change use. Alchemy already depends on Foundation, so nothing new is wired between the mods.

## What Changes

- New item **Humus**, made only by the Venter Equinus. It has no crafting recipe and no calcination recipe.
- New build **Venter Equinus**: a 3 by 3 bed, one block high, formed with the Journal. Blocks:
  - the **Cucurbit** in the middle (a sealed terracotta gourd with a cut copper seal), the controller;
  - the **Bed Hatch** in the middle of the front side (a spruce wall with a small oak hatch and a steel handle);
  - seven **Dung Beds** on the other cells (spruce posts and walls with straw).

  Formed, the frame pieces join into one bed with four corner posts and continuous walls. The Cucurbit stands bedded in the straw. Unformed, each block shows a plain model of its own.
- Putrefaction: rot (item tag `servus_alchemy:putrefiable`: Rotten Flesh, Spider Eye, Poisonous Potato, mushrooms, crops, leaves) goes into the Cucurbit by hand or by item pipe. Humus comes out of the Bed Hatch, by an empty hand or by a pipe or hopper that pulls. Nothing automated inserts at the hatch. The work is slow and runs in big batches.
  - The recipe is a datapack recipe of type `servus_alchemy:putrefaction`: a counted ingredient, the result, the time in a warm bed and the time in a cold bed. Example: 8 rot give 1 Humus in 60 s in fresh straw, or 240 s in an empty bed.
  - The Cucurbit holds a large store of rot (machine stats, example 256 items) and the hatch holds one stack of Humus.
  - No power, no fuel, no GUI.
- Warmth: Hay Bales are the bed's warmth. Each Dung Bed takes Hay Bales by hand or by pipe. The bed holds one bale per frame block (eight). Straw rots down only while the bed works, one bale at a time.
  - The more straw, the faster the putrefaction, from the cold time with an empty bed to the warm time with a full one. An empty bed slows the work but never stops it.
  - Which items are bedding, and how long each one keeps the bed warm, is a data map `servus_alchemy:bedding` on items (example: Hay Bale, 6,000 work ticks).
- The look shows the state: the straw fill rises and falls with the straw left, an empty bed shows rotted litter, dung mats lie on top and steam rises while it works. Jade shows the rot, the straw, the warmth, the progress and the Humus.
- New item **Grass Seed**, crafted only on the Athanor at Nigredo (example: 1 Wheat Seeds, 20 Sulfur and 20 Mercury give 1 Grass Seed). Used on a Dirt block that grass could live on, it turns the block into a Grass Block and is used up. It is its own item, not the gnome vein seed `servus_alchemy:seed`, and the Calciner does not take it.
- Dirt from the Mixer: Alchemy ships two `servus_foundation:mixing` recipes (examples):
  - bulk: 4 Sand, 1 Clay Ball, 1 Humus and 250 mB Water give 8 Dirt;
  - quick: 2 Sand, 1 Clay Ball, 1 Bone Meal and 250 mB Water give 2 Dirt.
- The Venter Equinus reports a `putrefy` journal event with the Humus it made. No chapter task uses it yet.
- Journal: a Nigredo Workshop entry "The Venter Equinus" with the schematic, the crafting recipes, how to feed rot and straw, the putrefaction and Dirt recipe lines generated from the shipped data, and flavor text in Paracelsus' confused diary voice about the horse's belly. It does not reveal the homunculus. The Grass Seed appears among the Athanor's works through the generated lines.
- JEI: a "Putrefaction" category; the Grass Seed shows in the Athanor category, and the Dirt recipes in the Mixer's "Mixing" category from `m14-mixer`.

## Capabilities

### New Capabilities
- `alchemy/venter-equinus`: the Humus item, the putrefiable tag, the bedding data map, the putrefaction recipe type and the Venter Equinus build (layout, forming, warmth, ports, hand use, look, Jade, schematic, journal event, book list). It also covers the Dirt mixing recipes that Alchemy ships.

### Modified Capabilities
- `alchemy/athanor`: a new requirement for the Grass Seed, crafted on the Athanor at Nigredo, and what it does on Dirt.
- `alchemy/journal`: the Workshop tutorial lists the Venter Equinus entry, the forming effect covers the Venter Equinus, and a new requirement keeps the entry's horse's belly text in Paracelsus' voice without telling the homunculus secret.
- `alchemy/recipe-viewer`: a new requirement for the Putrefaction category.

## Impact

- Alchemy, new package `alchemy/venter`:
  - pure `VenterLayout`, `StrawBed`, `RotStore`, `PutrefactionWork` and `PutrefactionText`;
  - blocks and logic: `CucurbitBlock`, `DungBedBlock`, `BedHatchBlock`, `VenterFrameBlock`, `VenterStructure`, `VenterEquinusBlockEntity`, `PutrefactionRecipe` and `GrassSeedItem`.
- `structure/Facing`: an offset on the ground plane next to the existing front plane offset.
- Registries: `ServusAlchemyBlocks`, `ServusAlchemyItems`, `ServusAlchemyBlockEntities`, `ServusAlchemyRecipes`, the new `AlchemyDataMaps` (bedding), `AlchemyMachineDefaults` (`venter_equinus`), capability registration in `ServusAlchemyEvents`.
- Client: `VenterRenderer` (straw, litter, mats), registration in `ServusAlchemyClient`, steam in `CucurbitBlock.animateTick`.
- Datagen:
  - models in `AlchemyModels`, blockstates, crafting recipes, loot, tags;
  - the putrefaction, Grass Seed and mixing rows in `AlchemyRecipeTable`, written by `ServusAlchemyRecipeProvider`;
  - a new `ServusAlchemyDataMapProvider` for bedding, the schematic in `ServusAlchemyMultiblockProvider`, the Workshop entry in `WorkshopCategory`, and lang.
- Jade: a Venter provider in `ServusAlchemyJadePlugin`. JEI: `PutrefactionRecipeCategory` in `ServusAlchemyJeiPlugin`.
- Textures: `humus.png` and `grass_seed.png` from a generator script in `tools/venter-textures/gen.py`; the blocks use vanilla textures and the family steel.
- GameTests: the new `VenterGameTests`, and a Grass Seed test in `AlchemyGameTests`.
- `alchemy/CHANGELOG.md` gets its line at archive time. `alchemy_plan.md` (Nigredo products) and PLAN.md are groomed by the parent session.

## Non-goals

- Ordering Humus through the Crafting CPU (production capability) on the Venter Equinus; it may follow after play tests.
- A journal chapter task for `putrefy`, and any homunculus story. Paracelsus only hints that the horse's belly served "other work".
- Horse dung as an item, or horses feeding the bed.
- Fertilizer: the Coagulator already makes Bone Meal.
- Other soils (Podzol, Mycelium, Rooted Dirt) and more Grass Seed targets than plain Dirt.
- A dispenser behaviour for the Grass Seed.
- Docs pages: the docs site describes released versions only.

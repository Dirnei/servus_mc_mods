---
title: Recipes
description: The calcination, purification, coagulation, pressing and putrefaction recipe formats of Servus Alchemy and the milling and mixing recipe formats of Servus Foundation.
sidebar:
  order: 4
---

Servus Alchemy %alchemy_version% adds `servus_alchemy:calcination` for the Calciner, `servus_alchemy:purification` for the Purifier, `servus_alchemy:coagulation` for the Coagulator, `servus_alchemy:pressing` for the Prelum and `servus_alchemy:putrefaction` for the Venter Equinus. The Athanor's `servus_alchemy:athanor` recipes are on the [Athanor](/servus_mc_mods/reference/datapack/athanor/) page. Servus Foundation %foundation_version% adds `servus_foundation:milling` for the Mill and `servus_foundation:mixing` for the Mixer. Times and amounts on this page are the shipped values; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/recipe/<path>.json
```

These are ordinary Minecraft recipes, so they live in the normal `recipe` folder and are reloaded with `/reload`. The built in ones use subfolders named after their type, such as `calcination/` and `coagulation/`, which gives ids like `servus_alchemy:coagulation/salt_crystal`. Overriding a built in recipe works by using the same path.

## Principles

Recipes write principle amounts the same way: an object with `salt`, `sulfur` and `mercury` in mB. A missing key is 0.

```json
{"salt": 100, "mercury": 10}
```

## Calcination

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:calcination` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `output` | principles | required | What one cycle makes. Sulfur and Mercury go into the Calciner's Fluid Tanks; Salt collects in the Ash Pan as Ash. |
| `time` | int | 80 | Ticks per cycle. |
| `dimension` | string | `overworld` | `overworld`, `nether` or `end`: the group the recipe is listed under in the journal's Calciner table. It does not limit where the Calciner runs. |

An item without a calcination recipe is refused by the Calciner. The Ash Pan holds Salt in mB and gives it out as Ash: one Ash for the Salt that one Ash makes in the first purification recipe that takes Ash (100 mB with the shipped recipe, and 100 mB when no recipe takes Ash).

When several recipes take the same item, the most specific one wins: an input that names items beats a tag, an input that matches fewer items beats one that matches more, and the lower recipe id breaks a tie.

Built in calcination recipes (80 ticks each):

| Recipe | Input | Table | Salt | Sulfur | Mercury |
|---|---|---|---|---|---|
| `blaze_rod` | `#c:rods/blaze` | Nether | 0 | 400 | 0 |
| `bones` | `#c:bones` | Overworld | 100 | 0 | 0 |
| `chorus_fruit` | `minecraft:chorus_fruit` | End | 0 | 20 | 60 |
| `coal` | `#minecraft:coals` | Overworld | 0 | 200 | 0 |
| `cobblestone` | `#c:cobblestones` | Overworld | 100 | 0 | 10 |
| `crimson_stems` | `#minecraft:crimson_stems` | Nether | 0 | 150 | 0 |
| `crops` | `#c:crops` | Overworld | 10 | 30 | 30 |
| `dirt` | `#minecraft:dirt` | Overworld | 50 | 20 | 20 |
| `end_stone` | `#c:end_stones` | End | 40 | 0 | 40 |
| `ender_pearl` | `#c:ender_pearls` | End | 0 | 0 | 300 |
| `glowstone_dust` | `#c:dusts/glowstone` | Nether | 0 | 30 | 60 |
| `gravel` | `#c:gravels` | Overworld | 80 | 0 | 10 |
| `leaves` | `#minecraft:leaves` | Overworld | 0 | 40 | 20 |
| `logs` | `#minecraft:logs` | Overworld | 20 | 100 | 10 |
| `magma_cream` | `minecraft:magma_cream` | Nether | 0 | 80 | 40 |
| `nether_quartz` | `#c:gems/quartz` | Nether | 120 | 0 | 0 |
| `nether_wart` | `#c:crops/nether_wart` | Nether | 0 | 60 | 20 |
| `nether_wart_block` | `minecraft:nether_wart_block` | Nether | 0 | 540 | 180 |
| `netherrack` | `#c:netherracks` | Nether | 20 | 40 | 0 |
| `planks` | `#minecraft:planks` | Overworld | 5 | 25 | 0 |
| `popped_chorus_fruit` | `minecraft:popped_chorus_fruit` | End | 0 | 0 | 100 |
| `purpur_block` | `minecraft:purpur_block` | End | 50 | 0 | 20 |
| `raw_materials` | `#c:raw_materials` | Overworld | 150 | 0 | 50 |
| `redstone` | `#c:dusts/redstone` | Overworld | 0 | 0 | 100 |
| `sand` | `#c:sands` | Overworld | 80 | 0 | 10 |
| `saplings` | `#minecraft:saplings` | Overworld | 0 | 40 | 20 |
| `seeds` | `#c:seeds` | Overworld | 10 | 30 | 30 |
| `shulker_shell` | `minecraft:shulker_shell` | End | 200 | 0 | 80 |
| `soul_sand` | `#minecraft:soul_fire_base_blocks` | Nether | 20 | 60 | 0 |
| `stone` | `#c:stones` | Overworld | 100 | 0 | 10 |
| `warped_stems` | `#minecraft:warped_stems` | Nether | 0 | 150 | 0 |

## Purification

The Purifier washes an item with water into Salt and may leave a residue. It needs no power and no fuel.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:purification` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). |
| `water` | int | required | mB of water one cycle takes. |
| `salt` | int | required | mB of Salt one cycle makes. |
| `residue` | item stack | none | `id` and `count` (default 1) of what is left in the Residue Crock. |
| `time` | int | 40 | Ticks per cycle. |

Built in purification recipes:

| Recipe | Input | Water | Salt | Residue | Time |
|---|---|---|---|---|---|
| `ash` | 4 `servus_alchemy:ash` | 400 | 400 | 1 caput mortuum | 160 |
| `dried_kelp` | 4 `minecraft:dried_kelp` | 250 | 200 | none | 80 |
| `gunpowder` | 4 `minecraft:gunpowder` | 400 | 200 | 1 brimstone | 160 |
| `magma_cream` | 1 `minecraft:magma_cream` | 250 | 25 | 1 slime ball | 80 |
| `redstone` | 8 `minecraft:redstone` | 400 | 50 | 1 cinnabar | 160 |
| `rotten_flesh` | 4 `minecraft:rotten_flesh` | 250 | 100 | 1 leather | 120 |
| `soul_sand` | 1 `minecraft:soul_sand` | 250 | 25 | 1 sand | 80 |
| `sugar` | 8 `minecraft:sugar` | 500 | 100 | 1 kandis | 160 |

The Purifier also washes the colour out of 8 dyed Concrete Powder, Wool, Carpet or Terracotta at a time for 250 mB of water and no Salt (40 ticks): Concrete Powder sets into Concrete of its colour, and Wool, Carpet and Terracotta come out white or plain. These recipes are named `wash_<colour>_<block>`, such as `wash_red_wool`.

## Coagulation

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:coagulation` |
| `input` | principles | required | What one cycle takes from the Coagulator's tanks. |
| `ingredient` | sized ingredient | none | An item the cycle takes as well. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | 40 | Ticks per cycle. |
| `seed` | ingredient | the result item | The items in the Coagulator's seed slot that grow this recipe. |
| `chapter` | chapter id | none | A [journal chapter](/servus_mc_mods/reference/datapack/journal/) the team must have finished before a Coagulator grows this recipe. |

The item in the seed slot picks the recipe, and each cycle uses up one seed. A seed that is the recipe's own result comes back with the result, so the cycle gives one more; any other seed turns into the result. A seed item belongs to one recipe only: when two recipes name the same item, the recipe whose id sorts first keeps it and the server logs an error.

Built in coagulation recipes (40 ticks each, no item ingredient):

| Recipe | Salt | Sulfur | Mercury | Result | Seed |
|---|---|---|---|---|---|
| `bone_meal` | 60 | 20 | 0 | 1 bone meal | its result |
| `clay_ball` | 100 | 0 | 50 | 1 clay ball | its result |
| `flint` | 150 | 0 | 0 | 1 flint | its result |
| `glowstone_dust` | 0 | 100 | 100 | 1 glowstone dust | its result |
| `gunpowder` | 50 | 150 | 0 | 1 gunpowder | its result |
| `quicksilver` | 0 | 0 | 250 | 1 quicksilver | `servus_alchemy:cinnabar`, `servus_alchemy:quicksilver` |
| `redstone` | 50 | 0 | 150 | 1 redstone | its result |
| `salt_crystal` | 250 | 0 | 0 | 1 salt crystal | `servus_alchemy:kandis`, `servus_alchemy:salt_crystal` |
| `slime_ball` | 0 | 50 | 150 | 1 slime ball | its result |
| `sulfur` | 0 | 250 | 0 | 1 sulfur | `servus_alchemy:brimstone`, `servus_alchemy:sulfur` |

## Pressing

The Prelum runs `servus_alchemy:pressing` recipes. It needs no fuel and no principles.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:pressing` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). The Prelum presses once its input slot holds the count. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `time` | int | required | Ticks per pressing. |

An item without a pressing recipe is refused by the Prelum.

Built in pressing recipe:

| Recipe | Input | Result | Time |
|---|---|---|---|
| `netherite_scrap` | 16 `servus_alchemy:caput_mortuum` | 1 Netherite Scrap | 600 |

## Putrefaction

The Venter Equinus runs `servus_alchemy:putrefaction` recipes in its Cucurbit. It needs no power and no fuel, but the straw in its bed makes it faster.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_alchemy:putrefaction` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1): the rot one batch takes. |
| `result` | item stack | required | `id` and `count` (default 1). It lands in the Bed Hatch. |
| `time` | int | required | Ticks per batch in a bed full of fresh straw. At least 1. |
| `cold_time` | int | 4 x `time` | Ticks per batch in a bed without straw. It may not be below `time`. |

The speed lies between the two times, in step with the straw left in the bed. When the Cucurbit holds rot for several recipes, the recipe whose id sorts first among those with enough rot runs. What counts as straw is the [bedding data map](#bedding).

Built in putrefaction recipe:

| Recipe | Input | Result | Time | Cold time |
|---|---|---|---|---|
| `humus` | 8 `#servus_alchemy:putrefiable` | 1 Humus | 1200 | 4800 |

The tag `servus_alchemy:putrefiable` holds Rotten Flesh, Spider Eye, Poisonous Potato, Brown and Red Mushroom, the leaves (`#minecraft:leaves`) and the crops Beetroot, Cactus, Carrot, Cocoa Beans, Melon, Nether Wart, Potato, Pumpkin and Sugar Cane (their `c:crops/<crop>` tags). The Cucurbit takes only items in this tag.

### Bedding

The data map `servus_alchemy:bedding` names the items that warm the bed:

```
data/<namespace>/data_maps/item/bedding.json
```

```json
{
  "values": {
    "minecraft:hay_block": {"warmth": 6000}
  }
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `warmth` | int | required | Work ticks one item keeps the bed warm. At least 1. |

The bed holds up to eight bedding items, one per Dung Bed. While a batch runs, the oldest one rots down by one work tick per tick. Items without an entry are not bedding. Alchemy ships the Hay Bale with 6,000.

## Milling

The Mill runs `servus_foundation:milling` recipes. It has no power of its own: a Hand Crank or an Electric Motor gives it work, and a milling is done once it has collected the recipe's `work`.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_foundation:milling` |
| `input` | sized ingredient | required | One `item` or `tag` with a `count` (default 1). The Mill grinds once its input slot holds the count. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `work` | int | required | Work units one milling takes. A Hand Crank gives 25 per turn, an Electric Motor 5 per tick (see [Machine stats](/servus_mc_mods/reference/datapack/machines/)). |

An item without a milling recipe is refused by the Mill. Any mod or datapack can add milling recipes under `data/<namespace>/recipe/`.

Built in milling recipes (ids `servus_foundation:milling/<recipe>`):

| Recipe | Input | Result | Work |
|---|---|---|---|
| `amethyst_shard` | `minecraft:amethyst_block` | 4 amethyst shard | 200 |
| `blaze_powder` | `#c:rods/blaze` | 3 blaze powder | 200 |
| `bone_meal` | `#c:bones` | 4 bone meal | 100 |
| `charcoal_powder` | `minecraft:charcoal` | 2 charcoal powder | 100 |
| `clay_ball` | `minecraft:clay` | 4 clay ball | 100 |
| `cobbled_deepslate` | `minecraft:deepslate` | 1 cobbled deepslate | 100 |
| `cobblestone` | `minecraft:stone` | 1 cobblestone | 100 |
| `glowstone_dust` | `minecraft:glowstone` | 4 glowstone dust | 100 |
| `gravel` | `#c:cobblestones/normal` | 1 gravel | 200 |
| `gravel_from_deepslate` | `#c:cobblestones/deepslate` | 1 gravel | 200 |
| `quartz` | `minecraft:quartz_block` | 4 quartz | 200 |
| `red_sand` | `#c:sandstone/red_blocks` | 4 red sand | 200 |
| `sand` | `minecraft:gravel` | 1 sand | 200 |
| `sand_from_sandstone` | `#c:sandstone/uncolored_blocks` | 4 sand | 200 |
| `string` | `#minecraft:wool` | 4 string | 100 |
| `sugar` | `#c:crops/sugar_cane` | 2 sugar | 100 |

Flowers mill into their dye for 100 work: a small flower gives 2 dye, a tall flower and the pitcher plant 4. These recipes are named `<colour>_dye_from_<flower>`, such as `red_dye_from_poppy`.

## Mixing

The Mixer runs `servus_foundation:mixing` recipes. Like the Mill it has no power of its own: a Hand Crank or an Electric Motor gives it work.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `type` | string | required | `servus_foundation:mixing` |
| `ingredients` | list | required | 1 to 4 sized ingredients, each an `ingredient` (an item id or `#tag`) with a `count`. Shapeless. |
| `fluid` | sized fluid ingredient | none | An `ingredient` (a fluid id) with an `amount` in mB, drained from the Mixer's tank. |
| `result` | item stack | required | `id` and `count` (default 1). |
| `work` | int | required | Work units one mixing takes, as for milling. |

```json
{
  "type": "servus_foundation:mixing",
  "ingredients": [
    {"ingredient": "minecraft:dirt", "count": 1}
  ],
  "fluid": {"ingredient": "minecraft:water", "amount": 250},
  "result": {"id": "minecraft:mud"},
  "work": 100
}
```

A recipe matches only when every ingredient has an input slot of its own holding at least its count, and no input slot holds anything else. A recipe with a fluid needs that fluid in the tank in at least its amount; a recipe without one ignores the tank. When several recipes match, one with a fluid wins. The Mixer's input slots take only items some mixing recipe uses, and its tank only fluids some mixing recipe uses.

Built in mixing recipes of Foundation (ids `servus_foundation:mixing/<recipe>`):

| Recipe | Items | Fluid | Result | Work |
|---|---|---|---|---|
| `mud` | 1 `minecraft:dirt` | 250 mB water | 1 mud | 100 |
| `coarse_dirt` | 2 `minecraft:dirt`, 2 `minecraft:gravel` | none | 4 coarse dirt | 100 |
| `<colour>_concrete_powder` | 4 `minecraft:sand`, 4 `minecraft:gravel`, 1 dye of the colour | none | 8 concrete powder of the colour | 200 |

There is one concrete powder recipe for each of the 16 colours, such as `red_concrete_powder`.

Servus Alchemy ships two more (ids `servus_alchemy:mixing/<recipe>`):

| Recipe | Items | Fluid | Result | Work |
|---|---|---|---|---|
| `dirt_from_humus` | 4 `#c:sands`, 1 `minecraft:clay_ball`, 1 `servus_alchemy:humus` | 250 mB water | 8 dirt | 100 |
| `dirt_from_bone_meal` | 2 `#c:sands`, 1 `minecraft:clay_ball`, 1 `minecraft:bone_meal` | 250 mB water | 2 dirt | 100 |

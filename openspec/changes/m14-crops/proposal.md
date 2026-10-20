## Why

The Delicacies kitchen needs its own vegetables before any dish can be cooked (PLAN.md, M14 Delicacies, `m14-crops`): cabbage for Sauerkraut, red cabbage for the Red Cabbage side, onion, parsley and horseradish for the meat dishes and dumplings, and mustard seed for the Sweet Mustard of the Weisswurst. Delicacies brings its own crops instead of depending on Croptopia, and tags them with the common `c:` tags so other mods and packs can use them.

## What Changes

- Six crops in Servus: Delicacies, each a vanilla style age crop (eight ages, four looks) that grows on farmland, takes bone meal and is harvested by hand like wheat or carrots:
  - Cabbage and Red Cabbage, planted from Cabbage Seeds and Red Cabbage Seeds, harvested as a Cabbage or Red Cabbage head.
  - Parsley, planted from Parsley Seeds, harvested as Parsley.
  - Onion and Horseradish, planted from the vegetable itself like a vanilla carrot or potato.
  - Mustard, planted from Mustard Seeds and harvested as Mustard Seeds (the harvest used later for Sweet Mustard).
- Block loot tables like vanilla: an unripe crop gives back what planted it, a ripe crop gives its harvest plus extra seeds, raised by Fortune.
- First seeds come from breaking grass and from village house chests, through two global loot modifiers whose chances live in data.
- `c:crops/*`, `c:seeds/*`, `minecraft:crops`, `minecraft:maintains_farmland`, `minecraft:villager_plantable_seeds` and `minecraft:chicken_food` tags, compost values in the `neoforge:compostables` data map, `en_us` names, and the items in the Delicacies creative tab.
- Item sprites ported from the approved mockup (`kraut_head`, `zwiebel`, `petersilie`, `kren`) plus new Mustard Seeds and seed sprites, and four growth stage textures per crop, all drawn by `tools/delicacies-textures/gen.py`.
- The Shuttles Field Head and Harvester farm these crops without any Shuttles change, since they accept every age crop whose planting item is a block item of a `CropBlock`; Delicacies does not depend on Shuttles, so this is checked in the play check, not in a GameTest.

## Capabilities

### New Capabilities
- `delicacies/crops`: the six Delicacies crops, how they are planted, grow, drop, are found, tagged and composted.

### Modified Capabilities
- None. The Delicacies creative tab requirement of `delicacies/mod-setup` already says the tab lists every Delicacies item.

## Non-goals

- Food values: raw vegetables are not edible in this change (see design.md, Assumptions).
- Recipes that use the crops (Sauerkraut, Red Cabbage, Sweet Mustard and so on) belong to the later M14 changes.
- Wild crop patches in world generation.
- Barley and hops (`m15-crops`).

## Impact

- New: `delicacies/.../crop/` (crop catalogue, crop block), registry classes for blocks, datagen providers for block states, loot tables, tags, data maps and loot modifiers, GameTests, JUnit tests, textures.
- Changed: `ServusDelicaciesItems` (crop items and tab order), `ServusDelicacies` (block register), datagen and lang, `tools/delicacies-textures/gen.py`, `delicacies/build.gradle` if the unit tests need JUnit wiring.
- No Foundation, Alchemy or Shuttles source changes.

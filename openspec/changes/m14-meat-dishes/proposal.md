## Why

The Delicacies kitchen can boil, fry and bake, but it has no meat dishes yet (PLAN.md, Open: M14 Delicacies, `m14-meat-dishes`). The Bavarian classics are the reason to build the Tiled Stove: Schnitzel, Cordon Bleu, Weisswurst with Sweet Mustard, Leberkaese and Roast Pork. The bakery (`m14-bakery`) now gives Flour, Salt and Bread Crumbs, the crops (`m14-crops`) give onion, parsley and mustard seeds, so every ingredient these dishes need exists.

## What Changes

- **Cuts on the Cutting Board**: a raw Porkchop cuts into a thin Pork Cutlet, a Pork Cutlet into Minced Pork, raw Beef into Minced Beef.
- **Schnitzel**: a Pork Cutlet crafted with Flour, an Egg and Bread Crumbs into a Breaded Cutlet, fried in the Pan to a Schnitzel.
- **Ham**: a raw Porkchop with Salt and water simmered in the Pot into Ham.
- **Cordon Bleu**: a Pork Cutlet crafted with Ham, Cheese (any item in `c:foods/cheese`), Flour, an Egg and Bread Crumbs into a Raw Cordon Bleu, fried in the Pan. This change declares the `c:foods/cheese` tag without entries; the Cheese itself comes from `m14-sides`.
- **Weisswurst**: Minced Pork, Minced Beef, Parsley and Salt crafted into Sausage Meat, simmered in the Pot with water into Weisswurst. No casings.
- **Sweet Mustard**: the Foundation Mill grinds Mustard Seeds into Mustard Flour (a `servus_foundation:milling` recipe in Delicacies data); the Pot cooks Mustard Flour, Sugar and water into Sweet Mustard, a jar item.
- **Leberkaese**: Minced Pork, Minced Beef and Salt baked in the Baking Oven into a Leberkaese loaf. The loaf is a placeable block; a right click with a knife cuts one Leberkaese Slice off it into the player's inventory until it is gone. The Cutting Board also cuts a loaf item into slices, for automation. A slice and a Bread Roll craft into a Leberkaese Roll.
- **Roast Pork**: raw Porkchops with an Onion and Salt roasted in the Baking Oven into Roast Pork with its crackling; **Gravy** (Chopped Onion, Flour and a Bowl with water in the Pot) is a bowl food that gives the bowl back.
- Food values for every dish in the `servus_delicacies:food` data map, above vanilla cooked meat for the finished dishes. Tags: `c:foods`, `c:foods/cooked_meat`, `c:foods/soup` (Gravy), `c:flours/mustard`, `c:foods/cheese`.
- JEI shows the new recipes in the existing Pot, Pan, Baking Oven, Cutting Board and milling categories, and an info page explains slicing the placed loaf.
- Textures in `tools/delicacies-textures/gen.py`: ported mockup sprites (`weisswurst`, `senf`, `leberkas`, `leberkassemmel`, `schnitzel`, `cordon_bleu`, `schweinebraten`) and new ones for the raw cuts, the minced meat, the breaded cutlets, Ham, Sausage Meat, Mustard Flour, the slice, Gravy and the loaf block faces.

## Capabilities

### New Capabilities
- `delicacies/meat-dishes`: the meat cuts, Schnitzel, Cordon Bleu, Ham, Weisswurst, Sweet Mustard, the sliceable Leberkaese loaf, Roast Pork and Gravy, with their recipes and food values.

### Modified Capabilities
None.

## Non-goals

- Cheese and the Fermenting Crock (`m14-sides`), plated meals and the Weisswurst breakfast (`m14-teller`); Sweet Mustard is made here and served there.
- Sausage casings, a meat grinder block, a pork shoulder item, lemons.
- Eating the placed loaf by hand like a cake; only a knife cuts it.
- Changes to Foundation, Brewery, the stove or the vessels' code.

## Impact

- New Delicacies code: package `meat` with `LoafSlices` (pure slicing logic) and `LeberkaeseBlock`; `kitchen/KitchenTags` gains the cheese and mustard flour tags.
- Changed (appended blocks): item and block registries, `ServusDelicaciesBlockLoot`, block state provider (calls a new `MeatModels`), block and item tags, recipe provider, data map provider, lang, JEI plugin, GameTests (`MeatGameTests`), `tools/delicacies-textures/gen.py`.
- New JUnit test `LoafSlicesTest`.
- No Foundation, Alchemy, Shuttles or Brewery source changes.

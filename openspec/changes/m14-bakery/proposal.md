## Why

The Delicacies kitchen can boil and fry but not bake (PLAN.md, Open: M14 Delicacies, `m14-bakery`). The Pretzel, the mod's emblem, is still a creative only item that cannot be eaten, and later dishes need bakery goods: the Schnitzel needs Bread Crumbs (`m14-meat-dishes`), the Bread Dumpling needs Bread Rolls (`m14-sides`), the Weisswurst breakfast needs the Pretzel (`m14-teller`). The Foundation Mill (`m14-mill`) and the Tiled Stove (`m14-tiled-stove`) now exist, so flour can be milled and dough can be baked.

## What Changes

- **Flour**: a `servus_foundation:milling` recipe in Delicacies' data grinds one Wheat into one Flour on the Foundation Mill.
- **Dough**: crafted shapeless from three Flour and a Water Bucket (the bucket comes back) into three Dough.
- **Baking Oven**: a new cooking vessel, a small steel box with an iron door and a window, that stands on a stove spot like the Pot and the Pan and bakes data driven `servus_delicacies:baking` recipes only while the firebox below is hot. Its window glows and it gives a little light while it bakes. By hand and through `Capabilities.Item` like the other vessels. The Tiled Stove and the firebox code stay unchanged; the oven asks the existing firebox capability.
- **Salt**: the Pot boils 1000 mB of water with nothing else in it down to one Salt. Salt is in `c:dusts/salt`, and recipes ask for that tag, so salt from other mods works too.
- **Wood Ash and Lye**: logs burned on a vanilla Campfire or Charcoal smelted in a Furnace give Wood Ash (`c:dusts/ash`, which also lists the Alchemy Ash when Alchemy is installed); the Pot makes Lye from one ash and 250 mB of water.
- **Foods**: Bread Roll (one Dough baked), Pretzel (two Dough with Lye and Salt baked into two Pretzels; the existing `pretzel` item becomes edible and gets its recipe), Bread Crumbs (a Bread Roll cut on the Cutting Board). Food values for Bread Roll and Pretzel in the `servus_delicacies:food` data map.
- Tags: Flour in `c:flours/wheat`, Dough in `c:foods/dough/wheat`, Bread Roll and Pretzel in `c:foods/bread`, Salt and Wood Ash in `c:dusts/*`.
- JEI: a Baking category with the Baking Oven and the Stove Tile as catalysts; flour shows in Foundation's milling category.
- Textures in `tools/delicacies-textures/gen.py`: Bread Roll (mockup `semmel`), Flour (mockup `mehl`), new Dough, Lye, Salt, Wood Ash and Bread Crumbs sprites, the oven door cold and lit. The Pretzel keeps its approved sprite. The oven body uses the Foundation steel texture.

## Capabilities

### New Capabilities
- `delicacies/bakery`: flour, dough, salt, ash and lye, the Baking Oven with the `servus_delicacies:baking` recipe type, and the baked goods with their food values.

### Modified Capabilities
- `delicacies/mod-setup`: the Pretzel requirement no longer says it is creative only and not edible; it now has a recipe and food values.

## Non-goals

- The Heating Element (`m14-heating-element`) and any change to the Tiled Stove, its firebox or its fuel (wood ash from the firebox would need stove code).
- A farmhouse loaf; vanilla Bread stays the big bread.
- Lye or brine as fluids; Lye is an item.
- Schnitzel, dumplings and plated meals; they only use what this change makes.
- A GUI for the oven, or showing the bakes inside it.

## Impact

- New Delicacies code: `cooking/BakingRecipe`, `cooking/BakingOvenBlock`, `cooking/BakingOvenBlockEntity`, parts in `cooking/VesselParts`; `kitchen/KitchenTags` gains the salt, ash, flour and dough tags.
- Changed: `cooking/VesselInput` (a fluid alone is not an empty input), `ServusDelicaciesRecipes` (baking type, serializer, sync), block, block entity and item registries, `ServusDelicaciesEvents` (oven item capability), datagen (models, block states, recipes including milling and vanilla cooking, tags, data map, loot, lang), JEI plugin, GameTests, `SlotMatcherTest`, `tools/delicacies-textures/gen.py`.
- The `pretzel` item turns from a plain item into a `DelicaciesFoodItem`.
- No Foundation, Alchemy, Shuttles or Brewery source changes. No change to `stove/` or `firebox/` sources.

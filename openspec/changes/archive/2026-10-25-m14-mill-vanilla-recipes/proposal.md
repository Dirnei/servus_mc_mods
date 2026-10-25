# Proposal

## Why

The Mill ships one recipe of its own, Gravel into Sand, so outside the Delicacies flour and the Brewery grist it has little to do. The explore session of 2026-10-25 decided that Foundation should give it a set of vanilla milling recipes. With them the Mill is worth building on day one in any pack, and every recipe can be automated with a crank or a motor. This is a follow-up to the `m14-mill` item of the M14 list in `PLAN.md` (Foundation part). It is not a listed open point, so this change adds it to M14 and removes it again when it is archived.

## What Changes

- Foundation ships these vanilla milling recipes next to Gravel into Sand. Counts and work are example values in datapack JSON:
  - Stone into 1 Cobblestone, Deepslate into 1 Cobbled Deepslate.
  - Cobblestone into 1 Gravel, Cobbled Deepslate into 1 Gravel.
  - Sandstone into 4 Sand, Red Sandstone into 4 Red Sand.
  - Sugar Cane into 2 Sugar.
  - Glowstone into 4 Glowstone Dust.
  - Bone into 4 Bone Meal.
  - Blaze Rod into 3 Blaze Powder.
  - Any Wool (`#minecraft:wool`) into 4 String.
  - Clay into 4 Clay Ball.
  - Block of Quartz into 4 Nether Quartz.
  - Block of Amethyst into 4 Amethyst Shard.
  - Every flower that vanilla crafts into a dye gives twice the vanilla dye count. The Golden Dandelion is left out because it is crafted from gold.
- No two shipped milling recipes take the same item, so the first-match lookup stays unambiguous.
- The Mill spec's examples that assumed Cobblestone has no milling recipe now use other items.
- Docs list the new built-in recipes. The Mill's line on the Foundation page names a few of them.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `foundation/mill`: the "Milling recipes" requirement ships the vanilla recipe set instead of only Gravel into Sand. The "Mill block" requirement's wrong-item scenario uses an item without a recipe.

## Non-goals

- Ores into raw ores, and gems (diamond, emerald, lapis, redstone, coal, quartz ore). This change takes no position on ore doubling.
- Chance outputs or byproducts on milling recipes (Gravel into Flint, a bonus Sugar). The recipe type stays one result with a fixed count.
- New items such as a coal dust or anything milled from Netherrack.
- Milling recipes for modded blocks.
- Wheat, mustard and malt recipes, which stay in Delicacies and Brewery.

## Impact

- Datagen: milling recipes in Foundation (`MachineDataParts` or a small helper beside it), generated into `foundation/src/generated/resources/data/servus_foundation/recipe/milling/`.
- GameTests: `MillGameTests` checks a few of the shipped recipes.
- Docs: `docs/src/content/docs/reference/datapack/recipes.md` (built-in milling table), `docs/src/content/docs/mods/foundation.md` (Mill line); `docs/src/data/gamedata.json` refreshed by `runData`.
- `foundation/CHANGELOG.md` under Unreleased; `PLAN.md` M14 gets the item while the change is open.
- No code changes to the Mill, the recipe type or JEI. The Milling category lists the new recipes as it is.

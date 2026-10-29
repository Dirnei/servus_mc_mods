# Proposal

## Why

The Delicacies aquarium (`m14-aquarium`) cleans its water with a filter that wears out, and another planned Servus mod needs the same filter. A filter that only Delicacies owns would force that mod to depend on Delicacies, so the filter, its materials and a small API to wear it belong in Foundation, which every Servus mod already depends on.

PLAN.md has no item for it yet: it comes from the fish tank explore of 2026-10-29 and joins the M14 Delicacies list as the Foundation part before `m14-aquarium`, like `m14-heating-element`. The PLAN.md line is added at archive.

## What Changes

- New item **Charcoal Powder**: the Mill grinds 1 Charcoal into Charcoal Powder (shipped milling recipe).
- New item **Activated Charcoal**: the vanilla Blast Furnace turns Charcoal Powder into Activated Charcoal (shipped blasting recipe).
- New item **Carbon Filter**: crafted from Activated Charcoal, a starch binder (wheat, or any flour from the `c:flours` tag) and paper. It has a number of cleaning points; its wear shows as a durability bar and in the tooltip. Fresh filters stack.
- New item **Spent Filter**: what a Carbon Filter becomes when its points are used up. It composts in the vanilla Composter, like nutrient laden biochar.
- **Filter API**: any mod can wear a filter by a number of points and learn how many were spent; a worn out filter turns into its spent item. Which items are filters, their points and their spent item come from a Foundation data map, so a datapack or another mod can add its own filters.
- All four items in the Servus creative tab, with lang, models and textures; JEI shows the recipes through the existing milling category and vanilla blasting and crafting.

## Non-goals

- The aquarium and any other block that holds a filter (`m14-aquarium` and the planned mod).
- A filter slot type, filter GUI widget or filter upkeep rules: each holder decides how it stocks filters (for example by pipe or by the existing upkeep capability).
- Cleaning or washing a Spent Filter back into a Carbon Filter.
- Other filter kinds (sand, wool) beyond what a datapack adds through the data map.
- A Foundation machine that activates charcoal: activation uses the vanilla Blast Furnace.

## Capabilities

### New Capabilities
- `foundation/carbon-filter`: the charcoal chain (Charcoal Powder, Activated Charcoal), the Carbon Filter and Spent Filter items, the data driven filter definition, wearing a filter through the API, and composting the spent filter.

### Modified Capabilities
None. The milling recipe for Charcoal is shipped data; the Mill's requirements do not change.

## Impact

- Foundation code: new package `filter` (pure wear logic, the filter data map, the API), items and a wear data component in `registry/ServusFoundationItems` and `registry/ServusFoundationComponents`, creative tab entries in `registry/ServusFoundationCreativeTabs`.
- Datagen: milling recipe in `datagen/MillingRecipes`, blasting and crafting recipes in `datagen/ServusFoundationRecipeProvider`, the binder item tag in `datagen/ServusFoundationItemTagsProvider`, a new Foundation data map provider (filter definition and compostable), item models and lang.
- Four new 16x16 item textures.
- Used by `m14-aquarium`; Delicacies then depends on this change.
- Foundation `CHANGELOG.md` gets an Added line at archive.

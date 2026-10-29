# Design

## Context

Foundation already has the Mill with the data driven `servus_foundation:milling` recipe type (`datagen/MillingRecipes` writes the shipped recipes), a shared creative tab (`registry/ServusFoundationCreativeTabs`), item and component registries, and datagen providers wired in `datagen/ServusFoundationDataGen`. It has no data map provider yet; Delicacies has one (`ServusDelicaciesDataMapProvider`) and registers its own data map types in `food/DelicaciesDataMaps`, which is the pattern copied here. Delicacies tags its flours as `c:flours`.

Sources checked: `minecraft-patched-26.1.2.116-sources.jar` (`Item`, `SimpleCookingRecipeBuilder`) and `neoforge-26.1.2.116-sources.jar` (`DataMapType`, `NeoForgeDataMaps`, `Compostable`, `Tags`) from the Gradle cache.

## Goals / Non-Goals

**Goals:**
- One filter item and one API every Servus mod can use without depending on any mod but Foundation.
- Filter points and compost chance as data, changeable by datapacks.

**Non-Goals:**
- Holder blocks, slot rules or upkeep for filters (decided by each holder).
- Filtering fluids itself: the API counts points; what a point means is up to the holder.

## Decisions

### PLAN.md decisions settled
None open in PLAN.md; this adds a new M14 Foundation item (see proposal). Settled in the explore of 2026-10-29: the filter lives in Foundation because a second planned mod needs it; activation in the vanilla Blast Furnace instead of a new machine; wheat as the starch binder; the spent filter composts.

### Filter definition as an item data map
`filter/FilterDataMaps.FILTER` is a `DataMapType<Item, FilterSpec>` built with `DataMapType.builder(Identifier.fromNamespaceAndPath("servus_foundation", "filter"), Registries.ITEM, FilterSpec.CODEC).synced(FilterSpec.CODEC, false)` and registered on `RegisterDataMapTypesEvent` (verified in `DataMapType` lines 85 and 152; same as `DelicaciesDataMaps`). `FilterSpec` is a record `(int points, Item spent)` with a `RecordCodecBuilder` codec (`points` positive int, `spent` by item registry id). Lookup through `stack.typeHolder().getData(FILTER)` as Delicacies does. Synced so the client draws the bar and tooltip from the same points.

Alternative: vanilla max damage via `Item.Properties.durability`. Rejected: the number would live in code, vanilla damaged items stack to 1, and only Foundation items could be filters.

### Wear on the stack
A data component `servus_foundation:filter_used` (`Integer`, persistent `Codec.INT`, network `ByteBufCodecs.VAR_INT`) in `registry/ServusFoundationComponents` holds the used points. Missing means fresh, so fresh filters stack (Carbon Filter `stacksTo(16)`) and used ones differ by component and do not stack with them.

The bar uses the plain `Item` overrides `isBarVisible`, `getBarWidth` and `getBarColor` (verified in `Item`, lines 237 to 245; `MAX_BAR_WIDTH` 13) in `filter/FilterItem`, and the tooltip uses `appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` (line 342). `FilterItem` reads the spec from the data map, so a datapack filter on a vanilla item (Wool) gets the wear and works with the API but shows no bar; that is accepted, the tooltip and bar are for the Carbon Filter.

### API
`filter/Filters` (static, server side callers):
- `WearResult wear(ItemStack stack, int points)`: a record of `int spent` and `ItemStack stack`. For an empty stack, a non-filter or a count above 1 it returns spent 0 and the stack unchanged. Otherwise `FilterWear` decides the points to spend, the used value is written to a copy, and when worn out the result stack is one of the spec's spent item. Holders store `result.stack()` back into their slot; the input is never mutated, so a holder cannot forget half of the swap.
- `int pointsLeft(ItemStack stack)` and `boolean isFilter(ItemStack stack)` for GUIs and slot checks.

The spec's "returns how many points were spent" is `WearResult.spent`.

### Pure logic
`filter/FilterWear` (no Minecraft imports): `record FilterWear(int points, int used)` with `Spend spend(int requested)` returning spent points, the new used value and whether it is worn out; `int left()`; `int barWidth(int maxWidth)`. JUnit `FilterWearTest`.

### Recipes and tags (datagen)
- Milling: `MillingRecipes` gains Charcoal (item `minecraft:charcoal`, not the `minecraft:coals` tag) to 2 Charcoal Powder, work 100.
- Blasting: `SimpleCookingRecipeBuilder.blasting(Ingredient, RecipeCategory, CookingBookCategory, ItemStackTemplate, float, int)` (verified line 111) in `ServusFoundationRecipeProvider`, Charcoal Powder to Activated Charcoal, 0.1 experience, 100 ticks (blast furnace half time), saved as `servus_foundation:activated_charcoal_from_blasting` so it does not land in the `minecraft` namespace.
- Crafting: shapeless 2 Activated Charcoal, 1 `#servus_foundation:filter_binders`, 1 Paper to 1 Carbon Filter.
- Tag `servus_foundation:filter_binders` in `ServusFoundationItemTagsProvider`: `addTag(Tags.Items.CROPS_WHEAT)` (verified `c:crops/wheat`, Tags line 462) and `addOptionalTag` for `c:flours` (no NeoForge constant; Delicacies writes it), so the tag loads without Delicacies. Both tag keys live in `filter/FilterTags`.

### Data maps provider
New `datagen/ServusFoundationDataMapProvider` (`net.neoforged.neoforge.common.data.DataMapProvider`, as Delicacies) writes `FILTER` for the Carbon Filter (512 points, Spent Filter) and `NeoForgeDataMaps.COMPOSTABLES` for the Spent Filter (`new Compostable(0.65F)`; record `Compostable(float chance, boolean canVillagerCompost)`, verified). Registered in `ServusFoundationDataGen`.

### Items and look
Items in `registry/ServusFoundationItems`: `charcoal_powder`, `activated_charcoal`, `carbon_filter` (`FilterItem`, stacks to 16), `spent_filter`. Four flat 16x16 item textures: grey black powder pile, dark glittering granules, a round cartridge with a copper band and dark core, the same cartridge brown and dull. Generated item models and English lang.

### Logic placement
Pure: `FilterWear` (JUnit). Minecraft: data map, component, item, API (GameTests: wear through the API, the spent swap, a non-filter, a stack of several, the data map entry loaded).

## Risks / Trade-offs

- [A datapack filter on a stackable vanilla item gets used values that split stacks] -> the API refuses stacks above one, so holders keep one filter per slot; documented on `Filters`.
- [The holder forgets to store the returned stack] -> the input is never mutated, so nothing changes until it is stored; GameTest covers the swap.
- [Spent Filter compost chance makes filters a bone meal farm] -> chance is data and a filter costs charcoal, wheat and paper.

## Open Questions

None.

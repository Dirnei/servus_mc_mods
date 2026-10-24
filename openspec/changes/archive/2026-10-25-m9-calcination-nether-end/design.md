# Design

## Context

`CalcinerBlockEntity.currentRecipe()` asks `ServusRecipes.first(level, CALCINATION, new SingleRecipeInput(stack))`, which is `RecipeMap.getRecipesFor(...).findFirst()`. `RecipeMap` keeps recipes per type in the order they were loaded; NeoForge 26.1.2 only reorders them on the server by the optional `neoforge:recipe_priorities.json` (`RecipePriorityManager`, `RecipeMap.order`). The client gets the calcination recipes through `ServusRecipes.syncToClients` (NeoForge `OnDatapackSyncEvent.sendRecipes`) in their sync order, without that reordering. So the server and the client can disagree on "the first", and both depend on load order.

`currentRecipe()` runs several times per tick (`canWork`, `workTicks`, `finishWork`). `calcinable()` tests every recipe's `accepts`. The JEI category fills its input slot with `ServusRecipes.items(recipe.input())`, every item of the ingredient.

Overlaps today, verified against the NeoForge 26.1.2.116 and vanilla 26.1.2 tag files: `#c:crops` contains `#c:crops/nether_wart`, `#minecraft:logs` contains `#minecraft:crimson_stems` and `#minecraft:warped_stems`. Every tag the new recipes use exists: `c:netherracks`, `minecraft:soul_fire_base_blocks` (item tag: Soul Sand, Soul Soil), `c:crops/nether_wart`, `minecraft:crimson_stems`, `minecraft:warped_stems`, `c:rods/blaze`, `c:dusts/glowstone`, `c:gems/quartz`, `c:end_stones`, `c:ender_pearls`.

## Goals / Non-Goals

**Goals:**
- One rule for "which recipe", computed the same on server and client, used by the Calciner, JEI and the journal.
- No per tick cost beyond what matching costs today.

**Non-Goals:**
- A generic Foundation mechanism for every recipe type; only calcination overlaps.

## Decisions

**Specificity by kind, then by matched item count, then by id.** Each matching recipe gets a key:
1. kind: an ingredient that names items (a direct item list or a NeoForge custom ingredient) ranks before a tag ingredient (`!ingredient.isCustom()` and `ingredient.getValues().unwrapKey()` present);
2. the number of items the ingredient matches (`ingredient.items().count()`), fewer first;
3. the recipe id as a string, lower first.

The first key wins. A tag that is a strict subset of another has fewer items, so step 2 covers the subset rule without a set comparison. Two tags that only overlap fall to step 2 as well, which is deterministic and close to "more specific". Custom ingredients count as named items because a simple custom ingredient reaches the client as a direct item list (`Ingredient.getValuesForSync`), so the kind must not differ between the sides. Alternatives: compare the item sets for subset (needs the sets, and gives no answer for partly overlapping tags); honour `recipe_priorities.json` (server only, so the client would disagree, and it needs a hand written entry per overlap); an explicit `priority` field on the recipe (one more number for pack makers to keep right; the automatic rule handles every shipped case).

**Pure ranking.** `alchemy/calciner/RecipeSpecificity` holds a record `Rank(boolean tag, int items, String id)` with its comparator and `best(List<Candidate<T>>)`; no Minecraft imports, JUnit in `RecipeSpecificityTest`: item beats tag, a subset tag beats its superset, equal counts fall to the id, the result is the same for every order of the input list, an empty list gives nothing.

**Lookup helper with a cache per recipe map.** `alchemy/calciner/CalcinationRecipes` (Minecraft side):
- `forItem(Level, ItemStack)`: every calcination recipe whose ingredient accepts the stack (count ignored), ranked, best one. Matching stays as today; only the ranks are cached.
- `burns(Level, RecipeHolder)`: the items of the recipe's ingredient for which `forItem` picks this recipe; JEI uses it, the journal table in `m9-calcination-table` too.
- Ranks are cached per recipe id in a `WeakHashMap<RecipeMap, Map<ResourceKey<Recipe<?>>, Rank>>` behind `synchronized` (server and client thread both ask in single player). A `/reload` and every client recipe sync make a new `RecipeMap`, so the cache follows the active recipes and tags without an invalidation hook; tags reload together with recipes.
- Foundation `ServusRecipes` gets a public `recipeMap(Level)` (the private `recipes(Level)` renamed), so the cache can key on it.

The Calciner's `currentRecipe()` and its `calcinable()` keep their contracts: `currentRecipe()` returns the winner when the stack holds at least its count, else nothing (the machine waits, it never falls back to a less specific recipe); `calcinable()` stays "any recipe accepts it".

**JEI.** `CalcinationRecipeCategory.setRecipe` fills the input slot with `CalcinationRecipes.burns(...)` at the recipe's count. `ServusAlchemyJeiPlugin.registerRecipes` drops calcination recipes whose `burns` is empty, since JEI cannot show a slot without items. Both run on the client level JEI registers with.

**The `dimension` field.** `CalcinationRecipe` gets a fourth component `CalcinationDimension dimension`, a pure enum (`OVERWORLD`, `NETHER`, `END`, each with its lower case id), read with `Codec.STRING.comapFlatMap` as `optionalFieldOf("dimension", OVERWORLD)`, so the existing JSON stays valid and an overworld recipe writes no field. An unknown name is a codec error: the recipe manager logs it and skips that recipe. The stream codec carries it as a var int ordinal, so the client knows it for the journal. It is in this change because the new recipe rows are written here; the journal reads it in `m9-calcination-table`. Alternatives: a tag per dimension or a group name in the book data (the book would drift from the recipes and datapack recipes could not say where they belong); a free string (the book needs a fixed set of headings).

**Recipe rows.** `AlchemyRecipeTable.Calcine` gets the dimension; existing rows are overworld. New rows (Principles are Salt, Sulfur, Mercury):

| id | input | Salt | Sulfur | Mercury | dimension |
|---|---|---|---|---|---|
| netherrack | `#c:netherracks` | 20 | 40 | 0 | nether |
| soul_sand | `#minecraft:soul_fire_base_blocks` | 20 | 60 | 0 | nether |
| nether_wart | `#c:crops/nether_wart` | 0 | 60 | 20 | nether |
| nether_wart_block | `minecraft:nether_wart_block` | 0 | 540 | 180 | nether |
| crimson_stems | `#minecraft:crimson_stems` | 0 | 150 | 0 | nether |
| warped_stems | `#minecraft:warped_stems` | 0 | 150 | 0 | nether |
| blaze_rod | `#c:rods/blaze` | 0 | 400 | 0 | nether |
| magma_cream | `minecraft:magma_cream` | 0 | 80 | 40 | nether |
| glowstone_dust | `#c:dusts/glowstone` | 0 | 30 | 60 | nether |
| nether_quartz | `#c:gems/quartz` | 120 | 0 | 0 | nether |
| end_stone | `#c:end_stones` | 40 | 0 | 40 | end |
| purpur_block | `minecraft:purpur_block` | 50 | 0 | 20 | end |
| chorus_fruit | `minecraft:chorus_fruit` | 0 | 20 | 60 | end |
| popped_chorus_fruit | `minecraft:popped_chorus_fruit` | 0 | 0 | 100 | end |
| ender_pearl | `#c:ender_pearls` | 0 | 0 | 300 | end |
| shulker_shell | `minecraft:shulker_shell` | 200 | 0 | 80 | end |

The Nether Wart Block gives nine times Nether Wart, since it is crafted from nine.

Dragon's Breath is in none of these tags and gets no row. The book's "What Burns" list grows with these rows until `m9-calcination-table` replaces it.

**Verification.** GameTests in `CalcinerGameTests`: `netherWartBurnsWithItsOwnRecipe` (one Nether Wart gives 60 Sulfur and 20 Mercury, not the Crops output), `netherWartBlockBurnsIntoNineWarts` (540 Sulfur and 180 Mercury), `crimsonStemBurnsAsAStem` (150 Sulfur), `dragonsBreathIsNotCalcinable`, and a lookup test that `CalcinationRecipes.burns` of the Crops recipe holds wheat and no Nether Wart.

## Risks / Trade-offs

- [A datapack wants a general recipe to beat a specific one] → it can name the items directly or replace the specific recipe; documented in the spec as the rule.
- [Counting a large tag costs a pass over it] → done once per recipe and recipe map, cached.
- [Partly overlapping tags pick by size, which may surprise] → deterministic and the same in JEI, so the player sees what happens.

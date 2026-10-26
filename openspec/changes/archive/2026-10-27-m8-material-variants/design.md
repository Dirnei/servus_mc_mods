# Design

## Context

See proposal.md for the why. State after `m8-pattern-variants` and `m8-shared-recipe-grid`:

- `pattern/PatternVariants.of(level, pattern)` tests every item of the recipe's `placementInfo().ingredients()` in all cells holding the same item; a different assembled result sets `Verdict.CHANGES_RESULT` and forbids Any. Candidates are plain items (`new ItemStack(holder)`), so a Rail with a material never gets other Rails as candidates.
- `PatternLibrary.Entry.variant(level, stock)` gives one variant per Any pattern (`VariantChoice.pick`), and `PatternRunners.withPatterns` adds one planner recipe per pattern. `OrderPlanner` plans by exact `ItemKey` (item plus component patch), so a spruce rack and an oak rack are different keys.
- `EncodedPattern.withKeys` keeps the stored result, which is right only while the result does not change. `OrderBook.runs` and `EncodedPattern.duplicates` compare Any patterns by `sameResult` (item, components and count).
- `CpuBlockEntity.patternFor` resolves a step's pattern again from `OrderStep.inputStock()`; `OrderBook.StepKey` includes the inputs, so several variants of one pattern are distinct steps.
- `terminal/TerminalCraftables.build` lists one stack per distinct first output `ItemKey` of the producers' recipes, with patterns resolved without stock.
- The rack's wood comes from `rack/HeavyRackRecipe.assemble` (`RackWood.pick` over the planks, component `RACK_WOOD`), the Elevator's material from `foundation/material/MaterialRecipe.assemble` (`MaterialPick` over block items of the kind's tag and the `MaterialContent.MATERIAL` of inputs such as a Rail).

All three delta specs modify requirements that reach the main specs when `m8-pattern-variants` and `m8-shared-recipe-grid` are archived; `openspec validate` reports them as info until then.

## Goals / Non-Goals

**Goals:**
- The planner stays exact: a material pattern becomes several exact planner recipes, so specific requests, "Exact stays fixed below" and one variant per step follow from the planner as it is.
- A generic order is turned into a specific one before planning, by the same most stocked rule as Any patterns.

**Non-Goals:**
- No new pattern mode, no new saved data, no change to `OrderPlanner`, the Crafting Station or `RackWood` / `MaterialPick`.

## Decisions

### PLAN.md
No open decision of PLAN.md is settled. It implements the non-goal of `m8-pattern-variants`; at archive nothing is removed from PLAN.md.

### Order of work
Applied after `m8-shared-recipe-grid`: it changes `PatternGridClient` and the mode rules that change introduces. Both were applied, at the user's request, while `m8-pattern-variants` and `m8-terminal-tabs` were still unarchived.

### Material verdict instead of a third mode
`PatternVariants.Verdict` gains `MATERIAL`: some candidate changes the result, but every changed result has the pattern's result item and count (`ItemStack.isSameItem` and count). `CHANGES_RESULT` stays for another item or count, `NO_ALTERNATIVES` as today. `possible()` is true for `POSSIBLE` and `MATERIAL`. `PatternVariants` also records `follows`: the group (ingredient) whose candidates change the result, for the tooltip ("Wood follows the planks" from the group's tag name, else "Material follows <item>").
Alternative: a third mode "Material" on the button. Rejected: the user wants JEI + to just work, and the button stays a plain Exact / Any switch.

### Candidates of component cells
For a cell whose own stack has a non-empty component patch (the Rail), the static candidates are only its own key. At planning, `PatternRunners` adds the keys of the planner's stock and of the line's recipe outputs whose item the cell's ingredient accepts (`Ingredient.test`). Outputs come from a first pass over the line's other recipes and non-material patterns, then material patterns are expanded in a second pass that also sees the first pass's material outputs (Rails made by an Any Rail pattern). Two passes cover the chains we have (planks to Rail to Elevator); deeper chains of material patterns resolve one level per pass, see Risks.

### Expansion: one planner recipe per variant
New pure `pattern/VariantExpansion` (no Minecraft imports): from `VariantChoice` groups, each with its candidate keys, and a stock map, it lists the variants (one key per group) ordered by how much stock they use, capped at `MAX_VARIANTS` (32). Groups that do not change the result keep `VariantChoice.pick` (one key each), so only the material group multiplies. JUnit `VariantExpansionTest`: `oneGroupGivesOneVariantPerCandidate`, `stockedVariantsFirst`, `capKeepsTheMostStocked`, `ownVariantAlwaysIncluded`, `nonMaterialGroupsArePicked`, `exactPatternGivesItself`.
`PatternLibrary.Entry.variants(level, stock, made)` returns the variant patterns; each is `pattern.withKeys(keys).assembled(level)` (re-assembled result) and is dropped when the recipe refuses it. `PatternRunners.withPatterns` adds one `ProductionRecipe` per variant; the existing `reports` check removes a variant an Exact pattern of the same recipe and inputs already adds.

### Generic orders
The generic key of a material pattern is its result item with the components that differ between its variants removed (`DataComponentPatch` of the result minus the varying `DataComponentType`s, found when `PatternVariants` assembles the candidates). In `OrderBook.create` and `preview`, when no planner recipe makes the requested key and a material pattern's generic key equals it, `OrderBook` resolves the key first: `VariantChoice.pick` on the planner's available stock gives the variant, its assembled result is the concrete key, and planning goes on with that key. The resolved key is returned in `Created` and `Preview`, and `TerminalRequests` makes the request for the resolved key, so the output slot and the request list name the variant. With several material patterns of one generic key (two recipes for racks) the one whose resolved variant has the most stocked inputs wins.

### `runs`, `patternFor` and duplicates
`OrderBook.runs` for an Any pattern compares the recipe id and, for a material pattern, the result item and count (the step's own output decides the variant); `CpuBlockEntity.patternFor` resolves the variant from the step's inputs as now and re-assembles its result. `EncodedPattern.duplicates`: two Any patterns of one recipe duplicate when their results are the same item and count (`ItemStack.isSameItem` and count), material ignored; Exact patterns keep "same recipe, same items".

### Craftable list
`TerminalCraftables.build` asks `PatternLibrary` for the line's Any material patterns and lists each once as its generic stack with the tooltip line; recipe outputs whose generic form is one of those keys are not listed again. Exact patterns are listed by their own result as today. The cache key adds the material patterns' generic keys.

### Preview in the shared grid
`PatternGridClient` gives `RecipeGridView` the result of the variant `(gameTime / 20) % n` for an Any material grid, where the variants are the static candidates of the material group (no stock on the Production Terminal's client). The tooltip adds the "follows" line. The amount panel already shows the server's plan, which names the resolved variant.

### APIs
Minecraft 26.1.2: `Recipe.placementInfo().ingredients()`, `Ingredient.items()` and `Ingredient.test`, `CraftingRecipe.matches` / `assemble` with `CraftingInput.of`, `ItemStack.isSameItem`, `ItemStack.getComponentsPatch`, `DataComponentPatch.builder` / `forget` to drop the varying types, `ClientLevel.getGameTime` for the cycle. NeoForge: `Tags.getTagTranslationKey` (already used for "Any planks"). No new event or payload.

### Tests
- Pure, JUnit: `VariantExpansionTest` (above), `VariantChoiceTest` unchanged.
- GameTests in `shuttles/gametest/PatternGameTests` and `OrderGridGameTests`: `rackPatternIsAnyMaterial`, `ironBlockStaysExact`, `spruceRackFromOakPattern`, `sprucePlanksCraftedForRack`, `genericRackTakesMostStockedWood` (5 oak, 40 birch: birch racks, request names birch), `genericRackWithoutStockCraftsOwnPlanks`, `spruceElevatorUsesSpruceRail`, `exactOakElevatorCraftsOakRail`, `secondAnyRackRefused`, `exactRackBesideAnyRackStored`, `craftableListShowsOneRack`, `oneOffAnyRackTakesMostStockedWood` (order grid).
- Manual in the client: JEI + on the rack in both terminals gives Any, the cycling preview, the generic entry and its craft dialog, no slowdown of the craftable list.

## Risks / Trade-offs

- [More planner recipes: up to 32 per material pattern] → Capped and ordered by stock; Any patterns that do not change the result still add one recipe. Watch the craft preview time in the play check.
- [Chains of three or more material patterns need more passes] → Two passes cover planks, Rail, Elevator; a deeper chain shows the item as missing rather than planning wrong, and can get a fixed-point loop later.
- [A generic key that is also a real stocked item (a rack without wood from the creative tab)] → Stock of that key is requested as today; only the craft part resolves to a variant.
- [Racks or Elevator patterns saved before stay Exact] → As decided in `m8-pattern-variants`; the player switches them in the editor or makes them again with JEI +.

## Migration Plan

None. Saved patterns keep their mode; nothing new is saved.

## Decisions made with the user

- **Cycling preview** (user's answer): the grid's result cycles through the variants' results once per second; the amount panel shows the variant the order will make.
- **Generic entry label** (user's answer): the plain item ("Heavy Duty Rack") with a tooltip line, no marker on the icon.

## Decisions made during apply

- **How a cell becomes material.** `PatternVariants.of` keeps a candidate whose result is the same item and count; when its components differ the own item is a material item and the differing `DataComponentType`s are recorded. A cell whose own stack has components (the Rail) is tried once with the plain item; a result that differs only in components makes it a material cell whose other stacks come only at planning (`variants(level, pattern, stock, made)` widens it with the stocked and made stacks of that item).
- **Tooltip text.** One line for every material: "Material follows: Planks" (`gui.servus_shuttles.pattern.follows`, the ingredient's tag name, else the item's name), not "Wood follows the planks", since the varying component does not say which material kind it is. Used by the grid's result, the Patterns tab and the generic craftable entry.
- **Where the generic key resolves.** `OrderBook.Host.resolve(key, available, oneOff)` (the CPU calls `PatternLibrary.resolveGeneric`) runs at the start of every plan; `Preview.made` returns the resolved key. `TerminalRequests.craft` and `craftOneOff` preview first and make the request and the order for `Preview.made`, so the output slot and the request list name the variant. A one-off material Any order asks for the generic key (`CpuOrders.oneOffKey`).
- **The order grid's own result.** `PatternGridClient.result()` cycles for the view; the Storage Terminal follows `ownResult()` for Next, the preview request and Save, so the cycling does not ask the server again every second. The amount panel's icon shows the preview's To craft entry of the item, which names the variant.
- **Craftable entries.** `StockSyncPayload` carries `MaterialCraftable(generic, follows)` per material pattern for the entry's tooltip; `ProductionRows.PatternRow` gets a `follows` list (empty or one), and an Elevator pattern whose only Any cell is its Rail is marked Any by it.
- **Made keys** for component cells come from the producer's own reported recipes and the line's patterns; outputs of other producers' reported recipes do not widen a Rail cell.

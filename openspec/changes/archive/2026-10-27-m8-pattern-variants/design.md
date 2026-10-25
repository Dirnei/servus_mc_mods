# Design

## Context

Every comparison in the pattern path is exact item plus component patch (`network/ItemKey`):
- `pattern/EncodedPattern(recipe, template, result)` holds 9 concrete stacks.
- `pattern/PatternRunners` (line 50) turns a pattern into a `ProductionRecipe` with `pattern.inputs()`.
- `order/OrderBook` (line 307) builds `OrderPlanner.Recipe`s keyed by `ItemKey` against a stock map keyed by `ItemKey` (line 283).
- `crafting/CraftingStationBlockEntity.plan()` gathers inputs by `ItemKey` against `job.required()` and runs `assemble` on the pattern's template (lines 321 to 326).

Results are compared exactly too (`OrderBook.makes`, `OrderStep.produces`), and there is no per-pattern setting. The editor's buttons are `foundation/block/RecipeEditorButtons` (OPEN_NEW 102, SAVE 103, DELETE 104, CANCEL 105) on `ProductionTerminalMenu.clickMenuButton`, and `PatternEditing.encode` already runs on the client for the preview (`m8-order-grid-assembled-result`).

## Goals / Non-Goals

**Goals:**
- An Any pattern feeds from whatever accepted item is most stocked, with no change to how the planner works: the variant is chosen before planning, so the planner still sees an exact recipe.
- Saved worlds keep working: old patterns are Exact.

**Non-Goals:**
- Material results, mixed variants in one craft, Machine Adapters, the order grid toggle (see proposal).

## Decisions

### PLAN.md
No open decision is settled. The change is a follow-up of `m8-order-grid-assembled-result` and touches no PLAN.md item; at archive nothing is removed.

### Mode on the pattern
`EncodedPattern` gains `Mode mode` (`EXACT`, `ANY`). Codec field `mode` is optional with default `EXACT`, so stored patterns load as Exact; the stream codec carries it. `equals` includes the mode. Alternative considered: a per-cell flag. Rejected: the user chose one switch per pattern, and per-cell adds editor clutter for a rare need.

### When Any is possible, and the candidates
`pattern/PatternVariants.of(level, pattern)` (Minecraft side) computes, for each used cell, the candidate items: every item of the recipe's ingredients (`placementInfo().ingredients()` item holders) that, put into every cell of the template holding the same item as that cell, still `matches` and `assemble`s to a stack equal to the pattern's result (same item, components and count). Any is possible when every used cell has its own item among its candidates and no candidate changes the result. Since only results equal to the stored result pass, the rack's planks never become candidates. The candidates are computed when the pattern is saved and again when `PatternStorageBlockEntity.refresh()` runs on load, and cached on the storage (not saved). The editor computes the same on the client for its button. A recipe from another mod whose `assemble` depends on more than one cell at once is still safe: each candidate is tested in all cells of its item at once, one variant is used per ingredient group (below), and the chosen template is tested as a whole before use (see "Decisions made while applying").

### Choosing the variant (pure)
`pattern/VariantChoice` (pure Java, no Minecraft imports) takes the cells' candidate lists as keys, the pattern's own keys and a stock map, groups cells whose candidate sets are equal (one ingredient), and per group picks the candidate with the most stock, ties to the pattern's own key, none in stock to the own key. It returns the 9 keys of the chosen template.

JUnit `VariantChoiceTest`:
- `picksTheMostStockedCandidate`
- `tiesGoToTheOwnItem`
- `noStockKeepsTheOwnItem`
- `cellsOfOneIngredientShareAVariant`
- `fixedCellsStay`
- `exactPatternIsUnchanged`

### Planning
Where `OrderBook` turns patterns into planner recipes (line 307, via `PatternRunners.withPatterns`), an Any pattern gets its variant from `VariantChoice` against the stock map the planner uses, and its `ProductionRecipe` inputs are the chosen template's counts. The planner and steps are unchanged; `OrderStep` already keeps inputs per `ItemKey`. `OrderBook.runs()` and the duplicate guard in `PatternRunners` (lines 55 to 65) compare an Any pattern by recipe id, mode and result instead of its items. The step's job for the station carries the chosen template.

### Station job
The Crafting Station's job is the pattern with the chosen template (the 9 stacks), saved with the job; for an Exact pattern it is the pattern itself. `plan()` gathers against the job's template counts and runs `matches`, `assemble` and `getRemainingItems` on `CraftingInput.of(3, 3, job.template())`. `uses()` (the automation insert filter) follows the job's template.

### Saving and duplicates
`PatternEditing.save` takes the mode. When Any is requested but not possible it stores Exact. Before storing an Any pattern it refuses when the line has an Any pattern with the same recipe id and an equal result, with the existing duplicate message. "Save as pattern" from the Storage Terminal's order grid passes the default (Any when possible).

### Editor
`RecipeEditorButtons.MODE = 106` toggles a `DataSlot mode` in `ProductionTerminalMenu`. `RecipeEditorPopup` draws an Exact / Any button left of Save, greyed with the tooltip "The result changes with these items" when Any is not possible (computed on the client from the grid with `PatternVariants`). A new pattern starts as Any when possible; editing an existing one loads its mode. The Patterns tab draws a small "Any" tag on Any patterns. The tooltip names each Any group: the ingredient's tag name ("Any planks") when the ingredient is a tag with a translation, else up to three candidate names.

### APIs
`Recipe.placementInfo().ingredients()`, `Ingredient.items()`, `CraftingRecipe.matches` and `assemble` with `CraftingInput.of`, `DataSlot`, `AbstractContainerMenu.clickMenuButton` (Minecraft 26.1.2). Tag names through the tag translation key convention `tag.item.<namespace>.<path>`, shown only when the language has it (check the exact helper in the 26.1.2 sources during apply). No new NeoForge event.

Pure Java with JUnit: `VariantChoice`. Minecraft side: `PatternVariants` and the planning, station and editor by GameTest (`PatternGameTests`, `CraftingStationGameTests`) and the play check.

## Risks / Trade-offs

- [A tag with many items makes `PatternVariants` assemble many times] → Done on save and load only, at most 9 cells times the candidates; cached.
- [The chosen variant runs short while the order runs, and the CPU then crafts the variant's own planks] → The step keeps its variant; that matches today's behaviour for Exact patterns.
- [Players expect mixing (2 oak + 2 spruce for a crafting table)] → Non-goal; one variant per ingredient keeps the recipe test exact.

## Migration Plan

None beyond the codec default; patterns load as Exact.

## Decisions made while applying

- **Candidates are tested on all cells with the same item, not one cell in place.** The Heavy Duty Rack takes its wood from the planks most of its cells hold, so one spruce plank among oak ones still made an oak rack and the in-place test called the rack Any. `PatternVariants` now puts each accepted item into every cell that holds the same item as the tested one; a spruce rack then shows the result changes. `variant()` still checks the chosen template as a whole and falls back to the pattern's own items when the recipe refuses it.
- **Any needs an alternative.** Besides "the result changes", a grid whose cells accept only their own items (an iron block) cannot be Any either; the button is greyed with "These cells take no other items". A new pattern asks for Any, and saving stores Exact for both cases.
- **The station job is the variant pattern.** Instead of a separate `template` field, the CPU hands the station an `EncodedPattern` whose template is the chosen variant (same recipe, result and mode). The station already plans, crafts, filters automation (`uses()`) and saves its job from the pattern's template, so it needed no change.
- **The CPU finds the variant again from the step's inputs.** `OrderStep` keeps its per batch inputs, so the step's start (`CpuBlockEntity.patternFor`) and `OrderBook.relocate` resolve Any patterns with the step's inputs as the stock (`OrderStep.inputStock()`); `VariantChoice` then picks exactly the step's items. Nothing new is saved with the step.
- **Stock reaches the planner's recipes.** `OrderBook.Host.producers` and `producersWith` take the stock the planner uses (line stock minus every reservation and minus the ordered item), and `PatternRunners.withPatterns` resolves each Any pattern against it. The Storage Terminal's craftable list passes no stock, so it sees the patterns' own items.
- **The planner's step key includes the inputs.** `OrderBook.StepKey` is now producer, recipe id and inputs. Two patterns of one recipe on a line (an Any sticks pattern resolved to spruce and an Exact birch one) gave two planner recipes with the same key, and the step could be built from the wrong one.
- **Duplicates.** `PatternRunners` keeps comparing the resolved recipe's inputs, which skips a second Any pattern of the same recipe and result since both resolve to the same variant. `OrderBook.runs()` compares a one-off Any pattern by recipe id and result. Saving uses `EncodedPattern.duplicates`: the same items as before, or two Any patterns of one recipe and result.
- **Tag names.** NeoForge's `Tags.getTagTranslationKey(TagKey)` gives `tag.item.<namespace>.<path>`; the client shows "Any %s" only when `I18n.exists` has that key, else "Any of" up to three item names and how many more. Vanilla has no tag translations, so the Shuttles language provider adds names for the vanilla item tags crafting recipes use (planks, logs, wooden slabs, wool, coal, stone and a few more); `c:` tags come with NeoForge's names. The tooltip therefore says "Any Planks".
- **Editor layout.** The mode button sits left of Save at x 62, 32 px wide; Save, Delete and Cancel moved 4 px right and became 4 px narrower so "Exact" fits.
- **`orderGridSavesAny` lives in `OrderGridGameTests`**, next to the order grid fixture it uses, not in `PatternGameTests`.

# Tasks

## 1. Pure variant choice

- [x] 1.1 `shuttles/.../pattern/VariantChoice` (pure Java): groups cells by equal candidate sets, picks the most stocked candidate per group, ties and no stock to the own key, fixed cells unchanged. JUnit `VariantChoiceTest`: `picksTheMostStockedCandidate`, `tiesGoToTheOwnItem`, `noStockKeepsTheOwnItem`, `cellsOfOneIngredientShareAVariant`, `fixedCellsStay`, `exactPatternIsUnchanged`. Check: `gradlew :shuttles:test`.

## 2. Pattern mode and candidates

- [x] 2.1 `pattern/EncodedPattern`: `Mode` with optional codec field `mode` (default `EXACT`), stream codec, `equals`. `pattern/PatternVariants`: candidates per cell and whether Any is possible; cached by `PatternStorageBlockEntity` on save and `refresh()`. Check: `gradlew :shuttles:build`.
- [x] 2.2 `pattern/PatternEditing.save` takes the mode, stores Exact when Any is not possible, refuses a second Any pattern for the same recipe and result; the order grid's "Save as pattern" passes the default. GameTests in `gametest/PatternGameTests`: `oldPatternLoadsExact`, `rackStaysExact`, `secondAnySticksRefused`, `orderGridSavesAny`. Check: `gradlew :shuttles:runGameTestServer`.

## 3. Planning and the station

- [x] 3.1 `order/OrderBook` and `pattern/PatternRunners`: an Any pattern becomes a planner recipe with the variant from `VariantChoice`; `runs()` and the duplicate guard compare Any patterns by recipe, mode and result; the step's station job carries the chosen template. Check: `gradlew :shuttles:build`.
- [x] 3.2 `crafting/CraftingStationBlockEntity`: the job's `template`, saved with the job; `plan()`, `uses()` and the craft use it. GameTests: `anySticksUseSpruce` (3 oak, 40 spruce, 16 sticks take 8 spruce), `anyWithoutStockCraftsOwnPlanks`, `stationKeepsVariantAfterReload`, existing Exact pattern tests unchanged. Check: `gradlew :shuttles:runGameTestServer`.

## 4. Editor

- [x] 4.1 `foundation/block/RecipeEditorButtons.MODE`, `foundation/client/RecipeEditorPopup` button beside Save, `menu/ProductionTerminalMenu` `DataSlot mode` and the toggle, `client/ProductionTerminalScreen` greyed state with tooltip, Any tag and tooltip in the Patterns tab, lang entries in the language provider. Check: `gradlew :shuttles:runData`, `gradlew build`.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` and `gradlew :foundation:runGameTestServer` green, the game launches with all mods; then the user's play check from main: a new sticks pattern shows Any; an order for sticks with only spruce planks in stock crafts from spruce; a rack pattern shows Exact greyed with its tooltip; the Patterns tab marks Any; an old world's patterns are Exact.

## Workflow follow-up

- At archive: a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Added: patterns can be set to Any, so the CPU uses whichever planks or logs are in stock when the result stays the same) and in `foundation/CHANGELOG.md` if the editor button changes Foundation's popup for other mods.

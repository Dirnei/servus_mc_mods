# Tasks

Start only after `m8-pattern-variants` and `m8-terminal-tabs` are archived.

## 1. Foundation layout and view

- [x] 1.1 Add pure `foundation/.../ui/RecipeGridLayout` (popup 236 x 90, optional title row, cells, arrow, result, extra rect, mode button, action row, `actions(n)` spreading first left, last right, rest centred) and make `foundation/.../block/RecipeEditorButtons.gridX/gridY` use it; add `RecipeEditorButtons.CLEAR = 107`. JUnit `RecipeGridLayoutTest`: `partsDoNotOverlap`, `partsInsidePopup` (with and without title), `actionsSpreadLeftMiddleRight` (1 to 4 actions), `cellsMatchMenuSlots`. Verify: `gradlew :foundation:test`.
- [x] 1.2 Move `GreyOut` from Shuttles to `foundation/.../client/GreyOut` and point the Storage Terminal, `ItemPopup` and `RulesList` at it. Verify: `gradlew build`.
- [x] 1.3 Add `foundation/.../client/RecipeGridView` (popup, title, cell frames, arrow, bare result with count and tooltip, mode button from `ModeState`, action row of vanilla buttons from `Action`s with greyed reasons, extra rect, drag cell tracking) and its lang strings in the Foundation language provider; delete `RecipeEditorPopup`. Verify: `gradlew :foundation:build` (Shuttles compiles again after 3.1).

## 2. Shared grid on the menus

- [x] 2.1 Add `shuttles/.../pattern/PatternGrid` (ghost `ItemSlots`, mode `DataSlot`, slot setup, `setGhost`, `cells`, `clear`, `fill`, `toggleMode`, `resetMode`, `pattern(level)` with the allowed mode) and move `ProductionTerminalMenu` onto it, with CLEAR in `clickMenuButton`. Verify: `gradlew :shuttles:build`, existing `PatternGameTests` green with `gradlew :shuttles:runGameTestServer`.
- [x] 2.2 Move `StorageTerminalMenu` onto `PatternGrid`: add `clickMenuButton` for MODE and CLEAR, reset the mode on Cancel and after a started order, `startOrder`, `orderPreview` and `savePattern` use `PatternGrid.pattern(level)` and the chosen mode, the `saved` flag compares with the mode. Remove `GridFillPayload` from `terminal/` and `ServusShuttlesPayloads`. GameTests in `gametest/OrderGridGameTests`: `oneOffAnyUsesSpruce`, `oneOffExactWaitsForBirch`, `oneOffAnyKeepsModeAfterReload`, `saveAsPatternKeepsExact`, `saveHiddenForSecondAnyPattern`, `clearButtonEmptiesGrid` (and `clearButtonEmptiesEditor` in `PatternGameTests`); `orderGridSavesAny` stays green. Verify: `gradlew :shuttles:runGameTestServer`.

## 3. Screens on the shared view

- [x] 3.1 Add `shuttles/.../client/PatternGridClient` (result, `ModeState` with Any groups and reasons, "no match" reason, variants cache moved from `ProductionTerminalScreen`) and move the mode lang keys to `gui.servus_shuttles.pattern_grid.mode.*` in `ServusShuttlesLanguageProvider`. Verify: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.
- [x] 3.2 `client/ProductionTerminalScreen` on `RecipeGridView`: title, actions Cancel, Clear, Delete (existing only), Save (greyed with "matches no recipe"), drag fill, mode button; `terminal/TerminalLayout.Production.POPUP` becomes (2, 30, 236, 90) and `TerminalLayoutTest` checks it ends above the inventory. Verify: `gradlew :shuttles:test`, `gradlew :shuttles:build`.
- [x] 3.3 `client/StorageTerminalScreen` order grid on `RecipeGridView`: actions Cancel, Clear (menu button), Next; Save as pattern icon in the extra rect; mode button; drag fill through the view; `TerminalLayout.Storage` order rects taken from `RecipeGridLayout` and `TerminalLayoutTest` updated. Verify: `gradlew :shuttles:test`, `gradlew :shuttles:build`.

## 4. JEI

- [x] 4.1 Add `terminal/OrderGridRecipePayload(recipe id)` and `StorageTerminalMenu.fillFromRecipe` through `CraftingGrid.templateOf`, resetting the mode when the transfer opens the grid; `integration/jei/StorageTerminalRecipeTransferHandler` sends it and keeps its refusal; drop `CraftingGrid.stockedChoice`'s unused overload if nothing else calls it. Verify: `gradlew :shuttles:build`, and in the client JEI's transfer on the stick recipe fills both terminals the same way.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` and `gradlew :foundation:runGameTestServer` green, the game launches with all mods. Then the user's play check from main: the editor and the order grid look the same; drag fills cells in both; Clear works in both; the order grid's sticks start as Any and an order of 16 sticks with only spruce planks in stock crafts from spruce; Exact shows oak planks missing; Save as pattern stores the chosen mode (Patterns tab mark); a rack shows Exact greyed in both; JEI's transfer fills both the same way and starts Any; no overlap or flicker in either popup.

## Workflow follow-up

- At archive: a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Changed: the Storage Terminal's order grid and the Production Terminal's recipe editor are one view; one-off orders and Save as pattern can be Exact or Any) and in `foundation/CHANGELOG.md` (Changed: the recipe editor popup became the recipe grid view).
- PLAN.md: nothing to remove; add any open point the play check raises.

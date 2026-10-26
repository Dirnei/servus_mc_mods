# Tasks

## 1. Pure logic

- [x] 1.1 Generalise the tab width split: add `TerminalLayout.tabs(Rect row, int[] labelWidths)` and let `Production.tabs` call it (`shuttles/.../terminal/TerminalLayout.java`). Verify: existing `TerminalLayoutTest` Production tab tests stay green with `gradlew :shuttles:test`.
- [x] 1.2 Add `Storage.TERMINAL_TABS` (14 px strip above `layout.top()`), `Storage.POPUP_TABS`, `RULE_TITLE`, `RULE_MAKE`, `RULE_LIMIT`, `RULE_NOTE`, `RULE_BUTTONS` and a `ruleAreas()` list; make `rowsFor` leave room for the strip (`TerminalLayout.java`). JUnit in `TerminalLayoutTest`: rule areas disjoint and inside `CRAFT_POPUP`, popup tabs on the popup's top edge, panel plus strip within `screen - 4` for every height and screen in `SCREEN_HEIGHTS`, popup bottom above `INVENTORY_LABEL`. Verify: `gradlew :shuttles:test`.
- [x] 1.3 Add `terminal/RuleInput` (minimum at least 0, maximum at least 1, minimum below maximum, reason key or none). JUnit `RuleInputTest`: valid 8 to 16, 16 to 16 refused, 0 to 1 valid, max 0 refused. Verify: `gradlew :shuttles:test`.
- [x] 1.4 Add `terminal/ItemPopupTabs` (enabled tabs and reason keys from requestable count, craftable flag, warehouse reached). JUnit `ItemPopupTabsTest`: out of stock and not craftable greys Request and Craft, offline greys Rule, stocked item enables Request. Verify: `gradlew :shuttles:test`.
- [x] 1.5 Add `terminal/RuleListing` (indices of rules sorted by item name, filtered by a case insensitive query). JUnit `RuleListingTest`: sort order, filter "torch", empty query keeps all. Verify: `gradlew :shuttles:test`.
- [x] 1.6 Extend `foundation/.../ui/AmountSteps` tests for a lowest value of 0 (`AmountStepsTest`: -1 from 0 stays 0, +stack from 10 gives 64, -stack from 10 gives 0). Verify: `gradlew :foundation:test`.

## 2. Foundation one line stepper

- [x] 2.1 Add `foundation/.../client/InlineStepper` (minus, `EditBox`, plus; click 1, shift click stack via `AmountSteps`, wheel 1, clamp on Enter or focus loss, `widgets()`, `place`, `setVisible`, `setValue`, `tick`, `render` like `NumberStepper`) and its tooltip strings in `ServusFoundationLanguageProvider`. Verify: `gradlew :foundation:build`.

## 3. Item popup

- [x] 3.1 Add `shuttles/.../client/ItemPopup`: tab strip drawn creative tab style from `POPUP_TABS` and `TerminalLayout.tabs`, greyed tabs with reason tooltips from `ItemPopupTabs`, page switching that keeps the item. Verify: `gradlew :shuttles:build`.
- [x] 3.2 Move the request prompt onto the Request page: `NumberStepper` with 1, 10 and stack at `CRAFT_STEPPER`, the item beside the amount, the `RequestSplit` line and preview rows, Cancel and Request with the refusal reason as tooltip (`ItemPopup`, `StorageTerminalScreen`). Verify: `gradlew :shuttles:build`.
- [x] 3.3 Put the existing amount panel on the Craft page and keep it for the order grid's second step, with `Dialog` reduced to `NONE`, `ITEM`, `ORDER`, `ORDER_AMOUNT` (`StorageTerminalScreen`, `ItemPopup`). Verify: `gradlew :shuttles:build`.
- [x] 3.4 Build the Rule page: title row with the rule state, Make group with two `InlineStepper`s ("Order when below" lowest 0, "Fill up to" lowest 1), `RULE_LIMIT` left empty, validation line from `RuleInput`, Remove (greyed without a rule), Cancel, Save (greyed with reason) sending `SetStockRulePayload` (`ItemPopup`). Verify: `gradlew :shuttles:build`.
- [x] 3.5 Route clicks: right click on a stocked cell opens Request, the craft clicks open Craft, Escape and Cancel close, the old in-panel prompt and its step row and Rule toggle are removed (`StorageTerminalScreen`). Verify: `gradlew :shuttles:build`.

## 4. Stock and Rules tabs

- [x] 4.1 Draw the Stock and Rules tabs in `TERMINAL_TABS`, add the strip to `extraAreas()`, hide sort, direction and view buttons on Rules (`StorageTerminalScreen`). Verify: `gradlew :shuttles:build`.
- [x] 4.2 Add `shuttles/.../client/RulesList`: add slot row, rule rows (icon, "8 to 16", state) from `ClientStock.rules()` through `RuleListing`, search filter, wheel and scrollbar like the grid, row click opens the Rule page (`RulesList`, `StorageTerminalScreen`). Verify: `gradlew :shuttles:build`.
- [x] 4.3 Add slot: a click with a carried stack opens the Rule page for one of it without sending `PushCarriedPayload`; register an `IGhostIngredientHandler<StorageTerminalScreen>` with the add slot as target in `ServusShuttlesJeiPlugin.registerGuiHandlers` (`StorageTerminalScreen`, `integration/jei/`). Verify: `gradlew :shuttles:build`.
- [x] 4.4 Lang strings for tabs, pages, labels, reasons and the add slot hint; remove the old prompt keys (`shuttles/.../datagen/ServusShuttlesLanguageProvider`). Verify: `gradlew :shuttles:runData` then `gradlew :shuttles:build`.

## 5. Verification

- [x] 5.1 Play check in the client: right click opens Request, tabs switch for the same item, a non craftable item greys Craft with its tooltip, a rule saved on the Rule tab shows its grid marker, Remove deletes it, the Rules tab lists and filters rules, a carried item and a JEI drag on the add slot open the Rule page, tall height at GUI scale 4 keeps the tabs on screen, no flicker of the tab strips. Verify: observed in game.
- [x] 5.2 Full gradle build, tests green, game launches with all three mods: `gradlew build` and `gradlew test` pass and the client run config starts.

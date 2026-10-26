# Design: m8-terminal-tabs

## Context

`StorageTerminalScreen` (about 1600 lines) holds four dialogs in one `Dialog` enum: `REQUEST` and `RULE` share the old in-panel prompt (`layout.dialog()`, buttons 36 to 46 px wide, a six button step row), `CRAFT` is the amount panel popup (`Storage.CRAFT_POPUP`, 236 x 128, Foundation `NumberStepper`, three `ItemListView` lists) used both for a craftable item and as the order grid's second step (`fromOrder`), and `ORDER` is the order grid. Rules reach the client in `StockSyncPayload` as `RuleRow(stack, min, max, state)` and are set with `SetStockRulePayload(item, min, max)`, a negative min removing the rule; the server side (`StorageTerminalMenu.setRule`) takes any `ItemKey`, so a rule for an item that is not in stock needs no server change. The Production Terminal already has a tab row (`TerminalLayout.Production.tabs`). `TerminalLayoutTest` checks that areas do not overlap and that the panel fits a 1080p screen at GUI scales 2, 3 and 4 (`MAX_HEIGHT` 266). See proposal.md for the why.

## Goals / Non-Goals

**Goals:**
- One item popup with Request, Craft and Rule tabs, every page on Foundation widgets with full size buttons.
- A Rules tab with an overview of all rules and an add slot for any item.
- A Rule tab layout whose rows do not move when `m8-rule-cap` and `m8-rule-export` add their Limit group.

**Non-Goals:**
- No server, payload or saved data change.
- No tabs on the order grid flow: its amount panel stays reachable only through Next with Back.

## Decisions

### Popup geometry: the craft popup with a tab strip above it
The item popup reuses `Storage.CRAFT_POPUP` (-23, 18, 236 x 128) for every tab, so the three pages never resize the popup and the Craft tab is the existing amount panel unchanged. A new `Storage.POPUP_TABS` strip (-17, 4, 224 x 14) sits on the popup's top edge, over the dimmed title and search row, with three tabs drawn creative tab style: the active tab in the panel colour joined to the panel, the others darker and 2 px lower. Tab widths come from a shared `TerminalLayout.tabs(Rect row, int[] labelWidths)`, the Production Terminal's existing `Production.tabs` generalised to any row.
Alternative: a taller popup for the Rule tab. Rejected: the popup would leave the space above the inventory label (y 148) once the Limit group arrives.

### Rule tab rows at fixed offsets, Limit row reserved
Inside the popup (x -17, width 224):
- `RULE_TITLE` (-17, 24, 224 x 16): item icon, name, the rule's state on the right.
- `RULE_MAKE` (-17, 44, 224 x 23): the group label "Make" in a 30 px column, then two field blocks of 94 px, each a 9 px label over a one line stepper.
- `RULE_LIMIT` (-17, 71, 224 x 23): left empty here; `m8-rule-cap` fills it with "Export above" and "Refuse at Inbound above".
- `RULE_NOTE` (-17, 98, 224 x 20): two text lines, the validation reason in the alert colour (the second line is for the export target later).
- `RULE_BUTTONS` = `CRAFT_BUTTONS` (-17, 126, 224 x 14): Remove on the left, Cancel and Save on the right, `Storage.BUTTON` and `WIDE_BUTTON` wide.
A 94 px block is minus (14), field (64), plus (14) with 1 px gaps. Reserving the Limit row now keeps the later changes to filling a rect instead of moving rows.

### Request tab on the tall stepper, preview as today
The Request tab uses the Foundation `NumberStepper` with steps 1, 10 and `AmountSteps.STACK` at the place of `CRAFT_STEPPER`, the item beside the amount, and below it the stock and crafted split line and the preview rows the old prompt draws (`RequestSplit`, `CraftPreviewPayload`). Cancel left, Request right, greyed out with the reason as tooltip where the old prompt refused.

### One line stepper in Foundation
New `foundation/.../client/InlineStepper`: minus button, `EditBox`, plus button on one row, 14 px high. A click steps by 1, a shift click by `AmountSteps.STACK` (checked with `Screen.hasShiftDown()` style `Minecraft.getInstance().hasShiftDown()`, as the terminal grid already does), the wheel over the field by 1, and typing is clamped on Enter or focus loss, mirroring `NumberStepper`. The step arithmetic stays in `AmountSteps.step(amount, step, stack, min, max)`, which already takes a lowest value, so the Rule tab can use 0 as the lowest minimum.
Alternative: a layout option on `NumberStepper`. Rejected: its API is built around rows of several steps above and below the field.

### Screen split into popup and list classes
To keep `StorageTerminalScreen` from growing further, the popup moves into `client/ItemPopup` (tab strip, the three pages and their widgets, hit testing) and the Rules list into `client/RulesList` (rows, add slot, scroll position). The screen's `Dialog` enum becomes `NONE`, `ITEM` (the popup, which knows its tab), `ORDER` and `ORDER_AMOUNT`; the amount panel code serves both the Craft tab and `ORDER_AMOUNT`. Click routing stays in the screen: right click on a stocked cell opens `ITEM` on Request, the craft clicks open it on Craft, a Rules row or the add slot on Rule.

### Terminal tabs above the panel
`Storage.TERMINAL_TABS` is a 14 px strip right above the panel's top (`layout.top() - 14`), from x 8, with Stock and Rules. `rowsFor` subtracts the strip, so the panel with its tabs still fits at 270 GUI px (242 + 14 = 256 within `MAX_HEIGHT`), and `extraAreas()` reports the strip to JEI. The tab is screen state only and starts on Stock each time the screen opens.

### Rules list in the grid's rect
The Rules tab draws in `layout.grid()` with the grid's scrollbar: row 0 is the add slot (fixed), rows 1 and on are rules, 18 px each, icon at x 0, "8 to 16" at x 20 (60 px), state at x 84 (78 px), cut to fit. The search box filters by item name; the sort, direction and view buttons hide, search mode and height stay. Sorting and filtering are pure in `terminal/RuleListing` over item names, so they are unit tested.

### Add slot with carried stacks and JEI ghosts
A click on the add slot with a carried stack opens the popup on Rule for `carried.copyWithCount(1)` and leaves the cursor alone, so no `PushCarriedPayload` is sent. With JEI, `ServusShuttlesJeiPlugin.registerGuiHandlers` adds an `IGhostIngredientHandler<StorageTerminalScreen>` (`IGuiHandlerRegistration.addGhostIngredientHandler`, present in `jei-26.1.2-common-api` 29.43.0.107 as `mezz.jei.api.gui.handlers.IGhostIngredientHandler`) whose one target is the add slot while the Rules tab shows and no popup is open; accepting an `ItemStack` ingredient opens the Rule tab.

### Tab availability and rule validation are pure
`terminal/ItemPopupTabs` decides which tabs are enabled and their reason keys from the requestable count, the craftable flag and whether a warehouse is reached. `terminal/RuleInput` validates the Rule tab (minimum at least 0, maximum at least 1, minimum below maximum) and returns the reason key or none; `m8-rule-cap` extends it with the new thresholds.

### Which logic is tested how
- Pure, JUnit: `TerminalLayout` (new strips and rule rects in `TerminalLayoutTest`, panel plus tab strip fits every screen height), `RuleInput` (`RuleInputTest`), `ItemPopupTabs` (`ItemPopupTabsTest`), `RuleListing` (`RuleListingTest`), `AmountSteps` with a lowest value of 0 (`AmountStepsTest`).
- Minecraft, manual in the client: tab switching, the stepper look, the add slot with a carried stack and a JEI drag, no flicker of the tab strip over the dimmed title.

### PLAN.md decisions settled
The "Open: Shuttles" point about the request prompt and stock rule dialog on the Foundation stepper is settled for the Storage Terminal by the tabbed popup; the point stays open for the other screens it names.

## Risks / Trade-offs

- [The tab strip over the title row hides the site selector while a popup is open] → The area is dimmed and takes no clicks under any popup today, so nothing is lost.
- [Fixed 128 px popup leaves the Request and Craft tabs some empty space] → Accepted for a popup that keeps its size across tabs.
- [Players used to the Rule toggle] → The Rule tab is one click from the Request tab of the same item.
- [JEI ghost target only while no popup is open] → Dragging onto the add slot is the only ghost use; the popup's fields take typed numbers.

## Migration Plan

UI only. Lang keys of the old prompt (`rule_max`, the Rule toggle labels) go; worlds and saved rules are unaffected. Rollback is reverting the change.

## Decisions made while applying

- `client/ItemPopup` holds the tab strip, the Request page and the Rule page. The Craft page stays the screen's amount panel, which also serves `ORDER_AMOUNT`; the popup tells the screen through its `Host` which tab shows, and the screen shows the amount panel for Craft.
- The Request page has no status line: why Request is greyed out (preview on its way, missing items, a refusal) is its tooltip. Below the stepper it shows the split line and up to two preview rows per list, the second one "+N more" when they do not fit, or, when stock covers the amount, the hint "Lands in the Output row, one slot per request" from the mockup.
- Tabs are as narrow as their labels plus padding, from the strip's left edge, as in the mockup: `TerminalLayout.compactTabs(row, labelWidths, padding)` narrows the row and calls the shared `TerminalLayout.tabs(row, labelWidths)`.
- The Stock and Rules strip moves with the panel's top, so it is `Layout.tabs()` instead of a `Storage.TERMINAL_TABS` constant. The old prompt's `Layout.dialog()` is gone. With the strip, the tall height at GUI scale 4 on a 1080p screen shows the four base rows (five before).
- Without a reachable warehouse `ItemPopupTabs` greys all three tabs with "No warehouse"; a popup opened on a tab that cannot act opens on the first one that can.
- The Rule tab's title row shows "in stock N" before the state, as in the mockup. The Remove button reads "Remove" to fit the 50 px button.
- `GreyOut.apply` and `PanelPainter.smallText` and `PanelPainter.tab` are shared helpers of the screen, the popup and the Rules list.

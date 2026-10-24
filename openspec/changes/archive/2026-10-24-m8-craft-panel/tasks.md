# Tasks

## 1. Foundation widgets

- [x] 1.1 Move `AmountSteps` and `AmountStepsTest` from `shuttles/terminal` to `foundation/ui`; update `RequestSplit` and `StorageTerminalScreen` imports. Add pure `foundation/ui/ListScroll` with `ListScrollTest`: `clampsToLastPage`, `fewerRowsThanVisible`, `scrollStopsAtEnds`, `thumbSpansTrack`, `thumbMovesWithOffset`. Run `gradlew :foundation:test :shuttles:test`.
- [x] 1.2 Add `foundation/client/NumberStepper` (field, plus row above, minus row below, configurable steps, stack supplier, min, max, change callback, wheel over the field, clamp on Enter and focus loss) and `foundation/client/ItemListView` (heading, half size icon rows, cut names, thin scrollbar, wheel scroll, `rowAt` for tooltips). Run `gradlew :foundation:build`.

## 2. Server side

- [x] 2.1 `OrderStartPayload` drops `save`; new `OrderSavePayload` registered in `ServusShuttlesPayloads`; `StorageTerminalMenu.startOrder(amount)` and `savePattern()` (message `no_match`, `pattern_no_storage`, `pattern_full` or empty). Update `OrderGridGameTests`: `saveAsPatternStores` saves without starting an order, new `refusedSaveKeepsGrid`, one-off tests use `startOrder(1)`. Run `gradlew :shuttles:build` and `:shuttles:runGameTestServer`.

## 3. Screen

- [x] 3.1 `TerminalLayout.Storage`: new order grid rects (`ORDER_GRID`, `ORDER_ARROW`, `ORDER_RESULT`, `ORDER_SAVE`, `ORDER_BUTTONS`) and amount panel rects (`CRAFT_TITLE`, `CRAFT_STEPPER`, `CRAFT_LISTS`, `CRAFT_BUTTONS`); remove the old order amount, label, status, steps and preview rects; update `TerminalLayoutTest`. Run `gradlew :shuttles:test`.
- [x] 3.2 `StorageTerminalScreen` order grid: centred grid, arrow, bare result icon with one craft's count and its item tooltip, Save icon button with "Save as pattern" only while the pattern is not saved (sends `OrderSavePayload`, then asks a new preview), Cancel left and Next right, Next greyed with the reason as tooltip; remove `asking`, `saveButton`, `onceButton` and the order grid's amount, steps, status and preview. Run `gradlew :shuttles:build`.
- [x] 3.3 `StorageTerminalScreen` amount panel for `CRAFT` (from a craftable, Cancel) and from the order grid (Back): title, `NumberStepper` starting at 1 with the item and crafted count beside the field, three `ItemListView`s (From stock, To craft, Missing always shown, dark text), Start greyed with the reason as tooltip and no tooltip when ready, no status line; wheel and tooltips forwarded. Lang keys via `ServusShuttlesLanguageProvider` and `:shuttles:runData`. Run `gradlew :shuttles:build`.
- [x] 3.4 Fixes: `StorageTerminalScreen.removed` keeps the shown content while the menu is still open (JEI plus works with a CPU and a Crafting Station), and the screen draws in `extractContents` so a carried stack stays above the dialogs. Run `gradlew :shuttles:build`.
- [x] 3.5 Replace `shuttles/menu/GhostSlot` with Foundation's in `StorageTerminalMenu`, `ProductionTerminalMenu`, `OutboundStationMenu` and `ZoneLabelMenu`, delete the copy. Add the request prompt open point to PLAN.md. Run `gradlew :shuttles:build`.

## 5. Play check feedback

- [x] 5.1 `TerminalLayout.Storage`: popups `ORDER_POPUP` and `CRAFT_POPUP` with their parts as in the design (wider lists, 50 px Cancel and Clear, 60 px Next and Start); `NumberStepper` buttons 24 wide and field 48 wide; `TerminalLayoutTest` checks parts inside their popup and above the inventory label. Run `gradlew :foundation:build :shuttles:test`.
- [x] 5.2 `StorageTerminalScreen`: draw the order grid and the amount panel as popups with `PopupPanel` dimming the terminal above the inventory, swallow clicks and slot tooltips in the dimmed area, a Clear button, Cancel and Escape clear the cells (`GridFillPayload` with no options), drag over ghost cells sets each one. Run `gradlew :shuttles:build`.
- [x] 5.3 `StorageTerminalMenu.startOrder` clears the cells once the order is made; `OrderGridGameTests.oneOffOrderCrafts` checks the grid is empty after the start. Run `:shuttles:runGameTestServer`.
- [x] 5.4 `StorageTerminalScreen`: a press a dialog takes also swallows its release, so a carried stack never lands in a slot behind it; a ghost cell is set on press. Run `gradlew :shuttles:build`.
- [x] 5.5 Craft button as a hammer icon (`textures/gui/terminal/craft.png`) right of the panel (`TerminalLayout.Storage.RIGHT_SIDE_X`), title wider, JEI exclusion and click area; `TerminalLayoutTest` accepts both side columns. Run `gradlew :shuttles:build`.

## 4. Verification

- [x] 4.1 Full `gradlew build`, `gradlew test`, `:foundation:runGameTestServer` and `:shuttles:runGameTestServer` green; the game launches with all mods.
- [x] 4.2 Play check in game by the user: order grid and amount panel as popups at every height, Clear, Cancel and Start clear the grid, drag over cells places ghosts, wide lists and buttons without scrolling labels; order grid centred with the bare result and Save (tooltip, gone once saved, nothing starts); Next greyed on no match; amount panel stepper rows, crafted count, three scrolling lists with icons, Missing always shown; Start greyed with the reason, no tooltip when ready; Back returns to the grid; craftable click opens the same panel with Cancel; JEI plus opens the order grid with a CPU; a dragged stack shows above every dialog.

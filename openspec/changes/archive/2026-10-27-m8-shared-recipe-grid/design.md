# Design

## Context

See proposal.md for the why and the table of differences. State on main, with `m8-pattern-variants` and `m8-terminal-tabs` applied but not archived:

- **Editor.** `foundation/client/RecipeEditorPopup` (160 x 72) draws the dim, the frame, the cell frames, a framed preview after "=", flat buttons Save, Delete, Cancel in a column and the mode button left of Save. Its only user is `shuttles/client/ProductionTerminalScreen`, which computes the preview with `PatternEditing.encode` and the mode state with `PatternVariants.of` (cached per grid). The cells are `GhostSlot`s of `ProductionTerminalMenu` at `RecipeEditorButtons.gridX/gridY(POPUP_X/Y)`; the menu keeps the mode in a `DataSlot` and handles `RecipeEditorButtons` OPEN_NEW, SAVE, DELETE, CANCEL, MODE in `clickMenuButton`. JEI: `ProductionTerminalRecipeTransferHandler` sends `OpenRecipeEditorPayload(recipe id)` and the menu fills from `CraftingGrid.templateOf(level, id, stock)`.
- **Order grid.** `StorageTerminalScreen` draws `Storage.ORDER_POPUP` (236 x 90), cells at `Storage.orderX/orderY`, `ORDER_ARROW`, `ORDER_RESULT` (bare icon), the `ORDER_SAVE` icon and vanilla buttons Cancel, Clear, Next in `ORDER_BUTTONS`, and handles drag fill in `mouseDragged`. `StorageTerminalMenu` has its own `GhostSlot`s, `setGhost`, `clicked`, `fillGhosts(List<List<ItemStack>>)` (from `GridFillPayload`, also used for Clear), `startOrder` (via `OrderStartPayload(amount)`), `savePattern` (always `Mode.ANY`) and `orderPreview`, all on `PatternEditing.encode`, which returns an Exact pattern. JEI: `StorageTerminalRecipeTransferHandler` sends the JEI slot views in a `GridFillPayload` and the menu picks with `CraftingGrid.stockedChoice`.
- **Any for one-offs is already wired on the server.** `TerminalRequests.craftOneOff` hands the pattern to `CpuOrders.createOneOff` and `OrderBook.create(key, amount, requestId, oneOff)`. `OrderBook.recipes` gets producers through `Host.producersWith(oneOff, stock)`, which resolves an Any pattern with `PatternLibrary.Entry.unsaved(level, pattern).variant(level, stock)` (`PatternVariants.variant`, `VariantChoice.pick`), and `OrderBook.runs` compares an Any one-off by recipe id and result. `CpuBlockEntity.patternFor` resolves a one-off step again from `OrderStep.inputStock()`. `WorkOrder` saves `oneOff` with `EncodedPattern.CODEC`, which has the `mode` field. Only the mode is missing: the menu always encodes Exact.

The delta for `shuttles/production-terminal` modifies "Exact or Any in the recipe editor", which reaches the main spec only when `m8-pattern-variants` is archived; `openspec validate` reports this as info until then.

## Goals / Non-Goals

**Goals:**
- One Foundation view and one Shuttles grid helper, so a fix to the grid lands in both terminals.
- Any one-offs with no planner or CPU change: only the mode reaches the encoded pattern.

**Non-Goals:**
- No new saved data and no change to `OrderBook`, `PatternVariants`, `VariantChoice` or the Crafting Station.
- The amount panel, the item popup and the Storage Terminal's dialog flow stay as `m8-terminal-tabs` leaves them.

## Decisions

### PLAN.md
No open decision of PLAN.md is settled; the change came from the play check. At archive nothing is removed from PLAN.md.

### Order of work
Applied only after `m8-pattern-variants` and `m8-terminal-tabs` are archived (user's explicit request). Both change the files this change rewrites (`RecipeEditorPopup`, `ProductionTerminalScreen`, `StorageTerminalScreen`, `TerminalLayout`); rebasing on their archived state avoids a three way merge of the same screens.

### Foundation view: `RecipeGridView` replaces `RecipeEditorPopup`
`foundation/client/RecipeGridView` draws the popup (dim, frame, optional title), the cell frames, the arrow, the result as a bare icon with `itemDecorations`, the mode button and an action row of vanilla `Button`s. It knows nothing of patterns; the screen hands it:
- `ItemStack result` (empty for no match),
- `ModeState(Component label, boolean enabled, List<Component> tooltip)`,
- `List<Action>` with `Action(Component label, boolean enabled, Component reason, Runnable press)`, laid out first left, last right, the rest centred,
- an optional `extra` rect beside the result where the screen draws its own widget (the Storage Terminal's Save icon).
The view adds its buttons through the screen's `addRenderableWidget` (as `NumberStepper` does with `widgets()`), greys them with the existing `GreyOut` look moved to Foundation, and reports hover tooltips for the result and the mode button. Drag fill is in the view: `mouseDragged(x, y, button)` returns the cell under the mouse once per drag, and the screen sends the slot click (`slotClicked(slot, PICKUP)`) as `StorageTerminalScreen.clickGhostCell` does today.
Alternative: keep `RecipeEditorPopup` and give it the order grid's options. Rejected: its custom flat buttons, column layout and fixed 160 px size are the parts that lose to the order grid.
Alternative: put the view in Shuttles. Rejected: `RecipeEditorPopup` is Foundation API for every mod's recipe editor, and the mode and action hooks keep it free of Shuttles types.

### Layout: one popup size, pure
`foundation/ui/RecipeGridLayout` (pure, no Minecraft imports, its own small `Box(x, y, w, h)` record) gives, relative to the popup's top left: popup 236 x 90, optional title row, cells 3 x 18 at (36, 6) or (36, 16) with a title, arrow, result, the `extra` rect, the mode button (32 x 14) under the result, and the action row (224 x 14) at the bottom. The numbers start from the order grid's current rects and may be tuned during apply. `RecipeEditorButtons.gridX/gridY` call the layout so both menus put their `GhostSlot`s where the view draws the frames. JUnit `RecipeGridLayoutTest`: `partsDoNotOverlap`, `partsInsidePopup` (with and without title), `actionsSpreadLeftMiddleRight` (1, 2, 3 and 4 actions), `cellsMatchMenuSlots`.
The Production Terminal's `Production.POPUP` becomes (2, 30, 236, 90) so it fits the 240 px panel and ends above the inventory at y 122. The Storage Terminal's `ORDER_POPUP` keeps its place.

### Shuttles grid helper for both menus: `PatternGrid`
`shuttles/pattern/PatternGrid` (common) holds what both menus duplicate: the `ItemSlots` of 9, the mode `DataSlot`, `addSlots(menu, x, y, active)`, `setGhost`, `cells()`, `clear()`, `fill(template)`, `toggleMode()`, `resetMode()` (Any) and `pattern(level)`, which is `PatternEditing.encode(level, cells)` with `withMode(PatternEditing.allowed(level, pattern, mode))`. Both menus delegate `clicked` for grid slots to it.
`RecipeEditorButtons` gains `CLEAR = 107`. Both menus handle MODE and CLEAR in `clickMenuButton`; `StorageTerminalMenu` gets a `clickMenuButton` for the first time. The order grid's mode then needs no payload: `OrderStartPayload(amount)`, `OrderSavePayload` and `CraftPreviewRequestPayload(ORDER)` read the menu's mode on the server. `GridFillPayload` goes (Clear is a menu button, JEI uses the recipe id).

### Client helper for both screens: `PatternGridClient`
`shuttles/client/PatternGridClient` takes the menu's cells and mode and returns the result stack, the `ModeState` (label, enabled, tooltip with the Any groups) and the "no match" reason, caching `PatternVariants.of` per grid and recipe map exactly as `ProductionTerminalScreen.gridVariants` does now. That code moves here from the Production Terminal screen. Lang keys `gui.servus_shuttles.production.mode.*` become `gui.servus_shuttles.pattern_grid.mode.*`.

### Mode life in the order grid
Starts Any (`resetMode`) when the grid is opened empty, after Cancel, Escape and a started order; Next and Back keep it; Clear keeps it; a JEI transfer that opens the grid resets it, one into the open grid keeps it. Saving or ordering a grid where Any is not possible stores or orders Exact (`PatternEditing.allowed`). The Production Terminal keeps its rules (`openNew` and `openEditorWith` on a closed editor reset to Any, `openExisting` loads the pattern's mode).

### One-off order with the mode
`StorageTerminalMenu.startOrder` and `orderPreview` use `PatternGrid.pattern(level)` instead of `PatternEditing.encode`. The rest is the existing path (Context). `savePattern` passes the menu's mode to `PatternEditing.save` instead of `Mode.ANY`, and `orderPreview`'s `saved` flag uses `PatternLibrary.contains` on the pattern with its mode, which uses `EncodedPattern.duplicates` (same items, or two Any patterns of one recipe and result).

### One JEI transfer path
`StorageTerminalRecipeTransferHandler` keeps its refusal while `ClientStock.orderGrid()` is false and sends a new `OrderGridRecipePayload(recipe id)`; `StorageTerminalMenu.fillFromRecipe(id)` fills through `CraftingGrid.templateOf(level, id, stock)` like `ProductionTerminalMenu.openEditorWith`. `fillGhosts` and `CraftingGrid.stockedChoice`'s list overload stay only if another caller needs them, else they go. JEI API: `IRecipeTransferHandler.transferRecipe` and `IRecipeTransferHandlerHelper.createUserErrorWithTooltip` (jei-26.1.2 common api, as used today).

### APIs
Minecraft 26.1.2: `AbstractContainerMenu.clickMenuButton`, `DataSlot`, `addDataSlot`, `Slot.isActive`, `AbstractContainerScreen.slotClicked`, `Screen.addRenderableWidget`, `Button.builder`, `Tooltip.create`, `GuiGraphicsExtractor.item` / `itemDecorations` / `setTooltipForNextFrame` (as used in the current screens). NeoForge: `CustomPacketPayload` registration in `ServusShuttlesPayloads` (`toMenu`), `ClientPacketDistributor.sendToServer`. No new event.

### Tests
- Pure, JUnit: `RecipeGridLayoutTest` (Foundation).
- GameTests in `shuttles/gametest/OrderGridGameTests`: `oneOffAnyUsesSpruce` (3 oak, 40 spruce, 16 sticks take 8 spruce, the oak stays), `oneOffExactWaitsForBirch` (the shared fixture always stocks oak planks, so the grid uses birch: Exact misses birch planks, Any does not), `oneOffAnyKeepsModeAfterReload` (order saved and loaded keeps Any and spruce), `saveAsPatternKeepsExact`, `saveHiddenForSecondAnyPattern` (crafting tables, since the fixture's Exact oak sticks pattern already blocks an Any oak sticks save), `clearButtonEmptiesGrid` (Clear keeps the mode, Cancel resets it), `transferSetsStockedGhosts` (now by recipe id, opens as Any, keeps an open grid's mode); `clearButtonEmptiesEditor` in `PatternGameTests` for the Production Terminal. `orderGridSavesAny` (from `m8-pattern-variants`) stays green.
- Manual in the client: both screens' look, drag fill in the editor, greyed buttons, JEI transfer into both.

## Risks / Trade-offs

- [The Production Terminal's editor gets a wider popup over its list and tab row] → The area is dimmed and inert under the editor today; the inventory below stays free (popup ends at y 120, inventory at 122).
- [Any as default makes one-offs take other planks than the player put in] → The amount panel's From stock lists the planks before Start, and Exact is one click away; it matches the editor's default.
- [Removing `GridFillPayload` breaks a mixed client and server] → Client and server ship in one jar; no persisted data uses it.
- [Moving `GreyOut` to Foundation touches `m8-terminal-tabs` code] → Applied after that change is archived.

## Migration Plan

None. Saved one-off orders and patterns already carry their mode; old orders load Exact.

## Decisions made with the user

- **Default mode for one-off orders: Any when Any is possible** (user's answer), like a new pattern in the editor, so Next shows the stocked planks. `PatternGrid.resetMode` sets it.
- **Clear also in the Production Terminal** (user's answer), between Cancel and Delete.

## Decisions made during apply

- **Layout numbers.** Popup 236 x 90; without a title the cells' first item origin is at (59, 6), arrow (118, 28), result (139, 25), extra (159, 24, 18 x 18), mode button (131, 44, 32 x 14); a title row (6, 4) moves these down by 9. The action row is (6, 72, 224 x 14) with 52 px buttons, 4 px apart, at most four. `RecipeEditorButtons.gridY(popupY, titled)` takes the title flag; `GRID_OFFSET_X/Y` went.
- **View API.** `RecipeGridView(x, y, Supplier<Component> title or null, Source, List<Action>)`. `Source` gives `result()`, `resultNotes()`, `mode()` and `toggleMode()`; `PatternGridClient` implements it for both screens. `Action(label, shown, refusal, press)` with factories `cancel`, `clear` (Foundation lang `gui.servus_foundation.recipe_grid.*`) and `needsResult` (greyed with "The grid matches no recipe"). The view rearranges the shown actions every frame, so Delete only takes a place for an existing pattern. Its buttons are vanilla `Button`s; the screen adds `build(left, top)` as widgets. `press` and `drag` take the cursor state and a cell callback; the screens send `slotClicked(PICKUP)` and swallow the release.
- **Cancel in the order grid is a menu button.** `StorageTerminalMenu.clickMenuButton` handles `CANCEL` (clear and reset to Any), `MODE` and `CLEAR`; `OrderStartPayload`, `OrderSavePayload` and the preview read the menu's mode.
- **JEI open or keep.** The server cannot see whether the client's order grid is open, so `OrderGridRecipePayload(recipe, keepMode)` carries it; `StorageTerminalScreen.requestOrderGrid()` returns whether the grid was open, also while JEI's recipe view is in front.
- **The "+ New pattern" line button** of the Patterns tab drew with the removed `RecipeEditorPopup.drawButton`; it moved to `PanelPainter.flatButton` in Shuttles, its only user.
- **Removed lang keys:** `crafting_station.cancel`, `crafting_station.no_match`, `terminal.order_clear` (now the Foundation keys).

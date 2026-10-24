# Design

## Context

`StorageTerminalScreen` draws five dialogs over the item grid from one set of widgets: the shared `amount` field and six `stepButtons` (request prompt, craft dialog, order grid), `confirmButton`, `cancelButton`, and the order grid's `saveButton` and `onceButton` for the "Save it?" question. The craft dialog lays out amount, steps, three preview columns of three rows, a status line and Start. The order grid lays out its ghost cells (menu slots at fixed `TerminalLayout.Storage.ORDER_*` offsets), the result in a slot frame, the amount, a status, the steps, a two row preview and Start. `OrderStartPayload(amount, save)` starts the order and saves the pattern on the way. The layout was settled with the user on the mockup `temp/craft-flow.html`.

## Goals / Non-Goals

**Goals:**
- One amount panel for the craft dialog and the order grid's second step.
- Two reusable Foundation widgets with pure, tested rules.
- Saving a pattern without starting an order.
- The JEI transfer and carried stack fixes found in the play check of `m8-request-slots`.

**Non-Goals:**
- The request prompt and the stock rule dialog keep their widgets and layout (open point in PLAN.md).
- No change to `OrderBook`, `OrderPlanner` or what `CraftPreviewPayload` carries.

## Decisions

### Foundation widgets as helpers the screen forwards to
Like `ItemPicker`, the widgets are plain classes, not `AbstractWidget` subclasses: the screen creates them in `init`, adds the vanilla widgets they own with `addRenderableWidget`, places them with absolute coordinates, draws them again over its dialog panel and forwards wheel events. This fits how the terminal already draws dialogs over the grid (widgets drawn by super below the panel, then again on top).

- `foundation/client/NumberStepper`: owns an `EditBox` (digits only, five characters) and one `Button` per step and sign. Constructed with the font, the steps (for example `1, 10, STACK`), a stack size supplier, min, max and an `IntConsumer` for changes. `place(x, y)` puts the plus row at `y`, the field (40 by 14) at `y + 16` and the minus row at `y + 32`; buttons are 20 by 14 with 1 px gaps, so three steps make a block 62 wide. `fieldRight()` lets the screen draw beside the field. Stack labels follow the supplier (+64, +16, +1). Wheel over the field steps by 1; Enter or focus loss clamps.
- `foundation/client/ItemListView`: a heading line (9 px) and rows of 9 px with a half size icon, then `count name` cut by `Font.plainSubstrByWidth`; a 2 px scrollbar on the right while rows exceed the height. `setRows` keeps the offset where it still fits; `mouseScrolled` moves one row; `rowAt` returns the hovered row for the item tooltip. The text colour is passed in.
- Pure rules in a new package `foundation/ui` (no Minecraft imports): `AmountSteps` (moved from `shuttles/terminal`, unchanged API) and `ListScroll` (`clamp(offset, rows, visible)`, `scroll(offset, delta, rows, visible)`, `thumb(offset, rows, visible, height)`). Tests: `AmountStepsTest` (moved), `ListScrollTest`: `clampsToLastPage`, `fewerRowsThanVisible`, `scrollStopsAtEnds`, `thumbSpansTrack`, `thumbMovesWithOffset`.

### One amount panel, two flows
The `Dialog` enum keeps `CRAFT` for the amount panel and `ORDER` for the order grid; a `fromOrder` flag on `CRAFT` says whether Back returns to the grid, whether the preview mode is `ORDER` (server encodes the grid) or `CRAFT` (the item), and whether Start sends `OrderStartPayload(amount)` or `CraftStartPayload(item, amount)`. The ghost cells stay active only in `ORDER` (`menu.setOrderOpen`), so they take no clicks behind the amount panel. The request prompt and rule dialog keep `amount`, `maximum` and `stepButtons`; the amount panel uses its own `NumberStepper`, so the two never fight over one field.

The crafted count beside the amount is the To craft entry of the preview for the item itself, else the amount while the preview is on its way. This is exact for multi output recipes and needs no recipe lookup on the client.

### Panel geometry: a popup at the base rows
The order grid and the amount panel are popups drawn with Foundation's `PopupPanel`, which also dims the terminal from its top down to just above the inventory label, so with a tall grid there is no stretched panel and no empty band. The ghost cells are menu slots (`Slot.x` and `Slot.y` are final in 26.1), so the popups sit at fixed offsets of the base rows in `TerminalLayout.Storage`, centred on the panel's middle (x 95) and wider than it (236), and they end above the inventory label (y 148) so the inventory stays usable for picking ghost items. Clicks in the dimmed area above the inventory are swallowed, and slot tooltips there are not shown.

- Order grid popup `ORDER_POPUP` (-23, 40, 236, 90): grid, arrow, result (16 by 16, no frame) and Save (18 by 18) as one centred group from x 36 to 154, the grid at y 46; buttons at y 110: Cancel 50 wide on the left, Clear 50 wide in the middle, Next 60 wide on the right.
- Amount popup `CRAFT_POPUP` (-23, 18, 236, 128): title at y 24, the stepper block centred at x 58 from y 36 to 82 with the item beside the field, three lists 72 wide 4 apart from x -17, headings at y 86 and three rows to y 122, buttons at y 126: Back or Cancel 50 wide on the left, Start 60 wide on the right.
- `NumberStepper` buttons are 24 by 14 and its field 48 by 14, so "+64" and five digits fit without the vanilla label scrolling.
`TerminalLayoutTest` checks the parts are disjoint and inside their popup, and that both popups end above the inventory label.

### Clearing the order grid
Clear, Cancel and Escape send `GridFillPayload` with no options, which `fillGhosts` turns into nine empty cells. `startOrder` clears the cells on the server once the order is made, so a refused order keeps them. Back and Next keep the cells.

### Dragging over ghost cells
Vanilla quick crafting only collects slots that accept the item, and a ghost cell accepts nothing. The screen therefore clicks a ghost cell on press while an item is carried, and `mouseDragged` clicks each further cell the mouse passes (`slotClicked` with `PICKUP`), once per cell per drag; `StorageTerminalMenu.clicked` sets the copy as for a click.

### Presses a dialog takes
`AbstractContainerScreen` places a carried stack on mouse release into the slot under the mouse. A press a dialog or popup takes (a widget, a ghost cell, the dimmed area) sets a flag, and `mouseReleased` then only releases the focused widget, so Cancel with a stack in hand drops nothing into the Input row behind the popup.

### Craft button
The Craft button is an 18 by 18 icon button with a hammer (`textures/gui/terminal/craft.png`) in a column right of the panel (`RIGHT_SIDE_X`, level with the first view button), so the title has room up to the search box. The column is a JEI exclusion area and counts as inside the screen for clicks.

### Start greyed with the reason as tooltip
`Button.active = false` plus `setTooltip` each tick from the current preview: no preview yet ("Working out the craft"), refusal key (`message.<refusal>`), missing (`message.missing`). A startable preview clears the tooltip. Next in the order grid uses the same with `message.no_match`. Vanilla shows a button's tooltip also while inactive (`AbstractWidget` tooltip handling), so no extra drawing is needed.

### Save without starting
New `OrderSavePayload` (no fields) calls `StorageTerminalMenu.savePattern()`, which encodes the grid, saves it with `PatternEditing.save` on the terminal's line and sets the message (`no_match`, `pattern_no_storage`, `pattern_full`, or empty). `OrderStartPayload` drops its `save` flag and `startOrder(amount)` orders once. The Save button shows while the order grid's last preview says `patternSaved == false` for the current cells; after a save the screen asks for a new preview, which hides it. The order grid asks its preview for one craft's count, since it shows no amount.

### Fixes
- JEI: `StorageTerminalScreen.removed` calls `ClientStock.close()` only when the player's container menu is no longer this terminal's menu. `LocalPlayer.clientSideCloseContainer` resets `containerMenu` before `setScreen(null)`, so a real close still forgets the content, while JEI's recipe screen in front keeps it and `ClientStock.orderGrid()` stays true for `StorageTerminalRecipeTransferHandler`.
- Carried stack: the screen overrides `AbstractContainerScreen.extractContents` instead of `extractRenderState`; in 26.1 `extractRenderState` calls `extractContents`, then `extractCarriedItem` and `extractTooltip` (checked in the patched Minecraft sources), so dialogs drawn at the end of `extractContents` lie below the carried stack.

### GhostSlot
`shuttles/menu/GhostSlot` is the same class as `foundation/logistics/menu/GhostSlot` apart from one word of Javadoc. The four Shuttles menus import Foundation's and the copy is deleted.

## Pure vs Minecraft

- Pure, JUnit: `foundation/ui/AmountSteps`, `foundation/ui/ListScroll`.
- GameTests: `OrderGridGameTests` (save without starting, refused save keeps the grid, one-off start without saving, preview for the order panel).
- GameTests: a started one-off order clears the grid.
- Manual play check: popups, widgets, tooltips, drag over cells, Clear, JEI transfer, carried stack.

## Risks / Trade-offs

- [Three visible rows per list are few] → they scroll, and the item tooltip names cut rows.
- [The request prompt still looks different] → noted as an open point in PLAN.md.

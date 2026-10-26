# Proposal

## Why

The Production Terminal's recipe editor and the Storage Terminal's order grid are two views of the same thing, a 3x3 ghost grid with the recipe's result, built twice with different looks and rules. Since `m8-pattern-variants` only the editor has the Exact / Any button, so a one-off order from the order grid always crafts with exactly the planks in its cells, and "Save as pattern" can only save the default. The user asked during the play check for one shared view with the same options in both terminals.

PLAN.md milestone M8 On-demand autocrafting (patterns, Crafting CPU, Production Terminal, Storage Terminal). PLAN.md has no item for it: it was asked for in the play check of `m8-pattern-variants`.

**Order of work.** This change touches the code of two unarchived changes, `m8-pattern-variants` (mode button in `RecipeEditorPopup`, `ProductionTerminalMenu` mode, `PatternEditing.save` with a mode) and `m8-terminal-tabs` (the Storage Terminal's `Dialog` enum, `ItemPopup`, the amount panel). The user asked for it explicitly. It is applied only after both are archived, so its delta specs build on their requirements.

## What Changes

- **One shared recipe grid view in Foundation** used by both terminals: the 3x3 ghost cells, an arrow, the result as a bare item icon with the count of one craft and its normal tooltip, the Exact / Any mode button, Clear, and an action row whose buttons each screen supplies. Ghost behaviour is the same in both: click with an item sets a single ghost copy, dragging the cursor's item over cells fills each cell it passes, click with an empty cursor clears the cell, nothing real lies in the grid. One popup size for both.
- **Production Terminal actions:** Cancel on the left, Clear and Delete (existing pattern only) in the middle, Save on the right. Save is greyed out with "matches no recipe" while the grid matches nothing.
- **Storage Terminal actions:** Cancel on the left, Clear in the middle, Next on the right (then the shared amount panel with Back and Start, unchanged), and the "Save as pattern" icon beside the result.
- **Exact / Any in the order grid.** The order grid gets the same mode button. It starts as Any when Any is possible, is kept by Next and Back, and starts again as Any when the grid is opened empty.
- **One-off orders honour Any.** Next's preview and Start plan the one-off order with the chosen mode: an Any order takes, per ingredient, the accepted item the warehouse holds most of, as a saved Any pattern does. The order keeps its mode over a restart.
- **Save as pattern keeps the chosen mode.** The icon shows only while the line has no pattern this one would duplicate under the chosen mode.
- **One JEI transfer path.** JEI's transfer button fills either grid the same way (by recipe id on the server, each cell the most stocked accepted stack with its components, else the ingredient's first item), and a transfer that opens the grid starts as Any when possible.

### Where the two views differ today, and which behaviour wins

| Behaviour | Production Terminal editor | Storage Terminal order grid | Shared view |
|---|---|---|---|
| Filling cells | click only | click and drag over cells | click and drag (order grid) |
| Result | framed slot after "=", tooltip "matches no recipe" when empty | arrow and bare icon with count, normal tooltip | arrow and bare icon (order grid); the "no match" reason moves to the greyed confirm button |
| Mode button | Exact / Any left of Save | none | both |
| Clear | none | Clear button | both |
| Buttons | column right of the grid, 58 px flat buttons (Save, Delete, Cancel) | row below the grid, vanilla buttons (Cancel, Clear, Next) | row below, vanilla buttons, Cancel left, confirm right (order grid, as the craft flow decided) |
| Popup | 160 x 72 with title | 236 x 90 without title | 236 x 90 with an optional title row |
| JEI transfer | recipe id, server fills from stock | JEI slot views, server picks from stock, refused without CPU and Station | recipe id path (editor); the Storage Terminal keeps its refusal |
| Saving | Save always pressable, refusal in the action bar | icon only while the line lacks the pattern, refusal on the message line | kept per screen; both store the chosen mode |
| Grid life | cleared when the editor closes | kept through Next, Back and a screen in front | kept per screen |

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/gui-widgets`: a shared recipe grid view (ghost cells, result, mode button, Clear, action row supplied by the screen).
- `shuttles/storage-terminal`: the order grid on the shared view with Exact / Any and Clear; one-off orders and their preview honour the mode; Save as pattern stores the chosen mode; the JEI transfer sets the mode.
- `shuttles/production-terminal`: the recipe editor on the shared view (drag fill, Clear, result look, buttons in a row, Save greyed without a match); the JEI transfer into the editor.

## Non-goals

- Any for recipes whose result changes with the ingredients (the Heavy Duty Rack's wood, the Elevator's rail): the follow-up change `m8-material-variants`.
- Tabs or a pattern list in the order grid, or editing saved patterns from the Storage Terminal.
- Moving the amount panel, the item popup or the Production Terminal's lists to Foundation.
- Recipe kinds other than vanilla 3x3 crafting in the shared view (machine recipes of other mods).

## Impact

- New `foundation/.../client/RecipeGridView` (replaces `RecipeEditorPopup`), new pure `foundation/.../ui/RecipeGridLayout` with JUnit, `foundation/.../block/RecipeEditorButtons` gains `CLEAR`.
- New `shuttles/.../pattern/PatternGrid` (common: ghost cells, mode, fill and clear for both menus) and `shuttles/.../client/PatternGridClient` (result, variants and mode tooltip for both screens).
- `shuttles/.../menu/ProductionTerminalMenu`, `menu/StorageTerminalMenu` (mode data slot, `clickMenuButton` for MODE and CLEAR, mode in preview, start and save), `client/ProductionTerminalScreen`, `client/StorageTerminalScreen`, `terminal/TerminalLayout` (one popup rect for both), `terminal/OrderStartPayload` unchanged (the mode lives in the menu), `terminal/GridFillPayload` replaced by the recipe id transfer, `integration/jei/StorageTerminalRecipeTransferHandler`.
- Lang entries move from `gui.servus_shuttles.production.mode.*` to shared keys.
- No saved data change: `WorkOrder` already saves its one-off pattern with `EncodedPattern.CODEC`, which carries the mode.

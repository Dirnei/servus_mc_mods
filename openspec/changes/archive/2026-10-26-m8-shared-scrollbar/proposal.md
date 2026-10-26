# Proposal

## Why

During a play check the user asked: "the production terminal needs a scrollbar. any view that is scrollable should get a scrollbar". The Production Terminal list scrolls only with the mouse wheel and shows no hint that there is more below. Other views do have scrollbars, but each draws its own: the Storage Terminal grid has a fixed size thumb that can be dragged, the item picker has a proportional thumb that can be dragged, the channel popup draws a thumb that cannot be grabbed, the craft preview lists draw a 2 pixel bar that cannot be grabbed, and the Claim an export picker has none. Players cannot tell which bars work.

Milestone M8 (Storage Terminal, Production Terminal), where the Foundation GUI widgets were introduced (`m8-craft-panel`). It also covers the "Production Terminal lists on the Foundation widgets" part of the Shuttles open point in PLAN.md about moving screens onto the Foundation widgets.

## What Changes

- Foundation gets one shared scrollbar widget with one behaviour and one look: a sunken track beside the list, a raised thumb whose length is the visible share of the content, dragging the thumb, a press on the track that jumps there and keeps dragging, the mouse wheel over the list or the bar, and a full length greyed thumb that takes no drag when nothing scrolls.
- Every scrollable list or grid in the family uses it. Views that get a scrollbar or move to the shared one:
  1. **Production Terminal** list, on all five tabs: Crafting (jobs with their buffer and overflow lines), Patterns, Machines (machines, the data line and Packing Stations), CPUs and Storage. Each tab keeps its own scroll position. New scrollbar; the panel gets wider so no row part moves.
  2. **Storage Terminal** item grid on the Stock tab and the rule rows on the Rules tab: the existing bar moves to the shared widget; the thumb becomes proportional instead of a fixed 15 pixels, and pressing on the thumb grabs it where pressed instead of jumping.
  3. **Storage Terminal amount panel** lists From stock, To craft and Missing (Foundation item list): the 2 pixel bar becomes the shared one and can be dragged.
  4. **Item picker popup** (Foundation): Utility Duct filter picker and fluid filter picker, the channel form's icon picker, the Field Head crop, sapling and other pickers, the Fluid Gantry fluid lock picker. Moves to the shared widget; behaviour stays, look and greyed state follow the shared style.
  5. **Channel popup** of the Utility Duct endpoint GUI: its thumb becomes draggable and the track clickable.
  6. **Claim an export picker** of the Outbound Station: new scrollbar beside its rows.
  7. **Warehouse Controller "Other sites" panel**: still shows 16 rows, but scrolls past 16 sites with the shared scrollbar beside the rows.
- The bar is always shown where a view puts it, greyed with a full length thumb while nothing scrolls.
- The recipe editor popup and the order grid get the bar on any scroll area they have once `m8-shared-recipe-grid` has made them one shared view; that change is in flight in parallel and owns those screens.
- The scroll math (thumb length with a usable minimum, thumb position, offset from a drag with a grab point) stays pure in `ListScroll` and is covered by JUnit tests.

Views that are skipped and why:
- Heavy Duty Rack GUI: its spec says it grows with the section and does not scroll.
- Storage Terminal item popup previews (two rows, then "n more"): not scrollable.
- Field Head and Fluid Gantry status lines (cut after three or two wrapped lines): text, not lists.
- Number steppers, the one line stepper, Outbound keep slots: the wheel changes a value, not a view.
- Alchemy screens (Coagulator, Receptaculum), Delicacies and Brewery: no scrolling views. The journal is a Modonomicon book and draws its own pages.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/gui-widgets`: a shared scrollbar widget, the rule that every scrollable list or grid shows it, the item list's scrollbar becomes the shared one, and the scrollbar math joins the pure widget rules.
- `shuttles/production-terminal`: the list shows the shared scrollbar on every tab; the screen layout gives it its own column.
- `shuttles/storage-terminal`: the grid scrollbar becomes the shared one with a proportional thumb and a grab point, on the Stock and the Rules tab.
- `shuttles/warehouse-controller`: the "Other sites" panel scrolls past 16 sites with the shared scrollbar.

## Non-goals

- No scrolling for the item popup previews.
- No change to wheel step sizes: one row per notch in row based views, 10 pixels per notch in the Production Terminal.
- No horizontal scrollbars.
- No keyboard scrolling (Page Up, Page Down, arrows).
- The other screens of the Shuttles open point (Field Head amounts, Fluid Gantry keep steps, the request prompt and stock rule dialog) stay as they are.

## Impact

- `foundation/ui/ListScroll` (pure): minimum thumb length, offset from a drag with a grab point; `ListScrollTest` extended.
- New `foundation/client/Scrollbar` widget; `ItemListView`, `ItemPicker` and `logistics/client/ChannelPopup` use it; `UtilityDuctScreen` forwards drag and release to the channel popup bar.
- `shuttles/terminal/TerminalLayout.Production`: wider panel, a scrollbar rectangle, popup and inventory moved to stay centred; `ProductionTerminalMenu` slot positions follow the layout constants; `TerminalLayoutTest` updated.
- `shuttles/client/ProductionTerminalScreen`, `StorageTerminalScreen`, `ExportPicker`, `OutboundStationScreen` (forward drag and release), `WarehouseControllerScreen` (site list).
- Overlaps with unarchived changes that touch the same screens: `m8-pattern-variants` (ProductionTerminalScreen), `m8-terminal-tabs` (StorageTerminalScreen, RulesList, `foundation/gui-widgets`), `m8-rule-export` (ExportPicker), `m5-duct-endpoint-gui` (UtilityDuctScreen), and the parallel `m8-shared-recipe-grid` and `m8-material-variants` (recipe editor, order grid, `TerminalLayout`). The edits to the terminal screens stay small and limited to their own lists; see design.

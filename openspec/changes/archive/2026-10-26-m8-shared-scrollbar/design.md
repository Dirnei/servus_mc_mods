# Design

## Context

See proposal.md for the request and the list of views. What the code does today:

| View | Class | Scroll unit | Scrollbar today |
|---|---|---|---|
| Production Terminal list, all five tabs | `shuttles/client/ProductionTerminalScreen` | pixels, 10 per wheel notch, one offset per tab | none, wheel only |
| Storage Terminal grid (Stock) and rule rows (Rules) | `shuttles/client/StorageTerminalScreen`, `RulesList` | rows | own code: inset track `TerminalLayout.Storage.Layout.scrollbar()` (x 174, 8 wide), fixed 15 px thumb, light `0xFFE0E0E0`, grey `0xFF9A9A9A` when nothing scrolls, drag and track press via `ListScroll.offsetAt` (thumb centres on the mouse, also when pressed on the thumb) |
| Amount panel lists From stock, To craft, Missing | `foundation/client/ItemListView` | rows | 2 px bar, proportional thumb via `ListScroll`, hidden when it fits, no drag |
| Item picker popup (duct item and fluid filter, channel icon, Field Head crop, sapling and tag pickers, Fluid Gantry lock) | `foundation/client/ItemPicker` | rows | own code: 6 px dark bar, proportional knob (min 8), drag and track press, hidden when it fits |
| Channel popup | `foundation/logistics/client/ChannelPopup` | rows | own code: 6 px dark bar, proportional knob (min 8), hidden when it fits, wheel only |
| Claim an export picker | `shuttles/client/ExportPicker` | lines (headings count) | none, wheel only |
| Warehouse Controller "Other sites" panel | `shuttles/client/WarehouseControllerScreen` | rows | none, cut after 16 sites |

`foundation/ui/ListScroll` already holds pure clamp, thumb length (minimum 2), thumb start and `offsetAt` (centre on the mouse) with `ListScrollTest`. Four views draw a bar three different ways; only two can be dragged.

Unit independent: `ListScroll` works for rows and for pixels alike, as long as total, visible and offset share one unit.

## Goals / Non-Goals

**Goals:**
- One Foundation widget that owns the track, the thumb, the drag state and the drawing; views only tell it the content size, the visible size and their offset, and forward mouse events.
- Every view in the table uses it.
- All arithmetic stays in `ListScroll`, unit tested.

**Non-Goals:**
- No generic scrolling container that also draws rows: each view keeps its own row drawing, hit testing and wheel step.
- No change to wheel step sizes or to what a view does on a row click.

## Decisions

### D1. One widget, `foundation/client/Scrollbar`

```java
public final class Scrollbar {
    public static final int WIDE = 8;    // panels: Storage and Production Terminal
    public static final int NARROW = 4;  // popups and the item list

    public void setArea(int x, int y, int width, int height); // absolute track inside; the frame draws 1 px around it
    public void setContent(int total, int visible);           // same unit as the offset
    public int offset();
    public void setOffset(int offset);                         // clamped by ListScroll.clamp
    public boolean scrollBy(int delta);                        // for the view's wheel step
    public boolean scrolls();
    public boolean contains(double mouseX, double mouseY);     // frame included
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
    public boolean mouseClicked(double mouseX, double mouseY, int button); // left button on the bar: grab or jump, start the drag; true when on the bar
    public boolean mouseDragged(double mouseY);                // true while it holds the drag
    public boolean mouseReleased();                            // true when it ended a drag
}
```

The widget owns the offset, so a view reads `offset()` when it draws and hit tests. Views with several offsets (Production tabs, Storage Stock and Rules) keep one `Scrollbar` per offset, or one bar whose offset and content they swap on a tab change; the Production Terminal keeps one bar per tab, which replaces its `int[] scrolls`. The Storage Terminal keeps one bar and sets content and offset from `scrolled()` and `maxScroll()` when the tab changes, because `RulesList` owns its own scroll (`setScroll`), which `m8-terminal-tabs` introduced.

Alternatives: a static painter plus per view drag code, like today. Rejected: the drag state and the grab point would be copied into six places again. A vanilla `AbstractScrollArea`: rejected, it is a widget that also owns the content area and its own look, and our views draw their rows in panel coordinates inside `extractLabels`.

### D2. Pure rules in `ListScroll`

- `MIN_THUMB = 6`; `thumbLength` returns `max(min(MIN_THUMB, track), track * visible / total)`. The old minimum of 2 cannot be grabbed. ItemPicker (126 px track) and ChannelPopup (108 px track) used 8; 6 keeps a 3 of 11 thumb on a 27 px track at its true 7 px.
- `grabAt(double along, int thumbStart, int thumbLength)`: the grab point, `along - thumbStart` when the press is on the thumb, else `thumbLength / 2.0` (track press centres the thumb).
- `offsetFor(double along, double grab, int thumb, int track, int maxOffset)`: the offset whose thumb puts its grab point at `along`, clamped to 0 and `maxOffset`, 0 when nothing scrolls. `offsetAt` had only the Storage Terminal as caller; it is removed and its tests run against `offsetFor` with the centre as grab.

### D3. Look

Same family colours as `PanelPainter` and `PopupPanel`:
- Track: the sunken frame of `PanelPainter.inset` (dark `0xFF373737` top and left, white bottom and right, 1 px outside the area) filled with slot grey `0xFF8B8B8B`.
- Thumb, scrollable: a small raised box over the full track width, face `0xFFC6C6C6`, 1 px white top and left, 1 px `0xFF555555` bottom and right; face `0xFFE0E0E0` while hovered or dragged.
- Thumb, nothing to scroll: flat `0xFF9A9A9A` (the inactive tab grey) over the whole track, no highlight. The Storage Terminal already greys this way; the bar is shown, not hidden, so a layout never jumps and the player sees the list is complete.

The widget draws these itself with `GuiGraphicsExtractor.fill`, so Foundation does not depend on the Shuttles `PanelPainter`. Width `WIDE` (8) in panels, `NARROW` (4) in popups and the item list; with the 1 px frame the narrow bar has the same 6 px footprint the pickers use today.

### D4. Fitting each view

All coordinates are relative to the panel or popup origin; "area" is the track inside, the frame is 1 px around it.

1. **Production Terminal** (`TerminalLayout.Production`). Panel `WIDTH` 240 to **252**. `LIST` stays `(8, 34, 224, 180)` so no row part moves (they end at x 220 of the list). New `SCROLLBAR = (236, 34, 8, 180)`, frame 235 to 245, 7 px to the panel edge like the Storage Terminal (bar 174 to 182 on a 190 panel). `TITLE` and `TABS` widen to `(8, 6, 236, 9)` and `(8, 18, 236, 12)` so the tabs span the list and the bar. The editor stays centred: `POPUP` x 40 to 46 ((252 - 160) / 2), `INVENTORY` and `HOTBAR` x 40 to 45 ((252 - 162) / 2); `ProductionTerminalMenu` reads these constants, so its slots follow. `areas()` gains `("scrollbar", SCROLLBAR)`; `editorAreas()` is unchanged (the bar is hidden while editing). In the screen every `WIDTH - MARGIN` that means the list's right edge (scissor, `lineAt`, hover shade, pattern row Any tag, capacity bar, busiest duct clip) becomes `LIST.right()` or `LIST.w()`; the separator under the tabs keeps `WIDTH - MARGIN` and spans the bar. Content is the sum of the line heights, visible 180, wheel `scrollBy(-scrollY * ROW)`. A tab click rebuilds the new tab's lines at once, so its bar is right in the same frame. 252 px is far below the 480 px of a 1080p screen at GUI scale 4.
   Alternative: keep 240 and narrow the list to 212. Rejected: every row part (job bar and cancel, machine and CPU bars, storage numbers and bar) would have to move or shrink, and the play checked row layouts would change.
2. **Storage Terminal**: area `layout.scrollbar()` as today, `WIDE`. Content is `maxScroll() + visible rows` of the shown tab, offset `scrolled()`, set on every frame and before a press (`syncScrollbar`), because the grid, the Rules tab and the height setting all change them; a press or drag writes the bar's offset back through `scrollTo`. `THUMB`, `dragThumb`, `renderScrollThumb` and `draggingThumb` go. No layout change.
3. **ItemListView**: bar `NARROW` at the right of the list's width, from the first row to the bottom of the visible rows; text width `width - TEXT_X - NARROW - 3` always (the bar no longer appears and disappears). Gains `mouseClicked`, `mouseDragged`, `mouseReleased`; `mouseScrolled` also answers over the bar. `StorageTerminalScreen` forwards these to `craftLists` while the amount panel is open.
4. **ItemPicker**: the bar footprint stays at `barX()` (list right + 3), 6 wide; area `(barX() + 1, listY() + 1, NARROW, ROWS * ROW - 2)`. `dragging`, `drag`, `clickBar`, `knobHeight` go; `mouseDragged` and `mouseReleased` keep their signatures, so the five screens that forward to it are untouched.
5. **ChannelPopup**: same footprint as today (`lx + LIST_WIDTH + BAR_GAP`, 6 wide), area as in the picker. `mouseClicked` asks the bar first, `mouseDragged(MouseButtonEvent)` passes to the bar when the form is closed, `mouseReleased` ends a bar drag. `UtilityDuctScreen` already forwards all three.
6. **ExportPicker** (popup `WIDTH` 168 stays): bar footprint x `WIDTH - 10` to `WIDTH - 4`, y `LIST_Y` to `LIST_Y + ROWS * ROW`; area `(x + WIDTH - 9, y + LIST_Y + 1, NARROW, ROWS * ROW - 2)`. Rows end at `WIDTH - 13` instead of `WIDTH - 4`, the threshold text right aligns at `WIDTH - 15`, the name gets `WIDTH - 43 - aboveWidth`. Unit is lines, headings included. `OutboundStationScreen` forwards `mouseDragged` and `mouseReleased` while the picker is open.
7. **Warehouse Controller "Other sites" panel** (panel 120 wide, right of the GUI): still at most 16 rows (`SITES_MAX`), so the panel's height does not change; with more sites it scrolls. Bar `NARROW` with its frame at x 109 to 115 of the panel, area `(110, 19, NARROW, rows * 11 - 2)` from the panel's top; names end at x 107 (`SITES_WIDTH - 13`) instead of 114, and a hover over the bar shows no site tooltip. Content the site count, visible 16. Shown only while the panel lists sites; the hint lines and "no other sites" have no bar. The wheel over the panel scrolls one row; the screen gains `mouseClicked`, `mouseDragged`, `mouseReleased` and `mouseScrolled` overrides that ask the bar first.

### D5. Event order

A view asks its bar first in `mouseClicked` (left button only) and returns when it took the press, so a press on the bar never clicks a row or a slot. While a drag is held, `mouseDragged` returns true before `super`, so `AbstractContainerScreen` does no quick craft drag with a carried stack, and `mouseReleased` returns true for the release that ends it. A press on a greyed bar is taken and does nothing, so it does not fall through to whatever lies under the bar.

### D6. Answers from the user

- **Bar always shown.** Every view shows its bar where its layout puts it, greyed with a full length thumb when nothing scrolls, so a layout never jumps (D3).
- **Production Terminal 12 px wider.** The panel goes from 240 to 252 so the bar gets its own column and no row part moves (D4.1).
- **Controller site list scrolls.** The "Other sites" panel scrolls past 16 sites instead of cutting the list (D4.7).
- **One shared widget, no copied code.** "Any view that is scrollable gets a scrollbar": all views use `Scrollbar`, none keeps its own bar code.

### D7. Recipe editor and order grid later

`m8-shared-recipe-grid` turns the Production Terminal's recipe editor and the Storage Terminal's order grid into one shared Foundation view, and runs in parallel with this change. This change does not touch those popups; any scroll area of the shared view gets the bar in a task that runs after `m8-shared-recipe-grid` lands. The edits to `ProductionTerminalScreen`, `StorageTerminalScreen` and `TerminalLayout` stay limited to the lists, the grid scrollbar and the amount panel's lists, to keep the rebase small.

### NeoForge and Minecraft APIs

No new API. The views already override `AbstractContainerScreen.mouseClicked(MouseButtonEvent, boolean)`, `mouseDragged(MouseButtonEvent, double, double)`, `mouseReleased(MouseButtonEvent)` and `mouseScrolled(double, double, double, double)` and draw with `GuiGraphicsExtractor.fill`, as `StorageTerminalScreen` does since `m8-terminal-scrollbar-drag` (archived 2026-10-25). The widget uses only these.

### Pure and Minecraft parts

- Pure, JUnit: `ListScroll` (D2) in `ListScrollTest`; `TerminalLayout.Production` areas in `TerminalLayoutTest` (disjoint, inside the panel, row parts inside `LIST.w()`, bar right of the list).
- Minecraft, play check: the `Scrollbar` drawing and event handling and every view in D4.

### PLAN.md

Settles the "Production Terminal lists" part of the Shuttles open point "Storage Terminal request prompt and stock rule dialog on the Foundation number stepper ... other screens (...) on the Foundation widgets". At archive that part is removed from the point. The Warehouse Controller sites panel scrolls with this change, so no open point is added for it.

## Risks / Trade-offs

- [Overlap with unarchived changes] `m8-pattern-variants` (ProductionTerminalScreen: mode button, Any tag in pattern rows), `m8-terminal-tabs` (StorageTerminalScreen scroll code with `rulesTab`, `RulesList`, the item popup, an ADDED gui-widgets requirement), `m8-rule-export` (creates ExportPicker), `m5-duct-endpoint-gui`, `m5-duct-fluid-filter`, `m5-duct-keep-amounts` (UtilityDuctScreen and its filter and fluid pickers), `m8-rule-cap` and `m8-station-numbers` (Storage Terminal and station screens) are applied on main and wait for their play check. → This change builds on their code as it is on main; if their play check reworks those screens, the later change merges onto the result. None of them modifies the requirements this change modifies; `m8-terminal-tabs` only adds gui-widgets requirements and modifies other storage-terminal requirements (not "Dragging the grid scrollbar").
- [Parallel `m8-shared-recipe-grid` and `m8-material-variants`] Both rework the recipe editor, the order grid and `TerminalLayout` (one popup rect for both terminals). → This change moves `Production.POPUP`, `INVENTORY` and `HOTBAR` only to keep them centred in the wider panel and leaves the popups alone (D7); whichever lands second keeps the popup centred in the 252 px panel.
- [Upcoming M13 fluid screens] `m13-fluid-inbound`, `m13-fluid-outbound`, `m13-fluid-rack` and `m13-fluid-side-modes` are not applied; any list they add falls under "Every scrollable view has a scrollbar" and uses the widget. The Fluid Gantry lock picker gets the bar through `ItemPicker` even if the gantry is replaced later.
- [Wider Production Terminal] 12 px wider changes a play checked layout. → Rows do not move; only the editor shifts to stay centred, and `TerminalLayoutTest` covers overlap and the 1080p width.
- [Bar always shown in the craft preview lists] Their text gets 4 px narrower when a list fits. → Names were already cut to the width; the Missing list keeps the same width whether full or empty.
- [Grabbing changes the Storage Terminal feel] A press on the thumb no longer jumps the thumb's centre to the mouse. → This is the usual scrollbar behaviour; a track press still jumps.
- [Pixel offsets in the Production Terminal] A drag can stop between lines. → The list already scrolls by 10 px per notch, not by lines; the scissor clips partial lines as today.

## Context

The Field Head GUI (`FieldHeadScreen`, `FieldHeadMenu`) shows everything on one page: crop slot, toggle, Range Upgrade slot, size line, ripe bar, threshold buttons, work status, a vertical water bar and, since `m10-rail-corrosion`, an extra row with the anti-corrosion slot. The panel is the garage texture drawn in two parts with 22 rows stretched in. The Utility Duct endpoint GUI (`m5-duct-gui-tabs`) already solved the same problem with tabs on the left edge: the selected page is client view state in the menu, slots of other pages are inactive, the screen draws the tabs with plain fills and an item icon.

## Goals / Non-Goals

**Goals:**
- Three tabs (Field, Water, Upgrades) in the duct tab look, the compact 176 by 178 garage panel, one blit.
- Shift click routing unchanged and independent of the tab.
- A red marker on a tab whose page holds a problem.

**Non-Goals:**
- No new data in the menu, no payloads, no block entity change.
- No new GUI texture.

## Decisions

- **Page state in the menu, client only.** `FieldHeadMenu.Page` (FIELD, WATER, UPGRADES) with `page()` and `setPage(Page)`, FIELD by default, like `UtilityDuctMenu`. The server never learns the page: shift click and the ghost slot need no page, and keeping the server out of view state avoids a payload.
- **Inactive slots.** Each of the three head slots is a subclass with `isActive()` that is true on the server and, on the client, true only on its page (crop on FIELD, anti-corrosion on WATER, Range Upgrade on UPGRADES). Vanilla `AbstractContainerScreen.findSlot` and slot rendering skip inactive slots, so slots of hidden pages may share a position: the crop and the Range Upgrade slot both sit at 8,18. Keeping server slots active means server side click handling never depends on a page it does not know. `moveItemStackTo` checks only `mayPlace`, so shift click into a hidden slot works on both sides.
- **Static page lookup.** `FieldHeadMenu.pageOf(int slot)` names the page of a head slot (null for inventory slots); the slot subclasses and the GameTest use it.
- **Layout** (panel 176 by 178, inventory at 96, hotbar at 154 as in the garage):
  - Tabs: 28 by 26 at the left edge, top 10, stride 28, the selected one 2 wider so it joins the panel; icons wheat, water bucket, Range Upgrade.
  - Field: crop slot 8,18 and toggle 30,19; ripe count 8,40 with the bar 60..168; "Harvest at" 8,53 with - and + at 136 and 152; the work status from 66, wrapped to two lines of 160. Without a served field the status warning sits at 40 and the counts are hidden.
  - Water: "Water x / y mB" at 8,20, a horizontal bar 8..168 at 31 with the old tooltip, hydrated or dry at 8,43, the anti-corrosion slot at 8,58 with the stage text beside it.
  - Upgrades: Range Upgrade slot 8,18 with "Radius n" beside it, the size line "n rails, n plots" at 8,44, or the status warning when no field is served.
- **Red marker.** A tab with a problem gets a red border and a red strip on its outer edge, and its tooltip adds the problem in a second line. Field: status not SERVING, or a work status drawn in the warning colour (same switch as the status line). Water: serving and not hydrated, or corrosion stage CORRODED.
- **Lang.** New keys `gui.servus_storage.field_head.tab.field|water|upgrades` and `gui.servus_storage.field_head.radius`; `gui.servus_storage.field_head.size` drops the radius ("%s rails, %s plots").

## Risks / Trade-offs

- [The work status text can be wider than the panel] Wrapped with `font.split` to two lines at 160 pixels.
- [Clicks on the tabs left of the panel could count as clicks outside] `mouseClicked` handles the tabs before `super`, as in the duct screen.

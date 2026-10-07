# Design

## Context

`UtilityDuctScreen` is 200 by 204 and draws a row per line at fixed y positions with 10 pixel channel buttons, the filter row at y 50 and the priority at y 72; `UtilityDuctMenu` adds nine `GhostSlot` filter slots at `SLOT_X + i * 18`, `FILTER_Y` and the inventory at `SLOT_X`. Buttons go through menu button ids; the priority comes through one data slot.

## Goals / Non-Goals

**Goals:**
- Each line gets the full panel; tabs are a client side view.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Layout
Panel 176 by 196: title at y 6 with side and block name; page area y 18 to 96; inventory at y 114, hotbar at y 172. Page: mode button at (8, 18) 160 wide 20 high; channel cells 16 by 16 at (8 + i * 20, 44) with a 2 pixel frame on the current one; remove button (152, 70) 16 wide; Item Pipe page: filter slots at (8 + i * 18, 70) and the priority line at y 94 with minus at 8 and plus at 60, the remove button moving to (152, 92). Tabs 28 by 26 at x = leftPos - 28, y = topPos + 10 + index * 28, drawn as a panel coloured block with the line's item centred; the selected tab is 2 pixels wider and flush with the panel.

### Page state
`UtilityDuctMenu.page` (a `Line`, default the first present line) is client side state set through `setPage`; the filter `GhostSlot` overrides `isActive()` to return `page == Line.ITEM`, which hides the slots and stops clicks on other pages. `rebuildButtons` runs on page change and on a line mask change. Lang keys `gui.servus_logistics.tab.item` and `.energy` for tooltips.

### Minecraft side verification
Client only; the existing Logistics GameTests keep covering the behaviour. Launch the client and open a two line duct: two tabs, both pages, filter slots gone on the energy page.

## Risks / Trade-offs

- [Slot positions on the server differ from the page] → ghost slots carry no items, so a hidden slot is harmless.

## Open Questions

None.

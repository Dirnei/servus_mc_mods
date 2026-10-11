## Why

The Field Head GUI grew with every M10 change (radius, threshold, work status, corrosion) and is now cramped: text lines run into the Range Upgrade slot and the water bar, and the panel had to grow a row for the anti-corrosion slot. Splitting it into tabs, like the Utility Duct endpoint GUI, gives each topic room and lets the panel shrink back to the compact garage size.

## What Changes

- The Field Head GUI gets three tabs on its left edge, drawn like the Utility Duct tabs (an item icon per tab, the selected tab joins the panel):
  - **Field** (default): crop ghost slot, harvest only toggle, ripe count with its bar and tooltip, harvest threshold with - and + buttons, the work status line with the harvester name, and the field status warnings (no field, another head, too large).
  - **Water**: the water bar with its tooltip, hydrated or dry, the anti-corrosion slot with its tooltip, the corrosion stage with the next stage percent, or the corroded warning.
  - **Upgrades**: the Range Upgrade slot with its tooltip, the radius and the field size line (rails, plots).
- The player inventory always shows below the tabs' page. Slots of other tabs are hidden and inactive; the page is client view state in the menu.
- Shift click keeps routing honeycomb (`servus_storage:anti_corrosion`) and Range Upgrades into their slots and back, whatever tab is open.
- A problem stays visible on every tab: a red marker on the tab that holds it (Field: field status or work status warning; Water: dry or corroded).
- The panel goes back to the compact 176 by 178 garage panel, drawn in one piece.
- Lang keys for the tab names and the new radius line, via datagen.
- Docs (`docs/src/content/docs/mods/storage.md`) and `PLAN.md` M10 list describe the tabs.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/field-head`: the Field Head GUI requirement describes the three tabs, the red tab markers and shift click across tabs.

## Impact

- `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/FieldHeadMenu.java`: page state, slot positions, inactive slots per page.
- `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/FieldHeadScreen.java`: tabs, pages, compact panel.
- `storage/src/main/java/io/github/dirnei/mc/servus/storage/datagen/ServusStorageLanguageProvider.java` and generated `en_us.json`.
- `storage/src/main/java/io/github/dirnei/mc/servus/storage/gametest/FieldGameTests.java`: a menu GameTest for slots and shift click.
- No payload, block entity or data change.

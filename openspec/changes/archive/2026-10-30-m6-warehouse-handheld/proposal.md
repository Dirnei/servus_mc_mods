# Proposal

## Why

Zoning feels like stamping paper today: a Zone Label copies its name and filter onto one rack bay per click, so renaming or refiltering a zone means walking back to every bay, and a bay gives no feedback about what it was or how full it is. Real warehouses work with a handheld scanner against master data. This change turns zoning into that: team zones as data, and one handheld that scans shelves, finds stock and shows what a shelf holds. It is the first step of the WMS multi tool; the wireless terminal (PLAN.md open point "Wireless Terminal with range tiers") joins it in a later change.

PLAN.md milestone M6 Warehouse automation, item "Zone Labels", reworked as the Warehouse Handheld.

## What Changes

- New item **Handheld HH-1** (yellow rubber bumper, red scan window, green screen). It has modes switched with sneak + scroll: **ZONE**, **FIND** and **INFO**. A screen panel on the HUD shows the mode and live data while the handheld is held.
- **Shelves, not bays** (revised after the play check, 2026-10-30): a **bay** is one rack block, a **shelf** is what specs and code called a section (up to `bays_per_shelf` bays side by side on one level), and a **rack** is the shelves stacked over all levels. The shelf is the unit of the handheld and of zones: one location code, one zone, one scan and one glow per shelf, for Heavy Duty Racks and Fluid Racks alike. A bay joining a zoned shelf takes its zone, a zoned bay joining an unzoned shelf zones the whole shelf; a shelf losing a bay keeps its zone.
- **Zones become team data**: each team keeps one list of zones with a name, one of the 16 dye colours and a filter (ghost items, item tags). Shelves point at a zone, so renaming, recolouring or refiltering a zone applies to every shelf at once. Any team member edits the list in the handheld's GUI.
- **ZONE mode**: right clicking any bay puts its whole shelf into the active zone (or clears it with the "No zone" entry). Holding right click sweeps across shelves, the beep rising per shelf. There is no sneak variant. Every scan beeps and flashes the shelf, and the action bar shows the shelf's code, before and after.
- **FIND mode**: pick an item, or scan a bay to take its shelf's top item. Every shelf of the bound warehouse holding it is tinted and outlined as a whole, seen through walls, for a few seconds, and the panel lists the shelf codes.
- **INFO mode**: scan a bay (beep and flash) to see its shelf's location code, zone, filter, slots in use against all slots of the shelf and top items across its bays on the handheld panel and the GUI; aiming alone shows nothing, and the readout stays until the next scan.
- **The GUI is the device** (revised after the first play check): drawn as the handheld with its yellow bumper, red window and a green CRT screen with scanlines and vignette, items in their own colours with short counts; it opens on the current mode's screen, and softkeys under the screen switch modes.
- **Zone filters through the item picker**: items and tags are added in Foundation's item picker popup with search instead of ghost slots and a tag line.
- **Foundation item picker tag search**: typing `#` lists item tags (pack tags included) with icon and item count, Tab completes the top hit, hover shows some items; only the zone form uses it here.
- **Binding**: the handheld binds to a warehouse by scanning its Warehouse Controller. FIND searches that warehouse.
- **Location codes**: every shelf of a warehouse with a controller has one code relative to the controller, north or south first, then east or west, then the signed height: `N12.E5+2`, `N8.E0+0`. It is taken from the shelf's bay closest to the controller (the leftmost one on a tie). A zero distance counts as N or E and is never dropped. The code is shown once per shelf on a small sticker on that bay's top beam, on Heavy Duty Racks and Fluid Racks, and in the handheld.
- Zoned shelves show a colour stripe in the zone's colour along the whole shelf and the zone name once on the beam.
- **Section becomes shelf everywhere**: player text (Data Potato rack reports, "Fluid Rack shelves" in the storage lines), specs and code identifiers. The datapack key `bays_per_section` becomes `bays_per_shelf` in `rack.json` and `fluid_rack.json`; the old key is still read.
- **BREAKING**: the Zone Label item is removed, with no migration. Zones stamped onto racks by labels are dropped on load, and labels in inventories disappear. Players set their zones again with the handheld. The Shuttles changelog says so.

## Non-goals

- LINK mode, the Parcel Locker, handheld models HH-2 and HH-3 and range tiers (`m11-handheld-link`).
- Direct Delivery upgrade (`m11-direct-delivery`).
- Changes to what the Data Potato reports: its rack report only says shelf instead of section.
- Player-chosen location names, aisle letters or bay counting.
- Zones on blocks other than Heavy Duty Racks and Fluid Racks.

## Capabilities

### New Capabilities
- `shuttles/handheld`: the Handheld item, its modes, mode switching, HUD panel, binding to a warehouse, ZONE scanning of whole shelves with sweep, FIND glow per shelf, INFO readout per shelf, sounds and flashes.
- `shuttles/location-codes`: controller-relative shelf codes, their format, which bay of a shelf gives the code, where they are shown and when they change.

### Modified Capabilities
- `foundation/gui-widgets`: opt-in tag search with `#` and Tab completion in the item picker.
- `shuttles/zones`: zones move from stamped copies to a team zone list that shelves point at, one zone per shelf. The Zone Label item and stamping are removed, assigning goes through the handheld, and the rack beam gains the colour stripe along the shelf.
- `shuttles/heavy-duty-rack`: section becomes shelf in every requirement (GUI, joins, docking, layers, Data Potato report), the bay's inner boards are called floor board and middle board, `bays_per_shelf`.
- `shuttles/fluid-rack`: section becomes shelf in every requirement (tank, joins, port, docking, stock, report, layers), `bays_per_shelf`.
- `shuttles/tanker`, `shuttles/slot-locks`, `shuttles/storage-fill`, `shuttles/warehouse-controller`, `shuttles/warehouse-rail`, `shuttles/shuttle-traffic`, `shuttles/fluid-inbound`: wording only, section becomes shelf.

## Impact

- Shuttles code: `zone/` (Zone Label item, menu, screen and payloads go; new zone list, saved data, sync, handheld item, menu and screen; `ShelfZones` and the code bay choice in `LocationCode`), the section to shelf rename across `rack/`, `fluidrack/`, `menu/HeavyRackMenu`, `stats/ShuttlesStatsManager` and the GameTests, `network/StorageNodeBlockEntity`, `network/WarehouseZones`, `network/WarehouseNetwork` (zone ids, location codes pushed to bays), `rack/HeavyRackBlock`, `fluidrack/FluidRackBlock`, `client/HeavyRackRenderer`, `client/FluidRackRenderer`, the storage fill rows of the controller and terminal, `/servus_warehouse zones`, datagen (item model, recipe, lang), GameTests in `gametest/ZoneGameTests`.
- Foundation code: `client/ItemPicker` tag search, pure `picker/TagSearch` with JUnit.
- New client code: device GUI textures and CRT drawing, HUD panel layer, scan line and bay flash renderer, FIND glow renderer, sneak + scroll mode switching.
- Save data: new overworld saved data for team zones. Old rack zone tags are ignored.
- Datapack: `rack.json` and `fluid_rack.json` use `bays_per_shelf`; `bays_per_section` keeps working.
- Changelog: Shuttles `## Unreleased` gets Added (handheld, team zones and location codes per shelf), Changed (racks speak of shelves instead of sections, `bays_per_shelf`) and Removed (Zone Label, stamped zones are lost).

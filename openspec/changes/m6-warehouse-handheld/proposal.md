# Proposal

## Why

Zoning feels like stamping paper today: a Zone Label copies its name and filter onto one rack bay per click, so renaming or refiltering a zone means walking back to every bay, and a bay gives no feedback about what it was or how full it is. Real warehouses work with a handheld scanner against master data. This change turns zoning into that: team zones as data, and one handheld that scans bays, finds stock and shows what a bay holds. It is the first step of the WMS multi tool; the wireless terminal (PLAN.md open point "Wireless Terminal with range tiers") joins it in a later change.

PLAN.md milestone M6 Warehouse automation, item "Zone Labels", reworked as the Warehouse Handheld.

## What Changes

- New item **Handheld HH-1** (yellow rubber bumper, red scan window, green screen). It has modes switched with sneak + scroll: **ZONE**, **FIND** and **INFO**. A screen panel on the HUD shows the mode and live data while the handheld is held.
- **Zones become team data**: each team keeps one list of zones with a name, one of the 16 dye colours and a filter (ghost items, item tags). Bays point at a zone, so renaming, recolouring or refiltering a zone applies to every bay at once. Any team member edits the list in the handheld's GUI.
- **ZONE mode**: right clicking a bay puts it into the active zone (or clears it with the "No zone" entry). Holding right click sweeps across bays. Sneaking takes the whole section. Every scan beeps and flashes the bay, and the action bar shows before and after.
- **FIND mode**: pick an item, or scan a bay to take its top item. Every bay of the bound warehouse holding it glows through walls for a few seconds.
- **INFO mode**: aim at a bay to see its location code, zone, filter, slots in use and top items on the handheld panel.
- **Binding**: the handheld binds to a warehouse by scanning its Warehouse Controller. FIND searches that warehouse.
- **Location codes**: every rack bay of a warehouse with a controller has a code relative to the controller, north or south first, then east or west, then the signed height: `N12.E5+2`, `N8.E0+0`. A zero distance counts as N or E and is never dropped. The code is shown on a small sticker on the bay's top beam and in the handheld.
- Zoned bays show a colour stripe in the zone's colour next to the zone name on the beam.
- **BREAKING**: the Zone Label item is removed, with no migration. Zones stamped onto racks by labels are dropped on load, and labels in inventories disappear. Players set their zones again with the handheld. The Shuttles changelog says so.

## Non-goals

- LINK mode, the Parcel Locker, handheld models HH-2 and HH-3 and range tiers (`m11-handheld-link`).
- Direct Delivery upgrade (`m11-direct-delivery`).
- Changes to the Data Potato: its rack section report stays as it is.
- Player-chosen location names, aisle letters or bay counting.
- Zones on blocks other than Heavy Duty Racks and Fluid Racks.

## Capabilities

### New Capabilities
- `shuttles/handheld`: the Handheld item, its modes, mode switching, HUD panel, binding to a warehouse, ZONE scanning with sweep and section, FIND glow, INFO readout, sounds and flashes.
- `shuttles/location-codes`: controller-relative bay codes, their format, where they are shown and when they change.

### Modified Capabilities
- `shuttles/zones`: zones move from stamped copies to a team zone list that bays point at. The Zone Label item and stamping are removed, assigning goes through the handheld, and the rack beam gains the colour stripe.

## Impact

- Shuttles code: `zone/` (Zone Label item, menu, screen and payloads go; new zone list, saved data, sync, handheld item, menu and screen), `network/StorageNodeBlockEntity`, `network/WarehouseZones`, `network/WarehouseNetwork` (zone ids, location codes pushed to bays), `rack/HeavyRackBlock`, `fluidrack/FluidRackBlock`, `client/HeavyRackRenderer`, `client/FluidRackRenderer`, the storage fill rows of the controller and terminal, `/servus_warehouse zones`, datagen (item model, recipe, lang), GameTests in `gametest/ZoneGameTests`.
- New client code: HUD panel layer, scan line and bay flash renderer, FIND glow renderer, sneak + scroll mode switching.
- Save data: new overworld saved data for team zones. Old rack zone tags are ignored.
- Changelog: Shuttles `## Unreleased` gets Added (handheld, team zones, location codes) and Removed (Zone Label, stamped zones are lost).

# Proposal: m8-station-numbers

## Why

Stations have no identity a player can read: a warehouse with three Outbound Stations shows three GUIs titled "Outbound Station", and the coming stock rule work (import caps at Inbound Stations, exports claimed by an Outbound Station) has to tell the player which station refuses an item or receives an export. Coordinates are no help while debugging, so every station gets a short number per warehouse and may get a name.

This belongs to milestone M8 (warehouse logistics). PLAN.md "Open: Shuttles" lists no item for it; it is the first of four changes from the 2026-10-26 exploration of warehouse excess and import regulation (`m8-station-numbers`, `m8-terminal-tabs`, `m8-rule-cap`, `m8-rule-export`).

## What Changes

- Every Inbound, Outbound and Packing Station gets a number when it first joins an online warehouse. Numbers count up per warehouse, with one counter each for Inbound, Outbound and Packing, start at 1 and are never given twice in that warehouse. A Packing Station reads "Packing #1" and never takes an Inbound number.
- A station keeps its number while it belongs to the warehouse that gave it, also through a carrot dismantle and placing it again. Placed into another warehouse, it gets a new number from that warehouse.
- A player may type a name for a station into a field in the title row of its GUI, cleaned like the site name of the controller.
- A station is named by its label wherever one station is named: "Outbound #3" without a name, "Compost Dock (#3)" with one. The GUI title shows it.
- The Data Potato gives a report on a station: its label, its warehouse and whether it is online.
- Stations in worlds saved before this change get numbers when their warehouse comes online after loading, without errors. A Packing Station that saved an Inbound number gets a Packing number instead.
- The warehouse's counters and its identity travel with the Warehouse Controller when it is dismantled with the carrot, so its stations keep their numbers.

## Capabilities

### New Capabilities

- `shuttles/station-numbers`: numbering and naming of warehouse stations, their label, and the Data Potato station report.

### Modified Capabilities

None. The station specs keep their requirements; numbers, names and the label live in the new capability.

## Impact

- `shuttles/.../station/StationBlockEntity` (number, name, numbering warehouse id, save, sync, item components), `InboundStationBlockEntity`, `outbound/OutboundStationBlockEntity` (display name).
- New pure `station/StationLabel` and `station/StationCounters`; reuse of `site/SiteName.clean`.
- `controller/WarehouseControllerBlockEntity` (warehouse id and station counters, saved and carried in the dismantled item), `network/WarehouseNetwork` (hands out numbers to member stations when online).
- `registry/ServusShuttlesComponents` (station number, name and warehouse id components), `registry` payload registration, new `station/RenameStationPayload`.
- `menu/InboundStationMenu`, `menu/OutboundStationMenu`, `menu/PackingStationMenu`, `client/InboundStationScreen`, `client/OutboundStationScreen`, `client/PackingStationScreen` (name field in the title row).
- `packing/PackingStationBlockEntity` (own counter) and `packing/PackingStationBlock` (the carrot dismantle keeps number and name).
- New `station/StationReport` and the `PotatoInspectable` hook on `InboundStationBlock` and `outbound/OutboundStationBlock`.
- Lang strings in `datagen/ServusShuttlesLanguageProvider`.
- JUnit `StationLabelTest`, `StationCountersTest`; GameTests for numbering, dismantle, renumbering and renaming.
- `PLAN.md` "Open: Shuttles": numbers for the Fluid Inbound and Fluid Outbound stations once M13 builds them.

## Non-goals

- A number plate or any mark on the block; the number shows only in the UI and in reports.
- Numbers for Fluid Inbound and Fluid Outbound stations (M13, not built yet), garages or racks.
- Using the labels in stock rule states, the Rules tab and the export picker (`m8-rule-cap`, `m8-rule-export`).
- Renaming a station from the Storage Terminal or by command.

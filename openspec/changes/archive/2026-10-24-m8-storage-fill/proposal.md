## Why

A player cannot see how big the warehouse is or how full it is. The Storage Terminal lists what is stored, but nothing says when the racks run out of room or when a zone such as "Ores" is full and put-away starts filling general storage. This change belongs to M8 (warehouse overview on the data line). It is a new open point raised in play: "I want to see somewhere the size of the storage and how full it is". This change adds it to PLAN.md under M8.

## What Changes

- **Storage fill**: the warehouse counts its item room in Heavy Duty Rack slots: slots in use against all rack slots. A slot counts as in use when it holds items or a job has locked it, so room a put-away already reserved does not show as free. Inbound, Outbound and Packing Station buffers are transit space and do not count. Fluid room is the fill of the Storage gantries' tanks in mB against their capacity, Capacity Upgrades included. The counts are broken down per zone, with the racks without a zone as "(no zone)".
- **Controller GUI**: two new lines between the map row and the upgrade row: Items with slots in use, total slots and a bar, and Fluids with mB stored, capacity and a bar. The tooltips name how many racks and Storage gantries count and list the full zones. The GUI grows to make room.
- **Production Terminal Storage tab**: a fifth tab. It shows one header per warehouse on the line (site name, slots and bar), one row per zone with its own bar, full zones first and then the fullest, then a Fluids row. The tab row fits five tabs by sizing each tab to its label.
- The Storage Terminal does not change.

## Capabilities

### New Capabilities
- `shuttles/storage-fill`: how a warehouse counts its item and fluid room, what counts as in use, the per zone breakdown and its order.

### Modified Capabilities
- `shuttles/warehouse-controller`: "Controller GUI" gains the Items and Fluids lines.
- `shuttles/production-terminal`: "Tab bar" gains the Storage tab, "Screen layout" fits five tabs, and a new "Storage tab" requirement describes its rows.

## Non-goals

- Fill counts in the Storage Terminal (the screen has no room).
- Data Potato reports on a rack section or a gantry (`m5-potato-storage-inspect`, a separate change).
- Fill per item kind or per rack section in a GUI, warnings or comparator output when storage is full.
- Fluids in the Storage Terminal (a separate open point in PLAN.md).

## Impact

- `shuttles`: a new pure `storage` package (fill counting), `WarehouseNetwork` keeps each rack's slot fill from its stock poll, `WarehouseZones` exposes a block's zone, `SiteStatusPayload` and `WarehouseControllerMenu` and screen get the storage lines, `ProductionPayload`, `ProductionRows`, `ProductionView`, `ProductionTerminalMenu` and screen get the Storage tab, and `TerminalLayout.Production` sizes the tabs by label.
- Lang keys through datagen, unit tests for the pure counting and the tab layout, GameTests for the counts.
- No new dependencies, no datapack values.

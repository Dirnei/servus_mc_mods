## Why

A data line may reach several warehouses today: the Storage Terminal adds their stock up, splits requests between them, and the Input row sends to whichever Packing Station is physically closest, whatever warehouse it belongs to. The coming handshake for deliveries (`m8-wtp-ingress`, `m8-wtp-egress`) needs one warehouse that decides which Packing Station serves a delivery, and outposts are already served better by wireless sites and the terminal's site selector. A data line therefore belongs to at most one warehouse.

This change belongs to PLAN.md milestone M8 (On-demand autocrafting, Storage Terminal and Packing Station) and is the first of three changes that rework how items cross the data line.

## What Changes

- **One warehouse per data line**: a data line belongs to the warehouse whose controller or Packing Station is a member of it. A line that reaches two or more warehouses, online or not, is in **conflict**.
- **Conflict pauses the line, not the warehouses**: on a line in conflict, Storage Terminals show no stock, refuse requests and keep their Input row, Crafting CPUs start no step that needs the warehouse, Packing Stations start no data line delivery, the warehouse supplier answers 0 and Crafting Stations draw no power from a warehouse. Each of these blocks says "Two warehouses on this data line" in the alert colour where it shows its status. Every warehouse keeps running its racks, shuttles, put-away and wireless transfers. Open requests and orders wait and go on by themselves once the line is split again.
- **Merged stock goes**: the summed stock, split requests and the Storage tab with several warehouse headers are removed. A terminal shows the stock of its one warehouse.
- **Input row stays within its warehouse**: the Input row sends to the nearest Packing Station of the line's warehouse.

## Capabilities

### New Capabilities
- `shuttles/line-warehouse`: which warehouse a data line belongs to, the conflict state when a line reaches more than one, and what blocks on a line in conflict do.

### Modified Capabilities
- `shuttles/storage-terminal`: "Several warehouses on one line" is removed.
- `shuttles/production-terminal`: "Storage tab" shows the line's one warehouse and says when the line is in conflict.

## Impact

- Shuttles mod: `terminal/TerminalWarehouses` resolves the line's warehouse or a conflict instead of a list and loses the merged stock; callers in `terminal/TerminalRequests`, `terminal/RemoteTransfers`, `terminal/StorageTerminalBlockEntity`, `menu/StorageTerminalMenu`, `production/ProductionView`, `cpu/CpuBlockEntity`, `crafting/CraftingStationBlockEntity` and `packing/PackingStationBlockEntity` move to the single warehouse and show the conflict; the warehouse supplier (`packing/WarehouseSupplier`) answers 0 in conflict; lang via datagen.
- Pure logic with JUnit: the classification of a line's warehouses into none, one or conflict.
- GameTests: tests that put two warehouses on one line (merged stock, split request, two Storage tab headers) are replaced by conflict tests.
- Saves: nothing new is saved; the conflict is derived from the line.

## Non-goals

- Refusing the placement of a block that would join two warehouses; only the conflict state enforces the rule.
- The delivery handshake itself (`m8-wtp-ingress`, `m8-wtp-egress`).
- Changing wireless sites or the terminal's site selector.

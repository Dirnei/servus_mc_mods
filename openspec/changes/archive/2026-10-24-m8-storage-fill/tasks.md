## 1. Pure fill counting

- [x] 1.1 Add `SlotFill.inUse` in `shuttles/.../lock/SlotFill.java`: slots that hold items or are locked. JUnit in `SlotFillTest`: empty, holding, IN locked empty, OUT locked holding. Run `gradlew :shuttles:test`.
- [x] 1.2 Add `shuttles/.../storage/StorageFill.java` (pure): `Bay`, `Tank`, `Row`, `Report`, `of(bays, tanks)` with totals, zone rows, `(no zone)` row, order full first, then share, then name, unzoned last on ties. JUnit `StorageFillTest`: sums, zone rows add up, order with full and unzoned rows, no zones gives one unzoned row, empty warehouse, fluid sum. Run `gradlew :shuttles:test`.
- [x] 1.3 Add `TerminalLayout.Production.tabs(int[] labelWidths)` and use it for five tabs; JUnit in `TerminalLayoutTest`: five English label widths fit the row, no overlap, padding at least 4 px, a label wider than its share does not push tabs past the row. Run `gradlew :shuttles:test`.

## 2. Warehouse side

- [x] 2.1 `WarehouseNetwork`: keep a rack fill map from `poll` (rack bays only, from `LockedSlots.states()` via `SlotFill.inUse`), drop it in `dropSource`, copy it in `onAbsorb`, clear it in `onDisposed`; add `storageFill()` building `StorageFill.Report` from the map, `WarehouseZones.zoneOf` and the Storage gantry `TankView`s. `WarehouseZones`: package private `zoneOf(BlockPos)`. Run `gradlew :shuttles:build`.
- [x] 2.2 `ProductionRows.StorageRow` and `ZoneRow` with stream codecs and a `StorageRow.of(name, Report)` factory in `shuttles/.../production/ProductionRows.java`. Run `gradlew :shuttles:build`.

## 3. Controller GUI

- [x] 3.1 `SiteStatusPayload` carries a `StorageRow` (empty while not online); `WarehouseControllerMenu.status` fills it; layout constants `STORAGE_Y`, new `UPGRADES_Y`, `INVENTORY_Y`. Run `gradlew :shuttles:build`.
- [x] 3.2 `WarehouseControllerScreen`: Items and Fluids lines with bars, "no racks" and "no Storage gantry" texts, tooltips with bay count, full zones and gantry count. Lang keys in `ServusShuttlesLanguageProvider`, `runData`. Run `gradlew :shuttles:build`.

## 4. Production Terminal Storage tab

- [x] 4.1 `ProductionPayload` gains `List<StorageRow> storage`; `ProductionView.storage(level, pos)` from `TerminalWarehouses.reachable`; signature in `ProductionTerminalMenu` hashes the rows; `ClientProduction` keeps them. Run `gradlew :shuttles:build`.
- [x] 4.2 `ProductionTerminalScreen`: Storage tab with header, zone and Fluids rows, full rows in the alert colour, header tooltip, "no warehouse" text; tabs drawn and clicked from `Production.tabs`. Storage row rects in `TerminalLayout.Production`. Lang keys via datagen, `runData`. Run `gradlew :shuttles:build`.

## 5. GameTests

- [x] 5.1 `StorageFillGameTests` in `shuttles/.../gametest/`: two racks with items and a locked empty slot give the expected fill; a zoned rack gets its own row; a Storage gantry with a Capacity Upgrade counts its capacity and an Import gantry does not; the controller payload carries the row; a Production Terminal on the controller's line lists the warehouse. Run `gradlew :shuttles:runGameTestServer`.

## 6. Verification

- [x] 6.1 Full `gradlew build` and `gradlew test` green, shuttles GameTests green.
- [x] 6.2 Play check: controller GUI shows Items and Fluids lines that follow put-aways; the Production Terminal Storage tab lists warehouses and zones, full first; five tabs readable, no overlap; game launches with all mods.

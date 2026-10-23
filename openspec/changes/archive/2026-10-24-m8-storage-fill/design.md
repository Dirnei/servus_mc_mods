## Context

The warehouse keeps a stock index from a round robin poll of its sources (`WarehouseNetwork.poll`), the Fluid Gantry tanks as `TankView`s read on change (`readGantries`), and the zone of each stock block in `WarehouseZones`. Rack bays hold their slots in `LockedSlots`, whose `states()` already reports per slot what it holds and whether a job locked it (from `m8-slot-locks`). The controller GUI gets a `SiteStatusPayload` through `PayloadSync` every 10 ticks; the Production Terminal gets a `ProductionPayload` with a signature check every 10 ticks. `TerminalWarehouses.reachable(level, pos)` lists the warehouses on a data line.

## Goals / Non-Goals

**Goals:**
- Slot based item fill and mB based fluid fill per warehouse, with a per zone breakdown.
- Show it in the controller GUI and in a new Production Terminal tab, live while open.

**Non-Goals:**
- Storage Terminal changes, Data Potato reports (`m5-potato-storage-inspect`), alarms or redstone output.

## Decisions

### Slots, not items
Racks hold stacks, so the only fixed maximum is the slot count. A slot counts as in use when it holds items or a lock sits on it (`SlotFill.Slot.empty()` false or `locked()` true). The planned put-away room shows as used, so the bar does not jump down and up again while shuttles drive. A new pure helper `SlotFill.inUse(List<Slot<K>>)` counts this, next to `freeSlots`.

### Counted from the stock poll, not on demand
`WarehouseNetwork.poll(pos)` already reads each source's slots within a per tick budget. For a `HeavyRackBlockEntity` it also stores a `RackFill(used, total)` in a map keyed by position. `dropSource` removes the entry, `onAbsorb` copies the other network's entries, `onDisposed` clears them. No block is read for the GUI and nothing scans every tick: the GUI sums the map. A bay whose chunk unloads keeps the count it had until the poll finds no handler, which drops it from the stock index too, so stock and fill agree. Lock changes do not mark a source dirty, so a fresh lock shows when the round robin reaches the bay, within a few seconds. That is good enough for a fill bar.

Alternative considered: read every loaded rack's `LockedSlots` when the GUI asks. It is exact, but it reads hundreds of block entities every half second per open screen and leaves out unloaded bays.

### Pure model in `io.github.dirnei.mc.servus.shuttles.storage`
`StorageFill` (no Minecraft imports) takes a list of `Bay(zoneKey, zoneName, used, total)` and `Tank(amount, capacity)` and returns a `Report`:
- `items`: used and total over all bays, `bays` count,
- `zones`: one `Row(key, name, used, total)` per zone key plus a `(no zone)` row (empty key) for unzoned bays, ordered full first, then by share in use descending, then by name, with the unzoned row last among equal shares,
- `fluids`: amount and capacity, `tanks` count.
`Row.full()` is `total > 0 && used >= total`. The zone display name is the last name seen for the key, the same rule as `WarehouseZones.summary`.

`WarehouseNetwork.storageFill()` builds the bays from the fill map and `WarehouseZones.zoneOf(pos)` (new package private accessor), and the tanks from the `TankView`s of Storage gantries (`mode() == GantryMode.STORAGE`, `capacity()` already includes Capacity Upgrades). The gantry count is the number of Storage gantries.

### Payloads
`StorageFill.Report` is pure, so the wire form is a record in `production`: `ProductionRows.StorageRow(String name, int bays, long used, long total, List<ZoneRow> zones, int gantries, long fluid, long fluidCapacity)` with `ZoneRow(String name, long used, long total)` and a `StreamCodec` each. The unzoned row travels with an empty name and the client shows the translated "(no zone)". `ProductionPayload` gains `List<StorageRow> storage`, filled by `ProductionView.storage(level, pos)` from `TerminalWarehouses.reachable` (name from the warehouse's controller `displayName()`, else "Warehouse"). The menu signature hashes the rows.

`SiteStatusPayload` gains one `StorageRow` (the same record, so both screens draw from one shape), empty while the warehouse is not online.

### Controller layout
The status column right of the map is full (online, chunks, capacity, FE, wake). The storage lines go into a new band under the map: `STORAGE_Y = 100`, two blocks of a text line (9 px) and a 4 px bar, 15 px each, full width 160 px. `UPGRADES_Y` moves from 112 to 142 and `INVENTORY_Y` from 144 to 174; the panel grows by 30 px to 256 px, which still fits 1080p at GUI scale 4 (270 px). Item and fluid numbers in the controller and the Storage tab use Foundation's `PickerStock.shortCount` (999, 1.2k, 160k, 1.5M) so they stay short.

### Five tabs
Four tabs of 54 px fill the 224 px row, so a fifth does not fit at a fixed width. `TerminalLayout.Production.tabs(int[] labelWidths)` returns one `Rect` per tab: each tab is its label width plus an even share of the spare width (row width less labels and gaps), the remainder going to the first tabs. With the English labels (about 40, 41, 43, 23 and 37 px) every tab gets about 8 px padding. The screen measures labels with the font each frame it draws the tabs and uses the same rects for clicks. A pure unit test checks that the tabs fit the row, do not overlap and keep a minimum padding for the English label widths.

### Storage tab rows
Layout rects in `TerminalLayout.Production`: `STORAGE_HEADER_HEIGHT = 14`, `STORAGE_ROW_HEIGHT = 11`; parts relative to the row: name `(2, 2, 110, 9)` (zone rows indent by 8), numbers `(114, 2, 60, 9)` right aligned, bar `(178, 3, 42, 6)`. The Fluids row shows mB. Header and zone bars use teal, full rows the alert colour, the Fluids bar blue.

## NeoForge and Minecraft APIs

- `CustomPacketPayload`, `StreamCodec`, `ByteBufCodecs` (`net.minecraft.network.codec`), as the existing payloads use them in this repo (`ProductionPayload`, `SiteStatusPayload`).
- `GuiGraphicsExtractor.fill`, `text`, `setComponentTooltipForNextFrame` (26.1 screen API, as used by `ProductionTerminalScreen` and `WarehouseControllerScreen`).
- No new capability or event. `LockedSlots.states()` and `TankView` are our own.

## Pure and Minecraft parts

- Pure, JUnit: `StorageFill` (sums, zone rows, order, full), `SlotFill.inUse`, `TerminalLayout.Production.tabs`.
- Minecraft, GameTest: rack fill from the poll with a locked slot, Storage gantry capacity in the report, Import gantry left out, controller payload carries the row, Production Terminal payload lists the warehouse with its zones.
- Manual: both screens in game.

## Risks / Trade-offs

- [The fill lags a fresh lock by up to a poll round] -> Acceptable for a bar; the poll budget brings it within seconds on normal warehouses.
- [Translated tab labels may be wider than the row] -> Tabs clip their label to the tab width when the spare width goes negative; English fits.
- [The controller GUI grows by 30 px] -> Still fits 1080p at scale 4; the JEI side panel area follows `extraAreas`.

## PLAN.md

This change adds the open point "storage size and fill in the controller and the Production Terminal" under M8 and removes it again at archive. It settles no earlier open decision.

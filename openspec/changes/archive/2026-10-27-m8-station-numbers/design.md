# Design: m8-station-numbers

## Context

- `station/StationBlockEntity` is the shared base of `InboundStationBlockEntity` and `outbound/OutboundStationBlockEntity`. It saves wood and sides in `saveStation`/`loadStation`, carries the wood through `collectImplicitComponents`/`applyImplicitComponents` for the carrot dismantle, and names itself by the block's description id in `getDisplayName`.
- `network/WarehouseNetwork` already tracks every station node in `stations` (both kinds) when a node joins (`NodeKind.STATION`), and knows `status()` and `controller()`.
- The Warehouse Controller has a site name (`site/SiteName`, `RenameSitePayload`, a borderless `EditBox` as title in `WarehouseControllerScreen`, saved on focus loss and in `removed()`), carries name, FE and stock rules in its item on a carrot dismantle, but has no warehouse identity of its own.
- Shuttle numbers (`shuttles/shuttle-numbers`) are per world via `shuttle/CounterData` and live on the item; the label rule "Berta (#12)" is the model for station labels.
- The Data Potato calls `PotatoInspectable.potatoReport` on blocks that implement it (Foundation `item/PotatoInspectable`); stations do not implement it yet.
- Three M13 changes in flight modify `shuttles/warehouse-controller`; this change touches controller code but no requirement of that spec.

## Goals / Non-Goals

**Goals:**
- A stable per warehouse number for both station kinds, an optional name, one label rule, and a small API later changes call to name a station.
- Event driven: numbers are handed out when a station joins or the warehouse comes online, never by scanning.

**Non-Goals:**
- Fluid stations, racks, garages. Marks on the block. Using labels in stock rules (later changes).
- Packing Stations were a non-goal at first; they are numbered since the play check (see "Decisions made while applying").

## Decisions

### Warehouse identity: a UUID on the controller
The controller gets a `warehouseId` (UUID), created when the controller is first placed without one, saved with it and carried in its dismantled item together with the station counters. A station stores the id of the warehouse that numbered it. A station needs a number when it has none or when its stored id differs from the current warehouse's id. Alternative considered: the controller position as identity; rejected because a carrot dismantle and placing the controller elsewhere would then renumber every station. A controller broken without the carrot drops a plain item and so starts a new id with counters at 0, which matches "a new warehouse".

### Counters per kind on the controller
Pure `station/StationCounters` holds two last numbers (Inbound, Outbound) and hands out the next one; it saves to a small tag. The controller owns one instance next to its stock rules. Counting per warehouse rather than per world (unlike shuttles) keeps numbers small and readable per site. Breaking a station never lowers a counter, so numbers are never reused.

### When numbers are handed out
`WarehouseNetwork` keeps a `stationsToNumber` set. A station joining the network (the existing `NodeKind.STATION` branch) is added; when the network becomes online every station is added. The network drains the set in its server tick while online: for each loaded station that needs a number, in ascending position order (`BlockPos` compared by y, then x, then z) so a world load numbers deterministically, it takes the next number of the station's kind from the controller's counters, stores number and warehouse id on the station and marks both dirty. No block is scanned; the set only holds stations that joined.

### Name and label
The station stores an optional name, cleaned with `SiteName.clean` (same rules and length as the site name). Pure `station/StationLabel.of(kind, number, name)` returns the label text: "Outbound #3", "Compost Dock (#3)", or "Outbound Station" / the name when unnumbered. `StationBlockEntity.label()` returns it as a `Component`; `getDisplayName()` returns the label, so the menu title follows. This `label()` is the API `m8-rule-cap` and `m8-rule-export` use. The kind words ("Inbound", "Outbound", "Station") come from lang keys passed into `StationLabel` as plain strings so the pure class stays free of Minecraft.

### GUI field
Both station screens get the controller's title pattern: a borderless `EditBox` in the title row with the label as hint, max length `SiteName.MAX_LENGTH`, sending a new `station/RenameStationPayload(String)` (codec `stringUtf8(64)`, like `RenameSitePayload`) on focus loss and in `removed()` when the cleaned text changed. The payload is registered with `toMenu` in `terminal/ServusShuttlesPayloads` for both `InboundStationMenu` and `OutboundStationMenu`; the menu checks `stillValid(player)` before renaming. The title text is synced through the existing block entity update tag (number and name are added to `getUpdateTag`).

### Item components
Three components in `registry/ServusShuttlesComponents`: `STATION_NUMBER` (Integer), `STATION_NAME` (String) and `STATION_WAREHOUSE` (UUID), written in `collectImplicitComponents` and read in `applyImplicitComponents` of `StationBlockEntity`, and removed from the saved tag in `removeComponentsFromTag`. The controller gets a `WAREHOUSE_ID` component and a `STATION_COUNTERS` component (record of two ints with a codec). The loot tables already drop the plain block on a normal break, so number and name are lost there as specified.

### Data Potato
`InboundStationBlock` and `OutboundStationBlock` implement `PotatoInspectable` and let the potato through in `useItemOn` like `HeavyRackBlock`. New `station/StationReport.of(level, pos)` returns the label, the site's `displayName()` and online or the existing `offlineReason()` lang key.

### APIs used
- `BlockEntity.collectImplicitComponents`, `applyImplicitComponents`, `removeComponentsFromTag` (vanilla 26.1, already used by `StationBlockEntity`).
- `DataComponentType.builder().persistent(codec).networkSynchronized(streamCodec)` through NeoForge `DeferredRegister.DataComponents`, as in `ServusShuttlesComponents`.
- NeoForge `PayloadRegistrar.playToServer` via the existing `toMenu` helper.
- `EditBox` as in `WarehouseControllerScreen`.

### Pure and Minecraft parts
- Pure, JUnit: `StationCounters` (counting per kind, never reused, save round trip), `StationLabel` (all label forms), and the "needs a number" rule (`StationCounters.needsNumber(storedId, number, currentId)`).
- Minecraft, GameTest: numbering on join and on load order, carrot dismantle within and across warehouses, controller dismantle, rename through the menu, potato report.

## Risks / Trade-offs

- Two warehouses merged by one rail have two controllers and are offline (`MULTIPLE_CONTROLLERS`), so no numbers are handed out until one controller is removed; then stations numbered by the removed controller's id are renumbered. Acceptable and rare.
- Old worlds: every station gets numbered on first load in position order, not in the order the player built them. Acceptable; the spec only asks for numbers without errors.
- A station whose chunk is not loaded when the warehouse comes online stays in `stationsToNumber` until it loads, because only loaded stations are numbered.

## Decisions made while applying

- `StationCounters` stays free of Minecraft and has no tag code: the controller saves it and carries it in its item as `station/CarriedCounters`, a record of the two last numbers with a codec and a stream codec. The JUnit round trip goes through the two last numbers.
- `StationLabel.of(number, name)` returns the form and the cleaned name; `text(kindWord, plainName)` builds the plain text. `StationBlockEntity.label()` turns the form into translatable components (`station.servus_shuttles.label.inbound`, `.outbound`, `.named`, the block name for an unnumbered station), so a label in a server message is resolved in the player's language.
- Stations wait for a number when they join, and every station of the network waits again when a controller joins or leaves and when two networks merge. That covers "the warehouse comes online" without watching the status, and also renumbers the stations of a removed controller in a merged warehouse.
- `RenameStationPayload` is registered once with `playToServer` for any open menu implementing the new `menu/StationNameMenu`; the `toMenu` helper takes one menu class and a payload type can be registered only once.
- The Data Potato hook sits on the shared `station/StationBlock` instead of each station block.
- Following the mockup, a named station shows its number ("#3") at the right end of the title row; the field's hint already shows the numbered label while it is unnamed.
- Play check fix: `packing/PackingStationBlockEntity` extends `InboundStationBlockEntity`, so it inherited the Inbound kind, took Inbound numbers and read "Inbound #2" without a way to rename it. Packing Stations now count on their own: `StationCounters.Kind` gains `PACKING`, the Packing Station returns it from `stationKind()`, its label is `station.servus_shuttles.label.packing` ("Packing #1"), and Inbound numbering no longer counts it. The controller saves and carries the third counter in `CarriedCounters` (`packing`, optional in the codec, so a controller saved with two counters loads with 0).
- A station saves the counter that gave its number (`StationCounter` in the block entity tag and the update tag, `STATION_COUNTER` component on the dismantled item). On load a number whose counter is not the station's own is dropped, so the station is numbered again by its warehouse when it joins. A save without a counter name counts as numbered by the Inbound counter for a Packing Station and by its own counter for every other kind (pure `StationCounters.counterOf`). So in old worlds a Packing Station with an Inbound number gets the next Packing number on load or join. Inbound numbers are not renumbered: the Inbound number the Packing Station held stays unused, and the gap stays visible (for example Inbound #1 and #3). Acceptable, as numbers are never reused anyway.
- `menu/PackingStationMenu` implements `StationNameMenu` and `client/PackingStationScreen` gets the same `StationNameField` title row as the Inbound and Outbound screens, with the "#n" at the right of a named station's title. The client menu may have no station (block entity not loaded); the screen then shows the plain title.
- The Packing Station's carrot dismantle in `packing/PackingStationBlock` used to stamp only the wood; it now applies the block entity's `collectComponents()`, so number, name and warehouse travel with the item like at the other stations.
- Labels need no further change: the Data Potato report, the GUI title, the Rules tab refusal and export states of the Storage Terminal (`WarehouseNetwork.stationLabel`) and the export picker all call `StationBlockEntity.label()`, which now resolves the Packing kind.
- Cap and export with Packing Stations, left as they are: `WarehouseNetwork.capRoom` counts every `InboundStationBlockEntity` buffer as held stock, so a Packing Station's To storage buffer counts toward an import cap at the Inbound Stations. The Packing Station inherits the capped intake handler and the Pull side tick, but neither is reachable: it registers no item capability and its sides stay Input (no carrot side modes), so nothing is ever refused at a Packing Station and no cap state names one. Items offered over the data line (`PackingStationBlockEntity.offer`) are not capped. Exports are claimed only through the Outbound Station menu, so a Packing Station never holds an export.

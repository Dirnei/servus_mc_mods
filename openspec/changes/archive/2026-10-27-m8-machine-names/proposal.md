# Proposal: m8-machine-names

## Why

During the play check of `m8-station-numbers` the user asked for names on the Field Head too, and then for every machine a shuttle or the Crafting CPU works with: "names make error messages easier to debug". Today only Inbound, Outbound and (being added in `m8-station-numbers`) Packing Stations have a number and a name, built as station code in Shuttles. Doing the same by hand for nine more kinds across three mods would copy that code nine times, so this change turns station naming into one shared Foundation piece that a machine joins with one interface and one registered kind, and moves the stations onto it.

Milestone M8 (warehouse logistics), follow up of the 2026-10-26 exploration that produced `m8-station-numbers`. PLAN.md has no item for it yet; the "Station numbers and names for the Fluid Inbound and Fluid Outbound stations" open point is widened to the shared mechanism.

**Order:** apply after `m8-station-numbers` is archived (it creates the counters, the label rule, the rename field and the Packing Station number this change folds in). `m8-rule-cap`, `m8-rule-export` and `m8-terminal-tabs` use `WarehouseNetwork.stationLabel`; they are applied on main and should be archived first too.

## What Changes

- **Which machines.** A machine is named when it is on a warehouse's rail network or data line and shuttles or the Crafting CPU work with it. Racks stay unnamed. In: Inbound, Outbound and Packing Station (already named), Field Head, Shuttle Garage, Elevator shaft ("Lift"), Crafting Station, Crafting CPU, Machine Adapter, and the Alchemy producers on the data line (Salamander Forge, Receptaculum, Prelum, Coagulator, Athanor). Out: Heavy Rack, Pattern Storage, Fluid Gantry (removed by `m13-fluid-outbound`), Shuttle Workshop (not on any network), Storage and Production Terminal, Warehouse Controller (it has the site name).
- **Numbers for every kind.** Each warehouse counts each kind on its own, from 1, never reusing a number: "Field Head #2", "Garage #1", "Lift #3", "Crafting Station #2", "CPU #1", "Adapter #4", "Athanor #1". A machine gets its number when it joins the warehouse's rails, when its Field Head serves a field on those rails, or when it joins the warehouse's data line. The counters and the warehouse identity keep travelling with the carrot dismantled controller.
- **Optional name for every kind**, cleaned like a site name. Machines with a GUI show the name in the title row as text with a pencil button after it, which turns the row into a text box to rename, with "#n" at the right end while named. Machines without a GUI (Crafting CPU, Athanor) get a number only for now.
- **One label everywhere.** "Field Head #2", "Wheat North (Field Head #2)", the name alone or the block name while unnumbered. The GUI title, the Data Potato, the Production Terminal machine rows, CPU and order messages, stock rule states, the export picker and the shuttle report (stops, lifts) all name machines by it.
- **Data Potato** on any named machine prints its label first, also on machines that had no report before, and opens no GUI.
- **Without Shuttles** (Foundation and Alchemy alone) a machine can still be named; it just has no number.
- **One shared mechanism**: the Foundation interface `NamedMachine` with default methods, a `MachineNameSlot` field object (number, name, warehouse id: save, sync, item components), registered `MachineKind`s, one `RenameMachinePayload`, a `MachineNameMenu` interface and a `NamedMachineScreen` client base class. Inbound, Outbound and Packing Stations move onto it; `StationCounters`, `StationLabel`, `CarriedCounters`, `RenameStationPayload`, `StationNameMenu` and `StationNameField` go.
- **Old worlds**: machines without a number are numbered the first time their warehouse sees them. Station numbers and counters saved by the unreleased `m8-station-numbers` are not migrated (never shipped): play test worlds number their stations again from #1.

## Capabilities

### New Capabilities

- `foundation/machine-names`: what a named machine is, its optional name, the label rule, the name and its pencil in the title row, carrying number and name in the dismantled item, the Data Potato label line, labels in messages, and the kind registry that new machines join.
- `shuttles/machine-numbers`: numbering of every named machine kind per warehouse (rail members, Field Heads, data line members), the counters on the controller, and where Shuttles shows machine labels (production rows, CPU messages, shuttle report).

### Modified Capabilities

- `shuttles/station-numbers`: the generic requirements (numbering, counting, carrying, controller count, name, cleaning, label) move to the two new capabilities and are removed here; the station Data Potato report (site and online state) stays.
- `foundation/data-potato`: the potato names any named machine, also one whose block gives no report of its own.

## Impact

- Foundation: new package `naming` (`NamedMachine`, `MachineNameSlot`, `MachineKind`, `MachineKinds`, `MachineLabel`, `MachineCounters`, `RenameMachinePayload`, `MachineNameMenu`), client `NamedMachineScreen`, components in the Foundation component register, `DataPotatoItem`, `DataNetwork`/`DataMembers` (members revision), `adapter/MachineAdapterBlockEntity`, `AdapterMenu`, `AdapterScreen`, `machine/MachineBlockEntity`, `MachineMenu`, `client/MachineScreen`, lang.
- Shuttles: `station/*` (removals, `StationBlockEntity` on the slot), `controller/WarehouseControllerBlockEntity` (per kind counters), `network/WarehouseNetwork` (one numbering queue for rail, field and data members; `machineLabel`), `field/FieldHeadBlockEntity`, `garage/ShuttleGarageBlockEntity`, `rail/ElevatorBlockEntity` and `ElevatorBlock` (name moves with the upgrades), `crafting/CraftingStationBlockEntity`, `cpu/CpuBlockEntity`, `production/ProductionView`, `shuttle/ShuttleReport`, menus and screens of those blocks, `registry/ServusShuttlesComponents`, payload registration, lang.
- Alchemy: Salamander Forge, Receptaculum, Prelum, Coagulator, Athanor block entities implement `NamedMachine` and register their kinds; their screens already extend Foundation's `MachineScreen`.
- Tests: JUnit for `MachineLabel`, `MachineCounters`; one GameTest that runs the naming contract over every registered kind; GameTests for the three numbering paths and the labels in messages.

## Non-goals

- Renaming machines without a GUI (Crafting CPU, Athanor), by Name Tag or otherwise.
- Numbers on the block itself (plates, signs).
- Fluid Inbound and Fluid Outbound stations: they join with `NamedMachine` when M13 builds them (PLAN.md open point).
- Renaming from a terminal or by command; sorting or filtering terminal lists by label.
- Delicacies and Brewery machines: none is on the data line yet; they join the same way when one is.

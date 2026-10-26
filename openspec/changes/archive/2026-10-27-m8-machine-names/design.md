# Design: m8-machine-names

## Context

- `m8-station-numbers` (apply first, then archive) gives Inbound, Outbound and Packing Stations a number per warehouse and a name. Its pieces are station only: `station/StationCounters` (one int per station kind), `StationLabel`, `CarriedCounters`, `RenameStationPayload` with `menu/StationNameMenu`, `client/StationNameField`, number, name and warehouse id fields with save, update tag and item components in `StationBlockEntity`, `StationReport`, and a `stationsToNumber` queue in `WarehouseNetwork` filled on rail joins and controller changes. `WarehouseNetwork.stationLabel(pos)` names stations in stock rule states and the export picker.
- The machines to name have no shared base class: stations, garage and elevator extend `StorageNodeBlockEntity` (Shuttles); Crafting Station and CPU extend `DataLineBlockEntity` (Shuttles; so do Pattern Storage and the Production Terminal, which are not named); Salamander Forge, Receptaculum, Prelum, Coagulator and Athanor extend Foundation's `machine/MachineBlockEntity` (so do Foundation machines that are not named); Field Head and Machine Adapter extend `BlockEntity`. Machine Adapter and the Alchemy machines live in mods that cannot see Shuttles.
- Every screen involved extends `AbstractContainerScreen` directly, except the Alchemy screens, which extend Foundation's `client/MachineScreen`.
- Foundation's `material/MaterialSlot` is the existing pattern for "a field object that a block entity saves, syncs and carries in its item through a few one-line hooks".
- The Data Potato only reaches blocks that implement `PotatoInspectable` and let it through in `useItemOn`.

Audit of the shuttle and CPU code (job records `PickJob`, `PutAwayJob`, `FieldJob`, `FertilizeJob`, `SupplyJob`, `FluidJob`; `DeliveryTarget`, `PutAwaySource`; `WarehouseConnectionRule.NodeKind`; `CpuOrders.producers`, data line `DataRole`s):

| Machine | How shuttles or the CPU work with it | Screen | Result |
|---|---|---|---|
| Inbound Station | rail node; `PutAwaySource` for put-away runs | yes | number + name (moved onto shared code) |
| Outbound Station | rail node; `DeliveryTarget` of picks, export target | yes | number + name (moved) |
| Packing Station | rail node; `DeliveryTarget` + `PutAwaySource`; data line link | yes | number + name (moved; numbered in `m8-station-numbers`) |
| Field Head | `FieldJob`, `FertilizeJob`, `SupplyJob` target; `PutAwaySource` for its harvest buffer; reached through farm rails linked to the warehouse | yes (tabs) | number + name |
| Shuttle Garage | rail node (`GARAGE`); shuttles park, start and drop leftovers there | yes | number + name |
| Elevator | rail node (`ELEVATOR`); shuttles wait for and ride the car; `StandReason` names the shaft | yes, opened on the lowest segment | one number + name per shaft |
| Crafting Station | data line producer; the CPU sends it steps | yes | number + name |
| Crafting CPU | data line link; runs orders; one master per formed box | no | number only |
| Machine Adapter (Foundation) | data line producer for a faced machine | yes | number + name |
| Salamander Forge, Receptaculum, Prelum, Coagulator (Alchemy) | data line producers | yes (`MachineScreen`) | number + name |
| Athanor (Alchemy) | data line producer, multiblock | no | number only |
| Fluid Gantry | rail node, tanker runs | yes | out: removed by `m13-fluid-outbound` |
| Fluid Inbound / Outbound | tanker runs (not built) | yes | out now; join through `NamedMachine` when M13 builds them |
| Heavy Rack, Fluid Rack | storage | Heavy Rack yes | out: racks |
| Pattern Storage | data line link, read by the CPU | no | out: pattern storage, like a rack |
| Storage / Production Terminal | data line carriers; picks are made from them but delivered to stations | yes | out: player interfaces, not worked with |
| Shuttle Workshop | not on any network | yes | out |
| Warehouse Controller | the warehouse itself | yes | out: has the site name |

## Goals / Non-Goals

**Goals:**
- One shared naming piece that every machine joins with an interface, a slot field and a registered kind; stations folded in, no parallel copies.
- One numbering queue in `WarehouseNetwork` for rail members, Field Heads and data line members, event driven.
- One place that turns a machine into a label for messages.

**Non-Goals:**
- A shared base class for the block entities (the hierarchies differ and cross mod boundaries).
- Renaming GUI-less machines. Fluid stations (later).

## Decisions

### Shape: interface plus field object on the server, base class on the client
Server side there is no common block entity base, and Foundation and Alchemy cannot extend Shuttles classes, so the contract is a Foundation interface with default methods, `naming/NamedMachine`, backed by a field object `naming/MachineNameSlot` on the `MaterialSlot` pattern. Client side every screen already extends `AbstractContainerScreen`, so one abstract `client/NamedMachineScreen<M>` between them gives the field, keys, focus loss, sending and the "#n" with a one word change per screen. Alternatives: a common block entity base (impossible across the hierarchies), NeoForge `ScreenEvent` listeners that bolt the field onto any screen (no access to the protected title position and the screen still draws its title; rejected as fragile), copying `StationNameField` into each screen (the duplication the user wants gone).

### Foundation package `naming`
- `MachineKind(Identifier id, Supplier<? extends Block> block)`: the counter key, the lang key of the kind word (`machine.<ns>.<path>`, for example `"machine.servus_shuttles.field_head": "Field Head #%s"`) and the block used by the contract GameTest. `MachineKinds.register(kind)` / `all()` / `byId`: a plain registry filled at mod construction; every mod registers its own kinds.
- `MachineLabel` (pure, from `StationLabel`): `of(number, name)` gives the form `NUMBERED_NAMED`, `NUMBERED`, `NAMED` or `PLAIN` and the cleaned name; `text(kindWord, plainName)` builds the plain text, "<name> (<kind> #n)" when numbered and named. Name cleaning stays `SiteName.clean` rules; `SiteName.clean` and `MAX_LENGTH` move from Shuttles `site/SiteName` into Foundation `naming/NameText` and `SiteName` calls it, so both mods clean the same way.
- `MachineNumber.needsNumber(UUID numberedBy, int number, UUID current)` (pure, from `StationCounters.needsNumber`).
- `MachineNameSlot(BlockEntity owner, MachineKind kind)`: number, name, numbering warehouse id. `save(ValueOutput)`, `load(ValueInput)`, `writeUpdate(CompoundTag)`, `collect(DataComponentMap.Builder)`, `apply(DataComponentGetter)`, static `removeFromTag(ValueOutput)`, `rename(String)`, `assign(UUID, int)`, `moveTo(MachineNameSlot)` and `clear()`. Changes call `setChanged` and `sendBlockUpdated` on the owner, server side only. Tag keys `MachineNumber`, `MachineName`, `MachineWarehouse`.
- `NamedMachine`: `MachineNameSlot nameSlot()`; defaults `machineKind()`, `number()`, `name()`, `label()`, `numberLabel()` (hint of the field), `rename(String)`, `needsNumber(UUID)`, `assignNumber(UUID, int)`, `takesNumber()` (true; the elevator answers true on its lowest segment only, the CPU on its master only), `potatoLines()` (the label line). `label()` returns translatable components (`machine.servus_foundation.label.named` "%s (%s)" with the name and the numbered kind label, the kind key, the block name) so the client resolves them.
- Data components `MACHINE_NUMBER` (Integer), `MACHINE_NAME` (String), `MACHINE_WAREHOUSE` (UUID) in `registry/ServusFoundationComponents`. They replace `STATION_NUMBER`, `STATION_COUNTER`, `STATION_NAME`, `STATION_WAREHOUSE`.
- `RenameMachinePayload(String)` (`stringUtf8(64)`) registered once in `data/ServusFoundationPayloads` with `playToServer`: when `player.containerMenu instanceof MachineNameMenu menu`, `menu.renameMachine(player, name)`.
- `MachineNameMenu`: `Optional<NamedMachine> namedMachine()` (the block entity the menu was opened on, on both sides); default `renameMachine(Player, String)` checks `stillValid(player)` and renames. `Optional` lets Foundation's `MachineMenu` implement it for every machine, named or not.
- `MachineLabels.at(Level, BlockPos[, Component unloaded])`: the label of a loaded named machine there (the CPU master for any CPU block, the shaft for any elevator segment, through their `label()`), else the block's name, else the unloaded word. This is the one place messages ask; `WarehouseNetwork.stationLabel` becomes a call to it.

### Base class hooks
Where a base class exists, it calls the slot when `this instanceof NamedMachine`, so subclasses only implement the interface: `StorageNodeBlockEntity` (stations, garage, elevator), `DataLineBlockEntity` (Crafting Station, CPU) and Foundation `MachineBlockEntity` (Alchemy machines) call `save`, `load`, `writeUpdate`, `collect`, `apply`, `removeFromTag` and return `label()` from `getDisplayName()`. Field Head and Machine Adapter have no such base and call the six hooks themselves. A static `NamedMachines` helper holds those `instanceof` calls so each base adds one line per hook.

### Client: `NamedMachineScreen<M extends AbstractContainerMenu & MachineNameMenu>`
Holds a `TitleNameEditor` (Foundation `client`) and forwards to it: `init` adds its text box and button, `extractRenderState` lays out the title row every frame (`titleLabelX`, `titleLabelY`, width `imageWidth - 2 * titleLabelX` less room for the number), so screens that move their panel after `init` need no extra call, `mouseClicked`, `keyPressed`, `containerTick` and `removed` go to the editor, and `extractLabels` draws the name and "#n" through `extractTitle`, then the inventory label. Screens that draw their own labels call `extractTitle`. When `namedMachine()` is empty it draws the plain title, so `MachineScreen` extends it for every Foundation machine. Field position: the title row of every screen, as on the stations today (`titleLabelX`, `titleLabelY`).

### Counters on the controller
`network/MachineCounters` (Shuttles, pure): last number per kind id (`Map<String, Integer>`), `take(id)`, `last(id)`, `toMap()`, `load(map)`. Replaces `StationCounters` and `CarriedCounters`. The controller saves it as `MachineCounters` with `Codec.unboundedMap(Codec.STRING, Codec.INT)` and carries it in a `MACHINE_COUNTERS` component (stream codec `ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)`), next to the existing `WAREHOUSE_ID`. `takeNumber(MachineKind)` replaces `takeStationNumber`.

### One numbering queue, three ways in
`stationsToNumber` becomes `machinesToNumber`, drained as today (online only, loaded only, ascending position, `needsNumber` then `takeNumber` and `assignNumber`, skipping `!takesNumber()`).
- Rail members: the join branch adds any member block entity that is a `NamedMachine`, not only `NodeKind.STATION`; controller changes and merges add them all again, as today.
- Field Heads: `servedHeads()` already runs in the field passes; a served head that needs a number from this warehouse is added. No new scan: the list is the loaded heads index `FieldHeads`.
- Data line: Foundation `DataMembers` gets a `revision()` counter bumped by `touch`, `self`, `removeCable` and `absorb`, exposed as `DataNetwork.membersRevision()`. At the field check interval the warehouse looks up its line with `CpuOrders.network(level, this)` and, when the line or its revision changed, adds every loaded member that is a `NamedMachine` and no rail member (for CPU blocks the master).

### Elevator
The slot sits on every segment's block entity, but only the lowest segment's counts (`takesNumber()`), next to the upgrades. `ElevatorBlock` already moves upgrades in `onPlace` (segment placed below) and on removal (`LiftUpgrades.heirOnRemove`); the same two places call `moveTo` on the slot. When two shafts join, the lower shaft keeps its slot and the upper one is cleared. `ElevatorMenu` returns the lowest segment as its named machine.

### Labels in messages
- `production/ProductionView`: machine rows, buffer lines and step states use `MachineLabels.at`; an unnamed adapter reads "<faced machine> (Adapter #n)" through its own `label()`. `CpuOrders.producers` names producers the same way. `MachineRow` carries the name as a `Component` (`ComponentSerialization.TRUSTED_STREAM_CODEC`, as `terminal/RuleRow` does) instead of a server side `getString()`, so labels resolve in the player's language.
- Order step states that name a producer, and the CPU summary (`message.servus_shuttles.cpu.summary` gets the label as first argument).
- `shuttle/ShuttleReport`: `STOP` names the named machine beside that rail when there is one (else coordinates), `LIFT`, `DECK` and `RIDING` add the shaft's label, `FIELD_WORK` the Field Head's label.
- Stock rule states and the export picker keep calling `stationLabel`, now `MachineLabels.at`.

### Data Potato
`DataPotatoItem` overrides NeoForge `IItemExtension.onItemUseFirst(ItemStack, UseOnContext)` (verified in `neoforge-26.1.2.116-sources.jar`, `common/extensions/IItemExtension.java`: "called when the item is used, before the block is activated"). When the clicked block entity is a `NamedMachine` it prints `potatoLines()` and then the block's `PotatoInspectable` report if any, and returns `SUCCESS` so no GUI opens. Other blocks keep today's `useOn` path. `StationReport` drops its own label line.

### How to make a new machine nameable
1. Make its block entity `implements NamedMachine` with `private final MachineNameSlot nameSlot = new MachineNameSlot(this, MyKinds.MY_MACHINE)`. If it extends `StorageNodeBlockEntity`, `DataLineBlockEntity` or `MachineBlockEntity`, the base calls the hooks; otherwise call `NamedMachines.save/load/writeUpdate/collect/apply/removeFromTag` from its six hooks. Return `label()` from `getDisplayName()` (`MachineBlockEntity` already does).
2. Add its kind to its mod's kinds class (`new MachineKind(MyBlocks.MY_MACHINE.getId(), MyBlocks.MY_MACHINE)`, registered with `MachineKinds.register` from the mod constructor) and add the kind word to its mod's language provider (`"machine.<ns>.my_machine": "My Machine #%s"`).
3. With a GUI: its menu implements `MachineNameMenu` and its screen extends `NamedMachineScreen` (or `MachineScreen`).
It is numbered automatically when it is a rail member or a member of the warehouse's data line; anything else needs a way into `machinesToNumber`, as the Field Head has. A dismantle that builds its own drop calls `NamedMachines.stamp`. The contract GameTest picks it up from the registry.

### Kinds registered here
Shuttles: `inbound_station`, `outbound_station`, `packing_station`, `field_head`, `shuttle_garage`, `elevator` ("Lift #%s"), `crafting_station`, `crafting_cpu` ("CPU #%s"). Foundation: `machine_adapter` ("Adapter #%s"), `coal_generator`, `burner_pump`. Alchemy: `salamander_forge`, `receptaculum`, `prelum`, `coagulator`, `athanor`, `calciner`.

### APIs used
- `BlockEntity.collectImplicitComponents`, `applyImplicitComponents`, `removeComponentsFromTag`, `getUpdateTag`, `ValueOutput`/`ValueInput` (vanilla 26.1, already used by `StationBlockEntity` and `MaterialSlot`).
- `DeferredRegister.DataComponents` with `persistent` and `networkSynchronized` (as `MaterialContent`).
- `PayloadRegistrar.playToServer` (as `ServusFoundationPayloads`).
- `IItemExtension.onItemUseFirst` (above).
- `EditBox` as in the existing `StationNameField`.

### Pure and Minecraft parts
- Pure, JUnit: `MachineLabel` (all forms, name cleaning to empty), `MachineNumber.needsNumber`, `NameText.clean`, `MachineCounters` (per kind from 1, never reused, kinds independent, map round trip).
- GameTest: the contract test over `MachineKinds.all()`; numbering through rails, Field Heads and the data line; elevator moves; labels in production rows, CPU summary and shuttle report; potato; rename through menus.

### PLAN.md
No open decision is settled. The "Station numbers and names for the Fluid Inbound and Fluid Outbound stations" point becomes "Fluid Inbound and Fluid Outbound stations join machine names (`NamedMachine`) once M13 builds them". New open points: renaming machines without a GUI (CPU, Athanor), and a carrot dismantle for the Field Head that keeps its number and name.

## Risks / Trade-offs

- [Unreleased station data is not migrated] Play test worlds number their stations again from #1 and lose station names. → Stations were never released; say so in the play check.
- [Field Head numbering rides on the field pass] A Field Head is numbered at the next field check, not at once. → The pass runs every few seconds; acceptable for a label.
- [Data line revision] A missed `revision()` bump leaves a new producer unnumbered until the next change. → Bump in every `DataMembers` mutator; GameTest connects a station by cable.
- [CPU reforming] The number sits on the master; a box reformed with another master corner gets a new number. → Rare; carry it over in `CpuBlock` forming if play tests notice.
- [`station-numbers` spec emptied to one requirement] After archive it keeps only the potato report. → Acceptable; it could later merge into `shuttles/machine-numbers`.
- [Header drift] The REMOVED headers must match the archived `shuttles/station-numbers` spec; the Packing work in `m8-station-numbers` may change it. → Task 1.1 checks the headers before applying.
- [Production rows as Component] Changing `MachineRow.name` to a `Component` touches the Production Terminal payload. → Covered by the existing production GameTests.

## Decisions from review

The user answered the open questions before apply:

- **Which machines.** Every machine a shuttle interacts with is renameable, the Field Head included. Racks, the Shuttle Workshop and the Crafting Station stay as decided in the proposal (racks and the workshop unnamed, the Crafting Station named).
- **Label form.** A numbered and named machine reads "<name> (<kind> #n)", for example "Sticks (Crafting Station #2)", in every message, row, report and title: one rule, so lists that mix kinds stay readable. Only the GUI title row uses the short form: the name in the field and "#n" at the right end.
- **GUI-less machines** (Crafting CPU, Athanor) are not renameable in this change; renaming them is an open point in PLAN.md.
- **Field Head carrot dismantle** is not added: the Field Head has no carrot dismantle today and is no `SidedBlockEntity`, so it does not fit `SideConfigWrench`; it holds buffers, locks and a tank, so a dismantle is more than a small addition. It is an open point in PLAN.md.
- **Old worlds** number again from #1 and lose station names: nothing was released, so there is no migration. The `StationCounter` fallback of `m8-station-numbers` (a Packing Station numbered by the Inbound counter) goes with it.
- **No duplicated code.** Stations, the new machines and future ones share one path: `NamedMachine`, `MachineNameSlot`, `NamedMachines`, `MachineNameMenu`, `NamedMachineScreen`, `MachineLabels`, one payload and one numbering queue.

## Decisions made while applying

- **Components** `MACHINE_NUMBER`, `MACHINE_NAME` and `MACHINE_WAREHOUSE` are registered in the existing `registry/ServusFoundationComponents` instead of a new `NamingContent` register.
- **Kind registration.** Each mod keeps its kinds as constants in one class and registers them from its constructor: `adapter/AdapterContent.KIND` (in `AdapterContent.register`), `registry/ServusShuttlesMachineKinds.register()` and `registry/ServusAlchemyMachineKinds.register()`. `MachineKinds` has `register` and `all`; nothing needed `byId`.
- **Counters** are keyed by the kind's full id (`servus_shuttles:field_head`), so kinds of different mods never share a counter. `MachineCounters` stays pure (`take`, `last`, `toMap`, `load`); the map codec and stream codec live in `ServusShuttlesComponents.MACHINE_COUNTERS` and the controller.
- **Unloaded machines.** `MachineLabels.at(level, pos, unloaded)` names an unloaded machine with the caller's word ("Station" for stock rules and the export picker); `MachineLabels.at(level, pos)` uses "Machine". The block of an unloaded position is never read, so no chunk loads for a label.
- **Machine Adapter label.** An unnamed adapter reads "<faced machine> (<adapter label>)", for example "Furnace (Adapter #1)" or "Furnace (Machine Adapter)" while unnumbered, everywhere (`MachineAdapterBlockEntity.label()`), not only in the Production Terminal. The title row shows the adapter's own label.
- **CPU and elevator labels.** Every block of a formed CPU box reads as its master, every elevator segment as its shaft's lowest segment (`label()` overrides), so a Data Potato click or a report on any block names the machine.
- **Dismantles.** Machines dismantled through `SideConfigWrench` (stations, Crafting Station, Alchemy machines) or `collectComponents` (Packing Station) carry number and name through the base hooks. The Shuttle Garage and the Machine Adapter used `Material.stamp` only and now also call `NamedMachines.stamp`. Elevator segments and CPU blocks carry nothing: the shaft and the box keep the number.
- **Data line numbering skips rail members of other warehouses.** A Packing Station on another warehouse's rails (one with a controller) keeps that warehouse's number and is never numbered through the line, so it cannot flip between two warehouses. A Packing Station on no warehouse's rails, only on the data line, is numbered through the line like any other member (play check: such a station read "Packing Station").
- **Rule extended after the play check.** Every machine that a shuttle serves or that shows on a data line (a Production Terminal row) is nameable. Added: Coal Generator and Burner Pump (Foundation kinds in `machine/FoundationMachineKinds`, "Coal Generator #%s", "Burner Pump #%s", named in their `MachineScreen`) and the Calciner (Alchemy kind, "Calciner #%s", number only like the Athanor). The Electric Pump and the Ender Anchor never show on a data line and stay unnamed.
- **Parts stand for their machine.** A block without its own block entity implements `naming/NamedMachinePart` and returns where its machine's block entity stands: the upper halves of the Coal Generator and the beam pumps, and the Calciner's formed parts (the data line reaches the Calciner through its Firebox). `MachineLabels.machineAt(level, pos)` resolves a named block entity or such a part; labels, the Data Potato and data line numbering all go through it.
- **Production Terminal labels on every tab.** `PackingRow` and `CpuRow` carry the label as a `Component` like `MachineRow`; the CPU title reads "CPU #1, 2x2x2" and the CPU tooltip adds "At x, y, z". Pattern Storage stays unnamed, so the Patterns tab keeps its position line.
- **Chat colours.** `MachineKind` takes an optional `ChatFormatting chatColour`; `NamedMachine.chatLabel()` and `MachineLabels.chatAt` give the label in that colour, `ShuttleItem.chatLabel` gives a shuttle's label in `ChatFormatting.GREEN`. Chat and action bar messages (the potato's label line, the shuttle report) use these; GUI rows and titles keep `label()` uncoloured, since green and aqua read badly on the grey panels. Lifts use `ChatFormatting.AQUA` (the user's choice; `BLUE` is too dark on the chat background). No other kind has a chat colour.
- **Requeue.** The warehouse keeps `namedMembers` (its rail members that are named machines); a controller change, a merge or an elevator segment leaving queues them again and forgets the last checked data line, so the line is queued again at the next check.
- **Field Head screen.** No `nameFieldRightInset()` hook: the Field Head's tabs hang left of the panel, not in the title row. The Field Head screen moves its panel after `init`, so `NamedMachineScreen.placeNameField()` puts the field back into the title row.
- **Title row stays free.** The field covers the title row (y 6 to 16), so no screen draws text, slots or widgets there. The Outbound Station drew its Keep label in that row: its keep row, buffer row, status line and inventory move down 12 to 14 px (panel 180 px high) and the panel is drawn with `PanelPainter` instead of the fixed `outbound_station.png`, which is deleted.
- **Shuttle report stop.** A shuttle stopping at a rail names the first loaded named machine beside that rail (the four horizontal neighbours), else the rail's coordinates as before. Lift, deck and riding lines name the shaft's label; field work names the Field Head's label.
- **Contract GameTest.** The GameTest registry registers annotated methods, so there is no generated test per kind. The shared checker `foundation/gametest/MachineNameContract.checkAll(helper, namespace)` runs the same checks for every registered kind of a namespace, failing with the kind's id; Foundation, Shuttles and Alchemy each run it for their own kinds.
- **Pencil and edit mode (play check).** The always-editable borderless field was not obviously editable. `client/TitleNameEditor` shows the name as plain text with an 11 px framed pencil button right after it (drawn with fills in the style of the duct GUI's icon buttons, tooltip "Rename"). The pencil switches to edit mode: a vanilla bordered `EditBox` over the row, focused with the cursor at the end, and the button becomes a green check ("Save name") at the row's right end. Enter, the check or a click anywhere else saves; Escape cancels and keeps the GUI open; a focus loss some other way (another widget took focus) saves on the next tick; closing the GUI saves. The editor takes the pencil click itself in `mouseClicked` before the children, because the vanilla click would focus the button and take the focus from the box. The title row spans y 4 to 15 on every screen, above the first slot frames at y 16 and clear of the side panels and tabs outside the GUI. The Warehouse Controller's site name uses the same editor (no number), so every renamable GUI title works one way.

# Proposal

## Why

The 26.1.2 play test showed that the crates add little next to Heavy Duty Racks and shuttles, and that the warehouse's indexing of every inventory touching a storage node causes real bugs: a Storage Terminal beside the controller was indexed as storage and looped its own put-away. At the same time pipes show item arms toward blocks that refuse items (stations, terminal, Field Head), because those blocks register NeoForge's item capability only so that data line packets can reach them. Both questions are "what counts as an inventory", so they are answered together, and the Crafting CPU buffer (`m8-cpu-buffer`) needs the packet inlet this change introduces.

## What Changes

This change implements a follow-up to PLAN.md milestone **M11 Wireless and scale**, as a fourth item next to the site changes (it cleans up M3/M6 storage before more sites are built). It follows the decision notes "drop-crates-decision" and "pipe-access-decisions" (2026-10-12).

- **Crates removed.** Crate, Double Crate and Quad Crate go completely: blocks, items, recipes, loot, models, textures, block entity, renderer, the `crate.json` stats file and the `storage/crate` spec. Nothing is released, so there is no migration.
- **The warehouse indexes only its own racks.** Inventories touching a storage node (vanilla chests, other mods' inventories, Servus blocks) are no longer discovered, indexed, picked from or filled. The instant serve path for crates and foreign inventories goes: every pick and every put-away runs through Heavy Duty Racks and shuttles. The `NoStock` marker added in `m11-wireless-packing-station` becomes unnecessary and is removed.
- **Early game is a normal warehouse.** The first warehouse is a Heavy Duty Rack, a Warehouse Rail, a Shuttle Garage and a Mk1 shuttle; nothing new is built for the start (no instant rack access without a garage, no starter storage in the controller). Without a shuttle a request is refused with "needs a shuttle".
- **The controller's stock capabilities shrink.** The stock provider still counts, but taking from it gives nothing (items leave only by shuttle through a Packing Station); the controller no longer exposes a stock sink. `/servus_warehouse take` stays as an op tool.
- **Storage Terminal pantry features go.** The crafting grid no longer refills from the warehouse and Alt click no longer pulls a stack, because both needed instant stock; JEI transfer fills the grid from the player inventory only.
- **Pipe access only where pipes belong.** These blocks register no item capability any more, so item pipes show no arm and hoppers neither insert nor extract: Inbound Station, Outbound Station, Packing Station, Wireless Packing Station (while it exists), Storage Terminal (Input and Output rows; the "Automation inserts" requirement is dropped), Field Head and Crafting Station (the Crafting CPU already has none). The stations keep working because they act on their neighbours themselves: the Inbound Station pulls, the Outbound Station pushes, the Crafting Station pulls job inputs and pushes leftovers. Their side modes lose the passive modes (Input, Output, Both, Auto).
- **Heavy Duty Rack as a chest only while standalone.** A rack exposes its item capability (a big chest for hoppers and pipes) only while no warehouse rail docks to the front of any bay of its section; once a rail docks, the section is shuttles only. The switch is event driven: a rail placed or removed in front of a bay, or a section change, invalidates the capabilities of the section's bays.
- **Servus packet inlet.** Foundation gets a packet inlet capability (`ServusFoundationCapabilities.PACKET_INLET`, an item resource handler under its own capability id). The data line delivers into and collects from a member through its packet inlet first, then its item capability (foreign machines, the Machine Adapter, Manufacturing machines), then its stock sink or provider. Item pipes and hoppers never see the inlet. Packing Station, Storage Terminal, Crafting Station, Field Head (anti-corrosion upkeep) and the future CPU buffer switch to it.
- **No more pushed outputs into a Packing Station.** A producer can no longer push into a Packing Station face, so the "Pushed outputs count" crediting is removed; the CPU collects outputs as packets.
- **Docs and plan.** `PLAN.md` (progression item 1, the "Vanilla and modded inventories ... are indexed too" line, the rails or cable rule list, the network topology section, the architecture principles and the Foundation API list) and `docs/src/content/docs/mods/storage.md` describe racks only and the packet inlet.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `storage/crate`: every requirement removed.
- `storage/warehouse-controller`: sources are only the warehouse's racks; no instant stock operations; the controller's stock provider counts but gives nothing and it has no stock sink; commands, chunk span and examples without crates.
- `storage/storage-terminal`: requests and pushes by shuttle only; output and input rows get no pipe access and the automation inserts requirement goes; packets land through the inlet; grid refill and Alt pull removed; JEI fills from the player inventory.
- `storage/inbound-station`: Pull or Off sides, no item capability; put-away only by shuttle.
- `storage/outbound-station`: Push or Off sides, no item capability; restocked only by shuttle.
- `storage/packing-station`: packets enter through the inlet only; shipments only by shuttle.
- `storage/crafting-station`: sides Pull, Push or Off, no item capability, packets through the inlet.
- `storage/crafting-cpu`: the pushed outputs crediting goes.
- `storage/heavy-duty-rack`: item capability only while no rail docks to the section; joining wording without foreign inventories.
- `storage/zones`: zones are stamped onto racks only; put-away routing without crate and foreign tiers.
- `storage/connection-indicator`: lamps on controllers and terminals only.
- `storage/warehouse-rail`: anchors, docks and joining without crates and instant blocks.
- `storage/rail-elevator`, `storage/farm-rail`, `storage/shuttle-tiers`: examples use racks.
- `storage/shuttles`: no instant service; a warehouse job no shuttle fits is refused.
- `storage/harvester`, `storage/spreader`: no instant targets; examples use racks.
- `storage/rail-corrosion`: the anti-corrosion slot is filled by hand or upkeep packets, not by hoppers.
- `foundation/data-packets`: the packet inlet, and delivery and collection through it before the item capability.
- `foundation/gamedata-export`, `docs/recipe-graph`: examples without the crate.

## Non-goals

- Balance of the first shuttle setup (rack, rail, garage, Mk1 shuttle, controller, terminal, Packing Station recipes). The design notes what looks expensive; values change in the open M9 balance pass.
- Bringing the grid refill, Alt pull or a warehouse fed JEI transfer back as shuttle requests.
- A starter storage in the controller or instant rack access without a garage (rejected by the user).
- Changing the Machine Adapter, Manufacturing machines or Foundation machines: they keep their item capability and the data line keeps reaching them through it.
- The CPU buffer itself (`m8-cpu-buffer` registers the CPU's inlet after this change).
- Removing the Wireless Packing Station (done by `m11-controller-wireless`).
- Indexing other mods' storage through a dedicated adapter block; may come back later as its own change.

## Impact

- Code, Foundation: `production/ServusFoundationCapabilities` (new `PACKET_INLET`), `data/packet/PacketIo` and a public lookup helper used by `DataPackets` and the CPU, GameTests in `logistics/gametest`.
- Code, Storage: delete `crate/*` (blocks, entity, handler, compartments), `client/CrateRenderer`, `network/NoStock`, `index/CandidateNeighbours` discovery; move `crate/StorageStatsManager` and `crate/ShuttleStatsReader` to a new `stats` package; `network/WarehouseNetwork` (sources, index, put-away, picks, instant paths, held and deferred loads), `network/WarehouseConnectionRule`, `shuttle/RequestPlan`, `zone/PutAwayPlan`, `controller/WarehouseControllerBlockEntity`, `command/ServusStorageCommands`, `menu/StorageTerminalMenu`, `terminal/*`, `station/InboundStationBlockEntity`, `outbound/OutboundStationBlockEntity`, `packing/*`, `crafting/CraftingStationBlockEntity`, `cpu/CpuBlockEntity` and `CpuOrders`, `field/FieldHeadBlockEntity`, `rack/HeavyRackBlock` and `HeavyRackBlockEntity`, `rail/RailBlock`, `ServusStorageEvents`, registries, datagen, Foundation `side/SideConfig` mode lists.
- Data: `data/servus_storage/storage/crate.json` removed; generated crate assets and recipes removed by datagen.
- Tests: JUnit `CrateCompartmentsTest` and `CandidateNeighboursTest` removed, `RequestPlanTest`, `PutAwayPlanTest`, `WarehouseConnectionRuleTest` changed, `RackDockTest` added. About 200 of the 374 Storage GameTests build crates, chests beside nodes or use the item capability of the listed blocks and move to racks with shuttles through a shared GameTest helper.
- Specs: archives after `m11-wireless-packing-station` and before `m8-cpu-buffer`; `m11-controller-wireless` and `m11-warehouse-power` need small wording follow-ups (see design.md).
- Docs: `PLAN.md`, `docs/src/content/docs/mods/storage.md`, `docs/src/content/docs/index.mdx`, `docs/src/data/gamedata.json` and the crate icons in `docs/src/icons/servus_storage/`.

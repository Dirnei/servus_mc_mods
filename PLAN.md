# Servus Mod Family: Project Plan

A family of NeoForge mods for Minecraft 1.21.1, built to fit packs like ATM10 but fully standalone.

| Mod | Mod ID | Purpose |
|---|---|---|
| Servus Lib | `servus_lib` | Shared library: APIs, network graph, tick mailbox, wrench, config helpers |
| Servus: Manufacturing | `servus_manufacturing` | Factorio style resource production with its own tech tree |
| Servus: Logistics | `servus_logistics` | Bundled item and energy ducts (fluids later) with facades |
| Servus: Storage | `servus_storage` | In-world warehouse storage that scales to shuttles, autocrafting and wireless access |

Each content mod depends only on Servus Lib. No content mod depends on another. When several are installed, extra features light up through the Lib API.

---

## 1. Tech stack

- Minecraft 1.21.1, NeoForge 21.1.x (latest stable), Java 21
- Build: Gradle with ModDevGradle, one multi project repo
- Mappings: Parchment for readable parameter names
- Data generation for blockstates, models, loot tables, recipes, tags, lang
- Energy: NeoForge `IEnergyStorage` (FE). Items: `IItemHandler`. Fluids later: `IFluidHandler`
- Optional integrations (soft dependencies only): JEI and/or EMI, FTB Teams, FTB Chunks
- No Akka or Pekko. We use the actor pattern without the library (see section 3)

### Repository layout

```
servus_mc_mods/
  settings.gradle
  build.gradle              shared config
  gradle.properties         versions in one place
  lib/                      servus_lib
  manufacturing/            servus_manufacturing
  logistics/                servus_logistics
  storage/                  servus_storage
  docs/                     design notes, this plan
```

Repository: `github.com/Dirnei/servus_mc_mods`. Group id `io.github.dirnei.mc.servus`. Base package `io.github.dirnei.mc.servus`, with one subpackage per mod: `lib`, `manufacturing`, `logistics`, `storage`. License: MIT.

---

## 2. Design summary per mod

### 2.1 Servus: Manufacturing

**Core idea:** infinite resource deposits plus a factory you design and balance. Growth comes from research and scaling throughput, Factorio style.

Decisions already made:
- Deposits are **visible, infinite patches** in the world (iron, copper, coal, stone at Tier 1). No depletion, no drift. You pick a site once and keep it forever.
- Machines have **fixed speeds and clean ratios** so players can plan builds.
- **Own tech tree**, standalone. No dependency on items from other mods.
- Progression is gated by **research packs** consumed over time by a Research Lab.
- Research progress is **per team** (FTB Teams if present, otherwise per player).
- **FE** is the energy standard. The mod ships its own generators so it works without other mods.
- Techs, packs, recipes and deposit definitions are **datapack driven** so modpack authors can retune them.
- Production must work **overnight while chunk loaded**. A chunk loader upgrade with FE upkeep and a config cap per player/team is planned. Respect FTB Chunks if present.
- Includes a deliberately simple Item Duct and Power Cable so the mod is playable alone. Machines also push output into adjacent inventories.

**Tier 1 content (first playable slice)**

| Machine | Rate | Power |
|---|---|---|
| Burner Extractor | 1 ore per 4 s | burns coal |
| Burner Smelter | 1 ingot per 4 s | burns coal |
| Coal Generator | 40 FE/t | 1 coal per 20 s |
| Assembler Mk1 | recipe speed x1 | 20 FE/t |
| Research Lab | consumes packs | 10 FE/t |
| Item Duct | moves items | none |

Recipes:
- Iron Gear: 2 iron ingots, 1 s
- Copper Wire: 1 copper ingot to 2 wire, 0.5 s
- Red Pack: 1 gear + 2 wire, 5 s

Reference balanced block (1 Red Pack per 5 s): 2 extractors and 2 smelters on iron, 1 extractor and 1 smelter on copper, 1 extractor on coal, 3 assemblers, 1 lab.

First research options: Electric Extractor (2x speed), Crusher (ore to 2 grit), Splitter and Filter Ducts, Stone Processing, Logistics (unlocks green pack and Tier 2).

**Pack tiers (later):** Mechanical (red), Logistic (green), Chemical (blue), Advanced (purple), Exotic (yellow), plus repeatable infinite research at the end.

### 2.2 Servus: Logistics

- **Utility Duct**: one block with several channels (items and energy first, fluids later).
- Per side and per channel modes set with a wrench: input, output, both, disabled.
- Item channels: filters and priorities per side. Energy channels: tiered transfer rate.
- **Facades**: crafted from any full block plus an iron plate. The duct then looks like that block. Solid or hollow variants.
- Holding the wrench or wearing engineer goggles shows hidden ducts as a translucent overlay.
- Rendering: dynamic baked model driven by model data from the block entity. Facades reuse the target block's baked model.
- Separate selection and collision shapes per part so a single channel can be removed.
- Networks are cached graphs, rebuilt only on placement or removal, never by walking blocks every tick.

### 2.3 Servus: Storage

**Core idea:** the player builds a real warehouse. In-world storage, no storage disks.

Progression:
1. **Crates and shelving racks** (early, no power). Crates hold lots of one item type and show it on the front. Racks render their contents visibly.
2. **Warehouse Controller + Counter Terminal.** Search and pull items while standing at the counter. Vanilla and modded inventories in the network are indexed too.
3. **Shuttles on rails.** Requests and put-away are done by shuttles that travel along rails to a dispatch station. Throughput depends on shuttle count, rails and layout. Stats screen shows pick rates per item for slotting optimization. Put-away rules ("all ores to aisle 3") with overflow zones.
4. **Wireless.** Wireless Terminal with range tiers. Orders queue real picks. Delivery through an outbound dock: teleport for FE per item and distance, or courier drone to a landing pad.

**Network topology (decided direction):**
- Early: adjacency. The controller connects to touching crates and racks.
- Mid: **rails are the wiring.** Any rack next to a rail joins the network. The same rails carry shuttles later.
- Late: **Warehouse Relay** links remote depots wirelessly.

Shuttles are **simulated as data** on the controller and only rendered moving on the client. No real entities.

### 2.4 Integration: Storage + Manufacturing (autocrafting)

- **Work orders instead of patterns.** Requesting an item that is not in stock creates a work order.
- **Bill of materials explosion**: the order is broken into sub-orders. Stock on hand is reserved, the rest is produced.
- Recipes come from connected Manufacturing machines. Building an assembler for gears teaches the system to make gears.
- Machines get **input and output docks** next to the rails. Shuttles deliver ingredients and pick up results.
- **Min/max stock levels** per item create orders automatically ("keep 500 to 1,000 iron gears").
- **Order board** shows open orders, which machine works on what, and what is blocked and why.
- Standalone fallback: Storage alone gets a simple Crafting Station for vanilla recipes.

The bridge lives in Servus Lib as a small API (see section 3).

---

## 3. Architecture principles

1. **Server thread only for world access.** All reads and writes to the world, block entities and capabilities happen on the server thread inside the tick.
2. **Actor pattern without a library.** Each network (duct network, warehouse, factory site) is an object with its own event mailbox. Events: block placed, block removed, machine changed, order created, and so on. Every server tick, each network drains its mailbox and processes events in order. Deterministic and thread safe.
3. **Heavy work offloaded only when needed.** If something like a huge graph rebuild gets expensive, compute it on an executor from an immutable snapshot and apply the result on the next tick.
4. **Event driven, not polled.** Own blocks push change events. Foreign inventories (no events) are polled slowly and in batches.
5. **Data driven content.** Recipes, techs, deposits and balance numbers come from datapack JSON with codecs.
6. **Capabilities as the integration surface.** Machines and storage expose standard capabilities, so any mod's pipes and cables work.
7. **Pure logic separated from Minecraft.** Graph algorithms, BOM planning, ratio math, order scheduling live in plain Java classes with no Minecraft types, unit tested with JUnit.
8. **Persistence:** network and research state in `SavedData`, per chunk deposit data as data attachments, block state in block entities using 1.21 data components and codecs.

### Servus Lib API (initial sketch)

- `ServusNetwork` base: graph of nodes, mailbox, tick hook, split and merge handling
- `NetworkManager`: owns all networks per level, routes block events to the right network
- `ProductionProvider`: "this block can produce X from inputs Y at rate Z"
- `StockProvider` / `StockSink`: "this inventory can supply or accept items"
- `WorkOrder` model and a planner interface (implemented by Storage, fed by Manufacturing)
- Shared wrench item and goggles
- Config helpers

Keep the API small at first and grow it only when a second mod actually needs something.

---

## 4. Milestones

Each milestone ends in something playable or testable.

### M0: Repository and build
- Multi project Gradle setup with ModDevGradle, all four mods registered with empty main classes
- Shared versions in `gradle.properties`
- Run configs for client, server, data generation and GameTests
- GitHub Actions: build plus unit tests on every push
- Verify all four mods load together and each loads alone with Lib

### M1: Servus Lib foundations
- Network graph library (pure Java) with split and merge, plus JUnit tests
- Mailbox and tick scheduler
- `NetworkManager` hooked into block place and break events
- Wrench item
- Config scaffolding

### M2: Manufacturing first playable slice
- Deposit blocks and worldgen for iron, copper, coal, stone
- Burner Extractor, Burner Smelter (fuel handling, fixed rates)
- Coal Generator (FE)
- Assembler Mk1 with recipe selection GUI
- Item Duct and Power Cable (simple), adjacent output push
- Items: iron gear, copper wire, Red Pack
- Research Lab, tech tree data format, team progress, first five techs
- Minimal tech tree screen
- GameTest: the reference balanced block produces 1 Red Pack per 5 s

**Checkpoint: play it.** Is Tier 1 fun? Adjust numbers before building more.

### M3: Manufacturing Tier 1 completion
- Electric Extractor, Crusher, Splitter and Filter Ducts, Stone Processing
- JEI or EMI integration for machine recipes
- Chunk loader upgrade with FE upkeep and config caps
- Balance pass

### M4: Storage first playable slice
- Crates with front item display
- Shelving racks with visible contents (block entity renderer)
- Warehouse Controller with adjacency networking
- Counter Terminal with search and pull
- Indexing of foreign inventories with batched polling

### M5: Logistics first release
- Utility Duct with item and energy channels
- Per side modes, filters, priorities
- Facades and goggles overlay
- Dynamic baked model, per part shapes

### M6: Storage automation
- Rails as network connections
- Simulated shuttles with client rendering
- Dispatch station, put-away rules, overflow zones
- Pick rate stats screen

### M7: Autocrafting integration
- Lib API for production and stock
- Work orders, BOM explosion, reservations
- Input and output docks on Manufacturing machines
- Min/max stock rules
- Order board screen
- Storage only Crafting Station fallback

### M8: Wireless and scale
- Warehouse Relay
- Wireless Terminal with range tiers
- Outbound dock: teleport delivery and courier drone
- Manufacturing Tier 2 (green pack, steel, circuits)

---

## 5. Open decisions

- Order prioritization: explicit priorities vs first come first served
- Does a warehouse without shuttles feel complete enough on its own?
- Exact rail block design (floor rails, ceiling rails, both?)
- Tier 2 to 5 content details
- Art style and texture direction (shared look across the family)
- Final name for the storage mod (Servus: Storage vs Servus: Warehouse)

---

## 6. Working agreement for the coding sessions

- Work milestone by milestone. Do not start the next milestone before the current one builds, runs and is tested.
- Propose a short plan before larger changes and wait for confirmation.
- Prefer small commits with clear messages.
- Pure logic gets JUnit tests. Block behavior gets GameTests where practical.
- When unsure about a NeoForge 1.21.1 API, check the official NeoForge docs or existing source instead of guessing.
- Keep balance numbers in data files, not in code.

# Servus Mod Family: Project Plan

A family of NeoForge mods for Minecraft 1.21.1, built to fit packs like ATM10 but fully standalone.

| Mod | Mod ID | Purpose |
|---|---|---|
| Servus: Foundation | `servus_foundation` | Base of the family: shared APIs, network graph, tick mailbox, the Utility Duct (item, energy, fluid and data lines) with facades, the Fluid Tank, pumps, the Sink, the Chunk Loader, the Coal Generator, Configuration Carrot (the wrench role), goggles, config helpers |
| Servus: Manufacturing | `servus_manufacturing` | Alchemy after Paracelsus: the three principles, gnome grown ore veins, the Athanor and his journal as the tech tree |
| Servus: Storage | `servus_storage` | In-world warehouse storage that scales to shuttles, autocrafting and wireless access |

Manufacturing and Storage depend only on Servus: Foundation and never on each other. When both are installed, extra features light up through the Foundation API. Decided in M8: Servus Lib and Servus: Logistics were merged into Servus: Foundation, so every mod gets the ducts and the data line can live in the Utility Duct.

---

## 1. Tech stack

- Minecraft 1.21.1, NeoForge 21.1.x (latest stable), Java 21
- Build: Gradle with ModDevGradle, one multi project repo
- Mappings: Parchment for readable parameter names
- Data generation for blockstates, models, loot tables, recipes, tags, lang
- Energy: NeoForge `IEnergyStorage` (FE). Items: `IItemHandler`. Fluids: `IFluidHandler`, in mB
- Optional integrations (soft dependencies only): JEI and/or EMI, FTB Teams, FTB Chunks
- No Akka or Pekko. We use the actor pattern without the library (see section 3)

### Repository layout

```
servus_mc_mods/
  settings.gradle
  build.gradle              shared config for all mod projects, combined run configs
  gradle.properties         versions in one place
  gradlew, gradle/wrapper/  Gradle wrapper
  .github/workflows/        CI: build and unit tests on every push
  openspec/                 specs and changes (OpenSpec)
  docs/                     documentation site and Balance lab (Astro, Starlight), deployed to GitHub Pages
  foundation/               servus_foundation
  manufacturing/            servus_manufacturing
  storage/                  servus_storage
    gradle.properties       mod id and name
    build.gradle            dependencies and loaded mods
    src/main/java           mod code
    src/main/templates      neoforge.mods.toml template
    src/generated/resources datagen output
    src/test/java           JUnit tests
    runs/                   game directories per run config, gitignored
```

Repository: `github.com/Dirnei/servus_mc_mods`. Group id `io.github.dirnei.mc.servus`. Base package `io.github.dirnei.mc.servus`, with one subpackage per mod: `foundation`, `manufacturing`, `storage`. Inside Foundation the API packages sit directly under `foundation` and the duct, tank and facade content under `foundation.logistics`. License: MIT.

---

## 2. Design summary per mod

### 2.1 Servus: Manufacturing

**Core idea:** alchemy after Paracelsus. Matter is split into the three principles, Salt, Sulfur and Mercury, and products are made again from them. Paracelsus' elemental spirits do the work, and gnomes grow ore veins instead of the world generating them. The story is Paracelsus' own life, told by his journal, which is also the tech tree.

Decided in the alchemy rethink (replaces the Factorio style design of M2 and M3, which nothing released depends on):
- **Two audiences, one path.** Nothing forces a factory, everything rewards one. A casual player does alchemy with a handful of blocks and buckets; a planner pipes the principles, balances the ratios and lets the Crafting CPU order. Both reach the same chapters. No task ever asks for a sustained throughput.
- **Tria Prima.** The Calciner splits matter into Salt, Sulfur and Mercury, three fluids that travel through the Fluid Pipe and wait in Fluid Tanks. Salt leaves the fire as Ash, as in the lore, and the Purifier leaches the Ash with water into liquid Salt. The Coagulator makes products from a fixed ratio of the three. Every input and every product has its own ratio, so surplus of one principle is normal and goes to the Sink or to a later converter. In Nigredo the Calciner takes almost anything (cobble, wood, dirt, ore), so the early game needs no ore source but the pickaxe. Machines with several tanks assign a tank per side in the Fluids tab of the side panel, so each pipe arm and its channel carry one principle.
- **Machines are built, not placed.** The alchemy machines become multiblocks of vanilla blocks and custom parts with fixed ports by position instead of side configuration and GUIs. The player forms one by using Paracelsus' Journal on its controller (never the carrot); the journal shows each build as a schematic with Visualize, generated from the same layout that forming checks, and the formed build is drawn as one model. The Calciner comes first: a brick furnace with an Alembic, whose Ash Pan gives Ash, Receiver Mercury and Hood Sulfur into two Fluid Tank columns; the Coagulator follows with three Principle Vats over a casting table, and the Athanor stands on a diamond brick hearth and drinks from three Fluid Tanks two blocks out, which never touch. The Purifier is a Leaching Vat on a log, with a Catch Basin for the Salt and a Residue Crock for the Caput Mortuum beside the log, so each output has its own port; the Caput Mortuum is kept for a later diamond. Forming any build plays a sound and sends particles up from every block.
- **No world generation.** Modded packs have ores enough. The deposit worldgen of M2 goes away.
- **Gnomes grow the veins.** A Gnome Hollow is seeded with a sample of an ore, matched by tag (`c:ores/iron`, `c:ores/osmium`, ...), so ores of other mods can be grown too. The vein ripens over time while it is fed principles (mostly Salt, the body of metals). A bound gnome then works the mature vein at a fixed rate forever. In Paracelsus' time miners believed metals grow in the earth like plants; this is that belief made real. Seeds are gated by stage (iron and copper early, gold and diamond late).
- **Elemental Sight.** Paracelsus' elementals live in their element as we live in air, unseen. A Gnome Hollow looks like a plain block. Only under Elemental Sight are the ore buds in the rock around it, the working gnome and the ripeness shown, drawn on the client like the duct overlay; the server keeps only data (vein, ripeness, rate) and never changes a block. Ducts and the data cable reach the Hollow without sight. The sight comes from the **Elixir of Sight**, brewed from the principles.
- **The four elementals** are the workforce: Gnomes (earth: veins), Salamanders (fire), Undines (water), Sylphs (air). Gnomes come first; what the others do is decided with their stages.
- **Stages of the Great Work** replace the pack tiers: Nigredo (black), Albedo (white), Citrinitas (yellow), Rubedo (red).
- **The Athanor** is the heart of the lab, like the Blood Altar of Blood Magic. Important crafts happen on it (the Elixir of Sight, vein seeds, stage items): the item lies on top and the Athanor draws principles over time from the three Fluid Tanks of its build while its flame burns; any tank holds any principle, fed by bucket or by Fluid Pipe. Its tier is the structure built around it, one ring or part per stage (example: Salt Crystal blocks for Albedo, Sulfur pillars for Citrinitas, Mercury basins and the Stone for Rubedo), and its flame takes the stage's colour. The tier of the structure is the stage gate: there is no separate stage item and no stored stage progress.
- **The journal is the tech tree.** A Modonomicon book, Paracelsus' journal, with one chapter per part of his life (Villach, Schwaz, the wandering years, Basel, Salzburg). A chapter lists tasks of two kinds only: do something N times, or make something for the first time. When all tasks of a chapter are done, the chapter text appears and its recipes unlock. No Research Lab and no packs: Paracelsus held experience over books. Chapter progress is per team (FTB Teams if present, otherwise per player); the team, sync and recipe unlock code of the M2 research is reused.
- **Modonomicon** is a hard dependency of Manufacturing. Its NeoForge 1.21.1 Maven artifact is verified before the journal design is written.
- **FE** stays the energy standard. The Coal Generator moves to Foundation, so every Servus mod has an FE source. Nigredo itself runs on fire and time: the Calciner burns furnace fuel and the Coagulator needs no power (decided in the play check); FE versions arrive later as upgrades, together with the Salamanders.
- Machines have **fixed speeds and clean ratios** so players can plan builds. Alchemy machines expose the production capability and join the data cable, so the Crafting CPU orders "64 iron ingots" without caring that it is alchemy.
- Principle ratios, recipes, vein seeds, ripening, journal tasks and Athanor structures are **datapack driven** so modpack authors can retune them.
- Ducts and cables come from Servus: Foundation (the Utility Duct). Machines also push output into adjacent inventories.
- Every machine and station configures its six sides the same way: Auto, Input, Output, Both, Pull, Push and Off from one shared side panel beside the GUI. The Configuration Carrot cycles the clicked face, and the modes are drawn on the block as coloured frames, with glyphs while the carrot is in hand.

Prior art to stay apart from: Theurgy also works with Sulfur, Salt and Mercury, on single items in small setups. Servus stays factory scale, with grown veins, the elementals as workforce, Paracelsus' life as the story and production on demand through the Crafting CPU.

**Story outline of the journal**

| Stage | Chapter | Story | Unlocks (examples) |
|---|---|---|---|
| Nigredo | Villach | his father, metals and the first fire | Calciner, Coagulator, Athanor |
| Albedo | Schwaz | the silver mines, the miners' sickness, the gnomes | Elixir of Sight, Gnome Hollow, iron and copper seeds |
| Citrinitas | Wandering years | the Tria Prima, "the dose makes the poison", zinc | Salamanders, more seeds |
| Rubedo | Basel and Salzburg | the burned textbook, his death in 1541 | the Stone, late seeds, endless works |

Historical claims in the journal text are checked again when it is written.

**Nigredo content (first playable slice, examples, numbers in datapack JSON)**

| Block | Does | Power |
|---|---|---|
| Calciner (multiblock: Bricks, mouth, Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood) | splits one input into its principles, example 1 item per 4 s | burns furnace fuel in the Firebox |
| Purifier (build: Leaching Vat, Catch Basin, Residue Crock, log) | leaches Ash with water into liquid Salt, example 4 Ash and 400 mB water into 400 mB Salt and 1 Caput Mortuum in 8 s | none |
| Coagulator | makes a product from a principle ratio | none, it takes time to set |
| Athanor (Nigredo tier) | slow altar crafts that draw principles | none, burns principles |
| Coal Generator (Foundation) | 40 FE/t, 1 coal per 20 s | burns coal |

### 2.2 Servus: Foundation

The base mod: the shared API (networks, production and stock capabilities, side config, placement assist, config), the Configuration Carrot, the goggles, the transport content and the general utilities below. It was Servus Lib plus Servus: Logistics until M8. Pumps, the Sink, the Chunk Loader and the Coal Generator moved here from Manufacturing in the alchemy rethink, because they are not alchemy and Storage needs them as much: a warehouse voids overflow and runs overnight, and Foundation's fluid line gets its own source. Foundation has no research, so all of them use plain crafting recipes.


- **Utility Duct**: one block with several lines (Item Pipe, Power Cable and Fluid Pipe). Each line is placed as its own item and adds to a duct that lacks it.
- Per side and per line modes (in and out, pull in, push out, disabled) and one of eight colored channels per line and side, set in an endpoint GUI that the Configuration Carrot (the wrench role) opens on the arm that touches an inventory or machine. Items, energy and fluids only flow between sides on the same channel. A bare hand opens nothing; sneak carrot removes the facade, then the duct.
- Item Pipe: filters and priorities per side. Power Cable: tiered transfer rate (later). Fluid Pipe: moves any fluid between blocks with `IFluidHandler`, no rate limit of its own (pumps set the throughput), no filters or priorities. Data line: tiers with a bandwidth, simplex or duplex (see the M8 decisions in section 2.4).
- **Facades**: crafted from any full block plus an iron plate. The duct then looks like that block. Solid or hollow variants.
- Holding the Configuration Carrot or wearing engineer goggles shows hidden ducts as a translucent overlay.
- Rendering: a dynamic baked model composed from part models (a centre and arms per line set) driven by model data from the block entity. A duct grows thicker with its lines, each line has its own color (Item Pipe orange, Power Cable red, Fluid Pipe blue, the data line white, gray or black by tier, the same on the line items), all of them show as bands on a duct with several lines, and an arm that ends at an inventory or machine carries a ring in its channel color. Facades reuse the target block's baked model.
- The shape follows the lines: thin for one line, thicker for two, thickest for three, arms as thick as the lines that connect there. Lines are removed through the endpoint GUI, not by clicking a part.
- Networks are cached graphs, rebuilt only on placement or removal, never by walking blocks every tick.
- **Fluid Tank**: a glass block holding one fluid. Touching tank blocks that fill a complete rectangular box form one tank with a shared content and a capacity of blocks times `capacity_per_block` from data (example 16,000 mB); a group that is not a full box works as single block tanks until it is. Every block stores its own share and the tank network lays the shares out lowest layer first, so forming, splitting and chunk unloads move no fluid. Joined blocks hide glass and frame toward each other through a multipart model. Buckets, an empty hand read out and `IFluidHandler` on every face; a broken block keeps its share in its item.
- **Pumps** are the first fluid source: a Burner Pump (furnace fuel) and an Electric Pump (FE, faster) stand on an infinite water source, a water source block with at least two water source neighbours, and fill an internal water buffer without ever using the water up. Their sides are fixed: water leaves through every face but the front and the bottom, into the Fluid Pipe or a Fluid Tank. The Burner Pump looks like a small beam engine two blocks tall (firebox and boiler below, chimney, cylinder and a rocking beam above, smoke while it burns) and uses only the back and sides of its lower half; the Electric Pump stays a compact single block. Both are crafted at a crafting table. Rates and buffer size are datapack numbers (examples: 250 mB and 500 mB per second, 4,000 mB buffer).
- **Sink and Chimney** get rid of surplus: the Sink is a 2x2x2 multiblock of eight brick Sink blocks, formed by clicking one side of the cube with the Configuration Carrot. That side becomes the front and shows one big fan behind a grate; breaking any block unforms it. The formed Sink voids items, fluids and FE through the standard capabilities on every outer face except the front, with no side configuration, and from a one slot trash GUI or a bucket by hand. Brick Chimney blocks stacked on any of its four top corners raise its void rate per height (the tallest column counts), and from a configured height on (example 5 blocks) it voids without limit. While it voids, the fan spins and smoke rises from the top of the tallest column. Rate tables per height and the active time are datapack numbers (examples: 4 to 64 items per second, 50 to 800 mB and 100 to 1,600 FE per tick).
- **Chunk Loader**: keeps production and warehouses running overnight, with FE upkeep and a config cap per player or team. Respects FTB Chunks if present.
- **Coal Generator**: the family's FE source so no mod needs another mod for power (example 40 FE/t, 1 coal per 20 s).

### 2.3 Servus: Storage

**Core idea:** the player builds a real warehouse. In-world storage, no storage disks.

Progression:
1. **Crates** (early, no power). A crate has one, two or four compartments that split its capacity; each compartment holds one item type and shows it with its count on the front. The Heavy Duty Rack is the chest sized bay for mixed items: bays with the same facing and the same wood join side by side into sections of a few bays, and a section shares one GUI that shows the slots of every bay (each bay still stores its own part). Stacked racks stay separate. A shuttle serves a whole section from a rail in front of any of its bays; rails along a rack's side or back never reach it. The front shows the item types the bay holds the most of.
2. **Warehouse Controller + Storage Terminal.** Search and request items at the terminal and hand items in through its input row; deliveries land in its output row. Vanilla and modded inventories in the network are indexed too.
3. **Shuttles on rails.** Requests and put-away are done by shuttles that travel along rails to a inbound station. Throughput depends on shuttle count, rails and layout. Put-away rules ("all ores to aisle 3"): a Zone Label carries a name and an item filter and is stamped onto racks; stamped racks take matching items first, unzoned racks take everything and act as the overflow.
4. **Data cable and on-demand autocrafting.** The warehouse joins the data cable through Packing Stations, and the Storage Terminal, Crafting CPU and Pattern Storage live on the cable (see 2.4).
5. **Wireless.** Wireless Terminal with range tiers and a Wireless Packing Station that links remote places without a cable. Orders queue real picks. Delivery through an outbound dock: teleport for FE per item and distance, or courier drone to a landing pad.

**Rails or cable (decided rule):** only storage touches rails, so shuttles can reach it: racks, crates, Shuttle Garages, Rail Elevators, Inbound and Outbound Stations and Packing Stations. Everything else sits on the data cable: terminals, Crafting CPU, Pattern Storage, Crafting Stations, machines and Machine Adapters. The Warehouse Controller sits on the rails and is connected to the cable.

**Network topology (decided direction):**
- Early: adjacency. The controller connects to touching crates. Blocks that shuttles serve never join by touching, because shuttles are the only way in and out: a rack section joins only through a rail in front of one of its bays (m6-rack-rail-only), Inbound and Outbound Stations and Packing Stations only through a rail on any of their sides, and a Shuttle Garage only through the rail at its back (m6-stations-rail-only). Open question: whether crates, the controller and foreign inventories should become rail only as well.
- Mid: **rails are the wiring.** A rack section joins the network through a rail in front of any of its bays. The same rails carry shuttles later. Decided: the Warehouse Rail is a flat rail, one block wide, that auto connects to neighbouring rails on the same level like ducts do. It stands on a floor or hangs without one on a rack front, an elevator segment or beside a standing rail; free rails between two hung rails bridge a gap of up to `rail_max_span` blocks (3 by default, in `warehouse.json`), and a gap that is wider or held on one side only collapses and drops its rails. No slopes, no ceiling rails. Rails hold nothing and never pull plain inventories in. Floors are linked by the Rail Elevator: a shaft of stacked segments, each taking rails on its sides, that shuttles will ride vertically.
- Beyond the rails: the **data line** of the Servus: Foundation Utility Duct links the warehouse, through its Packing Stations, to terminals, the Crafting CPU and machines far away.
- Late: the **Wireless Packing Station** links remote places without a cable (it takes the place of the Warehouse Relay idea).

Shuttles are **simulated as data** on the warehouse network and only rendered moving on the client. No real entities. Decided: Shuttle items go into Shuttle Garages, a few per garage, and park on the rail touching their garage; more garages mean more shuttles. Heavy Duty Racks are served only by shuttles, so a rack warehouse needs a garage; crates and foreign inventories stay instant when no shuttle reaches them. Players request items at the Storage Terminal with an amount and collect the delivery from its output slots.

Decided: every elevator shaft has one simulated **lift car**. A shuttle that changes level stops on the rail beside the shaft, calls the car and waits; the car serves calls in the order they were made and carries one shuttle per trip at `lift_speed` from `warehouse.json`, then stays parked where it stopped. A shuttle that only crosses a shaft on one floor does not call the car; it drives over the floor's deck, two flaps hinged between the corner posts that fold up while the car is at or passing that floor, and the car waits for it. A floor without a rail beside it shows only its posts, and the top segment carries a head with the motor beam. Segments work like scaffolding: players walk into them from the side and climb inside, stand on every level from above and sneak to sink down, and a click with an elevator on any segment places the next one on top of the shaft. Breaking a segment under a riding shuttle drops its cargo at its garage.

### 2.4 Integration: Storage + Manufacturing (autocrafting)

**USP: production on demand.** Nothing runs without an order. A classic pipe line (Outbound Station, pipe, chest, hopper, furnace, hopper, Inbound Station) produces until storage is full or the input runs out; a machine on the data cable produces exactly what was ordered and then waits.

What M7 built and what M8 does with it:
- **Foundation production API** (production and stock capabilities, `OrderPlanner`): kept. The data cable finds its members through it and the CPU plans with it.
- **Work orders, bill of materials explosion, reservations**: kept, but they move from the Warehouse Controller to the Crafting CPU, and their steps run over the cable instead of through stations.
- **Min/max stock levels** per item ("keep 500 to 1,000 iron gears"): kept on the controller; a rule hands its order to the CPU on the controller's data network.
- **Inbound Station**: kept as the put-away entry of the warehouse. It no longer credits order steps.
- **Outbound Station**: kept as a plain buffer with keep slots for pipe lines that run all the time. It no longer feeds work orders. Station fronts, like machine fronts, never move items.
- **Crafting Station**: kept; it loses its own recipe list once patterns exist and becomes the hands of the CPU.
- **Order Board**: removed by the CPU change; the Production Terminal takes its place right after.

**One route for orders.** Everything on demand runs over the data cable; everything that runs all the time uses stations and pipes. There is no second order route through Outbound Stations.

Decided for M8 (on-demand autocrafting):
- **Data cable** (Servus: Foundation): the Data line of the Utility Duct. Like redstone, not like a real network: place it and it works, no addresses, channels or priorities. It never pushes or pulls on its own; it only routes packets for an order. Members are found by the capabilities they expose. The line itself is instant: items do not travel and distance does not matter. Throughput is set where items cross between the warehouse and the line, at the Packing Station (decided in `m8-packing-station`), and by the bandwidth of the cable (decided in `m8-data-bandwidth`): the data line comes in tiers, Data Cable (32 items per second shared by both directions, simplex), Duplex Data Cable (32 each way) and Fast Data Cable (128 each way), with stats in datapack JSON. A delivery moves no faster than the slowest duct on its path; deliveries that cross a duct share its budget in creation order, and a delivery takes the shortest path with the most bandwidth, no max flow. A higher tier cable used on a duct replaces the line in place and drops the old cable. Load shows in the duct summary, with the goggles (duct colours) and in the Production Terminal's Machines tab. Item pipes stay useful: they move items all the time and work with any mod.
- **Packing Station** (Storage). Sits on a rail and on the cable and converts between the physical warehouse and the digital line: shuttles bring items into its To network buffer and it packs them into packets, and arriving packets are unpacked into its To storage buffer and handed to shuttles for put-away. It converts `items_per_second` (8 by default, datapack JSON) in each direction, the two directions independent; the cable's bandwidth is the other limit. Items wait at their source until it can take them (the terminal's Input row shrinks as it converts, crate picks are released as its buffer has room). Its GUI shows both buffers read-only with a status line. A warehouse may have several; each adds its own rate. The controller picks the Packing Station with the shortest shuttle trip; on a tie any of them. It has upgrade slots (`upgrade_slots`, 2 by default) for Speed Upgrades, each multiplying its rate (x2 by default).
- **Upgrades** (decided in `m8-data-bandwidth`): Foundation owns upgrade items and the upgrade slot API, so every Servus mod can use them without depending on each other. The Speed Upgrade is crafted from vanilla materials; its multiplier comes from `upgrades.json`. Manufacturing may add better upgrade items later through the same API (an `UpgradeItem` or the `servus_foundation:upgrades` tag plus a JSON entry); Foundation never depends on Manufacturing.
- **Warehouse Controller** on the cable answers stock questions and assigns shuttles. It moves no items itself.
- **Storage Terminal** on the cable: the player requests items, shuttles bring them to a Packing Station, packets deliver them into the terminal's output slots.
- **Crafting CPU** (Storage, 3x3x3 multiblock on the cable) owns work orders; this moves order handling from the controller to the CPU. Every CPU adds order slots, more CPUs run more orders in parallel. Decided: a new order goes to the CPU with a free slot and the lowest position, else to the one with the fewest open orders; orders stay on their CPU.
- **Patterns for crafting recipes only.** A pattern is made with the recipe editor (with JEI transfer) and holds concrete items, so the planner never chooses between recipes or tag variants. Patterns live in **Pattern Storage** blocks on the cable, N per block (from datapack JSON); more blocks hold more patterns. Decided: no tiers, only more blocks. Decided after the play test: patterns are data, not items; they are made, edited and deleted only in the Production Terminal's Patterns tab, and a mined Pattern Storage keeps them in its item.
- **Crafting Stations are the hands.** They lose their own recipe list and craft what the CPU assigns, fed by packets. More stations craft more in parallel.
- **Devices carry the line.** Decided after the play test: the Storage Terminal, Production Terminal, Crafting Station, Pattern Storage and a formed CPU carry the data network to touching devices like Refined Storage devices, so a wall of devices needs one duct. Packing Station, Machine Adapter and Warehouse Controller do not carry it.
- **Machines say what they can do.** Servus machines expose the production capability and join the cable directly, no pattern needed. Building an assembler for gears teaches the system to make gears.
- **Machine Adapter** (Servus: Foundation) for machines of other mods. It touches the machine, inserts the ingredients and pulls the results itself, which counts them for the order. Six virtual side roles relative to the machine's facing: Ingredients, Upkeep, Output, Off. The first version uses **profiles** only: datapack JSON that names the blocks, the recipe type and the side roles (vanilla furnace, smoker and blast furnace ship with it, packs can add more). Hand made patterns for adapters may follow after play testing.
- **Upkeep is not part of a recipe.** Fuel, infuse items and the like are kept stocked in their slot at a level (an item and an amount set per machine), refilled always, independent of orders. A recipe stays "1 cobblestone to 1 stone". Native burner machines report their upkeep the same way. A machine whose upkeep is not in stock is blocked as out of fuel, the order is not.
- **Production Terminal** on the cable with three tabs: Patterns (list, add and edit, a capacity bar that shows when to add Pattern Storage), Machines (recipes, state, upkeep) and Orders. It replaces the Order Board.
- Storage alone crafts with patterns and can smelt through an adapter on a vanilla furnace; Manufacturing machines are the next step (power, speed, real chains).

The bridge lives in Servus: Foundation as a small API (see section 3).

---

## 3. Architecture principles

1. **Server thread only for world access.** All reads and writes to the world, block entities and capabilities happen on the server thread inside the tick.
2. **Actor pattern without a library.** Each network (duct network, warehouse, factory site) is an object with its own event mailbox. Events: block placed, block removed, machine changed, order created, and so on. Every server tick, each network drains its mailbox and processes events in order. Deterministic and thread safe.
3. **Heavy work offloaded only when needed.** If something like a huge graph rebuild gets expensive, compute it on an executor from an immutable snapshot and apply the result on the next tick.
4. **Event driven, not polled.** Own blocks push change events. Foreign inventories (no events) are polled slowly and in batches.
5. **Data driven content.** Recipes, principle ratios, vein seeds, journal tasks, Athanor structures and balance numbers come from datapack JSON with codecs.
6. **Capabilities as the integration surface.** Machines and storage expose standard capabilities, so any mod's pipes and cables work.
7. **Pure logic separated from Minecraft.** Graph algorithms, BOM planning, ratio math, order scheduling live in plain Java classes with no Minecraft types, unit tested with JUnit.
8. **Persistence:** network and journal progress in `SavedData`, block state in block entities using 1.21 data components and codecs.

### Servus: Foundation API (initial sketch)

- `ServusNetwork` base: graph of nodes, mailbox, tick hook, split and merge handling
- `NetworkManager`: owns all networks per level, routes block events to the right network
- `ProductionProvider`: "this block can produce X from inputs Y at rate Z"
- `StockProvider` / `StockSink`: "this inventory can supply or accept items"
- `WorkOrder` model and a planner interface (implemented by Storage, fed by Manufacturing)
- Data line of the Utility Duct and its network, members found by capability, instant packet transport limited by transfer gates (`TransferGate`, the Packing Station is one) and by the bandwidth of the cable tiers on the path (`CableTier` from `cable_tiers.json`, pure `DuctBudget` and `PathPick`)
- Upgrades: `UpgradeItem`, `UpgradeSlots` and the `servus_foundation:upgrades` tag, multipliers from `upgrades.json`, combined by pure `UpgradeMath`
- Machine Adapter with datapack profiles (blocks, recipe type, side roles) and upkeep (keep an item at a level in a slot), so machines of other mods join the data cable
- `ItemSupplier` capability (`servus_foundation:supplier`): a block that sends items to a data member on request; warehouses implement it, adapters use it for upkeep
- Shared Configuration Carrot (the wrench role) and goggles
- Placement assist: sneaking with a rail, duct or elevator item extends a straight run at its far end, with a ghost preview
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
- Configuration Carrot, the item that fills the wrench role
- Config scaffolding

### M2: Manufacturing first playable slice
Built, then superseded by the alchemy rethink (M9). Deposits, extractors, the Burner Smelter, the Assembler, the Research Lab and the packs go away there.

- Deposit blocks and worldgen for iron, copper, coal, stone
- Burner Extractor, Burner Smelter (fuel handling, fixed rates)
- Coal Generator (FE)
- Assembler Mk1 with recipe selection GUI
- Adjacent output push; items and energy travel through the Logistics Utility Duct
- Items: iron gear, copper wire, Red Pack
- Research Lab, tech tree data format, team progress, first five techs
- Minimal tech tree screen
- GameTest: the reference balanced block produces 1 Red Pack per 5 s

**Checkpoint: play it.** Is Tier 1 fun? Adjust numbers before building more.

### M3: Manufacturing Tier 1 completion
Built. Pumps, Sink and Chunk Loader move to Foundation in M9; the other machines are superseded by the alchemy rethink. The docs site and Balance lab stay.

- Electric Extractor, Crusher, Stone Processing
- Burner Pump and Electric Pump
- Sink and Chimney
- JEI or EMI integration for machine recipes
- Chunk loader upgrade with FE upkeep and config caps
- Balance pass
- Documentation site and Balance lab: public docs plus an in browser lab that reads the game data exported by the Manufacturing data run, shows production chains, research times and a recipe graph of every Servus recipe with raw costs per item, lets us edit numbers and draft new items, recipes and machines, and exports the result as a proposal. A target rate planner that designs a whole build follows after the first release.

### M4: Storage first playable slice
- Crates with front item display
- Crate family: Crate, Double Crate and Quad Crate with per compartment display (block entity renderer)
- Heavy Duty Rack: chest sized bay with a GUI, sections of the same facing and wood that share one GUI and are served by shuttles from any front rail, a sample display and climbable side posts
- Warehouse Controller with adjacency networking
- Storage Terminal with search and pull
- Indexing of foreign inventories with batched polling

### M5: Logistics first release
- Utility Duct with item and energy channels
- Fluid Pipe line
- Per side modes, filters, priorities
- Facades and goggles overlay
- Dynamic baked model, per part shapes
- Fluid Tank

### M6: Storage automation
- Rails as network connections
- Rail Elevator for multi level warehouses
- Simulated shuttles with client rendering
- Inbound station, Zone Labels with put-away filters, unzoned storage as overflow
- Shuttle Garage, rack picks by shuttle, terminal requests
- Sneak placement assist for rails, ducts and elevators

### M7: Autocrafting integration
- Lib API for production and stock
- Work orders, BOM explosion, reservations
- Outbound Station feeds machines, Inbound Station collects their output
- Min/max stock rules
- Order board screen
- Storage only Crafting Station fallback

### M8: On-demand autocrafting
One change per item:
One change per item, in this order. Each one builds only on the ones before it and leaves the game playable, so nothing is built twice:
1. `m8-foundation-merge`: Servus Lib and Servus: Logistics become one mod, Servus: Foundation (`servus_foundation`); Manufacturing and Storage depend only on it.
2. `m8-data-cable`: data network with members found by capability, carried by a Data line of the Utility Duct (the standalone Data Cable block goes away).
3. `m8-cable-packets`: packets over the cable with size and speed from datapack JSON; a packet is delivered into a member's item handler and results are taken from it. Tested with a command or GameTests, no player feature yet.
4. `m8-access-point`: Foundation `data_link` capability for members without production or stock; the Packing Station (under its first name) on rail and cable (shuttles to packets and back, shortest shuttle trip wins); Warehouse Controller as a cable member.
5. `m8-item-terminal-cable`: Storage Terminal joins the cable instead of the warehouse; requests are delivered as packets through a Packing Station.
6. `m8-crafting-cpu`: 3x3x3 multiblock on the cable. Work orders and the order book move from the controller to the CPU, stock rules and terminal shortfalls hand their orders to it, steps run over the cable on producers with the recipes they report today (Assembler, Crafting Station with its own list). Removes the Outbound Station order route, the Inbound Station step credit and the Order Board block; `/servus_warehouse orders` stays for diagnosis. More CPUs run more orders in parallel.
7. `m8-production-terminal`: terminal on the cable with Machines and Orders tabs, built on the CPU's orders.
8. `m8-pattern-storage`: patterns, Pattern Storage with N patterns per block, the Patterns tab in the Production Terminal with the editor and JEI transfer and a capacity bar; the CPU plans crafting recipes from patterns.
9. `m8-station-hands`: the Crafting Station drops its own recipe list and editor and crafts the pattern jobs the CPU sends it.
10. `m8-machine-adapter`: Foundation Machine Adapter with side roles, upkeep and profiles for the vanilla furnace, smoker and blast furnace.
11. `m8-native-machines`: Manufacturing machines report upkeep; burner machines join with fuel upkeep.
12. `m8-persist-production` (play test follow-up): requests, CPU orders, station jobs and Packing Station shipments are saved and survive restarts and chunk unloads; an unloaded CPU pauses its orders instead of cancelling them.
13. `m8-storage-terminal` (play test follow-up): the Counter Terminal becomes the Storage Terminal with an Input row that stores what is put in, an Output row for deliveries, and the crafting grid in a popup behind a Craft button.
14. `m8-packing-station` (play test follow-up): the converter between warehouse and data line is renamed Packing Station, gets a read-only GUI with To storage and To network rows and a status line, and becomes the throughput limit: the data network turns instant, the station converts `items_per_second` per direction, and items wait at their source (the terminal's Input row drains at the station's pace).
15. `m8-data-bandwidth` (play test follow-up): data cable tiers with a bandwidth, simplex and duplex cables, cables upgraded in the world, Packing Station speed upgrades, load shown with the goggles and in the Production Terminal. Replaces the old High-Speed Data Cable idea.

### M9: Manufacturing becomes alchemy
One change per item, in this order. Nothing is released, so the old content is removed, not migrated. Each change leaves the game playable:
1. `m9-foundation-utilities`: Burner and Electric Pump, Sink and Chimney, Chunk Loader and Coal Generator move to Foundation with plain crafting recipes.
1b. `m9-burner-pump-steam-engine` (play feel follow-up): the Burner Pump becomes a small beam engine two blocks tall, placed and broken as one, with a rocking beam and chimney smoke while it burns; it gives water through the back and sides of its lower half.
2. `m9-tria-prima`: Salt, Sulfur and Mercury as fluids, the Calciner and the Coagulator with principle ratios from datapack JSON, both on the data cable with the production capability. Removes deposits and their worldgen, the extractors, the Burner Smelter, the Assembler, the Crusher, Stone Processing and the gear, wire and pack items.
2b. `m9-side-config-tabs` (after the play check of the alchemy machines): the side configuration gets layers, Items and a Fluids layer that assigns one tank per side, as tabs in the side panel and a layer on the Configuration Carrot switched on the air. The Calciner and the Coagulator use it.
3. `m9-journal`: Modonomicon dependency and Paracelsus' journal, chapters with count and first time tasks per team, chapter completion unlocks recipes. Replaces the Research Lab, packs and techs.
4. `m9-athanor`: the Athanor with its altar crafts drawing principles and its structure tiers as the stage gate (Nigredo and Albedo first).
5. `m9-elixir-of-sight`: the Elixir of Sight and the Elemental Sight effect, with the client render hook that shows hidden things only under it.
6. `m9-gnome-hollow`: vein seeds by ore tag, ripening fed by principles, binding a gnome, a fixed rate forever, the vein drawn only under Elemental Sight, production capability for the Crafting CPU.
6b. `m9-calciner-multiblock`: the Calciner as a one block deep brick furnace with Firebox, Ash Pan, Alembic, Receiver and Sulfur Hood, a Fluid Tank column each for Mercury and Sulfur joined by spout and Copper Duct, formed with the Journal, fixed ports, no GUI or side configuration, one formed model, a schematic page with Visualize and a Jade tooltip.
6b2. `m9-salt-leaching`: the Ash Pan gives Ash instead of liquid Salt, and the Purifier build (Leaching Vat on a log, Catch Basin and Residue Crock beside the log) leaches Ash with water into liquid Salt and leaves Caput Mortuum; Villach asks to purify once.
6c. `m9-coagulator-multiblock`: the Coagulator as a build: three Principle Vats (Sulfur, Salt, Mercury from left to right) over the casting table between Stone Bricks, formed with the Journal, the Vats as input ports, items at the table; the recipe GUI stays for now, without the side panel.
6d. `m9-athanor-multiblock`: the Athanor stands on a diamond hearth of 13 Bricks with three non touching Fluid Tanks on its tips it drinks from, the Albedo ring fills the hearth layer out to 5 by 5 and its Salt Crystal Blocks grow only on the Athanor, and is formed with the Journal; it crafts only when formed; the hearth and every shipped tier are journal schematics with Visualize, generated from the hearth layout and the tier data.
7. Balance pass and the docs Balance lab on the new data.
- Later: Citrinitas and Rubedo, Salamanders, Undines and Sylphs, endless works at the end.

**Checkpoint: play it.** Is Nigredo to Albedo fun for a casual player and for a planner? Adjust before Citrinitas.

### M10: Wireless and scale
- Wireless Packing Station (replaces the Warehouse Relay idea)
- Wireless Terminal with range tiers
- Outbound dock: teleport delivery and courier drone

---

## 5. Open decisions

- Citrinitas and Rubedo content, and what Salamanders, Undines and Sylphs do
- Gnome upkeep: offerings or none, and of what
- A converter between principles (surplus Sulfur to Salt and so on), or only the Sink
- Whether too much Elixir of Sight hurts ("the dose makes the poison")
- Alchemist's Spectacles as a permanent Elemental Sight in a later stage
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

# Servus Mod Family: Project Plan

A family of NeoForge mods for Minecraft 26.1.2, built to fit packs like ATM11 but fully standalone. (Built on 1.21.1 up to M11; ported in m0-port-minecraft-26-1-2.)

| Mod | Mod ID | Purpose |
|---|---|---|
| Servus: Foundation | `servus_foundation` | Base of the family: shared APIs, network graph, tick mailbox, the Utility Duct (item, energy, fluid and data lines) with facades, the Fluid Tank, pumps, the Sink, the Ender Anchor, the Coal Generator, Configuration Carrot (the wrench role), goggles, config helpers |
| Servus: Manufacturing | `servus_manufacturing` | Alchemy after Paracelsus: the three principles, gnome grown ore veins, the Athanor and his journal as the tech tree |
| Servus: Storage | `servus_storage` | In-world warehouse storage that scales to shuttles, autocrafting and wireless access |

Manufacturing and Storage depend only on Servus: Foundation and never on each other. When both are installed, extra features light up through the Foundation API. Decided in M8: Servus Lib and Servus: Logistics were merged into Servus: Foundation, so every mod gets the ducts and the data line can live in the Utility Duct.

---

## 1. Tech stack

- Minecraft 26.1.2, NeoForge 26.1.2.x (latest stable), Java 25 (the target pack is ATM11)
- Build: Gradle with ModDevGradle, one multi project repo
- Mappings: none needed; 26.1 ships unobfuscated with Mojang's own names and parameter names
- Data generation for blockstates, models, loot tables, recipes, tags, lang
- NeoForge transfer API: energy `EnergyHandler` (FE), items `ResourceHandler<ItemResource>`, fluids `ResourceHandler<FluidResource>` in mB, all transactional
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

Decided:
- **Two audiences, one path.** Nothing forces a factory, everything rewards one. A casual player does alchemy with a handful of blocks and buckets; a planner pipes the principles, balances the ratios and lets the Crafting CPU order. Both reach the same chapters. No task ever asks for a sustained throughput.
- **Tria Prima.** The Calciner splits matter into Salt, Sulfur and Mercury, three fluids that travel through the Fluid Pipe and wait in Fluid Tanks. Salt leaves the fire as Ash, as in the lore, and the Purifier leaches the Ash with water into liquid Salt. The Coagulator makes products from a fixed ratio of the three. Every input and every product has its own ratio, so surplus of one principle is normal and goes to the Sink or to a later converter. In Nigredo the Calciner takes almost anything (cobble, wood, dirt, ore), so the early game needs no ore source but the pickaxe. Machines with several tanks assign a tank per side in the Fluids tab of the side panel, so each pipe arm and its channel carry one principle.
- **Machines are built, not placed.** The alchemy machines become multiblocks of vanilla blocks and custom parts with fixed ports by position instead of side configuration and GUIs. The player forms one by using Paracelsus' Journal on its controller (never the carrot); the journal shows each build as a schematic with Visualize, generated from the same layout that forming checks, and the formed build is drawn as one model. The Calciner comes first: a compact furnace of Stone Bricks over Deepslate Tiles with an Alembic on top, whose Ash Pan gives Ash while it fills its two Fluid Tanks, one beside the Firebox on each side, with Mercury from the Receiver and Sulfur from the Hood; the Coagulator follows with three Principle Vats over a casting table, and the Athanor stands on a diamond brick hearth and drinks from three Fluid Tanks two blocks out, which never touch. The Purifier is a Leaching Vat on a log, with a Catch Basin for the Salt and a Residue Crock for the Caput Mortuum beside the log, so each output has its own port; the Caput Mortuum is kept for a later diamond. Forming any build plays a sound and sends particles up from every block.
- **No world generation.** Modded packs have ores enough. The deposit worldgen of M2 goes away.
- **Gnomes grow the veins.** A Gnome Hollow is seeded with a sample of an ore, matched by tag (`c:ores/iron`, `c:ores/osmium`, ...), so ores of other mods can be grown too. The vein ripens over time while it is fed principles (mostly Salt, the body of metals). A bound gnome then works the mature vein at a fixed rate forever. In Paracelsus' time miners believed metals grow in the earth like plants; this is that belief made real. Seeds are gated by stage (iron and copper early, gold and diamond late).
- **Elemental Sight.** Paracelsus' elementals live in their element as we live in air, unseen. A Gnome Hollow looks like a plain block. Only under Elemental Sight are the ore buds in the rock around it, the working gnome and the ripeness shown, drawn on the client like the duct overlay; the server keeps only data (vein, ripeness, rate) and never changes a block. Ducts and the data cable reach the Hollow without sight. The sight comes from the **Elixir of Sight**, brewed from the principles.
- **The four elementals** are the workforce: Gnomes (earth: veins), Salamanders (fire), Undines (water), Sylphs (air). Gnomes come first; what the others do is decided with their stages.
- **Stages of the Great Work** replace the pack tiers: Nigredo (black), Albedo (white), Citrinitas (yellow), Rubedo (red).
- **The Athanor** is the heart of the lab, like the Blood Altar of Blood Magic. Important crafts happen on it (the Elixir of Sight, vein seeds, stage items): the item lies on top and the Athanor draws principles over time from the three Fluid Tanks of its build while its flame burns; any tank holds any principle, fed by bucket or by Fluid Pipe. Its tier is the structure built around it, one ring or part per stage (example: Salt Crystal blocks for Albedo, Sulfur pillars for Citrinitas, Mercury basins and the Stone for Rubedo), and its flame takes the stage's colour. The tier of the structure is the stage gate: there is no separate stage item and no stored stage progress.
- **The journal is the tech tree.** A Modonomicon book, Paracelsus' journal, with one chapter per part of his life (Villach, Schwaz, the wandering years, Basel, Salzburg). A chapter lists tasks of two kinds only: do something N times, or make something for the first time. When all tasks of a chapter are done, the chapter text appears and its recipes unlock. No Research Lab and no packs: Paracelsus held experience over books. Chapter progress is per team (FTB Teams if present, otherwise per player); the team, sync and recipe unlock code of the M2 research is reused.
- **Modonomicon** is a hard dependency of Manufacturing. Its NeoForge Maven artifact for the target Minecraft version is verified before the journal design is written.
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
| Calciner (multiblock 3 by 5: Stone Bricks, Deepslate Tiles, mouth, Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood, one Fluid Tank per side) | splits one input into its principles, example 1 item per 4 s | burns furnace fuel in the Firebox |
| Purifier (build: Leaching Vat, Catch Basin, Residue Crock, log) | leaches Ash with water into liquid Salt, example 4 Ash and 400 mB water into 400 mB Salt and 1 Caput Mortuum in 8 s | none |
| Coagulator | makes a product from a principle ratio | none, it takes time to set |
| Athanor (Nigredo tier) | slow altar crafts that draw principles | none, burns principles |
| Coal Generator (Foundation) | 40 FE/t, 1 coal per 20 s | burns coal |

### 2.2 Servus: Foundation

The base mod: the shared API (networks, production and stock capabilities, side config, placement assist, config), the Configuration Carrot, the goggles, the transport content and the general utilities below. Pumps, the Sink, the Ender Anchor and the Coal Generator live here because they are not alchemy and Storage needs them as much: a warehouse voids overflow and runs overnight, and the fluid line gets its own source. Everything in Foundation uses plain crafting recipes.

- **Utility Duct**: one block with several lines (Item Pipe, Power Cable and Fluid Pipe). Each line is placed as its own item and adds to a duct that lacks it.
- Per side and per line modes (in and out, pull in, push out, disabled) and one of eight colored channels per line and side, set in an endpoint GUI that the Configuration Carrot (the wrench role) opens on the arm that touches an inventory or machine. Items, energy and fluids only flow between sides on the same channel. A bare hand opens nothing; sneak carrot removes the facade panel it hits, elsewhere the duct.
- Item Pipe: filters and priorities per side. Power Cable: tiered transfer rate (later). Fluid Pipe: moves any fluid between blocks with `IFluidHandler`, no rate limit of its own (pumps set the throughput), no filters or priorities. Data line: tiers with a bandwidth, simplex or duplex (see the M8 decisions in section 2.4).
- **Facades**: crafted from any full block plus one nugget (any kind, the `c:nuggets` tag). A facade is a one pixel panel on the duct face the player clicks, up to six per duct. A solid facade also closes that face, so no line connects through it; a hollow facade (eight facades around an empty centre) lets the duct pass through its hole.
- Holding the Configuration Carrot or wearing engineer goggles shows hidden ducts as a translucent overlay.
- Rendering: a dynamic baked model composed from part models (a centre and arms per line set) driven by model data from the block entity. A duct grows thicker with its lines, each line has its own color (Item Pipe orange, Power Cable red, Fluid Pipe blue, the data line white, gray or black by tier, the same on the line items), all of them show as bands on a duct with several lines, and an arm that ends at an inventory or machine carries a ring in its channel color. Facade panels are baked from the target block's own face quads.
- The shape follows the lines: thin for one line, thicker for two, thickest for three, arms as thick as the lines that connect there. Lines are removed through the endpoint GUI, not by clicking a part.
- Networks are cached graphs, rebuilt only on placement or removal, never by walking blocks every tick.
- **Fluid Tank**: a glass block holding one fluid. Touching tank blocks that fill a complete rectangular box form one tank with a shared content and a capacity of blocks times `capacity_per_block` from data (example 16,000 mB); a group that is not a full box works as single block tanks until it is. Every block stores its own share and the tank network lays the shares out lowest layer first, so forming, splitting and chunk unloads move no fluid. Joined blocks hide glass and frame toward each other through a multipart model. Buckets, an empty hand read out and `IFluidHandler` on every face; a broken block keeps its share in its item.
- **Pumps** are the first fluid source: a Burner Pump (furnace fuel) and an Electric Pump (FE, faster) stand on an infinite water source, a water source block with at least two water source neighbours, and fill an internal water buffer without ever using the water up. Both are beam engines two blocks tall, placed and broken as one, and their sides are fixed: water leaves, and fuel or FE comes in, only through the back and the two sides of the lower half, into the Fluid Pipe or a Fluid Tank. The Burner Pump has firebox and boiler below and chimney, cylinder and a rocking beam above, and smokes while it burns; the Electric Pump is its electric sibling with a motor housing with copper windings and a terminal box below and a crank wheel on an axle held by a bearing on the cap, driving the beam through a con rod, and a fan grille above; a drive belt on the back runs from the motor pulley up to the crank axle, with sparks now and then. Both splash at the well on each stroke. Both are crafted at a crafting table. Rates and buffer size are datapack numbers (examples: 250 mB and 500 mB per second, 4,000 mB buffer).
- **Sink and Chimney** get rid of surplus: the Sink is a 2x2x2 multiblock of eight brick Sink blocks, formed by clicking one side of the cube with the Configuration Carrot. That side becomes the front and shows one big fan behind a grate; breaking any block unforms it. The formed Sink voids items, fluids and FE through the standard capabilities on every outer face except the front, with no side configuration, and from a one slot trash GUI or a bucket by hand. Brick Chimney blocks stacked on any of its four top corners raise its void rate per height (the tallest column counts), and from a configured height on (example 5 blocks) it voids without limit. While it voids, the fan spins and smoke rises from the top of the tallest column. Rate tables per height and the active time are datapack numbers (examples: 4 to 64 items per second, 50 to 800 mB and 100 to 1,600 FE per tick).
- **Ender Anchor**: keeps production and warehouses running overnight, with FE upkeep and a config cap per player or team. Respects FTB Chunks if present. Crafted from an ender pearl, iron, copper, redstone and obsidian. Its area is a square of chunks: its own chunk alone, 3 by 3 with one Anchor Upgrade, 5 by 5 with two (2 slots), shifted with four arrows in its GUI as long as its own chunk stays inside; upgrades taken out shrink the offset. It pays its `energy_per_tick` for every chunk of the square. The team cap counts chunks, not anchors: `maxLoadedChunksPerTeam` (default 49) holds the union of the chunks of the team's active anchors and woken warehouse sites, overlaps once; an anchor or a wake that would go over is refused as a whole, and an anchor that changes its area is checked without its old area. The GUI shows the area size, the team load ("14 / 49 chunks") and a "Show range" toggle, client only and off by default, that outlines the square's chunk borders in purple with a column at each corner. The **Anchor Upgrade** is a Foundation item (an Ender Anchor in the middle with ender pearls, iron and redstone) that also lets a Storage warehouse site wake. It has obsidian panels in the dark steel frame, a chunk grid on the sides and an ender eye on the front that shows whether it holds its chunk. The eye is shut while it does not, opens when it starts, blinks now and then and follows the nearest player in front of it; purple portal particles drift around it while active.
- **Coal Generator**: the family's FE source so no mod needs another mod for power (example 40 FE/t, 1 coal per 20 s). A dark steel cube with copper coils behind vents and a fire window that glows while it burns, and a square chimney centred on top that smokes, so it is two blocks tall, placed and broken as one; FE and fuel pass through the cube's faces, never its top or the chimney.

### 2.3 Servus: Storage

**Core idea:** the player builds a real warehouse. In-world storage, no storage disks.

Progression:
1. **Heavy Duty Racks, rails and a shuttle** (early, no power). The first warehouse is a Heavy Duty Rack, a Warehouse Rail, a Shuttle Garage and a Mk1 shuttle; nothing cheaper comes before it (decided in `m11-drop-crates-pipe-access`). The Heavy Duty Rack is the chest sized bay for mixed items: bays with the same facing and the same wood join side by side into sections of a few bays, and a section shares one GUI that shows the slots of every bay (each bay still stores its own part). Stacked racks stay separate. A shuttle serves a whole section from a rail in front of any of its bays; rails along a rack's side or back never reach it. The front shows the item types the bay holds the most of. A rack is a chest for hoppers and pipes only while no rail docks to its section; once a rail docks, the section is served by shuttles only, and players still open it by hand.
2. **Warehouse Controller + Storage Terminal.** Search and request items at the terminal and hand items in through its input row; deliveries land in its output row. Only the warehouse's own racks are stock: inventories touching a member (chests, other mods' storage, Servus blocks) are never indexed.
3. **Shuttles on rails.** Requests and put-away are done by shuttles that travel along rails to a inbound station. Throughput depends on shuttle count, rails and layout. Put-away rules ("all ores to aisle 3"): a Zone Label carries a name and an item filter and is stamped onto racks; stamped racks take matching items first, unzoned racks take everything and act as the overflow.
4. **Data cable and on-demand autocrafting.** The warehouse joins the data cable through Packing Stations, and the Storage Terminal, Crafting CPU and Pattern Storage live on the cable (see 2.4).
5. **Field work.** Shuttles with modules leave the warehouse on Farm Rails and tend fields: they till, plant and harvest, and the harvest comes home through put-away.
6. **Wireless.** Every Warehouse Controller is a site and, with a Wireless Upgrade and FE in its buffer, the site's antenna: wireless capable controllers of one team link their sites over a wireless line (FE per item and distance, paid by the receiving controller), normal Packing Stations pack and unpack for it, and a terminal requests stock of another site, which a Packing Station of this site passes on into that terminal's output slots (built in M11). Later: Wireless Terminal with range tiers, delivery through an outbound dock (teleport or courier drone to a landing pad).

**Power (decided in `m11-warehouse-power`):** the warehouse runs on FE through its controller, one power connection per site: shuttles pay per block driven, Crafting Stations per working tick, wakes per chunk while awake and wireless transfers per item and distance, all from the controller's buffer; nothing costs FE while idle. Without power each consumer stops where it is and resumes by itself; the stock stays visible and racks open by hand. Manufacturing machines keep their own fuel and power.

**Rails or cable (decided rule):** only storage touches rails, so shuttles can reach it: racks, Shuttle Garages, Rail Elevators, Inbound and Outbound Stations and Packing Stations. Everything else sits on the data cable: terminals, Crafting CPU, Pattern Storage, Crafting Stations, machines and Machine Adapters. The Warehouse Controller sits on the rails and is connected to the cable.

**Network topology (decided direction):**
- Early: the controller links to touching rails (and to a touching controller, which makes the network offline). Blocks that shuttles serve never join by touching, because shuttles are the only way in and out: a rack section joins only through a rail in front of one of its bays (m6-rack-rail-only), Inbound and Outbound Stations and Packing Stations only through a rail on any of their sides, and a Shuttle Garage only through the rail at its back (m6-stations-rail-only). Settled in `m11-drop-crates-pipe-access`: the first storage blocks and the indexing of foreign inventories are gone, and the controller keeps linking to touching rails.
- Mid: **rails are the wiring.** A rack section joins the network through a rail in front of any of its bays. The same rails carry shuttles later. Decided: the Warehouse Rail is a flat rail, one block wide, that auto connects to neighbouring rails on the same level like ducts do. It stands on a floor or hangs without one on a rack front, an elevator segment or beside a standing rail; free rails between two hung rails bridge a gap of up to `rail_max_span` blocks (3 by default, in `warehouse.json`), and a gap that is wider or held on one side only collapses and drops its rails. No slopes, no ceiling rails. Rails hold nothing and never pull plain inventories in. Floors are linked by the Rail Elevator: a shaft of stacked segments, each taking rails on its sides, that shuttles will ride vertically.
- Beyond the rails: the **data line** of the Servus: Foundation Utility Duct links the warehouse, through its Packing Stations, to terminals, the Crafting CPU and machines far away.
- Late: the **controller with a Wireless Upgrade** links remote sites without a cable; the Packing Stations on its rails send and receive (it takes the place of the Warehouse Relay idea; the Wireless Packing Station of `m11-wireless-packing-station` was replaced by `m11-controller-wireless`).

Shuttles are **simulated as data** on the warehouse network and only rendered moving on the client. No real entities. Decided: Shuttle items go into Shuttle Garages, a few per garage, and park on the rail touching their garage; more garages mean more shuttles. Every pick and every put-away runs through Heavy Duty Racks and shuttles, so every warehouse needs a garage and a shuttle; without one a request is refused with "needs a shuttle" and a put-away waits in its station. Players request items at the Storage Terminal with an amount and collect the delivery from its output slots.

Decided: every elevator shaft has one simulated **lift car**. A shuttle that changes level stops on the rail beside the shaft, calls the car and waits; the car serves calls in the order they were made and carries one shuttle per trip at `lift_speed` from `warehouse.json`, then stays parked where it stopped. A shuttle that only crosses a shaft on one floor does not call the car; it drives over the floor's deck, two flaps hinged between the corner posts that fold up while the car is at or passing that floor, and the car waits for it. A floor without a rail beside it shows only its posts, and the top segment carries a head with the motor beam. Segments work like scaffolding: players walk into them from the side and climb inside, stand on every level from above and sneak to sink down, and a click with an elevator on any segment places the next one on top of the shaft. Breaking a segment under a riding shuttle drops its cargo at its garage.

Decided (field work): everything stays in Storage.
- **Shuttle tiers:** the shuttle comes as Shuttle Mk1, Mk2 and Mk3, three items with their own base model. The tier sets how many stacks, and so how many jobs that share a way, a shuttle carries per trip (examples 2, 4 and 8) and the max count of each count upgrade. Mk1 is crafted; Mk2 and Mk3 are built from the tier below in the Shuttle Workshop, never in a crafting grid.
- **Shuttle Workshop:** the workbench for shuttles. One install slot takes every module: count upgrades (Speed, Handling) go up by one per item up to the tier's max and come back out one at a time; a tool (Harvester, later Lumber) replaces the fitted tool, which comes back. Modules and counts are a data component on the shuttle item, so the fleet tracks every shuttle stack in its garages and matches a job to a shuttle with the modules it needs. A tier build takes supplies from a datapack build sheet and then finishes either at once or by hand in a short minigame on the shuttle model that cannot fail; only a build by hand engraves a name plate, so only Mk2 and Mk3 shuttles carry a name, shown on the shuttle in the world and wherever the mod names it. The workshop keeps the shuttle, the supplies and the build progress.
- **Speed and Handling:** Speed works between stops (travel speed), Handling at stops (loading, unloading and field work per plot). Base values and the effect per count are datapack numbers; the base shuttle is slower than before so Speed matters.
- **Looks:** a shuttle shows what it is: the tier's body and hold, Speed parts, a Handling part, and its tool on the front and sides. Shuttles are drawn bigger than the first carts. The name plate shows only in the item name, never on the model.
- **Farm Rail:** a Warehouse Rail variant that joins the warehouse rail graph like any rail. One connected run of farm rails is one field. It is not waterlogged; while its field has water it hydrates farmland 4 blocks to each side through NeoForge `FarmlandWaterManager` tickets (not saved, re-added on load).
- **Field Head:** one block per field. Water comes in through the fluid pipe into a buffer; a GUI shows the field and sets one crop for the field or a harvest only mode. The head learns which plots are ripe from `CropGrowEvent.Post` and `BonemealEvent`, plus one pass over the field when it loads, never by scanning per tick. It sends a harvest run once a share of the field is ripe; the share is set per head in its GUI (default from JSON) and a bar shows how close the field is. A field covers only the soil its rails see in straight lines at 90 degrees, plus the corner squares at run ends and outer bends along an L; a pipe, a path, a wall or water ends its line, nothing is seen around it.
- **Field range:** how far a field reaches to each side of its rails is the Field Head's radius: 2 blocks at the start, raised to 3 and 4 by upgrades in the head. Hydration, the ripe share and the plots the rails see all use it.
- **Harvester shuttles** serve fields only, never warehouse jobs. A run tills dirt and grass inside the field's strip at farmland height, plants, harvests until the cargo is full, then puts the harvest away and comes back. Replanting takes seeds from the harvest first and picks them from the warehouse only when short. One harvester works a field at a time. A harvester is too wide for the lift car: it crosses a shaft on its own floor but never rides up or down, so its field, its garage and the storage it fills share a floor.
- **Spreader shuttles** fertilize fields: a second field only, wide tool module. The fleet is shared per warehouse, any Spreader serves any field. Each Field Head chooses a fertilizer from the tag `servus_storage:fertilizer` (bone meal by default, each item one bone meal application), a mode (Off, After planting, Until ripe share with its own threshold) and a Keep in stock reserve; the field calls a Spreader at the warehouse's field check, at most every `fertilize_interval`, and the Spreader picks only stock above the reserve, doses unripe crops rail by rail and puts the rest away. The reserve is per field, so a field with reserve 0 may empty the stock (accepted over a warehouse wide reserve).
- Trees come later as their own change with a Lumber module.

### 2.4 Integration: Storage + Manufacturing (autocrafting)

**USP: production on demand.** Nothing runs without an order. A classic pipe line (Outbound Station, pipe, chest, hopper, furnace, hopper, Inbound Station) produces until storage is full or the input runs out; a machine on the data cable produces exactly what was ordered and then waits.

**One route for orders.** Everything on demand runs over the data cable; everything that runs all the time uses stations and pipes. There is no second order route through Outbound Stations.

- **Foundation production API** (production and stock capabilities, `OrderPlanner`): the data cable finds its members through it and the CPU plans with it. Work orders, bill of materials explosion and reservations live on the Crafting CPU.
- **Min/max stock levels** per item ("keep 500 to 1,000 iron ingots") live on the controller; a rule hands its order to the CPU on the controller's data network.
- **Stations:** the Inbound Station is the put-away entry of the warehouse; the Outbound Station is a plain buffer with keep slots for pipe lines that run all the time. Station fronts, like machine fronts, never move items.
- **Data cable** (Servus: Foundation): the Data line of the Utility Duct. Like redstone, not like a real network: place it and it works, no addresses, channels or priorities. It never pushes or pulls on its own; it only routes packets for an order. Members are found by the capabilities they expose. The line itself is instant: items do not travel and distance does not matter. Throughput is set where items cross between the warehouse and the line, at the Packing Station (decided in `m8-packing-station`), and by the bandwidth of the cable (decided in `m8-data-bandwidth`): the data line comes in tiers, Data Cable (32 items per second shared by both directions, simplex), Duplex Data Cable (32 each way) and Fast Data Cable (128 each way), with stats in datapack JSON. A delivery moves no faster than the slowest duct on its path; deliveries that cross a duct share its budget in creation order, and a delivery takes the shortest path with the most bandwidth, no max flow. A higher tier cable used on a duct replaces the line in place and drops the old cable. Load shows in the duct summary, with the goggles (duct colours) and in the Production Terminal's Machines tab. Item pipes stay useful: they move items all the time and work with any mod.
- **Packing Station** (Storage). Sits on a rail and on the cable and converts between the physical warehouse and the digital line: shuttles bring items into its To network buffer and it packs them into packets, and arriving packets are unpacked into its To storage buffer and handed to shuttles for put-away. It converts `items_per_second` (8 by default, datapack JSON) in each direction, the two directions independent; the cable's bandwidth is the other limit. Items wait at their source until it can take them (the terminal's Input row shrinks as it converts). Its GUI shows both buffers read-only with a status line. A warehouse may have several; each adds its own rate. The controller picks the Packing Station with the shortest shuttle trip; on a tie any of them. It has upgrade slots (`upgrade_slots`, 2 by default) for Speed Upgrades, each multiplying its rate (x2 by default).
- **Upgrades** (decided in `m8-data-bandwidth`): Foundation owns upgrade items and the upgrade slot API, so every Servus mod can use them without depending on each other. The Speed Upgrade is crafted from vanilla materials; its multiplier comes from `upgrades.json`. Manufacturing may add better upgrade items later through the same API (an `UpgradeItem` or the `servus_foundation:upgrades` tag plus a JSON entry); Foundation never depends on Manufacturing.
- **Warehouse Controller** on the cable answers stock questions and assigns shuttles. It moves no items itself.
- **Storage Terminal** on the cable: the player requests items, shuttles bring them to a Packing Station, packets deliver them into the terminal's output slots. Its crafting grid crafts from what the player puts in; it is not refilled from the warehouse.
- **Packet inlet** (decided in `m11-drop-crates-pipe-access`): the data line delivers into and collects from a member through its packet inlet first (`ServusFoundationCapabilities.PACKET_INLET`), then its item capability (other mods' machines, the Machine Adapter, Manufacturing machines). Pipes and hoppers never see the inlet, so the Packing Station, Storage Terminal, Crafting Station and Field Head take packets but show no pipe arm. Inbound and Outbound Stations keep their item capability, since pipes belong there.
- **Crafting CPU** (Storage, a multiblock box on the cable) owns work orders; this moves order handling from the controller to the CPU. Every CPU adds order slots, more CPUs run more orders in parallel. Decided: a new order goes to the CPU with a free slot and the lowest position, else to the one with the fewest open orders; orders stay on their CPU.
- **The CPU is a box you design** (decided 2026-10-12, `m8-cpu-buffer`): any solid box without gaps, every side at least 2 blocks (smallest 2x2x2), of Crafting CPU blocks and CPU Buffer blocks, with at least one CPU block. Each CPU block adds order slots, each Buffer block adds buffer room in stacks (`orders_per_cpu_block`, `stacks_per_buffer_block` in `cpu.json`). The Configuration Carrot forms it and says why when it does not (gap with its position, not a box, too small, no CPU block, too big).
- **Intermediates wait in the CPU buffer**, not in storage: outputs a later step of the same order needs go into the buffer when that step runs on the same producer or its producer is busy, and back to the producer as packets when the step runs. Only a full buffer sends them to storage, and the Production Terminal notes it. The buffer takes packets through the Servus packet inlet only, so item pipes and hoppers never use the CPU. Breaking the box sends what waits to a Packing Station. Balance of slots, room and recipes follows in the M9 balance pass.
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
4. **Event driven, not polled.** Own blocks push change events. A warehouse indexes only its own racks, polled slowly and in batches; it never polls foreign inventories.
5. **Data driven content.** Recipes, principle ratios, vein seeds, journal tasks, Athanor structures and balance numbers come from datapack JSON with codecs.
6. **Capabilities as the integration surface.** Machines and storage expose standard capabilities, so any mod's pipes and cables work. Only blocks meant for pipes register the item capability; blocks that take data line packets but no pipes register the packet inlet instead.
7. **Pure logic separated from Minecraft.** Graph algorithms, BOM planning, ratio math, order scheduling live in plain Java classes with no Minecraft types, unit tested with JUnit.
8. **Persistence:** network and journal progress in `SavedData`, block state in block entities using data components and codecs.

### Servus: Foundation API

- `ServusNetwork` base: graph of nodes, mailbox, tick hook, split and merge handling
- `NetworkManager`: owns all networks per level, routes block events to the right network
- `ProductionProvider`: "this block can produce X from inputs Y at rate Z"
- `StockProvider` / `StockSink`: "this inventory can supply or accept items"
- `PACKET_INLET`: the door through which data line packets enter a block, an item handler under its own capability id that pipes and hoppers never see; `PacketInlets.handler` looks it up before the item capability
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

### Done
What every finished milestone built lives in `openspec/specs/`; the archived changes under `openspec/changes/archive/` hold the reasons.

| Milestone | Built |
|---|---|
| M0 Repository and build | multi project Gradle with ModDevGradle, run configs, CI |
| M1 Foundations | network graph with split and merge, mailbox tick, `NetworkManager`, Configuration Carrot, config |
| M2, M3 Factorio style Manufacturing | superseded by M9; what lived on: pumps, Sink, Ender Anchor, side config, JEI, the docs site and Balance lab |
| M4 Storage first slice | Heavy Duty Rack with sections, Warehouse Controller, terminal (its first storage blocks and the indexing of foreign inventories went in M11) |
| M5 Logistics | Utility Duct with item, power and fluid lines, modes, channels, filters, priorities, facades, goggles, Fluid Tank |
| M6 Storage automation | rails, elevators with lift cars, simulated shuttles, garages, Inbound Station, Zone Labels, placement assist |
| M7 Autocrafting integration | production API, work orders, stock rules, Outbound Station, Crafting Station |
| M8 On-demand autocrafting | Servus: Foundation merge, data cable with tiers, Packing Station, Storage Terminal, Crafting CPU, Production Terminal, Pattern Storage, Machine Adapter |
| M9 Alchemy | Tria Prima, Calciner, Purifier, Coagulator and Athanor builds, journal, Elixir of Sight, Gnome Hollow, facade panels, beam engine Burner Pump, electric beam engine Electric Pump, Ender Anchor area with Anchor Upgrades and a chunk cap (`m9-anchor-range`) |

### M9: what is left
- Balance pass and the docs Balance lab on the new data.
- Later: Citrinitas and Rubedo, Salamanders, Undines and Sylphs, endless works at the end.

**Checkpoint: play it.** Is Nigredo to Albedo fun for a casual player and for a planner? Adjust before Citrinitas.

### M10: Field work
One change per item, in this order. Decided on 2026-10-11 that M10 may start before the M9 balance pass. Built: `m10-shuttle-workshop` (workshop, modules as a data component, fleet matching by module), `m10-farm-rail-field-head` (Farm Rail, fields, Field Head with water, crop choice, ripeness from events) and `m10-harvester` (field jobs: till, plant, harvest, put-away, seeds). Next, decided on 2026-10-11:
1. `m10-shuttle-tiers`: Shuttle Mk1, Mk2 and Mk3 with stacks and max counts per tier, Speed and Handling count upgrades, one install slot in the Shuttle Workshop, tier builds from datapack build sheets that finish at once, a slower base shuttle. Removes the Cargo Hold, `cargo_hold_stacks` and `harvester_stacks`. Built: Mk1 keeps the item id `shuttle`, Speed reuses the Foundation Speed Upgrade item (its block multiplier does not apply to shuttles), stops take a stop time plus a time per stack shortened by Handling, all numbers and build sheets in `storage/shuttle.json`, and the shuttle sync carries each shuttle's item for the looks change.
2. `m10-shuttle-looks`: shuttles drawn from parts (tier body, Speed parts, Handling part, tool) on the rails and as items, bigger than before.
3. `m10-shuttle-build-minigame`: the tier build by hand on the shuttle model, and the name plate it engraves. Built: three step kinds (bolts, wires in the eight duct colours, weld) from `steps` per tier in `storage/shuttle.json`, played on the client in a work panel beside the workshop while the server only takes the next step; the shuttle and supplies are locked during the build, Cancel uses nothing, progress is saved; the plate is its own data component of at most 20 characters, shown in the item name (not on the model, decided in the play check), and the Field Head names the shuttle working its field.
4. `m10-field-range`: the field radius from the Field Head (2 at the start, upgrades to 3 and 4), and harvesters that never ride a lift car. Built: `radius` and `max_radius` in `storage/field.json`, a Storage Range Upgrade in its own Field Head slot (not the Foundation upgrade API, which is about speed multipliers), tools carry a wide trait and the Harvester is wide, so its ways leave out the steps up and down shafts; its put-away and seed fetching skip storage behind a lift, and a field no harvester reaches without one shows its own reason and gets no run.
5. `m10-lift-upgrades` (play check follow up): Speed Upgrade slots per elevator shaft, opened with an empty hand from any segment and kept in the lowest one; each upgrade raises the car's top speed and acceleration (`warehouse.json`). Built: a segment placed under the shaft takes the upgrades over, a bottom replaced by a solid block hands them up, and a broken bottom (the shaft above falls like scaffolding) drops them where it was.
6. `m10-field-head-look`: the Field Head in the station family: wood from the recipe, a facing, a glass window with the water buffer drawn as real water on every side, the crop on a plate on top, and a ripe share bar on the front (gold steps toward the harvest threshold, green when a harvest is due).
7. `m10-rail-look`: both rails as wide gauge track: two raised rail heads 2 pixels wide at 3..5 and 11..13 so the shuttles' wheels stand on them, four separate sleepers per block, 14 pixels wide, in the wood of the recipe (rails become wood blocks). Steel heads on the Warehouse Rail, copper heads with a touch of verdigris on the Farm Rail, whose recipe now takes copper ingots and planks. Curves, crossings, docks, the elevator deck and car rail and the item follow; shuttles are drawn 2 pixels higher. Each rail type uses one head texture so `m10-rail-corrosion` can swap it per block later.
8. `m10-rail-corrosion`: the copper of a field's farm rails ages in five stages (copper, exposed, weathered, oxidized, corroded), one stage per field kept on every rail, driven by the water the Field Head uses (`corrosion_water` in `storage/field.json`). One item of the tag `servus_storage:anti_corrosion` (honeycomb) per aging step holds the stage; the head's slot for it fills by hand, by hoppers and ducts and as upkeep from the data line. A corroded field is no longer watered and shows a rust rim on its head; runs go on. An axe scrapes the whole field back one stage for one durability per rail. Crafted rails are fresh, placed rails take their field's stage, and joined fields take the stage of the head that serves them or the younger one. Built: Foundation's `WoodModel` swaps further textures named in model data, which draws the stage heads.
9. `m10-field-head-tabs`: the Field Head GUI in three tabs like the Utility Duct endpoint GUI: Field (crop, harvest only, ripe share, threshold, work status), Water (water, anti-corrosion slot, copper stage) and Upgrades (Range Upgrade, radius, field size), the inventory always below, shift click into hidden slots, a red marker on a tab with a problem, back to the compact panel.
10. `m10-crop-picker`: the Field tab's crop slot and harvest only toggle become a crop picker: a button with the current crop that opens a searchable list of every item that plants a crop (from the item registry, so mods' crops appear), sorted by name, with "None, harvest only" first. Each entry shows the warehouse stock, synced while the GUI is open; entries without stock stay selectable but greyed with a hint that the first planting needs seeds in the warehouse. The choice travels as an item id, no item needed in the inventory. Built as a reusable Storage client widget (`ItemPicker`) for the Fertilizer tab of `m10-spreader`.
11. `m10-spreader`: the Spreader tool module and the fertilize run: the Field Head decides at the field check (never per tick) whether its field needs doses by its mode (After planting from the plots planted since their last dose, Until ripe share below its own threshold), calls a Spreader at most every `fertilize_interval`, and the Spreader picks min(plots x `doses_per_plot`, stock above the head's reserve, cargo room) before it drives out, doses with bone meal semantics, never doses ripe plots and puts the leftover away. A Fertilizer tab between Water and Upgrades with the `ItemPicker`, mode buttons, threshold, reserve and a status line with a red marker for problems. Built: the field runs share a `FieldRun` base and the put-away, the head saves the plots still owed a dose, numbers in `storage/field.json`.
12. `m10-workshop-look`: the Shuttle Workshop joins the wood block family: the dark iron frame over boards in the wood of its recipe, on all four sides a wrench and a yellow-handled screwdriver hanging on the boards, a steel bench plate with an engraved shuttle outline and a vise on top. No facing, no lamp and no teal, because it is not on the warehouse network. The sneaking carrot dismantles it into one item that keeps its wood, shuttle and supplies; a hand build in progress is cancelled.
- Later: `m10-lumber` with a Lumber module for tree fields.

### M11: Wireless and scale
Decided on 2026-10-11: every Warehouse Controller is a **site** (an outpost or the base). Sites trade over wireless only at first; no shuttles travel between sites yet. One change per item, in this order:
1. `m11-controller-site`: the controller gets a name and its first GUI. It keeps the number of chunks its network spans, counted from member join and leave events (never by scanning), and shows them as a small chunk map. When the span is larger than its Anchor Upgrades cover, the controller shows an error state in the GUI and on the block. Built: the upgrade item (then the Storage Chunk Loader Upgrade, since `m9-anchor-range` the Foundation Anchor Upgrade) and the controller's upgrade slots came with this change (`anchor_upgrade_slots`, `chunks_per_upgrade` in `storage/warehouse.json`), so the error state is real before the wake exists; the span counts members and indexed foreign inventories, keeps chunks that unloaded and shrinks only while the whole site is loaded; the error lamp is the station activity lamp on the controller (red), and a controller without upgrades shows no error; right click opens the GUI instead of printing the summary, which became the tooltip of the online line.
2. `m11-chunk-loader-upgrade`: an Anchor Upgrade (first called Chunk Loader Upgrade) in the controller lets a sleeping site wake. A wake loads the whole site (every chunk with a member) for as long as a transfer needs it, plus a short grace time, then releases it. Capacity per upgrade from JSON (example 9 chunks). Hard limit: a site over its capacity never wakes. A wake counts against the team's Ender Anchor cap and respects FTB Chunks. Without the upgrade a sleeping site only shows its last known stock. Built: a site registry (SavedData) keeps every site's name, team, chunks, upgrades and last known stock, reported only while the whole site is loaded; wakes use Storage chunk tickets owned by the controller position, count the chunks of the span in the team's load of the Foundation Ender Anchor chunk cap (one slot of an anchor count before `m9-anchor-range`), stay while the site's own shuttle work runs and `wake_grace_seconds` (30) more, cost FE per chunk while awake since `m11-warehouse-power`, and end with a restart; a request wakes the site even while a player keeps it loaded; the lamp turns orange while awake; `/servus_warehouse wake <pos>` and `sites` for testing.
3. `m11-wireless-packing-station`: the first link between sites. Sites of one team see each other by name; a request at one site can be served from another, instantly, for FE per item and distance. Sleeping sites show their last known stock greyed out and wake when they have the upgrade. Built: the Wireless Packing Station is a Packing Station whose far side is the team's wireless line (same dimension) instead of the data line; FE buffer from the power line, the receiving station pays `fe_per_item` plus `fe_per_item_per_block` times the distance, rate and buffer in `storage/wireless_packing_station.json`; the terminal title cycles through the team's other sites with a Wireless Packing Station, a request there is a transfer that arrives in this site's storage (not the output slots) and holds a wakeable site awake until it ends; the controller GUI lists the team's sites at its side. Replaced by `m11-controller-wireless` after the play test: the Wireless Packing Station is gone, a Wireless Upgrade in the controller's own slot plus FE in the controller's buffer (`controller_energy_capacity`, costs in `storage/power.json`) make the controller wireless capable, every Packing Station on the rails sends and receives with or without a data line, the receiving controller pays per item and distance between the two controllers, and other sites show in the terminal and the controller panel only while this site's controller is wireless capable. After its play check a transfer requested at a terminal is no longer put away: the receiving Packing Station on the terminal's data line passes it on into the terminal's output slots.
4. `m11-drop-crates-pipe-access`: what counts as an inventory. The first storage blocks go, the warehouse indexes only its own racks and serves only by shuttle (no instant picks or put-aways, the controller's stock provider counts but gives nothing), the terminal grid is no longer refilled from the warehouse. Only blocks meant for pipes register the item capability: Inbound and Outbound Stations on every side that is not Off, a Heavy Duty Rack only while no rail docks to its section; the Packing Station, Storage Terminal, Crafting Station (now Push, Pull or Off sides) and Field Head take data line packets through the new Foundation packet inlet.
5. `m11-warehouse-power`: every warehouse consumer draws FE from the controller's buffer, the site's only power connection: shuttles per block driven (a lift ride per block of height), Crafting Stations per working tick, wakes per chunk and tick, wireless per item and distance; no idle upkeep. A controller with an empty buffer or a refused draw in the last second is in the no power state (red lamp, GUI and terminal line); each consumer stops only on its own refused draw and resumes by itself; a sleeping site wakes only with one second of wake cost in its record. The controller GUI shows the FE used per second.
- Later: SEND and KEEP rules between sites in the controller GUI (the local min/max stock rules join the same list), an In transit view, then explore shuttle portals between sites, overland cargo carts and a separate Servus: Transit mod for passenger travel.
- Wireless Terminal with range tiers
- Outbound dock: teleport delivery and courier drone

---

## 5. Open decisions

- Citrinitas and Rubedo content, and what Salamanders, Undines and Sylphs do
- Gnome upkeep: offerings or none, and of what
- A converter between principles (surplus Sulfur to Salt and so on), or only the Sink
- Whether too much Elixir of Sight hurts ("the dose makes the poison")
- Alchemist's Spectacles as a permanent Elemental Sight in a later stage
- Field kinds beyond crops (trees first, then others) and which modules they need
- Art style and texture direction (shared look across the family)
- Final name for the storage mod (Servus: Storage vs Servus: Warehouse)

---

## 6. Working agreement for the coding sessions

- Work milestone by milestone. Do not start the next milestone before the current one builds, runs and is tested.
- Propose a short plan before larger changes and wait for confirmation.
- Prefer small commits with clear messages.
- Pure logic gets JUnit tests. Block behavior gets GameTests where practical.
- When unsure about a NeoForge 26.1 API, check the official NeoForge docs or existing source instead of guessing.
- Keep balance numbers in data files, not in code.

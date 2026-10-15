# Servus Mod Family: Project Plan

A family of NeoForge mods for Minecraft 26.1.2, built to fit packs like ATM11 but fully standalone. This file holds the vision, the rules and above all what we still want to build. What is built lives in `openspec/specs/`, the reasons in `openspec/changes/archive/`.

| Mod | Mod ID | Purpose |
|---|---|---|
| Servus: Foundation | `servus_foundation` | Base of the family: shared APIs, network graph, tick mailbox, the Utility Duct (item, energy, fluid and data lines) with facades, the Fluid Tank, pumps, the Sink, the Ender Anchor, the Coal Generator, Configuration Carrot (the wrench role), goggles, config helpers |
| Servus: Alchemy | `servus_alchemy` | Alchemy after Paracelsus: the three principles, gnome grown ore veins, the Athanor and his journal as the tech tree |
| Servus: Shuttles | `servus_shuttles` | An automated warehouse run by shuttles on rails: storage, autocrafting, field farming and outposts |

Alchemy and Shuttles depend only on Servus: Foundation and never on each other. When both are installed, extra features light up through the Foundation API.

---

## 1. Tech stack

- Minecraft 26.1.2, NeoForge 26.1.2.x (latest stable), Java 25 (the target pack is ATM11)
- Build: Gradle with ModDevGradle, one multi project repo
- Mappings: none needed; 26.1 ships unobfuscated with Mojang's own names and parameter names
- Data generation for blockstates, models, loot tables, recipes, tags, lang
- NeoForge transfer API: energy `EnergyHandler` (FE), items `ResourceHandler<ItemResource>`, fluids `ResourceHandler<FluidResource>` in mB, all transactional
- Optional integrations (soft dependencies only): JEI and/or EMI, FTB Teams, FTB Chunks; Modonomicon is a hard dependency of Alchemy
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
  alchemy/                  servus_alchemy
  shuttles/                 servus_shuttles
    gradle.properties       mod id and name
    build.gradle            dependencies and loaded mods
    src/main/java           mod code
    src/main/templates      neoforge.mods.toml template
    src/generated/resources datagen output
    src/test/java           JUnit tests
    runs/                   game directories per run config, gitignored
```

Repository: `github.com/Dirnei/servus_mc_mods`. Group id `io.github.dirnei.mc.servus`. Base package `io.github.dirnei.mc.servus`, with one subpackage per mod: `foundation`, `alchemy`, `shuttles`. Inside Foundation the API packages sit directly under `foundation` and the duct, tank and facade content under `foundation.logistics`. License: MIT.

---

## 2. Vision per mod

### 2.1 Servus: Alchemy

**Core idea:** alchemy after Paracelsus. Matter is split into the three principles, Salt, Sulfur and Mercury, and products are made again from them. Paracelsus' elemental spirits do the work, and gnomes grow ore veins instead of the world generating them. The story is Paracelsus' own life, told by his journal, which is also the tech tree.

- **Two audiences, one path.** Nothing forces a factory, everything rewards one. A casual player does alchemy with a handful of blocks and buckets; a planner pipes the principles, balances the ratios and lets the Crafting CPU order. Both reach the same chapters. No task ever asks for a sustained throughput.
- **Tria Prima.** Every input and every product has its own ratio, so surplus of one principle is normal and goes to the Sink or to a later converter. In Nigredo the Calciner takes almost anything, so the early game needs no ore source but the pickaxe.
- **Machines are built, not placed.** Alchemy machines are multiblocks of vanilla blocks and custom parts with fixed ports by position, formed with Paracelsus' Journal, drawn as one model, with no side configuration and no GUI.
- **No world generation.** Gnomes grow the veins: a Gnome Hollow seeded with an ore by tag ripens while it is fed principles, and a bound gnome works it forever. Seeds are gated by stage.
- **The elementals** are the workforce: Gnomes (earth: veins) and Salamanders (fire), with the homunculus as the late servant. They live unseen in their element and show only under Elemental Sight.
- **Stages of the Great Work** replace pack tiers: Nigredo, Albedo, Citrinitas, Rubedo. The Athanor's structure is the stage gate, one ring or part per stage, and its flame takes the stage's colour.
- **The journal is the tech tree:** one chapter per part of Paracelsus' life, tasks of two kinds only (do something N times, make something for the first time), progress per team.
- **FE** stays the energy standard. Alchemy runs on fire and time.
- Machines have **fixed speeds and clean ratios** so players can plan builds, and they join the data cable so the Crafting CPU orders "64 iron ingots" without caring that it is alchemy.
- Principle ratios, recipes, vein seeds, ripening, journal tasks and Athanor structures are **datapack driven**.

Prior art to stay apart from: Theurgy also works with Sulfur, Salt and Mercury, on single items in small setups. Servus stays factory scale, with grown veins, the elementals as workforce, Paracelsus' life as the story and production on demand through the Crafting CPU.

**Story outline of the journal**

| Stage | Chapter | Story | Unlocks (examples) |
|---|---|---|---|
| Nigredo | Villach | his father, metals and the first fire | Calciner, Coagulator, Athanor |
| Albedo | Schwaz | the silver mines, the miners' sickness, the gnomes | Elixir of Sight, Gnome Hollow, iron and copper seeds |
| Citrinitas | Wandering years | the Tria Prima, "the dose makes the poison", zinc | gold, redstone and diamond seeds, Principle Dropper, Salamander, Receptaculum |
| Rubedo | Basel and Salzburg | the burned textbook, his death in 1541 | the Prelum: Netherite from Caput Mortuum |

The stage plan for Citrinitas and Rubedo, the hidden Gnome Hollow, the homunculus and the hints for hidden things are in `alchemy_plan.md`.

### 2.2 Servus: Foundation

The base mod every Servus mod builds on: the shared API, the Configuration Carrot, the goggles, the Utility Duct with its lines and the general utilities (Fluid Tank, pumps, Sink, Ender Anchor, Coal Generator). They live here because Shuttles needs them as much as Alchemy. Everything in Foundation uses plain crafting recipes.

### 2.3 Servus: Shuttles

**Core idea:** the player builds a real warehouse. In-world storage, no storage disks.

- **Progression:** Heavy Duty Racks, rails, a garage and a Mk1 shuttle first (no power); then the Warehouse Controller and Storage Terminal; shuttles on rails with put-away rules; the data cable with on-demand autocrafting; field work; wireless between sites.
- **Shuttles are simulated as data** on the warehouse network and only drawn moving on the client. No real entities. Every pick and put-away runs through racks and shuttles.
- **Rails or cable:** only storage touches rails, so shuttles can reach it. Everything else sits on the data cable. The Warehouse Controller sits on the rails and is connected to the cable.
- **Power:** the warehouse runs on FE through its controller, one power connection per site; nothing costs FE while idle.
- **Field work** stays in Shuttles: shuttles with tool modules leave the warehouse on Farm Rails and tend fields; one tool per shuttle.

### 2.4 Integration: Shuttles + Alchemy (autocrafting)

- **Production on demand** is the USP. Nothing runs without an order: a machine on the data cable produces exactly what was ordered and then waits, while stations and pipes cover what runs all the time.
- **One route for orders:** everything on demand runs over the data cable. The Crafting CPU owns work orders.
- **Machines say what they can do:** Servus machines expose the production capability and join the cable directly; machines of other mods join through the Machine Adapter.
- **Upkeep is not part of a recipe:** fuel and the like are kept stocked at a level, independent of orders.

---

## 3. Architecture principles

1. **Server thread only for world access.** All reads and writes to the world, block entities and capabilities happen on the server thread inside the tick.
2. **Actor pattern without a library.** Each network (duct network, warehouse, factory site) is an object with its own event mailbox, drained once per server tick in order. Deterministic and thread safe.
3. **Heavy work offloaded only when needed.** If something like a huge graph rebuild gets expensive, compute it on an executor from an immutable snapshot and apply the result on the next tick.
4. **Event driven, not polled.** Own blocks push change events. A warehouse indexes only its own racks, polled slowly and in batches; it never polls foreign inventories.
5. **Data driven content.** Recipes, principle ratios, vein seeds, journal tasks, Athanor structures and balance numbers come from datapack JSON with codecs.
6. **Capabilities as the integration surface.** Machines and storage expose standard capabilities, so any mod's pipes and cables work. Only blocks meant for pipes register the item capability; blocks that take data line packets but no pipes register the packet inlet instead.
7. **Pure logic separated from Minecraft.** Graph algorithms, BOM planning, ratio math, order scheduling live in plain Java classes with no Minecraft types, unit tested with JUnit.
8. **Persistence:** network and journal progress in `SavedData`, block state in block entities using data components and codecs.

The bridge between the mods lives in Servus: Foundation as a small API (production and stock capabilities, packet inlet, data line, upgrades, upkeep, Machine Adapter, item supplier). Keep it small and grow it only when a second mod actually needs something.

---

## 4. Milestones

### Done

| Milestone | Built |
|---|---|
| M0 Repository and build | multi project Gradle with ModDevGradle, run configs, CI, the port to 26.1.2 |
| M1 Foundations | network graph with split and merge, mailbox tick, `NetworkManager`, Configuration Carrot, config |
| M2, M3 Factorio style factory | superseded by M9; what lived on: pumps, Sink, Ender Anchor, side config, JEI, the docs site and Balance lab |
| M4 Warehouse first slice | Heavy Duty Rack with sections, Warehouse Controller, terminal |
| M5 Logistics | Utility Duct with item, power and fluid lines, modes, named team channels, filters with search and JEI, priorities, facades, goggles, Data Potato, Fluid Tank |
| M6 Warehouse automation | rails, elevators with lift cars, simulated shuttles, garages, Inbound Station, Zone Labels, placement assist |
| M7 Autocrafting integration | production API, work orders, stock rules, Outbound Station, Crafting Station |
| M8 On-demand autocrafting | Servus: Foundation merge, data cable with tiers, Packing Station, Storage Terminal, Crafting CPU, Production Terminal, Pattern Storage, Machine Adapter |
| M9 Alchemy, Nigredo and Albedo | Tria Prima, Calciner, Purifier, Coagulator and Athanor builds, journal, Elixir of Sight, Gnome Hollow, facade panels, beam engine pumps, Ender Anchor area |
| M10 Field work | Shuttle Workshop, Mk1 to Mk3 shuttles with Speed and Handling, shuttle looks, hand build minigame, Farm Rail and Field Head with range, corrosion and tabs, Harvester and Spreader, lift upgrades |
| M11 Wireless and scale | controllers as sites, Anchor Upgrade wakes, wireless between controllers, warehouse power, pipe access rules |

### Open: Alchemy

- **Balance pass** and the docs Balance lab on the new data, including CPU slots, buffer room and recipes.
- **Checkpoint: play it.** Is Nigredo to Albedo fun for a casual player and for a planner? Adjust before Citrinitas.
- **Journal guidance:** new journal events `build` (a machine formed or placed) and `make` (a first item made) (`find` exists since `m12-hidden-hints`), so chapters react to what the player builds; progress per team, unlocked for every member (decided 2026-10-12).
- **M12 Alchemy, Citrinitas and Rubedo**, as planned in `alchemy_plan.md` (decided 2026-10-15). Still open in M12:
  - `m12-house-gnome`: befriend the old gnome found through the hints and take him home as the House Gnome companion, a second voice next to the journal; what he does is open (`alchemy_plan.md`). The team's found state is already kept in world data.
  - Story texts for the storyteller: every line starting with "[storyteller]" (Wandering years, Basel and Salzburg, the homunculus' whispers and secret, the old gnome's greeting) and the knocker folklore of The Old Gnome; historical claims are checked when the text is written.
  - The open story and play questions in `alchemy_plan.md`, "Open".
- Better upgrade items from Alchemy through the Foundation upgrade API.

### Open: Foundation

- Deeper inspection with the Data Potato: throughput per channel, why items are stuck, machine and storage reports.
- Power Cable with a tiered transfer rate.
- Hand made patterns for Machine Adapters, if the play tests ask for them.

### Open: Shuttles

- `m10-lumber`: a Lumber module for tree fields; later field kinds beyond crops.
- SEND and KEEP rules between sites in the controller GUI (the local min/max stock rules join the same list) and an In transit view.
- Wireless Terminal with range tiers.
- Outbound dock: delivery by teleport or by courier drone to a landing pad.
- Explore shuttle portals between sites, overland cargo carts and a separate Servus: Transit mod for passenger travel.

---

## 5. Open decisions

- A converter between principles (surplus Sulfur to Salt and so on), or only the Sink
- Alchemist's Spectacles as a permanent Elemental Sight in a later stage
- Field kinds beyond crops (trees first, then others) and which modules they need
- Art style and texture direction (shared look across the family)

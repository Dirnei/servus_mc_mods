# Design: m9-foundation-utilities

## Context

See proposal.md for why. Today the six blocks live in `manufacturing/.../machine/{generator,pump,loader,sink}` and stand on Manufacturing's machine base in `manufacturing/.../machine`:

- `MachineBlock`, `MachineBlockEntity`, `BurnerMachineBlockEntity`, `PoweredMachineBlockEntity`, `MachineEnergyStorage`, `EnergyNeighbors`, `OwnedMachine`
- `MachineStats` and `MachineStatsManager` (a `SimpleJsonResourceReloadListener` on `data/<ns>/machine/`), `MachineDefaults` (code defaults table, also read by the game data export)
- `menu/MachineMenu`, `MachineSlot`, `OutputSlot`, and `client/MachineScreen`

The base imports nothing from Manufacturing except `MachineScreen`, which imports `PumpMachine` (moving anyway) and `ServusManufacturingIds` (texture paths). It already uses Foundation's side config, upkeep (`UpkeepHolder`) and Configuration Carrot. So the base can move as a unit.

Registration sits in Manufacturing's `ServusManufacturingEvents` (reload listeners, `RegisterCapabilitiesEvent`, pump neighbour handling), `ServusManufacturingClient` (menu screens, block entity renderers for the side markers, the Sink renderer), the registries, `ServusManufacturingConfig` (Chunk Loader keys) and `ServusManufacturingChunks` (the `TicketController`).

## Goals / Non-Goals

**Goals:**
- One machine base in Foundation used by Foundation's and Manufacturing's machines.
- The six blocks behave exactly as before under `servus_foundation` ids.
- Manufacturing keeps building and its remaining machines keep working until `m9-tria-prima` replaces them.

**Non-Goals:**
- Reworking the machine base API. Classes move with their names and contracts, packages change.
- Changing the textures or models of the moved blocks.

## Decisions

### D1: The machine base moves to `foundation.machine`

Classes go to `io.github.dirnei.mc.servus.foundation.machine` (blocks and block entities, stats, defaults), `foundation.machine.menu` (menu and slots) and `foundation.client` (`MachineScreen`). The moved blocks go to `foundation.machine.{generator,pump,loader,sink}`.

Alternative: copy the base into Foundation and leave Manufacturing's copy until the rewrite. Rejected: two diverging bases, and the alchemy machines will need the Foundation base anyway.

Alternative: keep the base in Manufacturing and only move the blocks. Not possible, because Foundation cannot depend on Manufacturing.

### D2: Machine stats and defaults are shared, keyed by full id

`MachineStatsManager` already keys by `ResourceLocation`, so one Foundation listener loads `data/<any namespace>/machine/*.json` for every mod. `MachineDefaults` becomes a registry with a public `register(id, kind, recipeTypes, stats)`: Foundation registers the four machines it owns, Manufacturing registers its own from its mod constructor. `all()` keeps its fixed order (registration order), which keeps the export deterministic, since Foundation loads before Manufacturing. `MachineDefaults.EXTRACTION` and `RESEARCH` are Manufacturing concepts and move to a Manufacturing constants class.

The Sink rate table (`SinkStatsManager`, directory `sink`) moves with the Sink; its file moves to `data/servus_foundation/sink/sink.json`.

### D3: Ids change namespace, no migration

The blocks become `servus_foundation:burner_pump`, `electric_pump`, `sink`, `chimney`, `chunk_loader`, `coal_generator`. Nothing is released, so there are no data fixers and no remapping (`MissingMappingsEvent` is not used). Old dev worlds lose these blocks.

### D4: Recipes are plain crafting

Foundation's `ServusFoundationRecipeProvider` gets shaped recipes for all six blocks. Burner Pump, Sink, Chimney and Coal Generator keep their current patterns. New patterns (examples, the datagen is the source):

- Electric Pump: Burner Pump in the centre, iron ingots on the sides, copper ingots and redstone around it.
- Chunk Loader: ender pearl in the centre, iron ingots in the corners, copper ingots and redstone on the edges.

The assembling recipes `assembling/electric_pump` and `assembling/chunk_loader` are deleted, and the `electric_pump` unlock leaves `tech/electric_extractor.json`.

### D5: Config and ticket controller move to Foundation

`maxChunkLoadersPerTeam` and `chunkLoaderRespectsFtbChunks` go into `ServusFoundationConfig` with the same names, defaults and comments. The ticket controller is registered by Foundation through `RegisterTicketControllersEvent` with id `servus_foundation:chunk_loader`; `ChunkLoaderData` (`SavedData`) moves along. FTB Chunks detection stays a runtime `ModList` check, no compile dependency.

### D6: Shared textures move with the base

`machine_side.png`, `machine_top.png` and the `burner_machine.png` GUI background move to `assets/servus_foundation/textures/...`. Manufacturing's model datagen points its remaining machines at the Foundation textures, so no texture is duplicated. The block textures and GUI backgrounds of the six moved blocks move to the Foundation namespace.

### D7: Registration follows the existing Foundation pattern

The six blocks, their block entities, items and menus are added to the Foundation registries, the Foundation creative tab, and Foundation datagen (block states, models, loot, tags, lang, recipes). Capabilities (`Capabilities.ItemHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK`, `Capabilities.FluidHandler.BLOCK`, `ServusFoundationCapabilities.UPKEEP`) are registered in Foundation's `RegisterCapabilitiesEvent` handler. The pump neighbour hook and the Sink renderer, screens and side marker block entity renderers move to Foundation's client and event classes.

NeoForge APIs used are those the code already uses today: `RegisterCapabilitiesEvent`, `AddReloadListenerEvent` with `SimpleJsonResourceReloadListener`, `RegisterTicketControllersEvent` and `TicketController`, `ModConfigSpec`, `RegisterMenuScreensEvent`, `EntityRenderersEvent.RegisterRenderers`. They are verified by the existing working code in this repository, not new.

### D8: Tests move with the code

Pure tests (`PumpSourceTest`, `SinkLayoutTest`, `SinkRatesTest`, `VoidBudgetTest`) move to `foundation/src/test/java` unchanged. `MachineDefaultsTest` splits: registration order and defaults of Foundation machines in Foundation, Manufacturing machines in Manufacturing. The pump and Sink GameTests move from `ServusManufacturingGameTests` into a new `foundation.machine.gametest` class registered like the existing Foundation GameTests. `NativeUpkeepGameTests` keeps living in Manufacturing and uses the Foundation generator.

### D9: Game data export

The export reads `MachineDefaults.all()` and the merged stats; with D2 it still sees every machine, now with `servus_foundation` ids. The machine entries keep their kinds. The crafting section picks the six recipes up from Foundation, as it already exports every Servus mod's crafting recipes. `gamedata.json` and the icons are regenerated, and docs pages that name `servus_manufacturing:` ids for these blocks are updated. The `foundation/gamedata-export` spec does not change.

### Pure and Minecraft logic

Pure, JUnit: `PumpSource`, `SinkLayout`, `SinkRates`, `VoidBudget`, `MachineDefaults` ordering. Minecraft, GameTest: pumps, Sink forming and voiding, generator as upkeep member. Manual: Chunk Loader (ticket survival over restart), GUIs and renderers in the client.

## PLAN.md decisions settled

PLAN.md M9 item 1 is implemented as written. No open decision in PLAN.md section 5 is settled by this change. PLAN.md already describes the moved blocks in section 2.2, so it needs no update.

## Risks / Trade-offs

- [Large move touches many files at once] → Move in steps that each build: base first (Manufacturing imports from Foundation), then one block family per task, each with its tests green.
- [A Manufacturing machine still references a removed constant or texture] → Full build plus the Manufacturing GameTests after every step; the reference block GameTest exercises the remaining machines.
- [Stale `gamedata.json` fails CI] → Regenerate it in the last data task and commit it with the icons.
- [Registration order changes `MachineDefaults.all()` order] → The export sorts entries by id anyway; `MachineDefaultsTest` pins the order per mod.
- [Players lose placed blocks in dev worlds] → Accepted, nothing is released.

## Migration Plan

None for players. For developers: dev worlds with the moved blocks need them placed again.

# Design

## Context

Manufacturing has powered machines, the assembler with a reserved `tech` field on recipes, and no research. See proposal.md and the delta specs.

APIs verified in the NeoForge 21.1.255 sources:
- `DataPackRegistryEvent.NewRegistry.dataPackRegistry(ResourceKey, Codec, networkCodec)` for a synced datapack registry.
- `SavedData` with `save(CompoundTag, HolderLookup.Provider)`, `SavedData.Factory`, `DimensionDataStorage.computeIfAbsent`.
- `RegisterPayloadHandlersEvent.registrar(version)`, `PayloadRegistrar.playToClient/playToServer`, `CustomPacketPayload.Type`, `PacketDistributor.sendToPlayer/sendToServer`, `IPayloadContext.player()`.
- `PlayerEvent.PlayerLoggedInEvent`, `Block.setPlacedBy`, `Button.builder(...).bounds(...).build()`, `RegistryAccess.registryOrThrow`.

## Goals / Non-Goals

**Goals:**
- Research rules are pure and tested; Minecraft code only stores, syncs and renders.
- Techs are plain data and reach the client through the registry sync.
- Everything per team through one Lib hook.

**Non-Goals:**
- Pretty tree rendering, FTB Teams, chunk loading.

## Decisions

### D1: ResearchState (pure)
`ResearchState` with String ids: `completed` set, `current` id, `progress` map tech to (item to consumed count). `TechNode(id, parents, cost)` record. Methods: `isCompleted`, `isAvailable(node)`, `start(node)`, `remaining(node, item)`, `consume(node, item)` returning whether the tech just completed, `consumed(tech, item)`. Switching research keeps the consumed counts of the other tech.

### D2: Teams in Lib
`TeamProvider` (`UUID teamOf(ServerPlayer)`) and `Teams` with `provider()`, `install(provider)` and `teamOf(player)`. Default returns the player UUID.

### D3: Tech registry
`TechDefinition(ItemStack icon, List<ResourceLocation> parents, List<ItemStack> cost, int ticksPerPack, List<ResourceLocation> unlocks)` with a codec; registry key `servus_manufacturing:tech`, registered as a synced datapack registry. `Techs` offers `all(RegistryAccess)` sorted by depth then id, `get`, `node(id, def)` to build `TechNode`s with item ids as strings, and `isPackItem(RegistryAccess, ItemStack)`. Display name key `tech.<ns>.<path>` with `.desc` for the description.

### D4: Saved data and sync
`ResearchSavedData` (file `servus_research` in the overworld storage) maps team UUID to `ResearchState`, serialized as NBT. `ResearchManager` is the server facade: `state(server, team)`, `start(player, techId)` (validates availability), `labConsumed(server, team, node, item)`, `sync(server, team)` which sends `ResearchSyncPayload` to all online players of that team. Login sends the same payload. `ClientResearch` holds the last payload on the client and answers `isCompleted`, `current`, `consumed`.

### D5: Payloads
`ResearchSyncPayload(List<ResourceLocation> completed, Optional<ResourceLocation> current, Map<String, Integer> progressOfCurrent)` server to client; `SelectResearchPayload(ResourceLocation tech)` client to server. Registered in `ServusManufacturingNetwork` on `RegisterPayloadHandlersEvent` with version "1". Handlers run on the main thread (NeoForge default).

### D6: Research Lab
`ResearchLabBlock extends MachineBlock`; `MachineBlock.setPlacedBy` passes the placer to block entities implementing `OwnedMachine`. `ResearchLabBlockEntity extends PoweredMachineBlockEntity` with two input slots and an owner UUID (saved). `canWork`: owner known, team has a current tech, a slot holds an item with `remaining > 0`. `finishWork`: extract one such pack, call `ResearchManager.labConsumed`, which updates the state, marks dirty and syncs. Work ticks come from the current tech's `ticks_per_pack`. Defaults: 10 FE/t, 4000 FE.

### D7: Screens
`ResearchLabScreen extends MachineScreen` (layout LAB on `research_lab.png`, arrow and energy) adds a "Tech Tree" button and a line with the current research. `TechTreeScreen extends Screen` lists techs: icon, name, cost, status; a click on an available row sends `SelectResearchPayload`; a Done button returns to the lab screen. Status comes from `ClientResearch` and updates live when a sync arrives.

### D8: Assembler gating
`AssemblerMenu` receives a `Predicate<ResourceLocation> techCompleted` (server: saved data of the player's team; client: `ClientResearch`). `isUnlocked(index)` and `clickMenuButton` refuse locked recipes; `AssemblerScreen` draws locked cells dark. Recipe indices stay stable because the full sorted list is kept.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: `ResearchState`.
- Minecraft, verified end to end on a dev server: the saved data file `world/data/servus_research.dat` was seeded with a team that has a current research, a lab got that team as owner through `data merge`, and ten packs completed the research in the expected 50 seconds with the completion logged. The tech tree screen and the sync payloads are checked manually in the playtest.

Applied during implementation: the powered and burner bases used the generic `work_ticks` stat for the cycle length, so the lab and assembler finished every tick. `MachineBlockEntity.workTicks()` is now overridable; the assembler returns the recipe time and the lab the tech's ticks per pack, and both tick logic and display use it.

## PLAN.md decisions settled

- Research per team: implemented through the Lib team hook, FTB Teams later (plan already says so).

## Risks / Trade-offs

- [Research cannot be fully exercised without a player] -> The rules are unit tested; the lab tick is a thin wrapper. The playtest covers it.
- [Owner stored as UUID, not team] -> The team is resolved at tick time through the provider, so a later team mod changes behavior without migration.

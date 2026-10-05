# Design

## Context

Powered machines, owned machines and the Lib team hook exist. NeoForge provides `TicketController(ResourceLocation)` with `forceChunk(ServerLevel, BlockPos owner, chunkX, chunkZ, add, ticking)` registered through `RegisterTicketControllersEvent`, and `ModList.get().isLoaded(modid)`.

## Decisions

### D1: Block entity
`ChunkLoaderBlockEntity extends PoweredMachineBlockEntity implements OwnedMachine`, no slots, defaults `powered(1, 10, 4000)`. `canWork` is the allowance check (owner known, FTB rule, cap); with `workTicks` 1 the powered base spends the upkeep every tick while allowed. After the base tick, `syncTicket(working)` calls the controller when the desired state changes; `setRemoved` releases the ticket and the cap registration. Status code in data index 6: active, no power, team limit, FTB Chunks, no owner.

### D2: Cap registry
`ChunkLoaderData` saved data in the overworld storage maps team UUID to a set of dimension plus position strings. A loader registers itself when it first becomes allowed and unregisters on removal. `countFor(team)` is compared with the config cap, counting a loader that is already registered as allowed.

### D3: Config
`ServusManufacturingConfig` (common): `maxChunkLoadersPerTeam` (1 to 64, default 4), `chunkLoaderRespectsFtbChunks` (true). Registered in the mod constructor.

### D4: Ticket controller
`ServusManufacturingChunks.CONTROLLER` registered on the mod bus event. No validation callback: tickets persist and the block entity reconciles on its first tick; a loader that no longer exists at the owner position is cleaned up by NeoForge's own owner based removal when the block entity is gone? It is not, so the block releases on removal and the restart case is covered by the block entity's first tick (release when not allowed).

### D5: GUI
`ChunkLoaderMenu` with no slots, layout `LOADER` on a generated background with only the energy bar, `ChunkLoaderScreen` drawing the status line from data index 6.

### D6: Recipe
Assembling, no tech: 4 iron gears, 8 copper wire, 1 ender pearl, 200 ticks.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: a powered loader with an extractor in a far chunk keeps producing after the vanilla force load is removed, while a control setup without a loader stops.

## Risks / Trade-offs

- [Tickets of a loader destroyed while unloaded (world not saved) could linger] -> The loader releases on its first tick when not allowed; a lingering ticket for a vanished block entity is reconciled when the chunk is next loaded by anything else. Acceptable for now; a validation callback can be added if it bites.

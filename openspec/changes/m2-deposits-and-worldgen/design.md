# Design

## Context

Manufacturing has only its empty entrypoint. Lib provides the Servus creative tab key and the datagen pattern. See proposal.md.

APIs verified in the NeoForge 21.1.255 sources and the vanilla data jar:
- `DeferredRegister.Blocks.registerSimpleBlock`, `DeferredRegister.Items.registerSimpleBlockItem`.
- `SimpleJsonResourceReloadListener(Gson, String directory)` with `apply(Map<ResourceLocation, JsonElement>, ResourceManager, ProfilerFiller)`, registered through `AddReloadListenerEvent.addListener`.
- `BlockStateProvider.simpleBlockWithItem(Block, ModelFile)` and `cubeAll`.
- Vanilla `minecraft:disk` feature config (`disk_gravel.json`) and placement modifiers `in_square`, `heightmap`, `rarity_filter`, `biome`.
- NeoForge biome modifier `neoforge:add_features` under `data/<ns>/neoforge/biome_modifier/`.

## Goals / Non-Goals

**Goals:**
- Deposits are blocks plus data. No code change is needed to add a deposit type in a datapack.
- Worldgen entirely in JSON so it can be tuned without a rebuild.

**Non-Goals:**
- Extraction logic, final art, balance.

## Decisions

### D1: Deposit blocks are plain blocks
`ServusManufacturingBlocks` registers four `Block` instances with `strength(-1.0F, 3600000.0F)` (unbreakable, like bedrock) and stone sounds. No loot table because nothing drops; the datagen loot provider is introduced with the first breakable block. Block items registered in `ServusManufacturingItems` and added to the Servus tab via `BuildCreativeModeTabContentsEvent` on the mod bus.

### D2: Deposit definitions
`DepositDefinition(Block block, ItemStack yield)` with a `Codec` (`BuiltInRegistries.BLOCK.byNameCodec()`, `ItemStack.CODEC`). `DepositManager extends SimpleJsonResourceReloadListener` reads directory `deposit`, parses each file with the codec, logs failures per file and the total count, and exposes `forBlock(Block)`. A static `DepositManager.INSTANCE` is registered in `AddReloadListenerEvent` (game bus). The four built in files live in `manufacturing/src/main/resources/data/servus_manufacturing/deposit/`.
Alternative: a datapack registry (`DataPackRegistryEvent`). Rejected for now; a reload listener is simpler, and nothing needs holders or tags over deposits yet.

### D3: Worldgen JSON
Per deposit type: configured feature `minecraft:disk` with `half_height` 1, radius uniform (iron 3 to 5, copper 2 to 4, coal 3 to 5, stone 3 to 6), target `matching_blocks` of dirt, grass block, stone, sand, gravel, coarse dirt and podzol. Placed feature with `rarity_filter` (iron 1 in 20, copper 1 in 28, coal 1 in 20, stone 1 in 16), `in_square`, `heightmap WORLD_SURFACE_WG` and `biome`. Using `WORLD_SURFACE_WG` puts the position above water in oceans and lakes, where no target block matches, so patches only appear on land. Biome modifier `neoforge:add_features` for `#minecraft:is_overworld` at step `underground_ores`. All JSON is hand written under `data/servus_manufacturing/worldgen/` and `neoforge/biome_modifier/`.

### D4: Verification tooling
The dev server runs do not forward stdin, so `server.properties` in the run directories gets RCON enabled and a small RCON client script sends `/place feature` and `/reload` for verification. Natural generation is checked by the user in the playtest; rarity is a balance number.

## Pure Java vs Minecraft

Everything touches Minecraft. Verified by datagen output, server log (deposit count, no worldgen parse errors) and RCON commands placing each feature.

## PLAN.md decisions settled

None. Yields use vanilla items (raw iron, raw copper, coal, cobblestone) so the mod stays standalone and pack friendly. That follows the plan's "no dependency on items from other mods" and avoids duplicate ingots.

## Risks / Trade-offs

- [Patches may generate too often or too rarely] -> Numbers are in JSON, tuned after the playtest.
- [Unbreakable deposits placed by hand in creative cannot be removed in survival] -> Intended. Creative players can still remove them.
- [Disk feature can carve into steep terrain and look odd] -> Acceptable for a first slice; a custom feature can replace it later without changing data paths.

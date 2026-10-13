# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116):

- Foundation `machine/anchor/EnderAnchorBlockEntity extends PoweredMachineBlockEntity`: `allowance()` checks owner, FTB Chunks and `EnderAnchorData.countFor(owner) >= maxEnderAnchorsPerTeam`, then registers the anchor's key; `syncTicket` forces one chunk with `EnderAnchorTickets.CONTROLLER.forceChunk(server, pos, x, z, add, ticking)`. `PoweredMachineBlockEntity.tick` consumes `stats().energyPerTick()` per tick. No item slots; GUI `client/EnderAnchorScreen` draws the energy bar and one status line.
- `machine/anchor/EnderAnchorData` (saved in the overworld): `team -> Set<"dimension@pos">`, one entry per anchor or woken site.
- Storage `site/SiteWakes` registers a woken controller's position in `EnderAnchorData` (one entry whatever the span) and checks `WakeAllowance.check(..., active, maxEnderAnchorsPerTeam, ...)`; `controller/ChunkLoaderUpgradeItem`, `ChunkLoaderUpgradeSlots extends UpgradeSlots` (count of upgrades), `chunk_loader_upgrade_slots` and `chunks_per_upgrade` in `warehouse.json`.
- Menu buttons: `AbstractContainerMenu#clickMenuButton` is used by `MachineScreen`, `AdapterMenu` and `UtilityDuctMenu` (vanilla `MultiPlayerGameMode#handleInventoryButtonClick`), no custom payload needed.
- World overlays: `client/PlacementAssistRenderer` draws in `RenderLevelStageEvent.AfterTranslucentBlocks`.

## Goals / Non-Goals

**Goals:**
- One anchor covers up to 5 by 5 chunks, shiftable, paid per chunk.
- One team cap in chunks shared by anchors and warehouse wakes, counted fairly.
- Area, offset limits and the team load decided by pure, tested code.

**Non-Goals:** see proposal Non-goals.

## Decisions

### D1. Pure area logic

New pure `machine/anchor/AnchorArea` (record: `radius`, `offsetX`, `offsetZ`): `of(upgrades, offsetX, offsetZ)` clamps the offset to `[-radius, radius]` per axis; `shift(direction)` returns the shifted area or the same when at the limit; `canShift(direction)`; `chunks(centreX, centreZ)` lists the chunk coordinates (`long` pairs) of the square; `size()` is `(2r+1)^2`. JUnit `AnchorAreaTest`: sizes 1, 9, 25; offset clamped; own chunk always inside for every legal offset; shift stops at the limit; clamping when the radius shrinks.

### D2. Team load as a union

`EnderAnchorData` keeps per team `entryKey -> Set<chunk>` where a chunk is `dimension` plus packed `ChunkPos` (`ChunkPos#pack` style long, stored as `"dimension@long"` strings in the codec, like the keys today). A new pure `machine/anchor/TeamLoad` computes with plain sets: `load(entries)` is the size of the union; `fits(entries, entryKey, newChunks, cap)` is the size of the union of all other entries plus `newChunks` against the cap (so an entry's own old area is ignored, decision "re-check without the old entry"). JUnit `TeamLoadTest`: overlap counted once, own entry ignored when re-checking, exactly at the cap fits, one over refuses.

`EnderAnchorData` API: `load(team)`, `fits(team, key, chunks, cap)`, `register(team, key, chunks)` (replaces the entry), `unregister(team, key)`, `isRegistered(team, key)`. The union is recomputed on demand; teams hold at most a few dozen entries, so no cache. The saved data id stays `servus_ender_anchors`; old saves without chunk lists load entries as empty sets (dev worlds only).

### D3. Anchor behaviour

- **Slots.** New Foundation `upgrade/AnchorUpgradeItem` (registered in `FoundationMachines` or the existing item registry, id `anchor_upgrade`) and `upgrade/AnchorUpgradeSlots extends UpgradeSlots` (moved from Storage `ChunkLoaderUpgradeSlots`, `isItemValid` only the Anchor Upgrade, `count()`). The anchor holds 2 slots, saved, dropped on break, kept in the dismantled item like other machine contents. No capability exposes them.
- **Area state.** The anchor saves `OffsetX`, `OffsetZ`; its `AnchorArea` comes from the upgrade count and the saved offset, clamped whenever upgrades change.
- **Activation.** `allowance()` asks `EnderAnchorData.fits(owner, key, area chunks, maxLoadedChunksPerTeam)` and registers the chunks when it fits. While active, a change of upgrades or offset recomputes the area: release tickets of chunks that left, re-check with `fits` ignoring the own entry, then force the new chunks or drop to `STATUS_TEAM_LIMIT` and release everything.
- **Tickets.** `syncTicket` becomes `syncTickets(Set<chunk> wanted)`: forces chunks missing from the held set and releases chunks no longer wanted, through the same `TicketController`; the held set is saved (`Tickets` list) so a restart releases correctly.
- **Energy.** `PoweredMachineBlockEntity` gets a protected hook `energyPerTickNow()` defaulting to `stats().energyPerTick()`; the anchor returns `energyPerTick * area.size()`. `energy_capacity` in `machine/ender_anchor.json` goes from 4000 to 20000 (example) so a 5 by 5 anchor rides out short gaps. If the FE input rate limits a full square, the input rate scales the same way (checked in apply).
- **Config.** `maxEnderAnchorsPerTeam` is replaced by `maxLoadedChunksPerTeam` (default 49, range 1 to 1024). `enderAnchorRespectsFtbChunks` stays.

### D4. Warehouse wakes

`SiteWakes` registers the woken site under the controller's key with the chunks of its span and checks `EnderAnchorData.fits(team, key, span, maxLoadedChunksPerTeam)` instead of the count; `WakeAllowance.check` takes a `fits` boolean instead of `active` and `cap` (its JUnit test is updated). The controller uses the Foundation `AnchorUpgradeSlots` and `AnchorUpgradeItem`; Storage `ChunkLoaderUpgradeItem`, `ChunkLoaderUpgradeSlots`, the Storage item, model, texture and recipe go; the texture moves to Foundation as `anchor_upgrade.png`. `chunk_loader_upgrade_slots` in `warehouse.json` becomes `anchor_upgrade_slots`; the saved NBT key `ChunkLoaderUpgrades` becomes `AnchorUpgrades`.

### D5. GUI and buttons

`EnderAnchorMenu` gets the two upgrade slots plus the player inventory and data slots for status, radius, offset x and z, team load and cap. Buttons use `clickMenuButton(player, id)`: ids 0 to 3 shift north, south, east, west on the server through `AnchorArea.shift`. `EnderAnchorScreen` draws the arrows (greyed when `canShift` is false), the size "3 x 3", the status line, "14 / 49 chunks" and the "Show range" toggle. A new GUI texture `textures/gui/ender_anchor.png` with slot frames, drawn by the job script.

### D8. GUI redesign (play check 2026-10-13)

The first GUI worked but was too small and plain, missed the FE cost and repeated "active", which the block's eye already shows. It becomes:

- **Size.** A larger panel (about 196 wide, about 210 tall with the player inventory), drawn from a 256x256 texture.
- **Ender look.** Obsidian black background with faint purple veins, a purple bevelled frame, dark purple slot frames (upgrades and the player inventory), light lavender text (title, labels, inventory label). Drawn by the job script, matching the anchor block's obsidian and purple.
- **Chunk map.** A 5 by 5 grid (the largest area) in the middle: chunks of the current area glow purple, the anchor's own chunk carries a marker, the rest stay dark. It follows upgrades and offset live.
- **Arrows.** 16 px arrow buttons round the map (north above, south below, west left, east right), greyed out at the limit as before.
- **Info lines.** "Area 3 x 3", "Upkeep 90 FE/t" (energy per chunk times the area size, synced through a menu data slot) and "Team 14 / 49".
- **Status.** No line while active. While inactive one red line gives the reason (no power, team limit reached, use FTB Chunks, no owner).
- **Toggle.** "Show range" checkbox kept, restyled in purple.
- **Energy bar.** Kept on the right, restyled to fit.

### D6. Range outline

The toggle lives in a client only `client/AnchorRangeView` (a set of anchor positions per dimension, cleared on logout, never sent to the server). A new `client/AnchorRangeRenderer` on `RenderLevelStageEvent.AfterTranslucentBlocks`, like `PlacementAssistRenderer`, draws for each shown anchor within 128 blocks that is still an Ender Anchor block entity on the client: a purple line along the square's outer chunk borders at the anchor's y plus 0.05, and a column of lines at each of the four corners from y minus 16 to y plus 32. The area comes from the anchor's synced block entity data (radius and offset in the update tag). Look only.

### D7. Tests

JUnit: `AnchorAreaTest`, `TeamLoadTest`, updated `WakeAllowanceTest`. GameTests (Foundation): `anchorLoadsItsSquare` (one upgrade, 9 chunks forced), `anchorOffsetKeepsOwnChunk`, `anchorRefusedOverCap` (load and status), `anchorOverlapCountedOnce`, `anchorGrowOverCapReleases`, `anchorPaysPerChunk`, `anchorSlotsTakeOnlyAnchorUpgrades`, `anchorBreakReleasesArea`. Storage GameTests: the existing site wake tests move to chunk counts and the Anchor Upgrade. The outline and GUI are checked in game.

PLAN.md open decisions settled: none. PLAN.md section 2.1 (Ender Anchor) and the M11 site text are updated with the area, the chunk cap and the Anchor Upgrade.

## Risks / Trade-offs

- [FE scale] 250 FE per tick for a full square may outrun early power. → Example numbers in data; the play check tunes them.
- [Union cost] The team load is recomputed per check from sets. → Teams have few entries; checks run on activation and area changes, not every tick.
- [Dev worlds] Chunk Loader Upgrades in dev controllers vanish. → Nothing is released (memory "unreleased, nothing is breaking").
- [Overlap with the open pump change] `m9-electric-pump-engine` touches pump files only; both may touch `client/MachineClient.java` registrations, a trivial merge.

## Decisions made during apply

- **Anchor Upgrade registration.** `ANCHOR_UPGRADE` is registered in `machine/MachineContent` next to the Ender Anchor (creative tab right after the anchor); the item and `AnchorUpgradeSlots` live in `upgrade/`. Storage imports `MachineContent.ANCHOR_UPGRADE` for its slots, menu, GameTests and recipe removal. The recipe and lang come from `datagen/MachineDataParts`, the item model from `ServusFoundationItemModelProvider`.
- **Energy data is not datagen.** `machine/ender_anchor.json` is a hand written resource under `foundation/src/main/resources`, so `energy_capacity` 20000 is set there and in the `FoundationMachines` default instead of in `MachineDataParts`. The FE input of `MachineEnergyStorage` is `Integer.MAX_VALUE` per call, so no input rate needs to scale with the square.
- **Only an anchor that can pay counts.** `allowance()` unregisters the anchor's entry while its buffer holds less than one tick of its square (`energy_per_tick` times the area size), with FTB Chunks, and when the area does not fit, so the team load holds only active anchors as the spec says. Before this change an unpowered anchor kept its team slot.
- **Re-check by comparing the area.** Each tick the anchor compares the chunk set registered in `EnderAnchorData` with its current area: equal means active without a new check (a lowered cap releases nothing); different (new, grown, shrunk or shifted area) runs `fits` without its own entry and registers or releases. A refused anchor retries every tick; the union is small (a few dozen entries).
- **Upgrades travel with a dismantled anchor.** Other machines spill their inventory before `SideConfigWrench` saves the item. `EnderAnchorBlock.onWrench` marks the block entity (`keepUpgradesOnRemove`) before the shared dismantle, and `dropContents` then leaves the upgrades in the saved data; breaking the block any other way drops them.
- **Held tickets.** Saved as `Tickets` (packed chunks) with `TicketsAt` (the position they belong to), so an anchor dismantled and placed elsewhere starts with no tickets; an old save with the `Ticket` flag maps to its own chunk so it is released correctly.
- **Saved data format.** `servus_ender_anchors` keeps `Teams` of `{Team, Entries: [{Key, Chunks}]}`; the old `Loaders` list loads as entries without chunks. Extra API beside D2: `key(level, pos)` and `chunks(dimension, packed)` are public, `chunksOf(team, key)` and `loadWith(team, key, chunks)`. `TeamLoad` is generic over the chunk type and has `loadWith` as well.
- **Wakes follow their span.** `SiteWakes` works out `fits` only while the site is not registered, registers the span's chunks on wake, and re-registers when the span changes while awake (no new cap check then; the capacity hard limit still applies). `WakeAllowance.check(hasTeam, ftbBlocks, capacity, registered, fits[, stored, toStart])`.
- **GUI layout.** Data entries after `DATA_COUNT`: radius, offset x and z, team load, cap (`DATA_COUNT_ANCHOR`). Upgrade slots at (8, 18) and (26, 18), "Area: 3 x 3" beside them, the toggle box at (8, 38) with "Show range", the four 11 by 11 arrows around (119, 27), the status line at y 52 (red for team limit) and "Team: 14 / 49 chunks" at y 62, both scaled down when they would run under the energy bar. Arrow and toggle sprites sit in `textures/gui/ender_anchor.png` below the panel (v 170 to 202), drawn by the job script `anchor-range/gui.py`. The active status reads "Active: area kept loaded". Arrows and the toggle play the UI click.
- **Client view.** `AnchorRangeView` came with task 6.1 because the toggle needs it. It clears on `ClientPlayerNetworkEvent.LoggingOut` and forgets an anchor whose loaded block is no longer an Ender Anchor. On the client the block entity's radius and offset come from its update tag (`Radius`, `OffsetX`, `OffsetZ`); a client load without `Upgrades` keeps the slots an open menu filled.
- **Outline drawn with quads.** Instead of lines, `AnchorRangeRenderer` draws thin quads with `RenderTypes.debugQuads()` (its pipeline has no culling, so each quad shows from both sides and is drawn once): each border is a flat band 0.12 wide 0.05 above the anchor's foot plus an upright band of the same height; the bands along x run through the corners, those along z stop at their edge; the corner columns are two diagonal upright quads 0.2 wide from 16 below to 32 above. No two faces share a plane: perpendicular or diagonal planes only, and every further anchor shown is stepped by 0.005 (the columns once more along x, since diagonals of two squares sharing a corner would coincide). Each square is drawn relative to its north west corner so the step survives float precision far from spawn. Anchors farther than 128 blocks (horizontal) are skipped.
- **GameTests.** Besides D7: `anchorMenuButtonsShift` (the menu buttons on the server, greyed limit, cap, slot validity) and `anchorDismantleKeepsUpgradesAndOffset`. `enderAnchorLitFollowsActive` now fills the team to one chunk below the cap with a stand-in entry. The Storage `wakeOverlapWithAnchorCountedOnce` uses an anchor holding the home chunk and a site spanning that chunk plus one far chunk (the load grows from 1 to 2), because the network shrinks a span of loaded chunks back to its members before the wake; `teamCapWaitsThenWakes` fills the cap with stand-in chunks. `FieldGameTests.hydrationReachesFourBlocks` failed once in a full run and passed on the rerun (unrelated, flaky).
- **APIs verified** (javap on `minecraft-patched-26.1.2.116-merged.jar`, NeoForge 26.1.2.116 sources): `ChunkPos.pack(int, int)`, `ChunkPos.containing`, `x()`, `z()`; `TicketController.forceChunk(ServerLevel, BlockPos, int, int, boolean add, boolean ticking)`; `ValueOutput.store(String, Codec, T)`, `ValueInput.read(String, Codec)`, `putLong`, `getLongOr`; `RenderLevelStageEvent.AfterTranslucentBlocks` with `getLevelRenderState().cameraRenderState.pos` and `getPoseStack()` as in `PlacementAssistRenderer`; `RenderPipelines.DEBUG_QUADS` built `withCull(false)`; `ClientPlayerNetworkEvent.LoggingOut`; `SimpleSoundInstance.forUI(Holder<SoundEvent>, float)` and `SoundEvents.UI_BUTTON_CLICK`; `GuiGraphicsExtractor.blit(RenderPipeline, Identifier, int, int, float, float, int, int, int, int)`; `ResourceHandlerSlot(handler, setter, index, x, y)`; `AbstractContainerMenu#clickMenuButton` with `MultiPlayerGameMode#handleInventoryButtonClick`.
- No spec needed to change.

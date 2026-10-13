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

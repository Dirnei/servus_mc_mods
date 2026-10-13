# Tasks

Paths are relative to `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/` and `storage/src/main/java/io/github/dirnei/mc/servus/storage/` (marked Storage) unless written in full. Tests mirror them under `src/test/java`. GameTests run with `gradlew :foundation:runGameTestServer` and `gradlew :storage:runGameTestServer`.

## 1. Pure logic

- [x] 1.1 `machine/anchor/AnchorArea.java` (new, design.md D1). JUnit `machine/anchor/AnchorAreaTest.java`: `sizesAreOneNineTwentyFive`, `offsetClampedToRadius`, `ownChunkAlwaysInside`, `shiftStopsAtTheLimit`, `shrinkingRadiusClampsOffset`, `chunksListTheSquare`. Run `gradlew :foundation:test`.
- [x] 1.2 `machine/anchor/TeamLoad.java` (new, design.md D2). JUnit `machine/anchor/TeamLoadTest.java`: `overlapCountedOnce`, `ownEntryIgnoredOnRecheck`, `exactlyAtTheCapFits`, `oneOverTheCapRefused`, `emptyTeamHasNoLoad`. Run `gradlew :foundation:test`.

## 2. Team load and config

- [x] 2.1 `machine/anchor/EnderAnchorData.java`: entries with chunk sets, `load`, `fits`, `register(team, key, chunks)`, `unregister`, codec (design.md D2); `config/ServusFoundationConfig.java`: `maxLoadedChunksPerTeam` (default 49) replaces `maxEnderAnchorsPerTeam`, lang via datagen. Run `gradlew build`.

## 3. Anchor Upgrade in Foundation

- [x] 3.1 `upgrade/AnchorUpgradeItem.java` and `upgrade/AnchorUpgradeSlots.java` (new, from Storage `controller/ChunkLoaderUpgradeItem` and `ChunkLoaderUpgradeSlots`), registration and creative tab entry; datagen: item model, lang "Anchor Upgrade" with tooltip, recipe (Ender Anchor in the middle, ender pearls, iron ingots, redstone as today); move the texture to `textures/item/anchor_upgrade.png`. Storage: delete the old item, slots, recipe, model, lang and texture; `controller/WarehouseControllerBlockEntity.java` (`AnchorUpgrades` NBT), `menu/WarehouseControllerMenu.java`, `registry/ServusStorageItems.java`, `stats/StorageStatsManager.java` and `storage/warehouse.json` (`anchor_upgrade_slots`), Storage GameTests (design.md D4). Run `gradlew :foundation:runData`, `gradlew :storage:runData`, `gradlew build`.

## 4. Anchor area

- [x] 4.1 `machine/PoweredMachineBlockEntity.java` hook `energyPerTickNow()`; `machine/anchor/EnderAnchorBlockEntity.java`: two Anchor Upgrade slots, saved offset, area from `AnchorArea`, activation through `EnderAnchorData.fits`, multi chunk `syncTickets` with the held set saved, area change while active, FE per chunk, drops and dismantle contents; `machine/anchor/EnderAnchorBlock.java` drops; `machine/ender_anchor.json` data `energy_capacity` 20000 via `datagen/MachineDataParts.java` (design.md D3). Run `gradlew :foundation:runData`, `gradlew build`.
- [x] 4.2 Foundation GameTests in `machine/MachineGameTests.java` (or a new `machine/anchor/AnchorGameTests.java`): `anchorLoadsItsSquare`, `anchorOffsetKeepsOwnChunk`, `anchorRefusedOverCap`, `anchorOverlapCountedOnce`, `anchorGrowOverCapReleases`, `anchorPaysPerChunk`, `anchorSlotsTakeOnlyAnchorUpgrades`, `anchorBreakReleasesArea`; existing anchor tests updated to the chunk cap. Run the Foundation GameTests.

## 5. Warehouse wakes

- [ ] 5.1 Storage `site/SiteWakes.java` (register the span's chunks, `fits` against `maxLoadedChunksPerTeam`), `site/WakeAllowance.java` (`fits` instead of active and cap) and its JUnit test, `site/SiteRecord.java` and `controller/*` for the renamed upgrade count, `command/ServusStorageCommands.java` messages (design.md D4). Storage GameTests `SiteWakeGameTests`: team cap by chunks, overlap with an anchor counted once. Run `gradlew :storage:test` and the Storage GameTests.

## 6. GUI and outline

- [ ] 6.1 `machine/menu/EnderAnchorMenu.java` (slots, data slots, `clickMenuButton` shifts), `client/EnderAnchorScreen.java` (arrows, size, status, team load, toggle), GUI texture `textures/gui/ender_anchor.png` by job script, lang keys via datagen (design.md D5). Run `gradlew :foundation:runData`, `gradlew build`.
- [ ] 6.2 `client/AnchorRangeView.java` and `client/AnchorRangeRenderer.java` (new, design.md D6), radius and offset in the anchor's update tag; verify `RenderLevelStageEvent.AfterTranslucentBlocks` usage against `client/PlacementAssistRenderer.java`. Run `gradlew build`.

## 7. Docs and plan

- [ ] 7.1 `PLAN.md` (section 2.1 Ender Anchor: area, offset, chunk cap, Anchor Upgrade; the M11 site text: Anchor Upgrade), `README.md` (config key), `docs/src/content/docs/mods/foundation.md`, `storage.md`, `reference/datapack/machines.md` (`energy_per_tick` per chunk, new capacity); regenerate `docs/src/data/gamedata.json` with `gradlew :manufacturing:runData`. Run `cd docs && npm test`.

## 8. Verification

- [ ] 8.1 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: craft an Anchor Upgrade from an Ender Anchor; an anchor with 0, 1 and 2 upgrades loads 1, 9 and 25 chunks (shown size and team load follow); arrows shift the area and grey out at the limit; "Show range" draws the purple outline with corner columns only for that player; a second anchor overlapping the first raises the team load only by the new chunks; over the cap the anchor stays inactive with "team limit reached"; a warehouse controller takes Anchor Upgrades and wakes as before.

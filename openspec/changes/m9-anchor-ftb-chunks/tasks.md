# Tasks: m9-anchor-ftb-chunks

## 1. Config default

- [x] 1.1 In `foundation/.../config/ServusFoundationConfig.java` set the default of `enderAnchorRespectsFtbChunks` to `false`, reword its comment (true keeps anchors inactive and sites asleep in favour of FTB Chunks) and add `leavesChunkLoadingToFtbChunks()` with the `FTB_CHUNKS` mod id, used by the anchor and `SiteWakes`. Update the config lang text in `foundation/.../datagen/MachineDataParts.java` and `foundation/src/generated/resources/assets/servus_foundation/lang/en_us.json`. Build.

## 2. GameTest batches and the option test

- [x] 2.1 Add `batch()` to `foundation/.../gametest/ServusGameTest.java` and register one environment per batch name in `ServusGameTests.java`. Build.
- [x] 2.2 Add `AnchorGameTests.anchorStaysInactiveForFtbChunks`: with FTB Chunks loaded and the option on for this anchor only (`respectFtbChunksAs(true)`, the config stays untouched), a powered anchor has status 3, holds no ticket and the team load is 0; without FTB Chunks it is active. Run `:foundation:runGameTestServer` and `:alchemy:runGameTestServer`; every anchor test passes in both.

## 3. Docs and plan

- [x] 3.1 Update the Ender Anchor entry in `docs/src/content/docs/mods/foundation.md` and the site wake entry in `docs/src/content/docs/mods/shuttles.md` to the new default.
- [x] 3.2 Add to `PLAN.md` under "Open: Foundation": the Ender Anchor respects FTB Chunks claims (loads only chunks the owner's team claimed or unclaimed ones, never another team's), a soft integration with the FTB Chunks API.
- [x] 3.3 Run `gradlew build test`.

## 4. Play check

- [ ] 4.1 The user checks in game that an Ender Anchor works with FTB Chunks installed.

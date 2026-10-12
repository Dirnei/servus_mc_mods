# Tasks

Paths are relative to `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/`, `foundation/src/test/java/io/github/dirnei/mc/servus/foundation/` and `foundation/src/main/resources/assets/servus_foundation/` unless written in full. GameTests run with `gradlew :foundation:runGameTestServer`.

## 1. Eye motion

- [x] 1.1 `machine/loader/EyeMotion.java` (new, pure, design.md D2). JUnit `machine/loader/EyeMotionTest.java`: `opensInAboutASecond`, `closesTheSameWay`, `blinkSpacingThreeToNineSeconds`, `blinksAreShort`, `seedsBlinkApart`, `irisPointsTowardPlayer`, `irisCentredBehindOrFar`, `irisStaysInLens`, `smoothingConverges`. Run `gradlew :foundation:test`.

## 2. Look

- [x] 2.1 Texture script `C:\Users\Dirnei\AppData\Local\Temp\servus-helpers\jobs\ender-anchor\gen_ender_anchor.py` (not in the repo) from the L1 sketch (`C:\Users\Dirnei\AppData\Local\Temp\claude\C--games-minecraft-servus-mc-mods\e63664e3-2e9f-4956-97ee-f355bc14ab46\scratchpad\genloader.py` and `round2.py`): `textures/block/chunk_loader_front.png` (socket with closed lid), `chunk_loader_side.png`, `chunk_loader_top.png`, `chunk_loader_top_lit.png`, `chunk_loader_eye.png` (sclera, iris with pupil, lid strips); delete `chunk_loader_front_lit.png`; check 16x16 RGBA. Run `gradlew build`.
- [x] 2.2 `datagen/MachineDataParts.java`: the loader's orientable models with the new front, side and top textures, lit variant changes only the top (design.md D1). Run `gradlew :foundation:runData` and `gradlew build`.

## 3. Renderer and particles

- [ ] 3.1 `client/EnderAnchorRenderer.java` (new: lid, sclera and iris quads from `chunk_loader_eye`, nearest player, wrapping `SideMarkerRenderer` for markers) and the client only eye view on `machine/loader/ChunkLoaderBlockEntity.java`; register in `client/MachineClient.java` instead of `SideMarkerRenderer` (design.md D1, D3). Verify `Level#getNearestPlayer` with `javap` on `build/moddev/artifacts/minecraft-patched-26.1.2.116-merged.jar`. Run `gradlew build`.
- [ ] 3.2 `machine/loader/ChunkLoaderBlock.java`: portal particles in `animateTick` while `LIT` (design.md D4). Run `gradlew build`.

## 4. GameTests

- [ ] 4.1 New GameTest `chunkLoaderLitFollowsActive` in the Foundation loader GameTests (where the existing Chunk Loader tests live, found with `grep -rn CHUNK_LOADER foundation/src/main/java/**/gametest* foundation/src/main/java/**/*GameTests.java`); existing loader tests stay green. Run the Foundation GameTests.

## 5. Docs and plan

- [ ] 5.1 `PLAN.md` section 2.1 (the Chunk Loader looks like an ender anchor whose eye shows whether it holds its chunk) and the machines list in `docs/src/content/docs/mods/foundation.md`; regenerate `docs/src/data/gamedata.json` with `gradlew :manufacturing:runData`. Run `cd docs && npm test`.

## 6. Verification

- [ ] 6.1 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: a Chunk Loader shows the obsidian panels, chunk grid sides and the eye lens; placed with power the eye opens, blinks now and then and follows the player walking past; two loaders side by side blink apart; over the team cap or without power the eye stays shut and no particles show; with the carrot the side markers still show.

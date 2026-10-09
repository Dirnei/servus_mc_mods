# Tasks

## 1. Terminal content after a round trip

- [ ] 1.1 Add pure `storage/.../client/TerminalStore` and `storage/src/test/.../client/TerminalStoreTest` (openShowsCached, updateWhileClosedShownOnOpen, roundTripShowsNewest, otherTerminalNotShown, forgetClears); make `ClientStock` and `ClientProduction` delegate to it; both screens call `open(pos)` in `init()`. Verify with `gradlew build` and `gradlew test`.

## 2. Search clear

- [ ] 2.1 `CounterTerminalScreen.mouseClicked`: a right click on the search box clears it and resets the scroll. Verify with `gradlew build`.

## 3. Lamps, animation and CPU margin

- [ ] 3.1 `gen_block_style.py`: CPU lights keep a 1 px glass margin; Crafting Station fronts with online lamp, still and animated working faces (`.mcmeta`); Pattern Storage fronts for lights 0 to 9 with lamp off and on. Regenerate the textures. Verify the generator reports no light touching the glass edge.
- [ ] 3.2 Crafting Station and Pattern Storage get `StorageNodeBlocks.ONLINE`, refreshed every 20 ticks from `ProductionView.online`; blockstate datagen for both; `:storage:runData`. Add GameTests that both lamps light on a line with a formed CPU and stay dark alone. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 4. Docs and integration

- [ ] 4.1 Note the lamps and the search clear in `docs/src/content/docs/mods/storage.md`; run the docs tests; full `gradlew build test` and `:storage:runGameTestServer` green.

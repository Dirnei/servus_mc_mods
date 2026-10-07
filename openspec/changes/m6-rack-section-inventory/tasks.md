# Tasks

## 1. Section walk

- [x] 1.1 Add pure `rack/RackSection.java` (D1) with JUnit `RackSectionTest`: lone bay, start at the left end, in the middle, at the right end, one sided flag does not join, limit stops the walk. Verify `gradlew :storage:test`.
- [x] 1.2 Add `HeavyRackBlock.section(BlockGetter, BlockPos)` on top of `RackSection` in `rack/HeavyRackBlock.java`. Verify `gradlew build`.

## 2. Section GUI

- [x] 2.1 `menu/HeavyRackMenu.java` takes the section's bays, lays out their slots in order, quick move across all bay slots, `stillValid` checks the section; `rack/HeavyRackBlock.java` opens it with the bay positions and slot counts in the buffer; `fromNetwork` rebuilds the layout with stand in handlers for missing bays (D3). Verify `gradlew build`.
- [x] 2.2 `client/HeavyRackScreen.java` draws the header, one row strip per row and the bottom part (D3). Verify `gradlew build`.
- [x] 2.3 GameTests in `gametest/ServusStorageGameTests.java`: a section of three gives 81 slots in left to right order, shift click spills into the next bay, breaking a bay spills only its items, the menu turns invalid after a bay is removed. Verify `gradlew :storage:runGameTestServer`.

## 3. Shuttle reach

- [x] 3.1 `shuttle/ShuttleFleet.java`: `Host.dockRails(NodePos)` replaces `railsBeside` in `canReach`, `canCarry`, `firstGoals` and the TO_TERMINAL, TO_TARGET and TO_STATION legs; `network/WarehouseNetwork.java` implements it with the front rails of a rack's section and every touching travel node for other blocks (D4). Verify `gradlew build`.
- [x] 3.2 GameTests: bread in the right bay of a section with only a left front rail reaches the terminal by shuttle; a lone rack beside that section without its own front rail is refused as needing a shuttle; a rack with a member rail only along its side is refused as needing a shuttle. Existing shuttle and rack tests stay green. Verify `gradlew :storage:runGameTestServer`.

## 4. Docs

- [ ] 4.1 `PLAN.md` section 2.3 item 1 and the M6 rack line: sections of the same facing and wood share one GUI, stacked racks stay separate, shuttles serve a section from any front rail. Verify `openspec validate m6-rack-section-inventory --strict`.

## 5. Verification

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, all GameTests green, the client launches with all four mods (from main, when the user asks). Commit.
- [ ] 5.2 Play check: a section of three opens one 9 row GUI from any bay, shift click fills bay after bay, breaking a bay spills only its items, a shuttle serves every bay of a section from one front rail, a side rail serves nothing, stacked racks keep separate GUIs.

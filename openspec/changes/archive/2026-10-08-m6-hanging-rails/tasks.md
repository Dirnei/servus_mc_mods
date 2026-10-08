# Tasks

## 1. Gap rule

- [x] 1.1 Add pure `RailSpan.java` in `storage/src/main/java/io/github/dirnei/mc/servus/storage/rail/` (`Kind` STANDING, HUNG, FREE; `check(start, kind, links, maxSpan)` returning holds and the drop set; walk stops once the span passes `maxSpan`). Add `RailSpanTest.java` in `storage/src/test/java/io/github/dirnei/mc/servus/storage/rail/` with: three free rails between two standing rails hold; four collapse; hung, free, hung holds; hung, free, free, hung collapses and drops all four; a hung rail with one free rail beside it collapses; a lone free rail with no links collapses; a gap with two holds on the same side of a T still needs span within the limit; the walk stops early on a long free run. Verify `gradlew :storage:test` is green.

## 2. Config

- [x] 2.1 Add `"rail_max_span": 3` to `storage/src/main/resources/data/servus_storage/storage/warehouse.json`, `DEFAULT_RAIL_MAX_SPAN` and `railMaxSpan()` to `storage/crate/StorageStatsManager.java`. Verify `gradlew :storage:build` is green.

## 3. Rail block

- [x] 3.1 `storage/rail/RailBlock.java`: classify a rail (standing below, hung beside an `ElevatorBlock` or a `HeavyRackBlockEntity` that accepts the rail on that side, free otherwise); `canSurvive` accepts standing or a horizontal neighbour that is a rail or an anchor; drop the floor check from `neighborChanged`. Verify `gradlew :storage:build` is green.
- [x] 3.2 `storage/rail/RailBlock.java`: schedule a tick in `onPlace` and `updateShape`; in `tick` run `RailSpan.check` for a free rail with `StorageStatsManager.INSTANCE.railMaxSpan()` and `level.destroyBlock(pos, true)` every rail of the drop set. Verify `gradlew :storage:build` is green.

## 4. GameTests

- [x] 4.1 `storage/gametest/ServusStorageGameTests.java`: open air refusal (and beside a rack side and a crate) through `BlockItem` use with the mock player; a rail hung on a raised rack front stays; a row of hung rails along five raised racks stays; a three rail bridge between two elevator shafts stays and links them; a three rail bridge between two raised facing racks stays; a fourth rail beside the middle of a three rail bridge drops all four; a four block gap cannot be bridged; a floor bridge (one rail off each edge, then the middle) stays and falls when one floor edge is broken; an overhang of one free rail drops both; breaking one rack of a bridge drops all three; breaking the floor under a rail in front of a rack leaves it hung. Verify `gradlew :storage:runGameTestServer` is green.

## 5. Plan and verification

- [x] 5.1 Update `PLAN.md` section 2.3: rails stand on a floor or hang on rack fronts and elevator segments and bridge gaps up to `rail_max_span`; slopes and ceiling rails stay out.
- [x] 5.2 Run `openspec validate m6-hanging-rails --strict`. Full `gradlew build` and `gradlew test` green, game launches with all four mods. Commit.
- [x] 5.3 Play check by the user: hang a rail on an upper rack front, bridge two elevators across three blocks from both ends, see a fourth rail collapse the bridge.

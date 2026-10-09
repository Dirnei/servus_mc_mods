# Tasks

## 1. Linking rules

- [x] 1.1 Add `NodeKind.RACK` to `storage/.../network/WarehouseConnectionRule.java` with the new table, `docksRail`/`holdsStock`/new `pullsNeighbours` and `refreshLinks` in `StorageNodeBlockEntity`, `kind()` RACK and `joinedTowards(side)` in `rack/HeavyRackBlockEntity.java`, the section check in the `WarehouseNetwork.TYPE` lambda, and link refreshes after the join mirror in `rack/HeavyRackBlock.java`. Add `storage/src/test/.../network/WarehouseConnectionRuleTest.java` (rack to rail, rack to rack, stock refuses rack, elevator refuses rack). Verify with `gradlew build` and `gradlew test`.
- [x] 1.2 Stop pulling around racks in `WarehouseNetwork` (`addCandidatesAround`, `rebuildCandidates`, foreign `addSource` skips storage nodes). Verify with `gradlew build`.

## 2. Tests

- [x] 2.1 Add GameTests to `storage/.../gametest/ServusStorageGameTests.java` (`frontRailConnectsSection`, `rackTouchingControllerNotListed`, `crateBehindRackNotLinked`): rack beside controller not listed, front rail connects a three bay section, side rail does not, breaking the rail disconnects, crate behind a rack not listed; rewire existing tests that relied on rack adjacency. Verify with `:storage:runGameTestServer` all green.

## 3. Docs

- [ ] 3.1 Update PLAN.md 2.3 network topology text and `docs/src/content/docs/mods/storage.md`. Verify with docs `npx vitest run`.

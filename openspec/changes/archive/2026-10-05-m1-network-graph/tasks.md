# Tasks

## 1. Positions and sides

- [x] 1.1 Add `Side` and `NodePos` in `lib/src/main/java/io/github/dirnei/mc/servus/lib/graph/` with JUnit tests `SideTest` (opposites, offsets, order matches DOWN, UP, NORTH, SOUTH, WEST, EAST) and `NodePosTest` (relative). Verify `gradlew :lib:test` passes.

## 2. Graph core

- [x] 2.1 Add `ConnectionRule`, `GraphChange` and `NetworkGraph` with `addNode` covering create, join and merge. JUnit `NetworkGraphAddTest`: isolated node, chain extension, bridging merge with survivor being the largest, asymmetric rule yields no edge, duplicate add throws. Verify `gradlew :lib:test` passes.
- [x] 2.2 Implement `removeNode` covering shrink, split and delete. JUnit `NetworkGraphRemoveTest`: ring stays one component, chain cut splits with largest keeping the id, three way split, last node removed, unknown position throws. Verify `gradlew :lib:test` passes.
- [x] 2.3 Implement queries (`componentOf`, `members`, `neighbors`, `componentIds`, `node`, `contains`) and `refresh`. JUnit `NetworkGraphQueryTest`: membership after merge, unknown position queries, refresh after a rule change splits a chain. Verify `gradlew :lib:test` passes.

## 3. Verification

- [x] 3.1 Full check: `gradlew clean build` green with all lib tests, `gradlew runClientAll` still reaches the title screen with four mods. Commit.

# Tasks

## 1. Foundation carriers

- [x] 1.1 `DataNode.dataCable`, `DataCarrier`, `DataMembers.self`, `DataNetwork` self probes, cable count and node skip in `foundation/src/main/java/.../foundation/data/`. Add `DataMembersTest.selfMembershipCountsOnceAndLeaves`. Verify with `gradlew :foundation:build` and `:foundation:runGameTestServer`.

## 2. Storage carriers

- [x] 2.1 Counter Terminal, Production Terminal, Crafting Station and Pattern Storage block entities hold a `DataCarrier` and join on load, leave on removal and unload; the Crafting CPU carries while formed (`onLoad`, `setBlockState`). Verify with `gradlew build`.
- [x] 2.2 GameTests in `storage/.../gametest/AdjacentDataGameTests.java`: `stackWithoutDuctIsOneNetwork`, `craftThroughStack`, `breakingMiddleSplits`, `chestDoesNotConnect`, `accessPointAndAdapterDoNotJoin`, `formedCpuJoinsStack`. Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer` and `:manufacturing:runGameTestServer` all green.

## 3. Docs

- [ ] 3.1 Storage docs page and PLAN.md 2.4: devices that touch share the data network. Verify with `npx vitest run` in `docs/` and `openspec validate m8-adjacent-data --strict`.

# Tasks

## 1. Pure logic

- [x] 1.1 Add `storage/.../rail/ShaftFooting` and `rail/ShaftTop` (no Minecraft imports) with JUnit `ShaftFootingTest` (`standsWhenAbove`, `descendingPasses`, `insidePasses`) and `ShaftTopTest` (`firstFreeAboveColumn`, `heightLimitStops`, `noSegmentNoWalk`). Verify with `gradlew :storage:test`.

## 2. Standing and sneaking down

- [ ] 2.1 `ElevatorBlock`: collision plate from `ShaftFooting`, `isScaffolding` returns true. Rework `elevatorHasNoCollision` and add `elevatorStandsFromAbove` in `ServusStorageGameTests`. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 3. Plain click builds up

- [ ] 3.1 Add `rail/ElevatorItem` (updatePlacementContext with `ShaftTop`, build height message) and use it in `ServusStorageItems.ELEVATOR`. GameTests `plainElevatorBuildsUp`, `plainElevatorBlockedTop`; the sneak tests stay green. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 4. Docs and plan

- [ ] 4.1 Storage docs page and PLAN.md elevator sentence. Verify with `npx vitest run` in `docs/`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, Storage GameTests green.

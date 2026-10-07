# Tasks

## 1. Closed fronts

- [x] 1.1 Lock the fronts in `InboundStationBlockEntity.java` and `OutboundStationBlockEntity.java` (back pushes, `machinePos()` behind), grey the front cell in both station screens, skip locked sides in `SideMarkerRenderer.java`, reword `PLAN.md` 2.4, and turn the outbound stations in `ServusStorageGameTests.java` with the new assertions. Verify `gradlew build` and `gradlew :storage:runGameTestServer` are green.

## 2. Verification

- [x] 2.1 Run `openspec validate m7-station-fronts-closed --strict`, `gradlew runClientAll` launches. Commit.
- [ ] 2.2 Play check: the front cells of both stations are greyed, no marker on the fronts with the carrot, an Outbound Station with its back to a machine feeds it.

# Tasks

## 1. Connection rule

- [ ] 1.1 Change `storage/src/main/java/.../storage/network/WarehouseConnectionRule.java` so STATION links only to RAIL and STOCK/GARAGE never to STATION; update `storage/src/test/.../network/WarehouseConnectionRuleTest.java` (`stationLinksOnlyToRails`, `stockAndGarageNeverLinkToStations`, symmetry stays). Verify with `gradlew :storage:test`.

## 2. GameTests

- [ ] 2.1 Add `storage/src/main/java/.../storage/gametest/StationRailGameTests.java` (station touching controller offline and keeps buffer, rail placed later connects and puts away, breaking the rail disconnects, crate touching only the station not linked, Access Point touching controller without rail is dark) and wire existing GameTests that relied on station adjacency with rails. Verify with `gradlew build` and the Storage GameTests, all green.

## 3. Docs

- [ ] 3.1 Update PLAN.md section 2.3 and `docs/src/content/docs/mods/storage.md` to say stations and Access Points join only through rails. Verify with `npx vitest run` in `docs/` and `gradlew build`.

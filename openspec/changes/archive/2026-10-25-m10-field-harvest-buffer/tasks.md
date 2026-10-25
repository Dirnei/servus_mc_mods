## 1. Pure logic

- [x] 1.1 `shuttles/.../field/HeadUnload.java`: fill the To storage buffer from the stacks a field shuttle brings back (same item slots with room first, then empty slots, locked slots take nothing) and keep the rest aboard; supply parts are still filled first by the existing `FieldRuns` path. Add `HeadUnloadTest`: wheat into empty slots; same item slot with room first; full buffer leaves the rest aboard; locked slots take nothing; free slot check. Check: `gradlew :shuttles:test`.

## 2. Field Head buffer

- [x] 2.1 `field/FieldHeadBlockEntity`: `toStorage` `LockedSlots` sized by `harvest_buffer_slots` (`stats/ShuttlesStatsManager`, `resources/data/servus_shuttles/shuttles/field.json` example 9), in `lockedSlots()`, saved and loaded (missing tag empty), dropped when broken. Check: `gradlew :shuttles:build`.
- [x] 2.2 `menu/FieldHeadMenu`, `client/FieldHeadScreen`: read-only To storage row with tooltip; lang in `datagen/ServusShuttlesLanguageProvider`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`, every payload type registered.

## 3. Field shuttles unload at the head

- [x] 3.1 `shuttle/FieldRuns`: Harvester and Spreader stores go to the head (`storeAtHead`) with `HeadUnload` instead of rack trips; what does not fit parks the shuttle (`StrandReason.HEAD_FULL`, garage lang) until the buffer has room; head lost mid run drops cargo at the garage. Check: `gradlew :shuttles:build`.

## 4. Put-away from the head

- [x] 4.1 `network/WarehouseNetwork`: generalise `drain` to a `PutAwaySource` implemented by `station/InboundStationBlockEntity` and `field/FieldHeadBlockEntity` (dock at the head's farm rail), head marks itself pending after an unload, blocked reason to the field work status; the fleet's put-away pickup resolves the source's dock rails (`shuttle/ShuttleFleet`). Check: `gradlew :shuttles:build`.
- [x] 4.2 Field job check waits for a free To storage slot; field work status "waiting for a shuttle to collect the harvest" (`field/FieldHeadBlockEntity` status, lang). Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 5. GameTests

- [x] 5.1 `gametest/HarvesterGameTests` and spreader tests: harvest lands in the head's To storage buffer and a plain shuttle brings it to the zoned rack, including a rack one floor up reached by the shaft; only Harvester shuttles leave the wheat in the buffer with the status; a full buffer queues no field job; buffer full mid run parks the harvester with the rest and it unloads next run; Spreader leftovers beyond the supply level go to To storage; the buffer survives a save and load; breaking the head drops it. Update tests that expected rack put-aways by the harvester. Check: `gradlew :shuttles:runGameTestServer`.

## 6. Plan and integration

- [x] 6.1 `PLAN.md`: nothing open for this; check the M10 list. Check: `openspec validate m10-field-harvest-buffer --strict`.
- [x] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green; then the user's play check: a basement farm with racks one floor up, harvest reaches the zoned rack through a plain shuttle, To storage row in the head GUI.

## Workflow follow-up

- Apply after `m8-wtp-egress`; archive after the three m8 changes once the user's play check passed; `shuttles/CHANGELOG.md` line under Unreleased at archive.

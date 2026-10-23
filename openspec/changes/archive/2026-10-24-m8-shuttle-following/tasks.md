## 1. Flaky field GameTests

- [x] 1.1 `HarvesterGameTests` and `FieldLevelGameTests`: replant checks accept wheat that is not ripe, failure messages print the crop ages. Check: `gradlew :shuttles:runGameTestServer` with `--tests servus_shuttles:harvestergametests.harvesterharvestsintostorage --repeatCount 100` green.

## 2. Pure traffic logic

- [x] 2.1 `shuttle/ShuttleTraffic`: directions, lanes, `Car`, node entry map, `limits` (room, leader, leader speed) with the distance and order priority and the garage yield rule, `rings`, `chainReaches`, `stepLoads`. JUnit `ShuttleTrafficTest`: follower on the same edge keeps the gap; opposite headings pass; a standing car blocks a same heading car and not an oncoming one; a car turning at a standing car's node is blocked; merge goes to the shorter distance, ties to the lower order; a perpendicular crossing passes; two in garage cars leave by order; an in garage car yields to a car that cannot stop and not to a far or standing one behind; out cars ignore in garage cars; rings found, tails left out; chain walk; step loads count same heading cars and stop ticks. Check: `gradlew :shuttles:test`.
- [x] 2.2 `shuttle/ShuttleDrive.glide` overload with a lead point that moves at a speed. JUnit in `ShuttleDriveTest`: a glide behind a standing lead stops at it; behind a lead at constant speed it settles to that speed and never passes the lead. Check: `gradlew :shuttles:test`.

## 3. Fleet

- [x] 3.1 `Shuttle`: `lastHeading`, `inGarage`, `room`, leader, leader speed, ring speed, follow state for syncs. `ShuttleFleet`: traffic snapshot once per tick before the shuttles move, binding leaders, rings and the lift exit rule; `drive` uses the room or the ring speed; no advance without room; `inGarage` set on park, recall, stranded park and arrival, cleared on the first move; restored shuttles start out. Check: `gradlew :shuttles:build`.
- [x] 3.2 Garage departure: in garage shuttles are hidden in `views` and wait for the home rail as the yield rule says. Check: `gradlew :shuttles:build`.
- [x] 3.3 `ShuttlesStatsManager` reads `follow_gap` and `travel_costs.traffic_per_shuttle`; `warehouse.json` gets both. `ShuttleFleet.costsFrom` and `path` add the step loads of a per search snapshot, leaving out the planned shuttle. Check: `gradlew :shuttles:build`.

## 4. Client

- [x] 4.1 `ShuttleView` gains `lead`; `views` fills it from the room and the leader's speed; sync at once when a shuttle's leader, binding or the leader's standing changes. `ShuttleGarageRenderer` glides with the lead and drops the parked shift. Check: `gradlew :shuttles:build`.

## 5. GameTests

- [x] 5.1 `ShuttleTrafficGameTests`: a fast shuttle follows a slow one in an aisle and never comes closer than the gap; opposite headings pass without slowing; a shuttle waits behind one standing at a bay and goes on after it; a merge at a junction goes to the closer shuttle; four shuttles in a full loop keep moving; two shuttles leave one garage one block apart; a busy aisle makes a planned path take the free one. Fix existing fixed tick GameTests honestly where travel got slower. Check: `gradlew build`, `gradlew :shuttles:test`, `gradlew :shuttles:runGameTestServer` green three times in a row.

## 6. Plan

- [x] 6.1 `PLAN.md`: note what this change decided under Open: Shuttles, keep the series lines; changelog lines go in at archive. Check: `openspec validate m8-shuttle-following --strict`.

## 7. Play check

- [x] 7.1 User play check: several shuttles in one aisle follow with a gap and pass the other way; a shuttle waits behind one loading at a bay; two shuttles leave a garage one after the other; nothing jumps sideways or back when a shuttle stops or starts behind another; a lift exit next to a bay works.

## 1. Pure logic

- [x] 1.1 `shuttles/.../shuttle/ShuttleMotion.java`: `restored(path, segment, progress)` (clamps segment and progress, a path of one node is parked). `shuttles/.../shuttle/Settle.java` (countdown that counts only while ready, starts over when not). `shuttles/.../network/OrphanSweep.java` (generic, releases keys unknown at two sweeps in a row). JUnit: `ShuttleMotionRestoreTest` (keeps segment and progress, clamps out of range, one node path arrived), `SettleTest` (counts while ready, restarts, done once), `OrphanSweepTest` (known kept, unknown once kept, unknown twice released, known again resets). Check: `gradlew :shuttles:test`.

## 2. Job ids

- [x] 2.1 `CounterData` replaces `ShuttleNumberData` (kinds shuttle numbers and job ids, same saved name and key for numbers); `ShuttleItem` uses it. `ShuttleJob.id()`; `int id` first component of `PickJob`, `PutAwayJob`, `FieldJob`, `FertilizeJob`, `SupplyJob`, `FluidJob`; `WarehouseNetwork.nextJobId()` at every construction; `ShuttleFleet.knows(id)`. Check: `gradlew :shuttles:build`.

## 3. Save and restore

- [x] 3.1 `shuttles/.../shuttle/FleetSaves.java`: jobs, loads, motion and field runs to and from compound tags. Check: `gradlew :shuttles:build`.
- [x] 3.2 `ShuttleFleet`: `saveGarage`, `restoreGarage`, `saveQueue`, `restoreQueue`, `releaseGarage(pos, forGood)`, `clearQueue`; restored shuttles settle, then drive on, re-route, go home or are recalled; field runs resume through `FieldRuns.resume`; assignment waits after a restore; `Host.stateChanged` for out garages and queue changes. Check: `gradlew :shuttles:build`.
- [x] 3.3 `StorageNodeBlockEntity`: `Warehouse` tag stash, `leavingForGood` via `setRemoved`, save from the live network. `WarehouseNetwork`: `saveState`, restore in `onNodeAdded`, stash or drop in `onNodeRemoved`, held loads saved and dropped at a broken destination, `stateChanged`. Check: `gradlew :shuttles:build`.
- [x] 3.4 `InboundStationBlockEntity`: claims hold job ids, saved as `Claims`; `WarehouseNetwork` claims with the job id and sweeps orphan claims while online and the site span is loaded. Check: `gradlew :shuttles:build`.

## 4. Edge of loaded chunks

- [x] 4.1 `ShuttleFleet.tickShuttle` holds at unloaded chunks; `refreshHome` keeps a home whose chunk unloaded; `WarehouseNetwork.deliverTo` keeps cargo for an unloaded destination. Check: `gradlew :shuttles:build`.

## 5. Tests

- [x] 5.1 GameTests in `shuttles/.../gametest/FleetPersistenceGameTests.java`: job ids count up; a garage reloaded mid put-away delivers the cargo to the rack; a controller reloaded with queued jobs keeps them and serves them; a station reloaded keeps its claim and makes no second job; an orphan claim is released; held loads survive a destination reload and drop when it is broken; an old garage, controller and station tag without fleet state loads empty; a restored shuttle whose rack is gone ends with the cargo at its garage; a harvester reloaded with wheat aboard puts it away. `PersistGameTests.reload` unloads the old block entity first, as a chunk unload does. Check: `gradlew :shuttles:test` and `gradlew :shuttles:runGameTestServer` green.

## 6. Plan and changelog

- [x] 6.1 `PLAN.md`: nothing to remove until archive; check the shuttle job series line still matches. `shuttles/CHANGELOG.md` gets its line at archive. Check: `openspec validate m8-job-persistence --strict`.

## 7. Play check

- [x] 7.1 User play check: send shuttles on put-aways, picks and a harvester run, quit and reload the world mid trip, fly away until the warehouse unloads and come back; cargo arrives, nothing drops, station slots are not planned twice.

## 1. Pure logic

- [x] 1.1 `shuttles/.../lock/SlotFill.java`: `sharedRoom(limit, count, reservedIn)` and `sharedAvailable(count, reservedOut)` for counted slots. Extend `SlotFillTest`: room less other jobs' IN, never below 0, unbounded limit, available less other jobs' OUT. Check: `gradlew :shuttles:test`.
- [x] 1.2 `shuttles/.../field/FieldCargo.java`: `stow(entry, putBack)` adds an entry, hands what does not fit to the put back function and returns what that refused. Extend `FieldCargoTest`: all fits, overflow put back, overflow refused by the source comes back, partial put back. Check: `gradlew :shuttles:test`.

## 2. Counted locks and the head buffer

- [x] 2.1 `shuttles/.../lock/LockedSlots.java`: counted mode (IN and OUT side by side, inserts and extracts by others limited to unreserved room and items), `reserved`, per slot `insertAs`/`extractAs`, `insertAllAs` (per item, release once), `extractAllAs` (a stack per item). Check: `gradlew :shuttles:build`.
- [x] 2.2 `shuttles/.../field/SupplyBuffer.java` on a counted `LockedSlots(3)` with unbounded capacity, old save format plus `Locks`, quiet load; `FieldHeadBlockEntity` implements `LockHolder`, `supplyRoom` less reserved room, `supplyAvailable`, `fillSupplyAs`, `takeSupplyAs`, `lockSupplyIn`, `lockSupplyOut`, `releaseSupply`, public `warehouse()`. Check: `gradlew :shuttles:build`.

## 3. Field runs with locks

- [x] 3.1 `ShuttleFleet.Host` and `WarehouseNetwork`: `lockStock`, `takeLocked`, `unloadAllInto`; drop `putAwayTarget`, `sourcesHolding`, `extractFrom` from the host interface; `servedHeads()` for `tickFields` and `sweepLocks`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `FieldRun` (`stops`, `locked`, no `storeTarget`/`waitTimer`), `SupplyRun` (`sources`, no `keys`/`remaining`/`reserves`/`sourcesTried`), `FieldRuns`: supply planning locks racks and head, fetch from locked sources, `stow` with `FieldCargo.stow`, unload at the head as the job; Harvester and Spreader lock what they fetch from the head; `addOrStore`; `continueStore` with `storeTargets`, `unloadAllInto`, re-route and `strand`; release on run end. Check: `gradlew :shuttles:build`.
- [x] 3.3 `ShuttleFleet`: `strand` package visible and its reason counting field run stacks, `retryStranded` hands field shuttles to `FieldRuns`, `dropFieldRun` releases the run's locks; `Leg.WAIT_STORE` removed; `FleetSaves` saves `Stops`, `Locked`, `Sources` and reads `WAIT_STORE` as `STRANDED`. Check: `gradlew :shuttles:build`.

## 4. GUI

- [x] 4.1 `MenuLocks.ofInventory`, `FieldHeadMenu` implements `LockedMenu` and syncs the buffer's locks; `LockOverlay` exposes its washes and `line`; `FieldHeadScreen` washes locked buffer cells and adds the lock lines to the buffer tooltip. Check: `gradlew :shuttles:build`, every payload type registered.

## 5. Tests

- [x] 5.1 GameTests `FieldLockGameTests`: a supply run locks the head part (IN) and the rack slot (OUT) before it leaves and leaves no lock when done; reserved room in the head refuses spare seeds and other inserts, reserved items refuse other takes, a locked head survives a save and load; harvest goes into the rack holding the item over a nearer empty one and the rack slot holds an IN lock on the way; a full warehouse parks the harvester stranded in its garage with the lamp and it stores once room appears; a harvest overflow is stored, nothing lies on the ground. Check: `gradlew :shuttles:runGameTestServer`.
- [x] 5.2 Fix existing field GameTests honestly where timing, chosen racks or the waiting place changed (`HarvesterGameTests`, `SpreaderGameTests`, `FieldSupplyGameTests`, `FleetPersistenceGameTests`). Check: `gradlew build`, `gradlew :shuttles:test`, `gradlew :shuttles:runGameTestServer` green twice.

## 6. Plan

- [x] 6.1 `PLAN.md`: note what this change built and decided in the shuttle job series lines; `shuttles/CHANGELOG.md` line goes in at archive. Check: `openspec validate m10-field-head-locks --strict`.

## 7. Play check

- [x] 7.1 User play check: a supply run shows the IN wash on the head's Stock tab buffer cell with the shuttle in the tooltip and the OUT wash on the rack slot; a harvester stores into the rack already holding its crop; with a full warehouse the harvester parks in its garage with a red lamp and the GUI line, and drives out once a rack has room; nothing drops on the field.

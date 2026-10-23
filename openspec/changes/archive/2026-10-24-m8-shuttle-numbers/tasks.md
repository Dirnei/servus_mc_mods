## 1. Plan

- [x] 1.1 PLAN.md: add the open points of the shuttle job series under "Open: Shuttles" (`m8-job-persistence`, `m8-slot-locks`, `m8-shuttle-following`, `m10-field-head-locks`, one line each). Check: `openspec validate m8-shuttle-numbers`.

## 2. Pure logic

- [x] 2.1 `shuttles/.../shuttle/ShuttleCounter.java` (last given number, `take()`, `last()`) and `shuttles/.../shuttle/ShuttleLabelForm.java` (enum `NUMBERED_NAMED`, `NUMBERED`, `NAMED`, `PLAIN`, `of(plate, number)`). JUnit: `ShuttleCounterTest` (starts at 1, counts up, continues from a loaded last), `ShuttleLabelFormTest` (all four forms). Check: `gradlew :shuttles:test`.

## 3. Numbers on the item

- [x] 3.1 `ServusShuttlesComponents`: `SHUTTLE_NUMBER` component; `shuttles/.../shuttle/ShuttleNumberData.java` SavedData on the overworld wrapping `ShuttleCounter`; `ShuttleItem.number(stack)` and `ShuttleItem.numberIfMissing(ServerLevel, stack)`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `ShuttleGarageBlockEntity`: number a slot's shuttle in `onContentsChanged` on the server before telling the network, and every slot in `onLoad` before joining. Check: `gradlew :shuttles:build`.

## 4. Label

- [x] 4.1 `ShuttleItem.label(stack)` from `ShuttleLabelForm`; `getName` returns the label; numbered shuttles get a tier line first in the tooltip. Lang in `ServusShuttlesLanguageProvider` (`shuttle.servus_shuttles.label`, `shuttle.servus_shuttles.label.named`), then `gradlew :shuttles:runData`. Check: `gradlew :shuttles:build`.

## 5. Tests

- [x] 5.1 GameTests in `shuttles/.../gametest/ShuttleNumberGameTests.java` (registered like the other shuttle GameTests): two shuttles into a garage get consecutive numbers; taken out and put into another garage keeps the number and the counter does not move; a garage loaded from a tag with unnumbered shuttles numbers them in slot order; label keys and arguments for plain, engraved and unnumbered shuttles; a tier build keeps the number. Check: `gradlew :shuttles:test` and `gradlew :shuttles:runGameTestServer` green.

## 6. Play check

- [x] 6.1 User play check: put shuttles into a garage of the existing dev world and a new one, hover them in the garage and inventory ("Shuttle #N", "Name (#N)", tier line), take one out and back, reload the world.

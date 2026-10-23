# Tasks

## 1. Server paths

- [x] 1.1 `shuttles/.../shuttle/ShuttleFleet.java`, `Garage.java`: park in the garage node through one `homeNode` helper; home paths end there, paths from inside start there; `ways(shuttle)` adds the private step to the home rail; saved shuttles parked on the home rail load into the garage node. Add JUnit for the pure path parts touched. Run `gradlew :shuttles:test`.
- [x] 1.2 Door time: `door_ticks` stat in the warehouse stats and `warehouse.json` (example 10); a shuttle starting from its garage node waits it as a stop; `syncPending` on leaving and parking; `views()` sends a shuttle waiting inside. Run `gradlew :shuttles:build`.
- [x] 1.3 GameTests: `shuttleParksInsideItsGarage`, `shuttleWaitsTheDoorTimeBeforeLeaving`, `noShortcutThroughAGarage`; update existing GameTests whose timing or end position changes. Run `gradlew :shuttles:runGameTestServer`.

## 2. Look

- [x] 2.1 Garage model with the 12 by 12 doorway (datagen, frame textures over wood board, inner walls), collision shape following the walls; list overlapping boxes and shared planes. Run `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [x] 2.2 `client/ShuttleGarageRenderer.java`, `ClientShuttles.java`: shutter openness per garage and its slats; a shuttle that came home kept drawn inside facing in until the shutter is down; a waiting shuttle drawn inside facing out. Run `gradlew :shuttles:build`.

## 4. Play check fix

- [x] 4.1 A shuttle on its home leg within the shutter's opening range of its doorway (the distance `GarageShutter` opens for an incoming shuttle) is not free for new jobs: it parks first and is free on the next tick, then leaves with the door time. Shuttles further from home stay free as today. Pure check in `GarageShutter` or the doorway helper with JUnit (`pullingInIsNotFree`, `farFromHomeIsFree`); GameTest `jobWhilePullingInWaitsForParking`. Run `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer`.

## 3. Integration

- [x] 3.1 Full `gradlew build`, `gradlew test` and `gradlew :shuttles:runGameTestServer` green; launch the game with all mods and check: leaving and coming home look smooth with no pop-in or visible turn, the shutter opens and closes in time, a garage with several shuttles, no flicker on the shutter and the tunnel.

## Workflow follow-up

- Archive with a Fixed line (smooth garage exits) and an Added line (roll-up door) in `shuttles/CHANGELOG.md`.

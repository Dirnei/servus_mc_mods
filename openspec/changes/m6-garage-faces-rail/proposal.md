# Proposal

## Why

A Shuttle Garage turns its front to the player who places it, so its doorway only meets the rail when the player happens to stand on the far side. Placed from the aisle against a rail, or beside a rail from any angle, it often ends up with the doorway sideways and has to be turned with the carrot.

## What Changes

- Placing a garage by clicking the side of a warehouse rail turns the doorway (back) towards that rail.
- Placing a garage anywhere else turns the doorway towards a warehouse rail beside it: the rail in the direction the player looks if there is one there, otherwise the only rail beside it.
- With no rail beside it, or several and none in the look direction, the front faces the player as today.
- The carrot still turns a placed garage.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/shuttle-garage`: "Doorway at the back" says how the garage picks its facing on placement.

## Impact

- `shuttles/.../garage/ShuttleGarageBlock.java` (`getStateForPlacement`), a new pure `garage/GarageFacing.java` with JUnit tests, GameTests for placement.
- No data, recipe or model changes. Placed garages keep their facing.

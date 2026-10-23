# Proposal

## Why

Shuttles leave and enter their garage in a way that reads as lag: a parked shuttle sits hidden on the home rail in front of the garage, so a leaving shuttle pops up on that rail one to five ticks late, already moving, and a returning one brakes to a stop on the rail and vanishes. The garage's doorway is only painted on, and nothing ever drives through it. Reported in play testing; the agreed look is the mock `temp/garage-door.html`. PLAN.md M6, warehouse automation (shuttles and garages).

## What Changes

- **Real doorway:** the garage model gets an opening 12 by 12 pixels on its back where the doorway is painted today, a short tunnel with the rail running in.
- **Paths through the doorway:** a parked shuttle stands inside its garage's cell. A shuttle leaving starts there and drives out through the doorway onto the home rail; a shuttle coming home drives through the doorway and stops inside. The step between the garage and its home rail belongs only to that garage's shuttles.
- **Door time:** before a shuttle drives out it waits a short time inside while the door opens (example 10 ticks, from `warehouse.json`).
- **Roll-up shutter:** steel slats close the opening and roll up into the lintel when a shuttle is about to pass and down once it is through, either way. The door is drawn by the client from where the shuttles are; nothing is saved or sent for it.
- **No pop-in:** the server sends the garage's shuttles the moment one starts to move or comes home, not only at the next sync interval.
- **Turning unseen:** a shuttle that came home stays drawn inside, facing in, until the shutter is down; a leaving one is drawn inside facing out from when the shutter starts to open.

## Non-goals

- Other door styles or a door on other blocks.
- Changes to how many shuttles a garage holds or how jobs are handed out.
- Animating several shuttles queued inside one garage; only the one passing the doorway is drawn.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/shuttle-garage`: home rail and doorway: shuttles park inside and pass through a real doorway with a roll-up shutter.
- `shuttles/shuttles`: how shuttles are drawn leaving and coming home.

## Impact

- `shuttles/.../shuttle/ShuttleFleet.java` (parked position, ways, home paths, door wait, instant sync), `Garage.java`, `Shuttle.java`, `ShuttleTraffic.java` if the doorway step needs a lane.
- `shuttles/.../client/ShuttleGarageRenderer.java` (shutter, lingering shuttle), `ClientShuttles.java`.
- Garage model datagen (`ServusShuttlesBlockStateProvider` or the models class), collision shape of the garage block.
- `warehouse.json` stats (`door_ticks`), GameTests for trip times, `shuttles/CHANGELOG.md` at archive.

# Proposal

## Why

Shuttles take bends at full speed. A Mk3 with eight Speed upgrades sweeps round a corner at almost ten blocks a second, which looks wrong next to the careful braking it does at the end of a leg. Slowing by a share of the top speed would still leave fast shuttles flying round corners, so the limit should be a fixed corner speed that faster shuttles beat only a little.

## What Changes

- A shuttle drives through a corner of its path (a node where the way turns) at most at its corner speed, braking before the corner at its normal rate and speeding up again after it.
- The corner speed is an absolute number of blocks per tick from the shuttle stats file, plus a small share per Speed upgrade (examples `corner` 0.1 and `corner_per_upgrade` 0.005), and never more than the shuttle's top speed. A plain shuttle (0.15) slows a little; a Mk3 with eight Speed upgrades (0.47) slows to 0.14.
- Clients draw the same slowing down, so the drawn shuttle stays where the server has it.
- Straight runs, junctions passed straight through, lift rides and the end of a leg behave as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/shuttles`: "Shuttles speed up and brake" gains the corner speed.

## Impact

- `shuttles/.../shuttle/ShuttleDrive.java` (corner limit, pure), `ShuttleFleet.drive`, `shuttle/module/ShuttleTiers.java`, `ShuttleProfile.java`, `stats/ShuttleStatsReader.java`, `data/servus_shuttles/shuttles/shuttle.json`.
- `shuttle/ShuttleView.Drive` carries the corner speed to clients; `client/ShuttleGarageRenderer` passes the path's corners to `ShuttleDrive.glide`. `ShuttleView` was last changed by the unarchived `m13-tanker-import` (sight glass); this change touches only the `Drive` record.
- Runs take slightly longer on winding rails; balance numbers stay in `shuttle.json`.

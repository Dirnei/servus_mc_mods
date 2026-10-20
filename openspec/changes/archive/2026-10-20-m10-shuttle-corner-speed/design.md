# Design

## Context

Paths: `S` = `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles`.

- `S/shuttle/ShuttleDrive` (pure) holds the speed rule: `next(speed, top, accel, toStop)` speeds up by `accel`, caps at `top`, brakes with `sqrt(2 * accel * toStop)` and crawls the last bit. `toStop` is the distance to the first vertical step or the path end. `glide(...)` replays `next` tick by tick for clients.
- `S/shuttle/ShuttleFleet.drive` calls `toStop` and `next` with `profile.speed()` and `profile.accel()`; paths are lists of `NodePos`, one block per segment.
- `S/shuttle/ShuttleView.Drive(speed, top, accel)` goes to clients; `S/client/ShuttleGarageRenderer.glide` calls `ShuttleDrive.glide` with the path's vertical steps. The renderer moves in straight lines between nodes and snaps the heading per segment.
- `S/shuttle/module/ShuttleTiers` reads `speed.base`, `per_upgrade`, `accel`, `accel_per_upgrade` from `shuttle.json` (`S/stats/ShuttleStatsReader`) with defaults; `ShuttleProfile.of` builds a shuttle's numbers from its tier and Speed count. `LiftCar` uses `ShuttleDrive.next` for the car and is not changed.

## Decisions

### D1. The corner limit is an extra speed cap from the path ahead

A corner is an interior node `i` whose step in (`i - 1` to `i`) and step out (`i` to `i + 1`) are both horizontal and point in different directions. Pure `ShuttleDrive.cornerLimit(nodes, segment, progress, IntPredicate corner, double cornerTop, double accel)` returns the highest speed allowed now for the corners ahead: for each corner node at distance `d = (i - segment) - progress` blocks, the zone is `d - 0.5` to `d + 0.5`; inside the zone the limit is `cornerTop`, before it the speed `v` with `v * v + 2 * accel * v <= cornerTop^2 + 2 * accel * (d - 0.5)`, from which braking at `accel` reaches `cornerTop` at the zone even counting the step `v` itself, so a step that ends inside the zone is never faster than `cornerTop` (settled during apply: the plain `sqrt(cornerTop^2 + 2 * accel * gap)` let the step into the zone run about 0.01 too fast). All corners share one corner speed, so the nearest corner ahead is the tightest and the only one looked at (`+inf` without one, or with a corner speed of 0, which the lift car view sends).

`ShuttleFleet.drive` passes `min(profile.speed(), cornerLimit(...))` as `top` to `next`, so the existing braking and crawl rules stay as they are. `glide` gets the corner predicate and `cornerTop` and does the same each tick, so clients follow the server.

A percentage of the top speed is rejected: a Mk3 would still take corners at three times the speed of a plain shuttle.

### D2. Numbers

`shuttle.json` `speed` gets `corner` (0.1) and `corner_per_upgrade` (0.005). `ShuttleTiers.cornerSpeed(tier, count) = min(speed(tier, count), max(accel(tier, count), corner + cornerPerCount * clamp(count)))`: never above the top speed, and never below one acceleration step so the crawl rule cannot push past it. Defaults match the example for packs whose file has no `corner` keys. `ShuttleProfile` gets `cornerSpeed`.

With the example numbers a plain shuttle (top 0.15) slows to 0.10, a Mk2 with four upgrades (0.31) to 0.12, a Mk3 with eight (0.47) to 0.14.

### D3. Clients

`ShuttleView.Drive` gets `corner` (float) after `accel` in its stream codec; `Drive.STILL` sets it to 0. `ShuttleGarageRenderer.glide` builds the corner predicate from the view's path (`BlockPos`, same rule as D1) and passes `drive.corner()`.

## NeoForge APIs

None new: pure Java and an extra field in an existing payload record codec (`ByteBufCodecs.FLOAT`).

## Pure and Minecraft

- Pure, JUnit: `ShuttleDrive.cornerLimit`, `next` with the limit, `glide` through a corner, `ShuttleTiers.cornerSpeed` and the stats reader default.
- Minecraft, GameTests: a fast shuttle on an L shaped rail is at or below its corner speed on the corner rail and above it on the straight before; a straight run keeps top speed.
- Manual: the drawn shuttle slows into corners without jumps.

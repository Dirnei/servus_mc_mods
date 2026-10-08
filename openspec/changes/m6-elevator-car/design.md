# Design

## Context

See proposal.md for the motivation and the specs for behavior. This is the code as it stands:

- `ElevatorBlock` extends `ConnectableBlock` with radius 4. Its `getShape` (posts plus the rail plate) also acts as the collision shape. The blockstate is a multipart: `elevator_frame` (posts), an unconditional `rail_center`, and a `rail_arm` per connected side. `ConnectableBlock` already has `UP` and `DOWN` properties, set when a segment is above or below.
- `ElevatorBlockEntity` is a connector node (`NodeKind.ELEVATOR`) with no state.
- `ShuttleFleet` lives in `WarehouseNetwork`. Shuttles travel on `ShuttleMotion` paths from `RailPaths.find` (BFS over rail and elevator nodes). `tick()` advances every moving shuttle by `shuttleSpeed` and calls `arrive` at the end of a leg. Sync goes out via `ShuttleSyncPayload` per garage. `ShuttleGarageRenderer` (infinite bounds) extrapolates each `ShuttleView` from its path, segment, progress and speed. A vertical step is just a vertical interpolation, and `headingAfter` returns `atan2(0, 0)`, which is yaw 0.
- `WarehouseNetwork.onNodeAdded` and `onNodeRemoved` are the membership events. Elevator nodes land in `connectors`.
- Shuttles are not saved mid job. On load they are all home.

## Goals / Non-Goals

**Goals:**
- The lift logic is pure Java (`LiftCar`, `Shafts`, `ShaftSteps`, `Flaps`), fully covered by JUnit. `ShuttleFleet` only wires it in.
- No block scanning on the server: shafts come from the network's elevator node set, rebuilt on membership events.
- One renderer per shaft for car and decks. The block model holds only static parts.

**Non-Goals:**
- Saving the car's position. Shuttles are not saved mid ride either.
- Any change to route choice. `RailPaths` stays as it is.

## Decisions

### PLAN.md decisions settled
PLAN.md section 2.3 says "a shaft of stacked segments ... that shuttles will ride vertically". This change settles how they ride: one car per shaft, a FIFO call queue, one shuttle per trip, folding decks, and sideways crossing without the car. Section 2.3 gets one paragraph about this. No item in PLAN section 5 is touched.

### Shafts: pure grouping of elevator nodes
`Shafts.group(Set<NodePos>)` (pure) groups elevator nodes by column (x, z) into maximal runs of consecutive y. A run is a `Shaft(bottom, topY)`. `WarehouseNetwork` keeps `elevators` (a subset of connectors, added and removed in the node events) and sets `shaftsDirty`. At the start of the next tick it calls `fleet.refreshShafts(Shafts.group(elevators))`.

- **Alternative:** walk the column with `level.getBlockState` when needed. This was rejected because of the no-scan rule, and because the network already knows its nodes.

### LiftCar: one pure state machine per shaft
`LiftCar` (pure, `storage/shuttle`) holds:
- `bottomY` and `topY`;
- `double y`;
- `Integer target`;
- `Integer rider` (shuttle id);
- `ArrayDeque<Call(id, floor)>`;
- `Map<Integer, Integer> crossers` (id to floor).

Its API:
- `call(id, floor)` is idempotent.
- `canBoard(id, floor)` is true when the car is idle at that floor, has no rider, and the id is first in line.
- `board(id)` and `send(id, floor)`.
- `arrivedFor(id, floor)` and `leave(id)`.
- `canCross(floor)` is true when the flaps there are fully down, or the car is idle at that floor with no rider.
- `beginCross(id, floor)` and `endCross(id)`.
- `tick(speed)`: with a rider and a target, the car moves to the target. Without a rider, it moves to the first call's floor. It does not start or continue a move while a crosser stands on a floor inside the sweep ahead of it.
- `resize(bottomY, topY)` clamps `y`.
- `remove(id)` drops a call, a ride or a crossing.

Shuttle ids are `int`s handed out by `ShuttleFleet` (a counter on `Shuttle`).

The car serves one rider at a time. It does not stop for calls on its way: strict FIFO, matching "the car serves calls in order".

**Alternative:** collective (elevator style) stopping on the way. It was rejected because one shuttle per trip makes pickups on the way pointless, and FIFO is easy to test and to explain.

### ShaftSteps: classifying path steps
`ShaftSteps` (pure) takes a path and a `shaftOf` lookup (`Function<NodePos, Shaft>`, null for rails) and classifies step `i` (node `i` to node `i+1`):
- `ENTER_RIDE`: rail to segment, and the path continues vertically from that segment or ends there.
- `ENTER_CROSS`: rail to segment, and the path leaves the segment sideways.
- `RIDE`: segment to segment, vertical.
- `EXIT`: segment to rail.
- `PLAIN`: rail to rail.

`rideEnd(path, i)` returns the index of the last node in that vertical run.

### ShuttleMotion gains a gate and a skip
- `advance(blocks, IntPredicate mayStart)` checks `mayStart(segment)` before it begins a segment (when progress is 0). When the gate says no, the shuttle stops on that node and `advance` reports "held".
- `skipTo(index)` jumps to a node with progress 0. The existing `advance(blocks)` delegates with an always-true gate.

### Fleet wiring
- `ShuttleFleet` keeps `Map<BlockPos, LiftCar> lifts` keyed by shaft bottom, and `Host.shaftOf(NodePos)`.
- Each moving shuttle advances with a gate that looks at the next step:
  - **`ENTER_RIDE`:** `call` the car to the segment's floor. Pass when `canBoard`, then `board`. The shuttle waits on the rail outside, so the deck it would stand on is never under a car that is arriving.
  - **`RIDE`:** never passed by driving. When the shuttle is the rider, the fleet `send`s the car to the floor of `rideEnd`. The shuttle is then marked `riding` and holds. When `arrivedFor` is true, `skipTo(rideEnd)` puts it at the exit node.
    - A shuttle that starts a leg on a segment without being the rider (a home rail that is a segment) first calls the car to its own floor and boards when `canBoard`.
  - **`ENTER_CROSS`:** pass when `canCross`, then `beginCross`. `endCross` runs when the shuttle reaches the rail on the far side.
  - **`EXIT`:** passes. The rider `leave`s when it reaches the rail beside the shaft, so the car does not drive off under a shuttle that is half way out.
- A leg that ends on a segment keeps the car. The next leg starts there as the rider.
- Waiting shuttles sync with speed 0, so `ShuttleGarageRenderer`'s extrapolation stands still.
- `LiftCar.tick(liftSpeed)` runs once per fleet tick for every car, before the shuttles move.

### Shaft changes and breaks
- `refreshShafts` keeps a car whose bottom is unchanged and only `resize`s it.
- A shaft that vanished, or was cut so a rider's or caller's floors are gone, loses its car. Every shuttle that was riding or calling it gets the `dropCargo` treatment (a loaded pick counts as missing, and an unloaded job goes back to the front of the queue), then `goHome`. When no path home exists, it is `park`ed at its garage home.
- Shafts without a car get a new one at their bottom. Crossers simply carry on.
- **Alternative:** keep cargo and re-route. It was rejected: the user chose "drops its cargo", and re-routing mid ride would need a path from inside a broken shaft.

### Fleets merge and split
- `absorb(other)` takes the other fleet's lifts.
- A split network gets a fresh fleet, and its shafts get cars on the next `refreshShafts`. A rider in a part that left goes home with its cargo dropped, the same as a break.

### Sync: `LiftSyncPayload`
`LiftSyncPayload(BlockPos bottom, int topY, float y, int target, float speed, boolean carrying)` goes to `PacketDistributor.sendToPlayersTrackingChunk` for the bottom's chunk. It is sent when a car starts or stops moving, and every `SYNC_INTERVAL` ticks while it moves, the same pattern as `ShuttleFleet.send()`.

A tracking player that joins later gets the current state the next time the car moves. For idle cars, the elevator block entity also writes `liftY` into its update tag (`getUpdateTag` and `getUpdatePacket` via `ClientboundBlockEntityDataPacket.create`, the vanilla way). The fleet sets it on the bottom block entity when a car parks, so chunk loading shows the parked car.

The client keeps `ClientLifts` (`Map<BlockPos, Lift>` with the receive tick). The payload is registered in `ServusStoragePayloads` next to `ShuttleSyncPayload.TYPE`.

`ShuttleView` gains `Optional<BlockPos> lift`, the shaft bottom when the shuttle rides. `ShuttleGarageRenderer` then draws it at the extrapolated car height in that column, so one renderer still draws every shuttle.

### Rendering
- **Block model (datagen, `ElevatorModels`).** The multipart has `elevator_frame` (posts only, always) and `elevator_head` when `up=false` (a cap beam over the posts with a motor box). `rail_center` and `rail_arm` are removed from the elevator blockstate. The decks fold, so they belong in the renderer.
- **`ElevatorRenderer` (`BlockEntityRenderer<ElevatorBlockEntity>`).**
  - It renders only when `down=false`, which marks the shaft's own bottom.
  - It walks the column up while the block is an `ElevatorBlock`. This is client side, a few block lookups per frame and only for bottoms.
  - For each segment with any horizontal arm it draws a deck. The deck plate lies at y -1 to 0 px (inside the block below, hidden by the floor on the ground level) and the rail pieces at y 0 to 1 px, flush with the outside Warehouse Rail. Flaps hinge only on edges without an arm, on axle bars between the posts, pivoting at the plate's lower outer edge so a folded flap stands outside the car's footprint. Arms only on the west and east (or one of them): two flaps hinged north and south. Arms only on north and south: two flaps hinged west and east. Arms on both axes with a free edge: one full flap on the first free edge. Arms on all four sides: four quarter plates that shrink towards their corner posts. Each part moves by `Flaps.angle(carY, floorY)`. Decided in the play check: the first layout hinged the flaps on the arm edges, so a folded flap stood in the shuttle's way and the rail sat 1 px above the outside rail.
  - It draws the car platform (frame at y -2 to 0 px, rail at 0 to 1 px) with four sleeves (3x3 px around the 2x2 px posts) and the rail pattern of the floor it is nearest to.
  - `getRenderBoundingBox` covers the column, and `shouldRenderOffScreen` returns true, as for the garage.
  - Parts are drawn as textured boxes from the `elevator` and `rail` block atlas sprites via `RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)`. That vertex format carries the overlay a block entity renderer passes, and without culling a flap shows from both sides whichever way it is folded. This avoids extra baked models.
- **`Flaps.angle(carY, floorY)` (pure).** It returns 90 degrees (up) when `|carY - floorY| <= 0.5`, 0 degrees when `|carY - floorY| >= 1`, and linear between the two. A car parked one floor away therefore leaves that deck down, as the "Parked car" scenario requires, and a parked car does not block crossings on its neighbour floors. A shuttle on a car rising from below reaches deck height at about half a block, where the flaps are already up. `LiftCar.canCross` uses the same function (crossing needs 0 degrees), so the server and the drawing agree.
- **Heading fix.** `headingAfter` skips vertical segments and uses the last horizontal one, or keeps the previous yaw.

### Collision and climbing
- `ElevatorBlock.getCollisionShape` returns `Shapes.empty()`. `getShape` (the outline) returns `Shapes.block()`, so the segment is easy to target, like scaffolding. Ray clips from inside a box do not hit it, so a player in the shaft still targets what is beyond.
- Climbing comes from the `minecraft:climbable` tag via `ServusStorageBlockTagsProvider`. NeoForge's `IBlockExtension.isLadder` defaults to `state.is(BlockTags.CLIMBABLE)`. `LivingEntity.handleOnClimbable` clamps the fall to -0.15, and stops it while sneaking unless `isScaffolding`, which stays false for us. `LivingEntity.travel` sets y to 0.2 when `jumping && onClimbable()`. These were checked in `neoforge-21.1.255-sources.jar` (`LivingEntity` lines 2329 to 2368, `IBlockExtension.isLadder`).
- No override of `isScaffolding`, so sneaking holds, ladder style.

### Data
`lift_speed` goes into `storage/warehouse.json` (example 0.1), read by `StorageStatsManager.liftSpeed()` with a default, and exposed via `Host.liftSpeed()`.

### Pure logic and tests
- **Pure, JUnit (`storage/src/test`):**
  - `ShaftsTest`: columns, gaps, single segments.
  - `LiftCarTest`: FIFO, boarding, ride time, parking, crossing blocks departure, `canCross` near a moving car, resize, remove.
  - `ShaftStepsTest`: enter ride, enter cross, ride, exit, goal in shaft.
  - `ShuttleMotionTest`: gate holds, skipTo.
  - `FlapsTest`: angles at 0, 0.5, 0.75, 1 and 3.
- **Minecraft, GameTest:**
  - A shuttle picks from an upper floor rack via the car.
  - Two shuttles queue.
  - A crossing passes without a call.
  - Breaking a segment mid ride drops the cargo.
  - A player falls through a segment without collision (`helper` entity test, optional).
- **Manual:** the visuals (decks folding, sleeves, head), climbing, and the heading on vertical steps.

## Risks / Trade-offs

- **[Risk] A rider whose leg ends in the car holds it**, for example a shuttle waiting with output full at a station beside a segment, and blocks every other caller. â†’ Mitigation: this is accepted for now and visible in game. Stations are normally on rails. The fix can be "leave the car while waiting" later.
- **[Risk] Gate and car disagree**, so the deck folds under a shuttle. â†’ Mitigation: both use `Flaps.angle`, and the car refuses to sweep a floor with a crosser. `LiftCarTest` covers this.
- **[Risk] Deadlock between a crosser and the car**: a crosser waits for the deck while the car waits for crossers. â†’ Mitigation: crossers only `beginCross` when `canCross` is true and leave within a few ticks. The car only checks crossers before and during its sweep. There is no hold and wait cycle.
- **[Risk] The client column walk every frame.** â†’ Mitigation: it runs only for the bottom segment and stops at the first non elevator block. Shafts are short.
- **[Trade-off] Routing ignores lift waits**, so a busy shaft may be chosen over a slightly longer rail route. This is accepted and listed as a non-goal.
- **[Trade-off] The car is not saved.** After a restart it sits at the bottom. This is harmless because shuttles restart at home.

## Migration Plan

Not released, no migration. Existing worlds re-render elevators from the new blockstate. Old segments keep working.

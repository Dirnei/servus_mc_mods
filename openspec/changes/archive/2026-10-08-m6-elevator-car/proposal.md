# Proposal

## Why

PLAN.md milestone M6, item "Rail Elevator for multi level warehouses". The elevator links floors in the network, but it is not a machine yet. Shuttles float straight up the middle of the shaft with nothing carrying them. A small rail plate floats in every segment without touching the corner posts. Players snag on the posts. To make the shaft feel like real warehouse equipment, it needs a lift car that shuttles wait for and ride, and a rail floor that clears the way for that car.

## What Changes

- Elevator segments lose their player collision and become climbable. A player walks into a shaft, holds jump to climb, slides down slowly otherwise and holds position while sneaking. Breaking a segment still drops every segment above it.
- Each shaft (a vertical stack of segments) gets one simulated **lift car**. It carries one shuttle per trip at `lift_speed` from `warehouse.json`, and stays parked at the last floor it served. Shuttles that need to change level queue at the shaft in the order they called it and wait for the car.
- A shuttle crossing a segment sideways on one floor does not call the car. It only waits while the car is at or passing that floor.
- Segment visuals:
  - A segment with no rail beside it draws only its four corner posts.
  - A segment with rails draws a **deck**: two flaps hinged on axles running from post to post, covering the shaft floor, with the rail drawn on top towards each connected rail. The flaps fold up against the shaft edges while the car is at or passing that floor.
  - The car is a platform at rail height with a sleeve around each corner post. When it stands at a floor, its platform carries the rail.
  - The top segment of a shaft is closed with a head and a motor beam.
- The lowest segment of a shaft renders the car and every deck of the shaft. The server syncs the car's state to clients.
- If a segment breaks while the car carries a shuttle, that shuttle drops its cargo and is recalled.
- A shuttle on a vertical path step no longer snaps its heading to yaw 0.
- PLAN.md section 2.3 describes the car and the decks.

## Non-goals

- Players riding the car, or the car colliding with players or entities.
- Standing on top of a shaft like on vanilla scaffolding.
- Path costs that include lift waiting time. Routing stays shortest path by node count.
- More than one car per shaft, or cars carrying several shuttles at once.
- Saving the car position across restarts. After loading, the car starts at the lowest floor.
- Power use by the lift.
- The pick rate stats screen (separate M6 item).

## Capabilities

### New Capabilities

None. The car is part of the rail elevator capability.

### Modified Capabilities

- `storage/rail-elevator`: the segment block becomes walk through and climbable, and renders posts, decks and a top head. The shaft gains a lift car with a queue, decks that fold for the car, and sync to clients.
- `storage/shuttles`: shuttles change level only by riding a shaft's car, wait for it at the shaft, cross a shaft floor on its deck, and lose their cargo when the shaft breaks under them.

## Impact

- `storage/rail`: `ElevatorBlock` (collision, outline, top and bottom state use) and `ElevatorBlockEntity`.
- New pure logic in `storage/shuttle`: lift car state, shaft runs in a path, and flap angle, all with JUnit tests.
- `ShuttleFleet` and `ShuttleMotion`: stopping at a node, waiting for the car, riding, and shaft break handling. `WarehouseNetwork` gains the shaft registry, rebuilt on node add and remove events.
- New `LiftSyncPayload` registered in `ServusStoragePayloads`. A new `ElevatorRenderer` on the lowest segment, and a change to `ShuttleGarageRenderer` so riding shuttles are drawn on the car.
- Datagen: elevator models (posts, head), the climbable block tag, and `lift_speed` in `warehouse.json` through `StorageStatsManager`.
- Storage GameTests for queueing, riding, crossing and shaft break.
- PLAN.md section 2.3.

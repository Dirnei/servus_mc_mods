# Proposal

## Why

The Rail Elevator segment is a solid centre pillar, so nothing can pass through a shaft and a shuttle could never drive into it. The user asked on 2026-10-06, after testing the elevator, for a frame of four corner posts with the rail running through the middle, so a shuttle can later drive in and climb inside the frame. This refines the PLAN.md milestone M6 item "Rail Elevator for multi level warehouses".

## What Changes

- The elevator segment renders as four corner posts with an open middle. The warehouse rail runs through the middle of the segment at floor level: a centre plate plus an arm towards every connected rail.
- Players walk through a segment between the posts. The collision shape is the four posts plus the flat rail parts.
- Stacked segments still read as one continuous frame. Rail arms into the segment, recipe and drops stay as they are.
- A segment must stand on a solid block or on another segment. It cannot be placed in the air, and when its support goes it drops as an item, taking the segments above with it.
- Segments no longer bridge straight into storage blocks. A crate or rack touching only a segment stays outside the warehouse; only rails (and direct contact between storage blocks) carry membership. The warehouse network gets a real connection rule for this: storage blocks link to storage blocks and rails on any side, rails link sideways to rails and segments, segments link vertically to segments and sideways to rails. The user asked for both on 2026-10-06 after testing the frame.
- Rails that lose their floor now drop as items instead of vanishing.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/rail-elevator`: the requirement "Shaft segment block" describes the four post frame with the rail through the middle and that players walk through it. "Rail levels join through a shaft" limits membership through a segment to rails and segments. "Elevator is obtainable and dismantled" says the shaft above a removed segment drops. A new requirement covers support.
- `storage/warehouse-rail`: the requirement "Storage blocks join through rails" states that rails stacked vertically do not link, and "Rail is obtainable and dismantled" that a rail drops when its floor goes.

## Non-goals

- Shuttle movement. The frame only leaves room for it.
- Power use or shuttle logic on the elevator.

## Impact

- `servus_storage`: `ElevatorBlock` shape, support and drop, `ElevatorModels` datagen, regenerated elevator blockstate and models, a pure `WarehouseConnectionRule` with a JUnit test, a `kind()` on the storage node block entities, the warehouse network type using the rule, `RailBlock` drop on lost floor, GameTest additions. The elevator texture stays.
- No Lib or data changes beyond the models.

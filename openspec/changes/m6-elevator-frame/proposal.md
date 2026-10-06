# Proposal

## Why

The Rail Elevator segment is a solid centre pillar, so nothing can pass through a shaft and a shuttle could never drive into it. The user asked on 2026-10-06, after testing the elevator, for a frame of four corner posts with the rail running through the middle, so a shuttle can later drive in and climb inside the frame. This refines the PLAN.md milestone M6 item "Rail Elevator for multi level warehouses".

## What Changes

- The elevator segment renders as four corner posts with an open middle. The warehouse rail runs through the middle of the segment at floor level: a centre plate plus an arm towards every connected rail.
- Players walk through a segment between the posts. The collision shape is the four posts plus the flat rail parts.
- Stacked segments still read as one continuous frame. Rail arms into the segment, connections, network behaviour, recipe and drops stay as they are.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/rail-elevator`: the requirement "Shaft segment block" describes the four post frame with the rail through the middle and that players walk through it.

## Non-goals

- Shuttle movement. The frame only leaves room for it.
- Any change to how segments connect or join the network.

## Impact

- `servus_storage`: `ElevatorBlock` shape, `ElevatorModels` datagen, regenerated elevator blockstate and models. The elevator texture stays.
- No Lib or data changes beyond the models.

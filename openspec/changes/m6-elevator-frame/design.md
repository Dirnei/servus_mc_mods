# Design

## Context

See proposal.md for motivation. `ElevatorBlock extends ConnectableBlock` with a cached shape of one pillar `box(4, 0, 4, 12, 16, 12)` plus a rail arm box per connected horizontal side. `ElevatorModels` writes the multipart blockstate: `elevator_center` (the pillar) always, the rail's `rail_arm` model per connected side, and the pillar as item model. `RailModels` writes `rail_center` (`from(5, 0, 5).to(11, 1, 11)`) and `rail_arm`.

## Goals / Non-Goals

**Goals:**
- Open middle on every level, so a shuttle can later enter from any connected rail and move vertically inside the frame.
- Keep the one block footprint and all connection logic.

**Non-Goals:**
- Any shuttle data or rendering.

## Decisions

### Four posts and the rail through the centre
Shape: four posts `box(0, 0, 0, 2, 16, 2)`, `box(14, 0, 0, 16, 16, 2)`, `box(0, 0, 14, 2, 16, 16)`, `box(14, 0, 14, 16, 16, 16)`, united with the rail centre plate `box(5, 0, 5, 11, 1, 11)` and the existing arm boxes per connected side. The middle stays free from y 1 to 16 between the posts, so the player fits. Alternative considered: two posts on the open sides only. Rejected, four posts read as a frame from every side and keep the shape independent of connections.

### Models
`ElevatorModels` writes `elevator_frame`: one model with four post elements using the `block/elevator` texture. The multipart blockstate applies `elevator_frame` always, `rail_center` always (reused from `RailModels` through `getExistingFile`), and `rail_arm` per connected side as today. The item model is `elevator_frame`. The `elevator_center` pillar model is no longer written; datagen rewrites the generated folder so it disappears.

## Pure versus Minecraft logic

No pure logic. Shape and models are Minecraft; the look is checked in the client, connectivity by the existing GameTest.

## Risks / Trade-offs

- [Thin posts are easy to miss when clicking] -> The flat rail plate in the middle is also part of the shape, so clicking the floor of the segment works.
- [Frame looks hollow from the side] -> Intended, it is a cage the shuttle climbs in.

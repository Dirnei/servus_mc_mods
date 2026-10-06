# Proposal

## Why

The Heavy Duty Rack frame flickers where its orange steel boxes overlap the beige boards, a long row of bays shows no posts at all between the ends, the shelf boards stop at every bay edge even when the posts are gone, and rails dock onto a rack from any side although goods only go in and out at the front. The user asked for all four fixes on 2026-10-06 after looking at the rack in game. This polishes the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- The frame model is rebuilt from boxes that never overlap, so no two faces share a plane with the same normal. The back panel is a thin full size sheet behind the posts and beams.
- A row of joined bays shows a post every few bays (`posts_every` in `rack.json`, example 3) and at both ends, so long rows read as real pallet racking. Posts between bays reappear and disappear as bays are added or removed anywhere in the row.
- Where no post stands between two bays, the floor board, the middle shelf and the top and bottom beams run through to the bay edge, so joined bays share continuous shelves.
- A rack links to the warehouse through rails only on its front side, and a rail docks onto a rack only from the front. Racks still link to touching storage blocks and to each other on any side. Rotating a rack with the Configuration Carrot refreshes its links.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Bays connect visually" gains the post spacing and continuous shelves; "Rack is a storage block" limits rail links to the front.
- `storage/warehouse-rail`: "Rails auto connect" says a rail docks onto a rack only from the rack's front.

## Non-goals

- Changing crates, controller or terminal docking. They stay all sided.
- New textures. The steel and board textures stay.

## Impact

- `servus_storage`: `HeavyRackBlock` computes its bay index along the row and refreshes the row on place and remove, `HeavyRackModels` is rebuilt with extension parts, a pure `RackRow` helper with a JUnit test decides where posts stand, `StorageNodeBlockEntity` gains a rail side check used by the warehouse connection rule and the rail dock, `HeavyRackBlockEntity` implements it from its facing, `rack.json` gains `posts_every`, a GameTest covers the front only rail link.
- Generated resources: the rack blockstate changes shape.

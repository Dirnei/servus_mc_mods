# Proposal

## Why

Warehouse Rails run on one floor only, so a multi storey warehouse needs one controller per floor. The Rail Elevator joins rail levels into one network today and is the block shuttles will ride between floors in the shuttle change. This adds a new PLAN.md milestone M6 item "Rail Elevator for multi level warehouses" right after "Rails as network connections"; the user asked for it on 2026-10-06 after testing the rails.

## What Changes

- New block **Rail Elevator** in `servus_storage`: a full height shaft segment. Segments stack on top of each other to form a shaft of any height. Each segment connects to Warehouse Rails beside it on its own level and to elevator segments directly above and below.
- A Warehouse Rail beside an elevator segment renders an arm towards it, like towards another rail.
- An elevator segment is a warehouse network node that holds nothing, like a rail: it is never indexed and never pulls plain inventories in. Storage blocks touching a segment join through it.
- The elevator is craftable from iron ingots and a rail in a batch, drops itself, sits in the Servus creative tab after the rail, and is dismantled by sneaking with the Configuration Carrot.
- A GameTest proves a crate on the ground floor and a controller three levels up form one online warehouse through a rail, a three segment shaft and another rail, and that breaking a middle segment splits them.
- PLAN.md: M6 gains the elevator item and section 2.3 states that shafts of stacked segments link rail levels.

## Capabilities

### New Capabilities
- `storage/rail-elevator`: what the elevator segment is, how segments stack and connect to rails, which blocks join through it, how it is obtained.

### Modified Capabilities
- `storage/warehouse-rail`: the requirement "Rails auto connect" also connects a rail to an elevator segment beside it.

## Non-goals

Deferred to later M6 changes:
- Moving shuttles up and down, elevator speed, capacity or queueing. This change only makes shafts connect.
- Elevator power use. Segments are passive until shuttles exist.
- Rails or elevators that connect diagonally, and ceiling mounted shafts.

## Impact

- `servus_storage`: new `ElevatorBlock` and `ElevatorBlockEntity` in the `rail` package, registry entries, datagen for the multipart blockstate reusing the rail arm model, lang, recipe, loot and tags, one placeholder texture, a GameTest, a one line change in `RailBlock.connectsTo`.
- `servus_lib`: none.
- Generated resources under `storage/src/generated/resources` gain the elevator files.
- PLAN.md: M6 item list and section 2.3.

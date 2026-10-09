# Proposal

## Why

PLAN.md M6 (storage automation, rails as network connections). A Heavy Duty Rack is only ever picked from and filled by a shuttle, which needs a rail in front of the rack's section. Today a rack also joins the warehouse by touching the controller, a crate or another rack, so its items show up as stock that no request can fetch, and a rack can link crates behind it into the warehouse. The play test asked that racks connect only through their front rail.

## What Changes

- A Heavy Duty Rack joins the warehouse network only through a rail in front of a bay of its section. Bays of one section link to each other, so one front rail connects the whole section.
- Racks no longer link to the controller, crates, stations, garages, elevators or racks of other sections, on any side, and pass no connection on: a crate behind a rack is not part of the warehouse through it.
- Racks no longer pull neighbouring foreign inventories into the index, and no storage block indexes a rack (or any other storage block) as a foreign inventory.
- A rack without a front rail is in no warehouse: its items are not listed as stock.
- Placing or breaking the front rail or a bay changes membership right away through the existing graph events.
- Crates and foreign inventories keep their adjacency behaviour.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Rack is a storage block" now says a rack joins only through a front rail of its section.

## Non-goals

- A connection lamp on racks (the spec keeps "no lamp").
- Changing how shuttles reach a section or how crates connect.

## Impact

- `storage/.../network/{WarehouseConnectionRule,StorageNodeBlockEntity,WarehouseNetwork}.java`, `storage/.../rack/{HeavyRackBlock,HeavyRackBlockEntity}.java`.
- Storage GameTests that relied on racks touching the controller.
- PLAN.md 2.3 topology text, the Storage docs page.

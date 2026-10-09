# Design

## Context

Warehouse links are decided by the pure `WarehouseConnectionRule` over node kinds (STOCK, RAIL, ELEVATOR, STATION, GARAGE, RACK), asked from both sides by `NetworkGraph`. Inbound Station, Outbound Station and the Access Point (a subclass of the Inbound Station) report `NodeKind.STATION`. Today STATION links to STOCK, STATION, GARAGE and RAIL, so a station touching the controller or a crate joins the warehouse. `m6-rack-rail-only` already made RACK link only to rails and its own section. See proposal.md for why.

## Goals / Non-Goals

**Goals:** stations join only through rails, decided in the pure rule, event driven through the existing graph.

**Non-Goals:** garage, crate and rail behaviour; which faces take rails; put-away and shipping logic.

## Decisions

- `WarehouseConnectionRule.accepts`: `STATION` accepts only `RAIL`. `STOCK` and `GARAGE` no longer accept `STATION`. `RAIL` keeps accepting `STATION` on every side. The rule stays symmetric, so neither side can link a station except a rail. Alternative considered: a per side check in `StorageNodeBlockEntity` like the rack's section link; not needed because stations take rails on every face they already accept, so the kind rule is enough.
- The Access Point needs no own code: it inherits `NodeKind.STATION` from the Inbound Station and therefore the rail only rule.
- Shuttle Garage: unchanged on purpose. It links to its rail and still to touching stock blocks; the user asked only for the stations. Recorded so it can be revisited.
- Lamps: the connection lamp already follows the network the node is in; a station without a rail is a network of its own with no controller, so its lamp is dark and its GUI names the missing controller.
- Updates: placing or breaking a rail adds or removes a graph node, which merges or splits networks through `NetworkSet`; no ticking scan.
- NeoForge APIs: none new.

## Testing

- JUnit: `WarehouseConnectionRuleTest` updated (station links only to rails; stock and garage never to stations; symmetry).
- GameTests (Storage): station touching the controller without a rail stays offline and keeps its buffer; adding a rail connects it and puts away; breaking the rail disconnects; a crate touching only a rail linked station is not part of the warehouse; existing tests that relied on station adjacency are wired with a rail.

## Risks / Trade-offs

- [Existing builds with stations pressed against controllers go offline after the update] -> intended by the user; the GUI and dark lamp say so.
- [Garage still links by adjacency] -> recorded; a follow up can make it rail only too.

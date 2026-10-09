# Proposal

## Why

Play test feedback: Inbound and Outbound Stations should only connect to the warehouse through rails, not by touching the controller, crates or racks, and then more generally: "the rule is for all blocks that shuttles interact with". Rails are how shuttles reach those blocks, the same reasoning that made racks rail only in `m6-rack-rail-only`. The change name predates the wider scope and is kept; it covers every shuttle served block. This refines the PLAN.md M6 item "Inbound station, Zone Labels with put-away filters" and the rails-as-wiring direction in section 2.3.

## What Changes

- One general rule: every block shuttles serve is rail only. Inbound Station, Outbound Station, the Access Point (which is an Inbound Station) and the Shuttle Garage join the warehouse network only through a rail on a face where they take rails, like the Heavy Duty Rack already does. They no longer link to touching controllers, crates, racks, garages or stations, and do not pass the warehouse connection on to neighbours.
- Their connection lamp is lit only while a rail links them to an online warehouse.
- Linking stays event driven: placing or breaking the rail joins or splits through the network graph.
- Crates, the controller and foreign inventories keep linking by touch because they are served instantly. Whether they should become rail only too is an open question for the user.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/inbound-station`: the station joins only through rails; the instant put-away and offline GUI scenarios use a rail.
- `storage/outbound-station`: the station joins only through rails.
- `storage/access-point`: the Access Point joins the warehouse only through rails.
- `storage/shuttle-garage`: the garage joins only through its back rail.
- `storage/warehouse-rail`: the general rule that shuttle served blocks join only through rails, with the faces per block.

## Non-goals

- Making crates, the controller or foreign inventories rail only (open question).
- Changing which faces of a station take rails.
- Any change to put-away routing, shuttles or zones.

## Impact

- `storage/.../network/WarehouseConnectionRule.java` and its JUnit test.
- Storage GameTests that placed stations beside a controller or crate without a rail.
- PLAN.md section 2.3 and the Storage docs page.

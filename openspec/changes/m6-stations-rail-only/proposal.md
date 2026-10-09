# Proposal

## Why

Play test feedback: Inbound and Outbound Stations should only connect to the warehouse through rails, not by touching the controller, crates or racks. Rails are how items reach and leave a station, the same reasoning that made racks rail only in `m6-rack-rail-only`. This refines the PLAN.md M6 item "Inbound station, Zone Labels with put-away filters" and the rails-as-wiring direction in section 2.3.

## What Changes

- Inbound Station, Outbound Station and the Access Point (which is an Inbound Station) join the warehouse network only through a rail touching them. They no longer link to touching controllers, crates, racks, garages or other stations, and they do not pass the warehouse connection on to neighbours.
- Their connection lamp is lit only while a rail links them to an online warehouse.
- Linking stays event driven: placing or breaking the rail joins or splits through the network graph.
- The Shuttle Garage is unchanged: it already parks on the rail at its front or back and keeps linking to touching stock blocks.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/inbound-station`: the station joins only through rails; the instant put-away and offline GUI scenarios use a rail.
- `storage/outbound-station`: the station joins only through rails.
- `storage/access-point`: the Access Point joins the warehouse only through rails.

## Non-goals

- Changing the Shuttle Garage, crates or rails.
- Changing which faces of a station take rails.
- Any change to put-away routing, shuttles or zones.

## Impact

- `storage/.../network/WarehouseConnectionRule.java` and its JUnit test.
- Storage GameTests that placed stations beside a controller or crate without a rail.
- PLAN.md section 2.3 and the Storage docs page.

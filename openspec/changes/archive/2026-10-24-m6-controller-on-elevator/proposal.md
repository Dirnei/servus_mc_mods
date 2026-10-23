# Proposal

## Why

A Warehouse Controller placed against a Rail Elevator segment does not join the rail network; the player has to put a rail between them. The rule dates from when the controller shared its link kind with crates and other storage blocks, which were not meant to join through a shaft. Crates are gone, so the controller is the only block left behind that rule. Reported in play testing. This belongs to PLAN.md milestone M6 (warehouse automation: rails and elevators).

## What Changes

- A Warehouse Controller links to an elevator segment that touches it on one of the four horizontal sides, the same way a segment links to a rail beside it.
- A controller directly above or below a shaft still does not link to it.
- Racks, stations, garages and Fluid Gantries keep joining only through rails; a segment touching them still passes no membership.
- The segment looks the same: a controller beside it adds no deck, rail or dock piece.
- Shuttles never drive into the controller; it stays a non-travel node.

## Non-goals

- Controllers linking to the top or bottom of a shaft.
- Any other block joining through a segment.
- Visual changes to the controller or the segment for this link.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/rail-elevator`: the "Floors join through a shaft" requirement lets a controller beside a segment join the shaft's network.

## Impact

- `shuttles/.../network/WarehouseConnectionRule.java`: controller and elevator accept each other on horizontal sides.
- `shuttles/.../network/WarehouseConnectionRuleTest.java`: the "never" test becomes a horizontal-yes, vertical-no test.
- `shuttles/.../gametest/ElevatorGameTests.java`: new GameTest for a controller touching only a shaft.
- `shuttles/CHANGELOG.md`: a Fixed line under Unreleased at archive.

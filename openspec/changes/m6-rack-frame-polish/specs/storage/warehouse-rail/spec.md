# Spec Delta

## MODIFIED Requirements

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail, every Rail Elevator segment and every stock holding storage block (crate, shelving rack, warehouse controller, counter terminal) on the same level and SHALL render the matching piece: straight, curve, T or cross. Towards a storage block the piece SHALL be a dock, visibly different from the arm towards a rail or segment. A Heavy Duty Rack SHALL take a dock only on its front side. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail, segment or storage block is placed, rotated or removed.

#### Scenario: Straight run
- **WHEN** three rails are placed in a line
- **THEN** the middle rail shows a straight piece and the end rails each show one arm

#### Scenario: Crossing
- **WHEN** a rail has rails on all four horizontal sides
- **THEN** it renders a cross

#### Scenario: Different level
- **WHEN** a rail is placed directly above another rail
- **THEN** the two rails are not connected to each other

#### Scenario: Rail meets a shaft
- **WHEN** a rail is placed beside an elevator segment
- **THEN** the rail shows an arm towards the segment

#### Scenario: Rail docks onto a rack
- **WHEN** a rack is placed beside a rail
- **THEN** the rail shows a dock piece towards the rack that looks different from an arm towards a rail, and the dock disappears when the rack is broken

#### Scenario: Rail behind a heavy duty rack
- **WHEN** a rail runs behind a Heavy Duty Rack
- **THEN** the rail shows no dock towards it, and turning the rack to face the rail makes the dock appear

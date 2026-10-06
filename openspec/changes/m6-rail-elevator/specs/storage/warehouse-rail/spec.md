# Spec Delta

## MODIFIED Requirements

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail and every Rail Elevator segment on the same level and SHALL render the matching piece: straight, curve, T or cross. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail or segment is placed or removed.

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

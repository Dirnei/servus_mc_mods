# Spec Delta

## ADDED Requirements

### Requirement: Fluid Rack facing on placement
A Fluid Rack SHALL take its facing on placement like the Heavy Duty Rack: from a Fluid Rack whose face was clicked, else from a Fluid Rack directly below, else facing the player while sneaking, else its front turned to a rail by the shared rail facing rule (see `shuttles/rail-facing`).

#### Scenario: Placed against a Fluid Rack
- **WHEN** a player clicks the east side of a Fluid Rack facing north with another Fluid Rack
- **THEN** the new rack stands east of it and faces north

#### Scenario: Placed beside a rail
- **WHEN** a player looking north places a Fluid Rack on the floor with a rail only on its west side and no rack to copy
- **THEN** its front faces west, towards the rail

### Requirement: Fluid Rack section layer on top
Clicking the top face of a Fluid Rack with an empty Fluid Rack item, without sneaking, SHALL place a Fluid Rack on top of every bay of the clicked rack's section when every spot above is free and the held stack holds an item for every bay (always in creative), using one item per bay. The new bays SHALL take the facing and section boundaries below, so the new section gets its port in the same bay. A held item carrying fluid, a short stack or a taken spot SHALL place one rack as usual.

#### Scenario: Layer over a section of three
- **WHEN** a player holding 3 empty Fluid Racks clicks the top of a three bay section
- **THEN** a section of three stands on top with its port above the port below

#### Scenario: Item with fluid
- **WHEN** the held Fluid Rack item carries water
- **THEN** one rack is placed on the clicked bay

### Requirement: Sneak extends a Fluid Rack row
Sneaking while looking at a Fluid Rack with a Fluid Rack item SHALL extend that rack's row like the Heavy Duty Rack row assist: the first free spot past the end of the row of Fluid Racks with the same facing, with ghost, arrow and reach from the Foundation placement assist, taking the row's facing.

#### Scenario: Extend a fluid row
- **WHEN** a player sneaks and looks at the left half of the front of the last Fluid Rack in a row of two facing east
- **THEN** a Fluid Rack is placed past the far end of the row, facing east

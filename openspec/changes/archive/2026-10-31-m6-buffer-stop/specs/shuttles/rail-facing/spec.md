## MODIFIED Requirements

### Requirement: Which blocks turn to a rail
The Inbound Station, Outbound Station, Packing Station, Fluid Inbound, Fluid Outbound, Heavy Duty Rack, Fluid Rack, Warehouse Controller, Shuttle Garage and Buffer Stop SHALL turn to a Warehouse Rail or Farm Rail; the Field Head SHALL turn to a Farm Rail only. Warehouse Rails, Farm Rails and the Rail Elevator SHALL keep their own placement. The Fluid Inbound and Fluid Outbound SHALL look for the rail beside their lower half.

#### Scenario: Field Head ignores a warehouse rail
- **WHEN** a Field Head is placed with a Warehouse Rail on its west side and a Farm Rail on its north side, the player looking east
- **THEN** its front faces north, towards the Farm Rail

#### Scenario: Controller turns to the rail
- **WHEN** a player looking north places a Warehouse Controller with a rail on its west side
- **THEN** its front faces west, towards the rail

#### Scenario: Rails keep their placement
- **WHEN** a player places a Warehouse Rail beside a rack
- **THEN** the rail is placed as before this change

#### Scenario: Buffer Stop turns to a farm rail
- **WHEN** a player looking east places a Buffer Stop with a Farm Rail on its north side
- **THEN** its front faces north, towards the Farm Rail

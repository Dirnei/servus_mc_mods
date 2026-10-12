## MODIFIED Requirements

### Requirement: Farm Rail is a Warehouse Rail
The Farm Rail SHALL be a craftable variant of the Warehouse Rail that looks different from it. Every requirement of the Warehouse Rail SHALL apply to it: it stands, hangs and bridges gaps, auto connects to rails of both kinds, elevator segments, storage blocks, stations and garages, links into the warehouse network and holds nothing. Shuttles SHALL drive over farm rails like over any rail. The Farm Rail SHALL NOT be waterloggable. Sneak placement SHALL extend a run of rails like the Warehouse Rail does.

#### Scenario: Joins the warehouse
- **WHEN** a controller touches a farm rail, three farm rails run on from it and a rack faces the last one
- **THEN** the rack is in the controller's warehouse

#### Scenario: Mixed run
- **WHEN** a Warehouse Rail and a Farm Rail are placed side by side
- **THEN** they connect to each other and are in the same network

#### Scenario: No water inside
- **WHEN** a player uses a water bucket on a farm rail
- **THEN** the farm rail does not hold water

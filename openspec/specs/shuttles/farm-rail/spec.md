# shuttles/farm-rail Specification

## Purpose

The Farm Rail is the rail shuttles drive along to tend crops. A connected run of farm rails is a field that a Field Head waters and watches.

## Requirements

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

### Requirement: A run of farm rails is a field
A field SHALL be a set of farm rails connected to each other through farm rail neighbours on their four horizontal sides. Warehouse Rails, elevator segments and other blocks SHALL NOT join or extend a field, even when they connect to the farm rails as rails. Placing or removing a farm rail SHALL update the field it belongs to: a removed rail in the middle of a run splits the field, a placed rail between two runs joins them. A field SHALL be checked only when one of its rails or its head is placed or removed or a chunk it lies in loads, never every tick.

#### Scenario: Rail between two runs
- **WHEN** two runs of three farm rails lie in a line with one block between them and a farm rail is placed in the gap
- **THEN** the seven rails are one field

#### Scenario: Warehouse rail splits
- **WHEN** a run of farm rails has a Warehouse Rail in its middle
- **THEN** the farm rails on either side are two fields

#### Scenario: Rail removed
- **WHEN** a Field Head stands at one end of a run of five farm rails and the middle rail is broken
- **THEN** the head's field is the two rails on its side of the gap

### Requirement: Farm Rail copper heads
The Farm Rail SHALL have the track look of the Warehouse Rail with rail heads of fresh copper with a light touch of verdigris instead of steel, and sleepers in the wood of its recipe. Its recipe SHALL take copper ingots and planks and SHALL give several farm rails carrying the wood of the planks.

#### Scenario: Crafting
- **WHEN** the player places copper ingots and oak planks in the rail pattern
- **THEN** the result is several farm rails (for example sixteen) carrying oak

#### Scenario: Next to a Warehouse Rail
- **WHEN** a Farm Rail and a Warehouse Rail of the same wood lie in one run
- **THEN** the heads change from copper to steel at the border and the gauge stays the same

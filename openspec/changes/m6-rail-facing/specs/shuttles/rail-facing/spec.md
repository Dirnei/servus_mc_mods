# Spec Delta

## Purpose

How blocks that dock to a rail pick their facing when placed, so they turn to the rail without the player walking around them, and how a held warehouse block places against another instead of opening its GUI.

## ADDED Requirements

### Requirement: Rail side turns to a rail on placement
A block that docks to a rail SHALL, when placed without sneaking, turn its rail side to a rail beside it: the rail whose side face was clicked; else a rail beside it in the direction the player looks; else the only rail beside it; else its front SHALL face the player. Only horizontal sides count. The rail side is the front for every block but the garage, whose rail side is its doorway.

#### Scenario: Placed from the aisle
- **WHEN** a player standing in the aisle looks north at a rail and places an Inbound Station on the floor just past it
- **THEN** the station's front faces south, towards the rail

#### Scenario: Clicked on the side of a rail
- **WHEN** a player looking north clicks the east side of a rail with an Outbound Station
- **THEN** the station stands east of the rail with its front facing west

#### Scenario: Rail in the look direction wins
- **WHEN** a player looking north places a Packing Station with rails on its north and its west side
- **THEN** its front faces north

#### Scenario: Several rails, none in the look direction
- **WHEN** a player looking north places a station with rails on its east and its west side
- **THEN** its front faces south, towards the player

#### Scenario: No rail beside it
- **WHEN** a player looking north places a station with no rail beside it
- **THEN** its front faces south, towards the player

### Requirement: Which blocks turn to a rail
The Inbound Station, Outbound Station, Packing Station, Fluid Inbound, Fluid Outbound, Heavy Duty Rack, Fluid Rack, Warehouse Controller and Shuttle Garage SHALL turn to a Warehouse Rail or Farm Rail; the Field Head SHALL turn to a Farm Rail only. Warehouse Rails, Farm Rails and the Rail Elevator SHALL keep their own placement. The Fluid Inbound and Fluid Outbound SHALL look for the rail beside their lower half.

#### Scenario: Field Head ignores a warehouse rail
- **WHEN** a Field Head is placed with a Warehouse Rail on its west side and a Farm Rail on its north side, the player looking east
- **THEN** its front faces north, towards the Farm Rail

#### Scenario: Controller turns to the rail
- **WHEN** a player looking north places a Warehouse Controller with a rail on its west side
- **THEN** its front faces west, towards the rail

#### Scenario: Rails keep their placement
- **WHEN** a player places a Warehouse Rail beside a rack
- **THEN** the rail is placed as before this change

### Requirement: Field Head needs a Farm Rail beside a Warehouse Rail
A Field Head SHALL NOT be placed where a Warehouse Rail touches it and no Farm Rail does, and the player SHALL be told it works on a Farm Rail. With no rail beside it, it SHALL be placed as usual.

#### Scenario: Field Head refused on a warehouse rail
- **WHEN** a player places a Field Head with only a Warehouse Rail beside it
- **THEN** nothing is placed and the action bar says the Field Head works on a Farm Rail

### Requirement: Sneaking faces the player
A block that turns to a rail SHALL, when placed while sneaking, face its front to the player and ignore every rail beside it, unless a rack's own sneak placement applies (see the rack specs).

#### Scenario: Sneak placement
- **WHEN** a player looking north sneaks and places an Inbound Station with a rail on its west side
- **THEN** the station's front faces south, towards the player

#### Scenario: Garage placed sneaking
- **WHEN** a player looking north sneaks and places a garage with a rail only on its west side
- **THEN** the garage's front faces the player, so its doorway faces north

### Requirement: Facing rule is pure logic
The choice of the rail side from the clicked rail, the rails beside and the look direction SHALL be pure logic without Minecraft types, covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the rule gets no clicked rail, rails on west only and a look to the north
- **THEN** it picks west

### Requirement: Racks and rails place against racks
Right clicking a Heavy Duty Rack or a Fluid Rack while holding an item in the item tag `servus_shuttles:places_on_racks` SHALL place the held block against the clicked face instead of opening the rack's GUI. The tag SHALL hold the Heavy Duty Rack, the Fluid Rack and the Warehouse Rail and SHALL be open to datapacks. Every other warehouse block SHALL open its GUI as before, whatever is held.

#### Scenario: Tag contents
- **WHEN** a datapack reads `servus_shuttles:places_on_racks`
- **THEN** it lists the Heavy Duty Rack, the Fluid Rack and the Warehouse Rail

#### Scenario: Rack against a rack front
- **WHEN** a player holding a Heavy Duty Rack right clicks the front of a rack
- **THEN** a rack is placed against that front and no GUI opens

#### Scenario: Rail against a rack front
- **WHEN** a player holding a Warehouse Rail right clicks the front of a rack
- **THEN** the rail is placed against that front and no GUI opens

#### Scenario: A station still opens
- **WHEN** a player holding a Warehouse Rail right clicks an Inbound Station
- **THEN** the station's GUI opens and no rail is placed

#### Scenario: Other items still open
- **WHEN** a player holding cobblestone right clicks the front of a rack
- **THEN** the rack's GUI opens as before

# Spec Delta

## ADDED Requirements

### Requirement: Rack joins through its front rail
A rack SHALL be a stock holding node of the warehouse network that joins only through a rail in front of a bay of its section: a section belongs to the network of a rail standing in front of any of its bays, and the bays of one section belong together. A rack SHALL NOT join through touching the controller, crates, stations, garages, elevators or racks of another section, on any side, and SHALL pass no connection on. A rack SHALL NOT pull neighbouring inventories into the warehouse, and no storage block SHALL index a rack as a foreign inventory. Rails dock onto a rack's front only. A rack SHALL be picked from and filled only through a shuttle parked on a front rail of its section. A rack SHALL NOT carry a connection lamp.

#### Scenario: Rack beside a rail
- **WHEN** a rack is placed with a rail of an online warehouse in front of it
- **THEN** the rail shows a dock towards it and the terminal lists its contents

#### Scenario: Rack touching the controller
- **WHEN** a rack holding bread touches the controller directly and has no front rail
- **THEN** the terminal does not list the bread

#### Scenario: Rail at the side
- **WHEN** a rail of an online warehouse runs along the side or the back of a rack and nothing else touches it
- **THEN** the rail shows no dock, and the rack's contents are not listed

#### Scenario: Front rail connects the section
- **WHEN** a section of three bays holds bread in its right bay and a rail of an online warehouse is placed in front of its left bay
- **THEN** the terminal lists the bread, and breaking that rail removes it from the list again

#### Scenario: Crate behind a rack
- **WHEN** a crate holding apples touches only the back of a rack that has a front rail of an online warehouse
- **THEN** the terminal does not list the apples

#### Scenario: Rack by shuttle
- **WHEN** a rack stands with its front on a rail and a garage with one shuttle is on that rail
- **THEN** a request for its bread sends the shuttle to the rack's front and the bread lands in the terminal's output slots

#### Scenario: Section served from one rail
- **WHEN** a section of three bays has a rail only in front of its left bay and the right bay holds bread
- **THEN** a request for bread sends the shuttle to the rail in front of the left bay and the bread lands in the terminal's output slots

#### Scenario: Neighbouring section not joined
- **WHEN** a lone rack stands beside a section with a front rail, does not join it, holds bread and has no front rail of its own
- **THEN** the terminal does not list that bread

## REMOVED Requirements

### Requirement: Rack is a storage block
**Reason**: Racks joined through any touching storage block, which listed items no shuttle could fetch.
**Migration**: See "Rack joins through its front rail"; give every rack section a front rail.

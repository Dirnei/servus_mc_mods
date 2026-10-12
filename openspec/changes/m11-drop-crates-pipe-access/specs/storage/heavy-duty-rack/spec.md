## MODIFIED Requirements

### Requirement: Automation access
A rack bay SHALL expose an item handler on every side, covering the slots that bay stores and not the rest of its section, while its section is standalone: no Warehouse Rail or Farm Rail stands in front of any bay of the section. Opening the section GUI by hand SHALL work whether or not the section is standalone.

#### Scenario: Hopper into a rack
- **WHEN** a hopper feeds cobblestone into a rack with no rail in front of it
- **THEN** the cobblestone appears in that bay's slots and in the section GUI

#### Scenario: Hand GUI on a docked rack
- **WHEN** a player right clicks the front of a rack whose section has a front rail of an online warehouse
- **THEN** the section GUI opens and the player can take and store items by hand

## ADDED Requirements

### Requirement: Racks join only through a front rail
A rack SHALL be a stock holding node of the warehouse network that joins only through a rail in front of a bay of its section: a section belongs to the network of a rail standing in front of any of its bays, and the bays of one section belong together. A rack SHALL NOT join through touching the controller, stations, garages, elevators or racks of another section, on any side, and SHALL pass no connection on. Neither a rack nor any other member SHALL pull neighbouring inventories into the warehouse. Rails dock onto a rack's front only. A rack SHALL be picked from and filled only through a shuttle parked on a front rail of its section. A rack SHALL NOT carry a connection lamp.

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

#### Scenario: Chest behind a rack
- **WHEN** a chest holding apples touches only the back of a rack that has a front rail of an online warehouse
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

### Requirement: Docked sections are shuttles only
While a rail stands in front of any bay of a section, every bay of that section SHALL expose no item handler, so hoppers and item pipes neither insert nor extract and pipes show no item arm toward it. Placing or breaking a rail in front of a bay, and a bay joining or leaving the section, SHALL update every bay of the section at once, without polling. Whether the rail belongs to an online warehouse SHALL NOT matter.

#### Scenario: Rail docks and closes the rack
- **WHEN** a hopper feeds cobblestone into a standalone rack and a rail is then placed in front of the rack
- **THEN** the hopper stops inserting, and a duct with the Item Pipe beside the rack loses its item arm toward it

#### Scenario: Rail removed opens the rack
- **WHEN** the only rail in front of a rack's section is broken
- **THEN** a hopper pointing into the rack inserts again and a duct with the Item Pipe beside it shows its item arm again

#### Scenario: One docked bay closes the section
- **WHEN** a section of three bays has a rail in front of its left bay only and a hopper points into its right bay
- **THEN** the hopper inserts nothing into the right bay

#### Scenario: Joining a docked section
- **WHEN** a standalone rack fed by a hopper is joined by a new bay placed beside it that has a rail in front of it
- **THEN** the hopper stops inserting into the first rack

#### Scenario: Rail without a controller
- **WHEN** a rail that belongs to no controller is placed in front of a rack fed by a hopper
- **THEN** the hopper stops inserting

## REMOVED Requirements

### Requirement: Rack joins through its front rail
**Reason**: The requirement referred to crates and to foreign inventory indexing, which are removed.
**Migration**: See "Racks join only through a front rail"; the joining rules are unchanged.

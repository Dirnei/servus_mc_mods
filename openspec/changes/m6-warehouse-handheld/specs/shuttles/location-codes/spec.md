# Spec Delta

## Purpose

Location codes give every rack bay of a warehouse a readable address relative to its Warehouse Controller, so players and the handheld can name and find a bay the way a real warehouse labels its shelves.

## ADDED Requirements

### Requirement: Code format
A bay's location code SHALL be its block offset from the Warehouse Controller of its warehouse, written as the north or south distance, a dot, the east or west distance, then the height difference with its sign: `N12.E5+2`. North is negative Z and east is positive X. A zero distance SHALL be written as `N0` or `E0` and SHALL never be left out, and a zero height SHALL be written `+0`.

#### Scenario: North east and up
- **WHEN** a bay is 12 blocks north, 5 blocks east and 2 blocks above the controller
- **THEN** its code is `N12.E5+2`

#### Scenario: South west and down
- **WHEN** a bay is 7 blocks south, 3 blocks west and 1 block below the controller
- **THEN** its code is `S7.W3-1`

#### Scenario: In line with the controller
- **WHEN** a bay is 8 blocks north, in line with the controller east to west, at the controller's height
- **THEN** its code is `N8.E0+0`

### Requirement: Which bays have a code
Every Heavy Duty Rack bay and every Fluid Rack bay that belongs to a warehouse with a Warehouse Controller SHALL have a location code. A bay of a warehouse without a controller, or of no warehouse, SHALL have none and SHALL show dashes on the handheld.

#### Scenario: No controller
- **WHEN** a rack stands on a rail network without a controller and a player aims at it with the handheld
- **THEN** the panel shows dashes as its code

### Requirement: Codes follow the controller
When a warehouse gets a controller, loses it, or a bay joins or leaves the warehouse, the affected bays SHALL show their new code or no code, without the player touching them. Moving the controller SHALL give every bay of the warehouse a new code.

#### Scenario: Controller moved
- **WHEN** the controller is dismantled and placed one block further east
- **THEN** the bay that read `N8.E0+0` reads `N8.W1+0`

### Requirement: Code on the rack beam
A Heavy Duty Rack bay with a code SHALL show it on a small white sticker with a barcode on its top beam, to every player who can see the bay. A bay without a code SHALL show no sticker.

#### Scenario: Looking at a bay
- **WHEN** a player looks at the front of the bay `N8.W3+0`
- **THEN** a sticker reading `N8.W3+0` with a barcode is drawn on its top beam

### Requirement: Code logic is pure
Building the code from the three offsets SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover each of the four directions, zero distances, positive, zero and negative heights

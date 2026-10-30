# Spec Delta

## Purpose

Location codes give every rack shelf of a warehouse a readable address relative to its Warehouse Controller, so players and the handheld can name and find a shelf the way a real warehouse labels its shelves.

## ADDED Requirements

### Requirement: Code format
A location code SHALL be a block offset from the Warehouse Controller of the warehouse, written as the north or south distance, a dot, the east or west distance, then the height difference with its sign: `N12.E5+2`. North is negative Z and east is positive X. A zero distance SHALL be written as `N0` or `E0` and SHALL never be left out, and a zero height SHALL be written `+0`.

#### Scenario: North east and up
- **WHEN** an offset is 12 blocks north, 5 blocks east and 2 blocks above the controller
- **THEN** its code is `N12.E5+2`

#### Scenario: South west and down
- **WHEN** an offset is 7 blocks south, 3 blocks west and 1 block below the controller
- **THEN** its code is `S7.W3-1`

#### Scenario: In line with the controller
- **WHEN** an offset is 8 blocks north, in line with the controller east to west, at the controller's height
- **THEN** its code is `N8.E0+0`

### Requirement: One code per shelf
Every shelf of Heavy Duty Racks and every shelf of Fluid Racks that belongs to a warehouse with a Warehouse Controller SHALL have one location code, shown for every bay of the shelf. The code SHALL be the offset of the shelf's code bay: the bay closest to the controller in straight-line distance, and on a tie the leftmost of the tied bays seen from the shelf's front. Shelves stacked over each other SHALL get different codes through their height.

#### Scenario: Closest bay gives the code
- **WHEN** a shelf of three bays stands 8 blocks north of the controller, its bays 4, 3 and 2 blocks west of it, at its height
- **THEN** the shelf's code is `N8.W2+0` and aiming at any of its bays shows `N8.W2+0`

#### Scenario: Stacked shelf
- **WHEN** a second shelf of three bays stands on top of that shelf
- **THEN** its code is `N8.W2+1`

### Requirement: Which shelves have a code
A shelf of a warehouse without a controller, or of no warehouse, SHALL have no code and SHALL show dashes on the handheld.

#### Scenario: No controller
- **WHEN** a rack stands on a rail network without a controller and a player aims at it with the handheld
- **THEN** the panel shows dashes as its code

### Requirement: Codes follow the controller and the shelf
When a warehouse gets a controller, loses it, a shelf joins or leaves the warehouse, or a shelf gains or loses a bay, the affected shelves SHALL show their new code or no code, without the player touching them. Moving the controller SHALL give every shelf of the warehouse a new code, chosen again from its bays.

#### Scenario: Controller moved
- **WHEN** a single bay shelf reads `N8.E0+0` and the controller is dismantled and placed one block further east
- **THEN** the shelf reads `N8.W1+0`

#### Scenario: Another bay becomes the code bay
- **WHEN** the shelf that reads `N8.W2+0` from bays 4, 3 and 2 blocks west of the controller sees the controller moved 3 blocks west
- **THEN** the middle bay is now the closest and the shelf reads `N8.E0+0`

#### Scenario: Shelf loses its code bay
- **WHEN** the shelf reads `N8.W2+0` from bays 4, 3 and 2 blocks west of the controller and the bay 2 blocks west is broken
- **THEN** the two bays left form a shelf that reads `N8.W3+0`

### Requirement: Code on the rack beam
A shelf of Heavy Duty Racks or of Fluid Racks with a code SHALL show it once, on a small white sticker with a barcode on the top beam of its code bay, to every player who can see the shelf. The other bays of the shelf SHALL show no sticker, and a shelf without a code SHALL show none.

#### Scenario: Looking at a shelf
- **WHEN** a player looks at the front of the shelf `N8.W2+0` of three bays
- **THEN** one sticker reading `N8.W2+0` with a barcode is drawn on the top beam of the bay 2 blocks west of the controller, and none on the other two

#### Scenario: Fluid Rack shelf
- **WHEN** a player looks at a Fluid Rack shelf of three bays with a code
- **THEN** one sticker with that code is drawn on the top beam of its code bay

### Requirement: Code logic is pure
Building the code from the three offsets and choosing the code bay of a shelf from its bays' offsets SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover each of the four directions, zero distances, positive, zero and negative heights, the closest bay of a shelf, a tie broken to the leftmost bay, and a shelf of one bay

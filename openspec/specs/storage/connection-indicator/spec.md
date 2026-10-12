# storage/connection-indicator Specification

## Purpose

The connection indicator shows on a storage block itself whether it is part of a working warehouse.

## Requirements

### Requirement: Lamp state is shared and event driven
The lamp state SHALL be visible to every player without holding a tool, and SHALL change only when a block joins or leaves the network or the controller count changes.

#### Scenario: Second player
- **WHEN** another player looks at a lit controller
- **THEN** that player sees it lit too

#### Scenario: Idle warehouse
- **WHEN** an online warehouse is left alone for a minute
- **THEN** no block state of its storage blocks changes

### Requirement: Lamp on controllers and terminals
Warehouse controllers and storage terminals SHALL show a small lamp on their front face. The lamp SHALL be lit while the block belongs to an online warehouse network and dark otherwise; a storage terminal's lamp SHALL be lit while a warehouse is reachable over its data line. Heavy Duty Racks SHALL carry no lamp.

#### Scenario: Controller joins a warehouse
- **WHEN** a controller is placed beside a rail that leads to a rack front and no other controller
- **THEN** the controller's lamp lights up within a tick

#### Scenario: Controller removed
- **WHEN** the only controller of a warehouse is broken
- **THEN** the lamps of the terminals that reached only that warehouse go dark

#### Scenario: Second controller
- **WHEN** a second controller is placed beside a rail of an online warehouse
- **THEN** all lamps in that warehouse go dark until one controller is removed again

#### Scenario: Rack has no lamp
- **WHEN** a Heavy Duty Rack joins an online warehouse
- **THEN** nothing on the rack lights up

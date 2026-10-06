# Spec Delta

## Purpose

The connection indicator shows on a storage block itself whether it is part of a working warehouse.

## ADDED Requirements

### Requirement: Lamp on storage blocks
Crates, shelving racks, warehouse controllers and counter terminals SHALL show a small lamp on their front face. The lamp SHALL be lit while the block belongs to an online warehouse network and dark otherwise.

#### Scenario: Crate joins a warehouse
- **WHEN** a crate is placed beside a rail of a warehouse with one controller
- **THEN** the crate's lamp lights up within a tick

#### Scenario: Crate alone
- **WHEN** a crate is placed with no storage block or rail beside it
- **THEN** its lamp stays dark

#### Scenario: Controller removed
- **WHEN** the only controller of a warehouse is broken
- **THEN** the lamps of all its crates and racks go dark

#### Scenario: Second controller
- **WHEN** a second controller is placed into an online warehouse
- **THEN** all lamps in that warehouse go dark until one controller is removed again

### Requirement: Lamp state is shared and event driven
The lamp state SHALL be visible to every player without holding a tool, and SHALL change only when a block joins or leaves the network or the controller count changes.

#### Scenario: Second player
- **WHEN** another player looks at a lit crate
- **THEN** that player sees it lit too

#### Scenario: Idle warehouse
- **WHEN** an online warehouse is left alone for a minute
- **THEN** no block state of its storage blocks changes

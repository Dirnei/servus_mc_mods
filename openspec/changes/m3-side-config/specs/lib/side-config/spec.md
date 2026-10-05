# Spec Delta

## Purpose

One vocabulary for what a block side does, relative to the block's facing, so machines, ducts and future blocks configure sides the same way.

## ADDED Requirements

### Requirement: Side modes
A side SHALL have exactly one mode out of Auto, Input, Output, Both and Disabled. Input and Both allow insertion, Output and Both allow extraction and pushing, Auto allows everything, Disabled allows nothing. Cycling moves Auto to Input to Output to Both to Disabled and back to Auto.

#### Scenario: Cycle
- **WHEN** a side in Auto is cycled five times
- **THEN** it passes Input, Output, Both, Disabled and is Auto again

#### Scenario: Permissions
- **WHEN** a side is Output
- **THEN** it allows extraction and pushing and refuses insertion

### Requirement: Relative sides follow the facing
Sides SHALL be addressed as front, back, left, right, top and bottom relative to the block's facing, so rotating a block keeps its configuration.

#### Scenario: Facing north
- **WHEN** a block faces north
- **THEN** front is north, back is south, left is west, right is east, top is up, bottom is down

#### Scenario: Facing east
- **WHEN** a block faces east
- **THEN** front is east, back is west, left is north, right is south

### Requirement: Default is Auto
A new side configuration SHALL have every side in Auto, so blocks behave as before configuration.

#### Scenario: Fresh config
- **WHEN** a side configuration is created
- **THEN** all six sides are Auto and allow both insertion and extraction

### Requirement: Pure Java
The side configuration types SHALL have no Minecraft dependency.

#### Scenario: Unit tests
- **WHEN** the side config tests run under `gradlew :lib:test`
- **THEN** modes, cycling and relative side mapping are verified without the game

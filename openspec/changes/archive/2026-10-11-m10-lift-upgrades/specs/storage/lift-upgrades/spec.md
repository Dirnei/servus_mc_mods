# Spec Delta

## Purpose

Lets players speed up the lift car of an elevator shaft with Speed upgrades, fitted in a small GUI on any segment and kept in the shaft's lowest segment.

## ADDED Requirements

### Requirement: A shaft has Speed upgrade slots
Every elevator shaft SHALL have a number of upgrade slots set in the warehouse stats file (example `lift_upgrade_slots` 4). A slot SHALL take exactly one Foundation Speed Upgrade and SHALL refuse every other item. The items SHALL be stored in the shaft's lowest segment.

#### Scenario: Only Speed upgrades fit
- **WHEN** a player tries to put a Handling Upgrade and then a stack of three Speed Upgrades into an empty shaft slot
- **THEN** the Handling Upgrade does not go in, and one Speed Upgrade goes in while two stay in the player's hand

### Requirement: The elevator GUI opens from any segment
A right click with an empty hand on any segment of a shaft SHALL open the elevator GUI, which shows the upgrade slots of that shaft and the player inventory. A click with an item in hand SHALL keep its own effect: an elevator item builds the shaft up and the Configuration Carrot dismantles. Climbing, standing on and sinking through segments SHALL stay as they are. Shift clicking SHALL move Speed upgrades between the slots and the player inventory.

#### Scenario: Open from the top
- **WHEN** a player with an empty hand right clicks the top segment of a three segment shaft and puts two Speed Upgrades into the GUI
- **THEN** the lowest segment of the shaft holds the two upgrades

#### Scenario: Elevator item still builds
- **WHEN** a player right clicks a segment with an elevator item in hand
- **THEN** a new segment is placed on top of the shaft and no GUI opens

### Requirement: Upgrades follow the lowest segment
When the lowest segment of a shaft changes, its upgrades SHALL move with it, with no scan per tick:
- A segment placed directly below a shaft SHALL become its lowest segment and take its upgrades.
- When the lowest segment is removed and the segment above still stands (the block that took its place holds it up), the segment above SHALL become the lowest and take the upgrades. When the segments above lose their footing and break like scaffolding, the upgrades SHALL drop where the lowest segment was.
- A segment placed between two shafts SHALL join them; the upper shaft's upgrades SHALL move into the joined shaft's lowest segment, and upgrades that find no free slot SHALL drop.
- When a segment in the middle of a shaft is broken, the lower part SHALL keep the upgrades; the segments above it lose their footing and break.

#### Scenario: Segment placed below
- **WHEN** a shaft with two Speed Upgrades gets a new segment placed under its lowest one
- **THEN** the new lowest segment holds the two upgrades and the old one holds none

#### Scenario: Bottom replaced by a block the shaft stands on
- **WHEN** the lowest segment of a three segment shaft with two Speed Upgrades is replaced by stone
- **THEN** the segment above holds the two upgrades and none drop

#### Scenario: Bottom broken
- **WHEN** the lowest segment of a three segment shaft with two Speed Upgrades is broken
- **THEN** the shaft above breaks too and the two upgrades drop where the lowest segment was

### Requirement: Speed upgrades make the car faster
Each Speed Upgrade in a shaft SHALL raise its car's top speed and its acceleration by amounts from the warehouse stats file (examples `lift_speed_per_upgrade` 0.05 and `lift_accel_per_upgrade` 0.0025 blocks per tick, on top of `lift_speed` and `lift_accel`). The car SHALL read its upgrade count when the shaft or its upgrades change, never per tick. Clients SHALL draw the car with the same speed and acceleration.

#### Scenario: Upgraded car
- **WHEN** two equal shafts carry a shuttle the same way, one with four Speed Upgrades and one without
- **THEN** the upgraded car arrives first

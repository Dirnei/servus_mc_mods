# shuttles/shuttle-numbers Specification

## Purpose
Shuttle numbers give every shuttle of a world a stable number and a short label, so players and later features such as slot locks can name the one shuttle that holds a job.

## Requirements

### Requirement: A shuttle is numbered in its first garage
A shuttle item without a number SHALL get one when it is put into a Shuttle Garage, by hand, by shift click or by any other way into a garage slot. Numbers SHALL count up per world, starting at 1, and no number SHALL be given twice in one world, whichever dimension the garage stands in. A shuttle that already has a number SHALL keep it.

#### Scenario: First shuttles of a world
- **WHEN** a player in a new world puts two crafted Mk1 shuttles into a garage, one after the other
- **THEN** the first shuttle is number 1 and the second is number 2

#### Scenario: Number survives a reload
- **WHEN** shuttles 1 and 2 were numbered, the world is saved and loaded again, and a third shuttle is put into a garage
- **THEN** the third shuttle is number 3

#### Scenario: Another dimension
- **WHEN** shuttle 3 was numbered in the overworld and a new shuttle goes into a garage in the Nether
- **THEN** the new shuttle is number 4

### Requirement: The number stays on the shuttle item
The number SHALL be stored on the shuttle item. Taking a shuttle out of a garage, putting it into another garage, fitting or removing modules, and building it into a higher tier in the Shuttle Workshop SHALL keep its number. A shuttle outside a garage that never was in one SHALL have no number.

#### Scenario: Out and back in
- **WHEN** shuttle 5 is taken out of its garage and put into a different garage
- **THEN** it is still shuttle 5 and the world's next number does not change

#### Scenario: Tier build keeps the number
- **WHEN** Mk1 shuttle 5 is built into a Mk2 in the workshop
- **THEN** the Mk2 shuttle is shuttle 5

#### Scenario: Freshly crafted
- **WHEN** a player crafts a Mk1 shuttle and looks at it in the inventory
- **THEN** it has no number yet

### Requirement: Shuttles in existing garages get numbers when they load
A garage that loads with shuttles that have no number, for example from a world saved before shuttle numbers existed, SHALL number them in slot order when it loads. Such worlds SHALL load without errors.

#### Scenario: Old world
- **WHEN** a world saved before shuttle numbers loads a garage holding three shuttles
- **THEN** the three shuttles get the next three numbers in slot order and the warehouse works as before

### Requirement: Shuttle label
A shuttle SHALL be named by its label wherever a single shuttle is named. A numbered shuttle without a name plate SHALL read "Shuttle #12"; a numbered shuttle with the plate "Berta" SHALL read "Berta (#12)". A shuttle without a number SHALL be named as before. A numbered shuttle item SHALL use its label as its item name and SHALL name its tier in the tooltip. An anvil name SHALL still win in the item name.

#### Scenario: Plain shuttle in the garage
- **WHEN** the player hovers over Mk1 shuttle 12 without a plate in a garage slot
- **THEN** the tooltip title reads "Shuttle #12" and a line below names Shuttle Mk1

#### Scenario: Engraved shuttle
- **WHEN** the player hovers over Mk2 shuttle 12 with the plate "Berta"
- **THEN** the tooltip title reads "Berta (#12)" and a line below names Shuttle Mk2

#### Scenario: Unnumbered shuttle
- **WHEN** the player hovers over a freshly crafted Mk1 shuttle
- **THEN** the tooltip title reads "Shuttle Mk1" as before

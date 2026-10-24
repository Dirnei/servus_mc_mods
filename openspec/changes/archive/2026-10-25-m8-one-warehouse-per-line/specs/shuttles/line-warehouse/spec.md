# Spec Delta

## Purpose

Decides which warehouse a data line belongs to, and what the blocks on a line do when it reaches more than one warehouse (the conflict state).

## ADDED Requirements

### Requirement: A data line belongs to one warehouse
A data line SHALL belong to the warehouse whose Warehouse Controller or Packing Station is a member of it. Blocks on the line that use a warehouse (Storage Terminal, Production Terminal, Crafting CPU, Crafting Station, Packing Station, the warehouse supplier) SHALL use only that warehouse, and only while it is online. A line with no such member SHALL belong to no warehouse.

#### Scenario: Controller and Packing Station of the same warehouse
- **WHEN** a data line has the controller of the base and a Packing Station on the base's rails as members
- **THEN** the line belongs to the base and its Storage Terminal shows the base's stock

#### Scenario: Packing Station only
- **WHEN** the only warehouse member of a line is a Packing Station on the base's rails
- **THEN** the line belongs to the base

### Requirement: Two warehouses put a line in conflict
A data line whose members belong to two or more warehouses SHALL be in conflict, whether those warehouses are online or not. The conflict SHALL be found again whenever members join or leave the line, with no player action, and SHALL end as soon as the line reaches one warehouse again.

#### Scenario: Two controllers
- **WHEN** a duct joins a line holding the controller of the base with a line holding the controller of the outpost
- **THEN** the joined line is in conflict

#### Scenario: Offline warehouse still counts
- **WHEN** a line holds the base's controller and a Packing Station of a second warehouse whose controller has no power
- **THEN** the line is in conflict

#### Scenario: Split again
- **WHEN** the duct that joined the two lines is broken
- **THEN** neither line is in conflict and each belongs to its own warehouse again

### Requirement: A line in conflict pauses its warehouse work
On a line in conflict, Storage Terminals SHALL show no stock and refuse new requests, and their Input row SHALL keep its items. Crafting CPUs SHALL start no step that needs warehouse stock, and Packing Stations SHALL start no data line delivery. The warehouse supplier SHALL answer 0, and Crafting Stations SHALL draw no power from a warehouse. Each warehouse SHALL keep running its racks, shuttles, put-away and wireless transfers.

#### Scenario: Request refused
- **WHEN** a player requests cobblestone at a Storage Terminal on a line in conflict
- **THEN** no request is made and the message line says that two warehouses are on this data line

#### Scenario: Input row waits
- **WHEN** the player puts logs into the Input row of a terminal on a line in conflict
- **THEN** the logs stay in the Input row

#### Scenario: Warehouses keep working
- **WHEN** a line is in conflict while a shuttle of the base puts away planks from an Inbound Station
- **THEN** the planks reach the rack as before

### Requirement: Open work waits through a conflict
Open terminal requests and work orders of a line SHALL NOT fail or end because the line is in conflict. They SHALL wait and SHALL go on by themselves once the line belongs to one warehouse again. Packets already on the line when the conflict starts SHALL still land.

#### Scenario: Request resumes
- **WHEN** a request for 64 planks waits while its line is in conflict and the player breaks the joining duct
- **THEN** the request goes on and the planks arrive in its output slot

### Requirement: Conflict is shown
Storage Terminals, Production Terminals, Crafting CPUs and Packing Stations on a line in conflict SHALL say "Two warehouses on this data line" in the alert colour where they show their status: the terminal's message line, the Production Terminal's Storage tab, the CPU's summary and the Packing Station's status line. The online lamp of Storage and Production Terminals on such a line SHALL be dark.

#### Scenario: Packing Station status
- **WHEN** the player opens a Packing Station on a line in conflict
- **THEN** its status line says that two warehouses are on this data line, in the alert colour

#### Scenario: Lamp
- **WHEN** a Storage Terminal's line goes into conflict
- **THEN** its lamp goes dark

### Requirement: Line classification is pure logic
Classifying the warehouses found among a line's members into none, one or a conflict SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Same warehouse twice
- **WHEN** the members name the same warehouse through its controller and two Packing Stations
- **THEN** the classification is that one warehouse

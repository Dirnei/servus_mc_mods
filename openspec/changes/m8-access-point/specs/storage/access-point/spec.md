# Spec Delta

## Purpose

The Access Point is the warehouse's door to the data line: shuttles bring items to it and it sends them as packets, and packets that arrive at it are put away like an inbound delivery.

## ADDED Requirements

### Requirement: Access Point joins rails and the data line
The Access Point SHALL be a block that links to rails on every side like a station, so shuttles can reach it, and SHALL be a link member of the data line it touches. It SHALL hold no stock of the warehouse and SHALL show the online lamp like other warehouse blocks.

#### Scenario: Both networks
- **WHEN** an Access Point stands beside a rail of a warehouse and a duct with the data line touches it
- **THEN** it belongs to that warehouse and is a link member of that data network

#### Scenario: Not stock
- **WHEN** items wait in an Access Point's buffers
- **THEN** the warehouse stock does not count them

### Requirement: Packets are put away
Packets delivered to an Access Point SHALL land in its intake buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) and the warehouse SHALL put them away by its zone rules like items in an Inbound Station, by shuttle for racks.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered as packets to an Access Point of a warehouse with a crate that takes planks
- **THEN** the planks end up in the crate and the intake buffer is empty again

### Requirement: Shipping to a data member
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan the picks like a terminal request: crates and foreign inventories deliver at once, racks by shuttle, into the outgoing buffer of the chosen Access Point; the Access Point SHALL send what arrives as packets to the member, one packet stream at a time. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Access Point on a data line reaching the member, or has none of the item.

#### Scenario: From a crate
- **WHEN** a warehouse with 32 planks in a crate and an Access Point on a data line to a Crafting Station ships 16 planks to the station
- **THEN** 16 planks arrive in the station's inputs as packets and the request reports 16 delivered and ended

#### Scenario: From a rack
- **WHEN** the planks sit only in a Heavy Duty Rack served by a shuttle
- **THEN** a shuttle brings them to the Access Point first, then they travel as packets to the station

#### Scenario: No Access Point
- **WHEN** the warehouse has no Access Point on a data line that reaches the member
- **THEN** the request is refused and nothing leaves the warehouse

### Requirement: Shortest shuttle trip
When several Access Points of a warehouse reach the member, the warehouse SHALL ship through the one with the shortest shuttle trip from the racks holding the item; on a tie, and when the items need no shuttle, any of them.

#### Scenario: Two Access Points
- **WHEN** a rack with planks is 2 rails away from Access Point A and 8 rails away from Access Point B, both on the data line to the station
- **THEN** the shipment goes through Access Point A

### Requirement: Access Point is obtainable
The Access Point SHALL be crafted from iron ingots, a chest and redstone with copper (example), drop itself and the contents of both buffers when broken, and be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Broken with items
- **WHEN** an Access Point holding planks in its outgoing buffer is broken
- **THEN** the Access Point and the planks drop

### Requirement: Ship command
`/servus_warehouse ship <controller> <target> <item> <count>` SHALL let an op ship items from the warehouse of the controller to a data member and report the refusal or the planned amount.

#### Scenario: Ship planks
- **WHEN** an op runs the command for 16 planks to a Crafting Station on the data line
- **THEN** chat says 16 planks are planned and they arrive at the station

# storage/packing-station Specification

## Purpose
The Packing Station is the warehouse's door to the data line: shuttles bring items to it and it sends them as packets, and packets that arrive at it are put away like an inbound delivery.

## Requirements

### Requirement: Packing Station joins rails and the data line
The Packing Station SHALL be a block that joins the warehouse only through rails on its sides, like a station, so shuttles can reach it; it SHALL NOT link to touching controllers, storage blocks, racks, garages or stations. It SHALL be a link member of the data line it touches, SHALL hold no stock of the warehouse and SHALL show the online lamp like other warehouse blocks.

#### Scenario: Both networks
- **WHEN** a Packing Station stands beside a rail of a warehouse and a duct with the data line touches it
- **THEN** it belongs to that warehouse and is a link member of that data network

#### Scenario: Not stock
- **WHEN** items wait in a Packing Station's buffers
- **THEN** the warehouse stock does not count them

#### Scenario: Touching the controller is not enough
- **WHEN** a Packing Station touches the controller and no rail touches it
- **THEN** it does not belong to that warehouse and its lamp is dark

### Requirement: Packets are put away
Packets delivered to a Packing Station SHALL land in its intake buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) and the warehouse SHALL put them away by its zone rules like items in an Inbound Station, by shuttle for racks.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered as packets to a Packing Station of a warehouse with a crate that takes planks
- **THEN** the planks end up in the crate and the intake buffer is empty again

### Requirement: Shipping to a data member
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan the picks like a terminal request: crates and foreign inventories deliver at once, racks by shuttle, into the outgoing buffer of the chosen Packing Station; the Packing Station SHALL send what arrives as packets to the member, one packet stream at a time. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, or has none of the item.

#### Scenario: From a crate
- **WHEN** a warehouse with 32 planks in a crate and a Packing Station on a data line to a Crafting Station ships 16 planks to the station
- **THEN** 16 planks arrive in the station's inputs as packets and the request reports 16 delivered and ended

#### Scenario: From a rack
- **WHEN** the planks sit only in a Heavy Duty Rack served by a shuttle
- **THEN** a shuttle brings them to the Packing Station first, then they travel as packets to the station

#### Scenario: No Packing Station
- **WHEN** the warehouse has no Packing Station on a data line that reaches the member
- **THEN** the request is refused and nothing leaves the warehouse

### Requirement: Shortest shuttle trip
When several Packing Stations of a warehouse reach the member, the warehouse SHALL ship through the one with the shortest shuttle trip from the racks holding the item; on a tie, and when the items need no shuttle, any of them.

#### Scenario: Two Packing Stations
- **WHEN** a rack with planks is 2 rails away from Packing Station A and 8 rails away from Packing Station B, both on the data line to the station
- **THEN** the shipment goes through Packing Station A

### Requirement: Packing Station is obtainable
The Packing Station SHALL be crafted from iron ingots, a chest, redstone, copper and planks (example), recording the wood of the planks, drop itself with its wood and the contents of both buffers when broken, and be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Broken with items
- **WHEN** a Packing Station holding planks in its outgoing buffer is broken
- **THEN** the Packing Station and the planks drop

### Requirement: Ship command
`/servus_warehouse ship <controller> <target> <item> <count>` SHALL let an op ship items from the warehouse of the controller to a data member and report the refusal or the planned amount.

#### Scenario: Ship planks
- **WHEN** an op runs the command for 16 planks to a Crafting Station on the data line
- **THEN** chat says 16 planks are planned and they arrive at the station

### Requirement: Warehouse as supplier
The Warehouse Controller and every Packing Station SHALL expose the supplier capability. A request SHALL be shipped like any shipment to a data member: through the Packing Station with the shortest shuttle trip, as packets. The answer SHALL be the amount planned, at most what the warehouse holds, and zero when the warehouse cannot ship.

#### Scenario: Upkeep from the warehouse
- **WHEN** a Machine Adapter on the line asks the controller's supplier for 16 coal and a crate holds 40
- **THEN** the answer is 16 and 16 coal arrive at the adapter as packets

#### Scenario: Out of stock
- **WHEN** the warehouse holds no coal
- **THEN** the supplier answers 0

### Requirement: Intake GUI stays open
Right clicking a Packing Station SHALL open its intake GUI, which SHALL stay open while the player is in reach, like the Inbound Station's.

#### Scenario: Open the GUI
- **WHEN** the player right clicks a Packing Station
- **THEN** the intake GUI opens and stays open

### Requirement: Shipments survive a reload
A Packing Station SHALL save its open shipments with their ids, targets, items and amounts, and their progress SHALL be kept by id so that whoever waits for a shipment can book it after a reload. After its block entity loads again, a shipment SHALL only wait for what the outgoing buffer holds for it; the rest SHALL end as a shortfall, so its owner ships it again. Items that arrive later for no open shipment SHALL be put away as before.

#### Scenario: Restart while items wait in the outgoing buffer
- **WHEN** a shipment of 16 planks has 8 planks in the outgoing buffer and 8 still on a shuttle when the server restarts
- **THEN** after the restart the shipment sends the 8 planks, ends 8 short, and its requester ships the 8 again

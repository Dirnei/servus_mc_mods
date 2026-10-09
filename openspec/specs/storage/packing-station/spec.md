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
Packets delivered to a Packing Station SHALL land in its To storage buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) at no more than the station's conversion rate into storage, and only while that buffer has room; the warehouse SHALL put the buffer's items away by its zone rules like items in an Inbound Station, by shuttle for racks. Items a full or busy station cannot take yet SHALL stay at their source.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered to a Packing Station of a warehouse with a crate that takes planks
- **THEN** the planks end up in the crate and the To storage buffer is empty again

#### Scenario: To storage full
- **WHEN** the To storage buffer is full because no shuttle is free and a terminal sends 64 cobblestone
- **THEN** the cobblestone stays in the terminal's Input row until the buffer has room again

### Requirement: Shipping to a data member
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan the picks like a terminal request: crates and foreign inventories deliver at once, racks by shuttle, into the To network buffer of the chosen Packing Station, never faster than that buffer has room; the Packing Station SHALL send what arrives to the member at no more than its conversion rate out of storage. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, or has none of the item.

#### Scenario: From a crate
- **WHEN** a warehouse with 32 planks in a crate and a Packing Station on a data line to a Crafting Station ships 16 planks to the station
- **THEN** 16 planks arrive in the station's inputs and the request reports 16 delivered and ended

#### Scenario: From a rack
- **WHEN** the planks sit only in a Heavy Duty Rack served by a shuttle
- **THEN** a shuttle brings them to the Packing Station first, then they go on to the station

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

### Requirement: Shipments survive a reload
A Packing Station SHALL save its open shipments with their ids, targets, items and amounts, and their progress SHALL be kept by id so that whoever waits for a shipment can book it after a reload. After its block entity loads again, a shipment SHALL only wait for what the outgoing buffer holds for it; the rest SHALL end as a shortfall, so its owner ships it again. Items that arrive later for no open shipment SHALL be put away as before.

#### Scenario: Restart while items wait in the outgoing buffer
- **WHEN** a shipment of 16 planks has 8 planks in the outgoing buffer and 8 still on a shuttle when the server restarts
- **THEN** after the restart the shipment sends the 8 planks, ends 8 short, and its requester ships the 8 again

### Requirement: Conversion rate per direction
A Packing Station SHALL convert items to packets and packets to items at `items_per_second` per direction (example 8, datapack JSON, read on load and reload). The direction into storage and the direction out of storage SHALL each have their own rate and SHALL NOT slow each other down. Several Packing Stations SHALL each add their own rate.

#### Scenario: Eight per second
- **WHEN** a terminal sends 64 cobblestone through one Packing Station with a rate of 8 and an empty To storage buffer with room for all of it
- **THEN** about 8 cobblestone leave the terminal's Input row each second, and all 64 are in the buffer after about 8 seconds

#### Scenario: Both directions at once
- **WHEN** a station ships 64 planks out while a terminal sends 64 cobblestone in
- **THEN** both move at about 8 per second each

#### Scenario: Two stations
- **WHEN** a warehouse ships 64 planks to two members through two Packing Stations at the same time
- **THEN** both shipments move at about 8 per second each

### Requirement: Read-only buffer GUI
Right clicking a Packing Station SHALL open a GUI with two labelled rows of slots, To storage (items waiting for put-away) and To network (picked items waiting to be sent), each with a tooltip explaining it, and a status line naming the current delivery (item and target) or why the station waits (no rail, no data line, waiting for a shuttle, To storage full). Players SHALL NOT be able to take items out of or put items into either row. The GUI SHALL stay open while the player is in reach.

#### Scenario: Look but do not touch
- **WHEN** the To network row holds 16 planks and the player clicks one of its slots
- **THEN** the planks stay in the slot and nothing lands on the cursor

#### Scenario: Status
- **WHEN** the station sends planks to a Crafting Station
- **THEN** the status line names the planks and the Crafting Station

### Requirement: Packing Station tooltip
The Packing Station item SHALL carry the tooltip "Packs items from the warehouse into packets for the data line, and unpacks arriving packets for storage".

#### Scenario: Hover
- **WHEN** the player hovers the Packing Station in an inventory
- **THEN** the tooltip explains that it packs and unpacks items between the warehouse and the data line

### Requirement: Speed upgrades
A Packing Station SHALL have `upgrade_slots` upgrade slots (example 2, datapack JSON) shown in its GUI, the only slots of that GUI the player can change. Its conversion rate in both directions SHALL be its base `items_per_second` multiplied by its upgrades, and the GUI SHALL show the current rate.

#### Scenario: Doubled
- **WHEN** a Packing Station with a base rate of 8 holds one Speed Upgrade of multiplier 2
- **THEN** it converts about 16 items per second in each direction and its GUI shows 16/s

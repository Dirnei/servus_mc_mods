## ADDED Requirements

### Requirement: Packing Stations carry wireless transfers
Every Packing Station on a site's rails SHALL be able to send and receive wireless transfers, with or without a data line. A sending station SHALL take the picked items from its To network buffer and hand them to the receiving station. A transfer requested at a Storage Terminal SHALL land in the receiving station's To network buffer, only while that buffer has room, and leave from there as packets to the requesting terminal like a data line delivery of a local request; it SHALL NOT be put away. A transfer without a requesting terminal (operator commands, tests) SHALL land in the To storage buffer, only while that buffer has room, and be put away by the zone rules. A transfer SHALL use one sending and one receiving station. The receiving station SHALL be, among the stations of its site that reach the requesting terminal by data line, the one with the fewest open incoming transfers, the lowest position on a tie.

#### Scenario: Station without a data line
- **WHEN** a Packing Station stands on the outpost's rails with no duct touching it and the base requests iron from the outpost
- **THEN** the iron is picked into that station and sent to a Packing Station of the base

#### Scenario: Passed on to the terminal
- **WHEN** 16 iron ingots from the outpost arrive at the base's Packing Station for a terminal request
- **THEN** they go on as packets into that terminal's output slots and no shuttle puts them away

#### Scenario: Receiver full
- **WHEN** the receiving station's To network buffer is full
- **THEN** the sending station keeps the items in its To network buffer until the buffer has room

#### Scenario: Two receivers
- **WHEN** the base has two Packing Stations and two transfers arrive at the same time
- **THEN** each station receives one of them

### Requirement: Wireless rate is the Packing Stations' rate
A wireless transfer SHALL move no faster than the sending station's conversion rate out of storage and the receiving station's conversion rate into storage, Speed Upgrades included. Wireless transfers and data line deliveries of one station SHALL share that station's rate in each direction.

#### Scenario: Eight per second
- **WHEN** 64 items are sent between two sites through Packing Stations of rate 8 without upgrades, with enough FE and room
- **THEN** about 8 items arrive each second

#### Scenario: Upgraded receiver, plain sender
- **WHEN** the sending station has rate 8 and the receiving one rate 16
- **THEN** about 8 items arrive each second

## MODIFIED Requirements

### Requirement: Packing Station joins rails and the data line
The Packing Station SHALL be a block that joins the warehouse only through rails on its sides, like a station, so shuttles can reach it; it SHALL NOT link to touching controllers, storage blocks, racks, garages or stations. It SHALL be a link member of the data line it touches, if any; a Packing Station without a data line SHALL still belong to its warehouse and serve wireless transfers. It SHALL hold no stock of the warehouse and SHALL show the online lamp like other warehouse blocks.

#### Scenario: Both networks
- **WHEN** a Packing Station stands beside a rail of a warehouse and a duct with the data line touches it
- **THEN** it belongs to that warehouse and is a link member of that data network

#### Scenario: Rails only
- **WHEN** a Packing Station stands beside a rail of a warehouse and no duct touches it
- **THEN** it belongs to that warehouse, its lamp is lit, and it can send and receive wireless transfers

#### Scenario: Not stock
- **WHEN** items wait in a Packing Station's buffers
- **THEN** the warehouse stock does not count them

#### Scenario: Touching the controller is not enough
- **WHEN** a Packing Station touches the controller and no rail touches it
- **THEN** it does not belong to that warehouse and its lamp is dark

### Requirement: Read-only buffer GUI
Right clicking a Packing Station SHALL open a GUI with two labelled rows of slots, To storage (items waiting for put-away) and To network (picked items waiting to be sent), each with a tooltip explaining it, and a status line naming the current delivery (item and target) or wireless transfer (item and the other site's name), or why the station waits (no rail, waiting for a shuttle, To storage full, the receiving site has no room, a site of the transfer has no power for wireless). A station without a data line SHALL say so as plain information, not as an alert, while its controller is wireless capable. Players SHALL NOT be able to take items out of or put items into either row. The GUI SHALL stay open while the player is in reach.

#### Scenario: Look but do not touch
- **WHEN** the To network row holds 16 planks and the player clicks one of its slots
- **THEN** the planks stay in the slot and nothing lands on the cursor

#### Scenario: Status
- **WHEN** the station sends planks to a Crafting Station
- **THEN** the status line names the planks and the Crafting Station

#### Scenario: Wireless status
- **WHEN** the outpost's station sends iron to the base
- **THEN** its status line names the iron and the base

#### Scenario: Waiting for power
- **WHEN** a transfer of the station waits because the base's controller has no power
- **THEN** the status line says that the base has no power for wireless, in the alert colour

### Requirement: Packing Station tooltip
The Packing Station item SHALL carry the tooltip "Packs items from the warehouse into packets for the data line and for other sites, and unpacks arriving items for storage".

#### Scenario: Hover
- **WHEN** the player hovers the Packing Station in an inventory
- **THEN** the tooltip explains that it packs and unpacks items between the warehouse and the data line or other sites

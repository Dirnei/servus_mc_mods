# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Intake GUI stays open
**Reason**: The intake GUI showed one buffer and let players take items out of system buffers.
**Migration**: See "Read-only buffer GUI".

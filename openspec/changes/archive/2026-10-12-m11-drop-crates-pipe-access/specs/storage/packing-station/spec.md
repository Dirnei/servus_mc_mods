## MODIFIED Requirements

### Requirement: Packets are put away
Packets delivered to a Packing Station SHALL enter through its packet inlet and land in its To storage buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) at no more than the station's conversion rate into storage, and only while that buffer has room; the warehouse SHALL put the buffer's items away by its zone rules like items in an Inbound Station, by shuttle into racks. Items a full or busy station cannot take yet SHALL stay at their source.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered to a Packing Station of a warehouse with a rack that takes planks and a garage with a shuttle
- **THEN** a shuttle puts the planks into the rack and the To storage buffer is empty again

#### Scenario: To storage full
- **WHEN** the To storage buffer is full because no shuttle is free and a terminal sends 64 cobblestone
- **THEN** the cobblestone stays in the terminal's Input row until the buffer has room again

### Requirement: Warehouse as supplier
The Warehouse Controller and every Packing Station SHALL expose the supplier capability. A request SHALL be shipped like any shipment to a data member: through the Packing Station with the shortest shuttle trip, as packets. The answer SHALL be the amount planned, at most what the warehouse holds, and zero when the warehouse cannot ship.

#### Scenario: Upkeep from the warehouse
- **WHEN** a Machine Adapter on the line asks the controller's supplier for 16 coal and a rack served by a shuttle holds 40
- **THEN** the answer is 16 and 16 coal arrive at the adapter as packets

#### Scenario: Out of stock
- **WHEN** the warehouse holds no coal
- **THEN** the supplier answers 0

## ADDED Requirements

### Requirement: Shipping to a data member by shuttle
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan the picks like a terminal request: shuttles pick the items from the racks into the To network buffer of the chosen Packing Station, never faster than that buffer has room; the Packing Station SHALL send what arrives to the member at no more than its conversion rate out of storage. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, has none of the item, or has no shuttle that reaches the racks holding it.

#### Scenario: From a rack
- **WHEN** a warehouse with 32 planks in a rack served by a shuttle and a Packing Station on a data line to a Crafting Station ships 16 planks to the station
- **THEN** a shuttle brings the planks to the Packing Station first, then 16 planks arrive in the station's inputs and the request reports 16 delivered and ended

#### Scenario: No shuttle
- **WHEN** the planks sit in a rack and the warehouse has no garage
- **THEN** the request is refused because it needs a shuttle and nothing leaves the warehouse

#### Scenario: No Packing Station
- **WHEN** the warehouse has no Packing Station on a data line that reaches the member
- **THEN** the request is refused and nothing leaves the warehouse

### Requirement: Packing Station has no pipe access
A Packing Station SHALL register no item capability: hoppers, item pipes and neighbouring machines SHALL neither insert into nor extract from either buffer, and item pipes SHALL show no item arm toward it. Items SHALL enter it only from shuttles and as packets through its packet inlet, and leave it only to shuttles and as packets.

#### Scenario: Hopper above the station
- **WHEN** a hopper above a Packing Station holds cobblestone
- **THEN** the cobblestone stays in the hopper

#### Scenario: Machine pushes against it
- **WHEN** a machine beside a Packing Station tries to push its outputs into it
- **THEN** nothing goes in and the outputs stay in the machine

#### Scenario: Item pipe beside the station
- **WHEN** a duct with the Item Pipe is placed beside a Packing Station
- **THEN** the duct shows no item arm toward it, while its data line still connects

## REMOVED Requirements

### Requirement: Shipping to a data member
**Reason**: Shipments were also picked instantly from crates and foreign inventories, which are no longer part of a warehouse.
**Migration**: See "Shipping to a data member by shuttle".

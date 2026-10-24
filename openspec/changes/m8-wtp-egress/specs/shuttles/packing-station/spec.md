## MODIFIED Requirements

### Requirement: Shipping to a data member by shuttle
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan and lock the picks like a terminal request, but shuttles SHALL bring picks into the To network buffer of the chosen Packing Station only as far as the member has granted room and the buffer has room. The station SHALL send what arrives at no more than its rate out of storage. A shipment its member refuses SHALL wait without holding up the station's other shipments and be offered again every push interval. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, has none of the item, or has no shuttle that reaches the racks holding it.

#### Scenario: From a rack
- **WHEN** a warehouse with 32 planks in a rack served by a shuttle and a Packing Station on a data line to a Crafting Station ships 16 planks to the station
- **THEN** a shuttle brings the planks to the Packing Station first, then 16 planks arrive in the station's inputs and the request reports 16 delivered and ended

#### Scenario: No shuttle
- **WHEN** the planks sit in a rack and the warehouse has no garage
- **THEN** the request is refused because it needs a shuttle and nothing leaves the warehouse

#### Scenario: No Packing Station
- **WHEN** the warehouse has no Packing Station on a data line that reaches the member
- **THEN** the request is refused and nothing leaves the warehouse

#### Scenario: Full target does not block the station
- **WHEN** a request for 128 copper has one Output slot that the player keeps full, and a request for 16 iron at the same station follows
- **THEN** the 16 iron arrive while the copper waits

#### Scenario: Nothing parks for a full slot
- **WHEN** the copper request's slot is full with 64 copper and the other 64 are still in the rack
- **THEN** no shuttle brings copper to the Packing Station until the player takes copper out of the slot

#### Scenario: Picks stay locked
- **WHEN** the other 64 copper wait in the rack for room in the slot
- **THEN** the terminal's requestable count does not include them and no other request can take them

### Requirement: Read-only buffer GUI
Right clicking a Packing Station SHALL open a GUI with two labelled rows of slots, To storage (items waiting for put-away) and To network (picked items waiting to be sent), each with a tooltip explaining it, and a status line naming the current delivery (item and target) or wireless transfer (item and the other site's name), or why the station waits (no rail, waiting for a shuttle, To storage full, a target has no room for its shipment, the receiving site has no room, a site of the transfer has no power for wireless). A station without a data line SHALL say so as plain information, not as an alert, while its controller is wireless capable. Players SHALL NOT be able to take items out of or put items into either row. The GUI SHALL stay open while the player is in reach.

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

#### Scenario: Target without room
- **WHEN** the station has nothing else to send and a shipment of copper waits because the Storage Terminal's slots for it are full
- **THEN** the status line says that it waits for room at the Storage Terminal, in the alert colour

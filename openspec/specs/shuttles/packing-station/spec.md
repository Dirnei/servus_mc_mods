# shuttles/packing-station Specification

## Purpose
The Packing Station is the warehouse's door to the data line: shuttles bring items to it and it sends them as packets, and packets that arrive at it are put away like an inbound delivery.

## Requirements

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

### Requirement: Packets are put away
Packets delivered to a Packing Station SHALL enter through its packet inlet and land in its To storage buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) at no more than the station's conversion rate into storage, and only into the slots granted to their delivery; the warehouse SHALL put the buffer's items away by its zone rules like items in an Inbound Station, by shuttle into racks. Items a station has not granted SHALL stay at their source.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered to a Packing Station of a warehouse with a rack that takes planks and a garage with a shuttle
- **THEN** a shuttle puts the planks into the rack and the To storage buffer is empty again

#### Scenario: To storage full
- **WHEN** the To storage buffer is full because no shuttle is free and a terminal sends 64 cobblestone
- **THEN** the cobblestone stays in the terminal's Input row until the buffer has a free slot again

### Requirement: Shortest shuttle trip
When several Packing Stations of a warehouse reach the member, the warehouse SHALL ship through the one with the cheapest shuttle trip by travel cost (see `shuttles/travel-cost`) from the racks holding the item; on a tie, and when the items need no shuttle, any of them.

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
The Warehouse Controller and every Packing Station SHALL expose the supplier capability. A request SHALL be shipped like any shipment to a data member: through the Packing Station with the shortest shuttle trip, as packets. The answer SHALL be the amount planned, at most what the warehouse holds, and zero when the warehouse cannot ship, together with the shipment as its ticket. The ticket SHALL be open while the shipment is open, also after a reload. Cancelling it SHALL end the shipment like a cancelled terminal request: its queued picks are dropped and what waits for it in the To network buffer goes back into storage.

#### Scenario: Upkeep from the warehouse
- **WHEN** a Machine Adapter on the line asks the controller's supplier for 16 coal and a rack served by a shuttle holds 40
- **THEN** the answer is 16 and 16 coal arrive at the adapter as packets

#### Scenario: Out of stock
- **WHEN** the warehouse holds no coal
- **THEN** the supplier answers 0

#### Scenario: Ticket of a delivered shipment
- **WHEN** all 16 coal of a supplier request arrived
- **THEN** the supplier reports its ticket as not open

#### Scenario: Cancel a refused shipment
- **WHEN** a supplier shipment of 8 coal waits in the To network buffer because its target refuses it, and its ticket is cancelled at the Packing Station's or the controller's supplier
- **THEN** the shipment ends, the 8 coal go back into storage, coal is no longer wanted in the buffer, and queued picks for it are dropped

### Requirement: Shipments survive a reload
A Packing Station SHALL save its open shipments with their ids, targets, items and amounts, and their progress SHALL be kept by id so that whoever waits for a shipment can book it after a reload. After its block entity loads again, a shipment SHALL only wait for what the outgoing buffer holds for it; the rest SHALL end as a shortfall, so its owner ships it again. Items that arrive later for no open shipment SHALL be put away as before.

#### Scenario: Restart while items wait in the outgoing buffer
- **WHEN** a shipment of 16 planks has 8 planks in the outgoing buffer and 8 still on a shuttle when the server restarts
- **THEN** after the restart the shipment sends the 8 planks, ends 8 short, and its requester ships the 8 again

### Requirement: Conversion rate per direction
A Packing Station SHALL convert items to packets and packets to items at `items_per_second` per direction (example 10, one item every 2 ticks, datapack JSON, read on load and reload). The direction into storage and the direction out of storage SHALL each have their own rate and SHALL NOT slow each other down. Several Packing Stations SHALL each add their own rate.

#### Scenario: Eight per second
- **WHEN** a terminal sends 64 cobblestone through one Packing Station with a rate of 8 and a free To storage slot
- **THEN** about 8 cobblestone leave the terminal's Input row each second, and all 64 are in the buffer after about 8 seconds

#### Scenario: One item every 2 ticks
- **WHEN** a terminal sends 64 cobblestone through one Packing Station with a rate of 10 and a free To storage slot
- **THEN** one cobblestone leaves the terminal's Input row every 2 ticks, and all 64 are in the buffer after about 6.4 seconds

#### Scenario: Both directions at once
- **WHEN** a station ships 64 planks out while a terminal sends 64 cobblestone in
- **THEN** both move at about 10 per second each

#### Scenario: Two stations
- **WHEN** a warehouse ships 64 planks to two members through two Packing Stations at the same time
- **THEN** both shipments move at about 10 per second each

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

### Requirement: Packing Station tooltip
The Packing Station item SHALL carry the tooltip "Packs items from the warehouse into packets for the data line and for other sites, and unpacks arriving items for storage".

#### Scenario: Hover
- **WHEN** the player hovers the Packing Station in an inventory
- **THEN** the tooltip explains that it packs and unpacks items between the warehouse and the data line or other sites

### Requirement: Speed upgrades
A Packing Station SHALL have `upgrade_slots` upgrade slots (example 2, datapack JSON) shown in its GUI, the only slots of that GUI the player can change. Its conversion rate in both directions SHALL be its base `items_per_second` multiplied by its upgrades, and the GUI SHALL show the current rate.

#### Scenario: Doubled
- **WHEN** a Packing Station with a base rate of 8 holds one Speed Upgrade of multiplier 2
- **THEN** it converts about 16 items per second in each direction and its GUI shows 16/s

### Requirement: Shipping to a data member by shuttle
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan and lock the picks like a terminal request, but shuttles SHALL bring picks into the To network buffer of the chosen Packing Station only as far as the member has granted room and the buffer has room. The station SHALL send what arrives at no more than its rate out of storage. A shipment its member refuses SHALL wait without holding up the station's other shipments and be offered again every push interval. A shipment to a machine target, any data member that is not a Storage Terminal, SHALL end once its target has refused it without a break for longer than the refusal timeout (example 1200 ticks). A terminal request SHALL wait for room in its Output slot with no timeout. A shipment whose target is cut from the line, because no route of data ducts reaches it any more, its route broke, or it no longer is a member of a data line reaching the station, SHALL wait and be judged like a refusal: it ends only once it has been cut or refused without a break for longer than the refusal timeout, for terminal requests too. A shipment whose target is removed, because no block at that loaded position takes items any more, SHALL end at once. A target in an unloaded chunk SHALL count as neither removed, cut nor refusing: its shipment waits. A shipment the station ends this way SHALL end short: what waits for it in the To network buffer goes back into storage, its queued picks are dropped, and the request reports it ended, so whoever asked may ask again. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, has none of the item, or has no shuttle that reaches the racks holding it.

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

#### Scenario: Full Output slot outlasts the timeout
- **WHEN** a terminal request's Output slot stays full for longer than the refusal timeout of 1200 ticks
- **THEN** the request is still open, its picks stay locked, and its items arrive once the player takes items out of the slot

#### Scenario: Refused past the timeout
- **WHEN** a Coal Generator whose fuel slot holds Coal Blocks refuses a shipment of 8 coal waiting in the To network buffer, with a refusal timeout of 1200 ticks, for longer than 1200 ticks
- **THEN** the shipment ends short, the 8 coal go back into storage, no queued pick for it remains, and the request reports it ended

#### Scenario: Room in between restarts the timeout
- **WHEN** a machine target refuses a shipment for 1000 ticks, then takes some of its items, then refuses again
- **THEN** the shipment ends only after the target has refused it for longer than the timeout counted from the new refusal

#### Scenario: Target removed
- **WHEN** the Crafting Station a shipment of 16 planks goes to is broken while 8 planks wait in the To network buffer and 8 are still in the rack
- **THEN** at the next push interval the shipment ends short, the 8 planks go back into storage and the queued pick for the other 8 is dropped

#### Scenario: Duct rebuilt within the timeout
- **WHEN** the duct between the Packing Station and a shipment's Crafting Station is broken and placed again 200 ticks later, with a refusal timeout of 1200 ticks
- **THEN** the shipment is still open and its items arrive at the Crafting Station

#### Scenario: Target cut from the line past the timeout
- **WHEN** the duct between the Packing Station and a shipment's target is broken and stays broken for longer than the refusal timeout
- **THEN** the shipment ends short and its buffered items go back into storage

#### Scenario: Target not loaded
- **WHEN** a shipment's target sits in a chunk that is not loaded while the Packing Station runs
- **THEN** the shipment waits, its items stay in the To network buffer and it is offered again once the target is loaded

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

### Requirement: Packing Stations carry wireless transfers
Every Packing Station on a site's rails SHALL be able to send and receive wireless transfers, with or without a data line. A sending station SHALL take the picked items from its To network buffer and hand them to the receiving station. A transfer requested at a Storage Terminal SHALL land in the receiving station's To network buffer, only while that buffer has room, and leave from there as packets to the requesting terminal like a data line delivery of a local request; it SHALL NOT be put away. A transfer without a requesting terminal (operator commands, tests) SHALL land in the To storage buffer only as far as the receiving station grants it whole slots, and be put away by the zone rules. A transfer SHALL use one sending and one receiving station. The receiving station SHALL be, among the stations of its site that reach the requesting terminal by data line, the one with the fewest open incoming transfers, the lowest position on a tie.

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

#### Scenario: Into storage by grant
- **WHEN** an operator sends 64 iron to the base without a terminal and the receiving station has one free To storage slot
- **THEN** the slot is reserved for the 64 iron, and they are put away in one shuttle trip once all have arrived

### Requirement: Wireless rate is the Packing Stations' rate
A wireless transfer SHALL move no faster than the sending station's conversion rate out of storage and the receiving station's conversion rate into storage, Speed Upgrades included. Wireless transfers and data line deliveries of one station SHALL share that station's rate in each direction.

#### Scenario: Eight per second
- **WHEN** 64 items are sent between two sites through Packing Stations of rate 8 without upgrades, with enough FE and room
- **THEN** about 8 items arrive each second

#### Scenario: Upgraded receiver, plain sender
- **WHEN** the sending station has rate 8 and the receiving one rate 16
- **THEN** about 8 items arrive each second

### Requirement: To storage grants whole slots
A Packing Station SHALL answer an offer into storage by reserving whole empty, unreserved slots of its To storage buffer: as many as the amount needs at the item's stack size and as are free. The grant SHALL be the amount those slots hold for the offer, and only that grant SHALL fill them. The station SHALL refuse with a reason when its warehouse is offline, no storage of the warehouse would take the item, or no slot is free.

#### Scenario: Two free slots
- **WHEN** 200 cobblestone are offered to a station whose To storage buffer has two free slots
- **THEN** the grant is 128 and both slots are reserved for it

#### Scenario: Ender pearls
- **WHEN** 16 ender pearls are offered to a station with a free slot
- **THEN** one slot is reserved and the grant is 16

#### Scenario: Nothing takes the item
- **WHEN** logs are offered and no rack or storage of the warehouse has room for logs
- **THEN** the station refuses with the reason that no storage takes logs

#### Scenario: Buffer full
- **WHEN** every slot of the To storage buffer holds items or is reserved
- **THEN** the station refuses with the reason that To storage is full

### Requirement: Put-away waits for its grant
Slots reserved for a grant SHALL NOT be put away while the grant runs. Once the grant is filled or has ended, its slots SHALL be free of the reservation and put away like any To storage items, so one shuttle trip takes what the grant brought, up to a stack per slot.

#### Scenario: A stack in one trip
- **WHEN** a terminal hands in 64 cobblestone through a station with a free shuttle
- **THEN** no shuttle is sent while the cobblestone arrives, and once all 64 are in the slot one shuttle takes all 64 to a rack

#### Scenario: Ended short
- **WHEN** a grant for 64 logs ends after 40 arrived because the terminal was broken
- **THEN** the 40 logs are put away

### Requirement: The warehouse chooses the Packing Station for ingress
Items sent into storage over the data line (a terminal's Input row, a Crafting CPU's outputs and leftovers) SHALL go to a Packing Station the warehouse chooses: among its stations on the sender's data line, the first to grant, asked in order of the shuttle trip cost from the station to the rack the put-away rule picks for the item, the lowest position on a tie. When none grants, the sender SHALL keep the items and try again later.

#### Scenario: Station near the racks
- **WHEN** Packing Station A beside the terminal is 12 rails from the rack that takes logs and Packing Station B is 2 rails from it, both on the terminal's line
- **THEN** logs from the Input row go to Packing Station B

#### Scenario: Nearest one full
- **WHEN** Packing Station B has no free slot and A has one
- **THEN** the logs go to Packing Station A

### Requirement: Grants are not kept over a reload
A Packing Station SHALL save no grants. After its block entity loads again it SHALL hold no reservations, and the items already in its To storage buffer SHALL be put away.

#### Scenario: Restart during a hand in
- **WHEN** 30 of 64 granted cobblestone are in the To storage buffer when the server stops
- **THEN** after the restart the 30 are put away and the other 34 are handed in again from the Input row

### Requirement: Picks of one item share To network slots
Picks that bring an item into a Packing Station's To network buffer SHALL join a stack of that item with room, counting what picks on their way already bring there, before they use empty slots. Many small picks of one item SHALL fill as few slots as their total needs, so the buffer counts as full only when its room is used, not when every slot holds a little.

#### Scenario: Small coal picks
- **WHEN** a shipment of 14 coal to a Machine Adapter is picked in six picks of 1 to 3 coal into an empty To network buffer of 9 slots
- **THEN** all 14 coal land in one slot and the other 8 slots stay free for other items

#### Scenario: Another item still finds room
- **WHEN** the To network buffer holds those 14 coal in one slot and a shipment of 16 iron ingots follows
- **THEN** the iron ingots are picked into a free slot without waiting

#### Scenario: Stack of one item overflows
- **WHEN** a To network slot holds 64 coal and a pick of 8 coal is sent
- **THEN** the 8 coal take an empty slot

#### Scenario: Pick larger than the room on a stack
- **WHEN** a To network slot holds 60 coal and a pick of 10 coal is sent
- **THEN** the pick fills that stack to 64 and its other 6 coal take an empty slot

### Requirement: Picks never ask for more than fits in the To network buffer
A pick SHALL be sent into a Packing Station's To network buffer only once room for its whole amount is reserved there, counting what picks already on their way reserved, so the items in the buffer and on their way to it SHALL never be more than it holds. A pick that does not fit SHALL wait unsent, reserving nothing, until room frees. Shipment demand or a target's grant SHALL never let a pick through without that reservation.

#### Scenario: Shipments want more than fits
- **WHEN** the To network buffer has room for 20 coal, spread over a partial stack of coal and empty slots, and open shipments want 50 coal in picks of 10
- **THEN** two picks are sent and reserve the 20, the other three wait, and each is sent once the station has sent enough coal to free room for it

#### Scenario: Picks on their way take the room
- **WHEN** the To network buffer has room for 20 coal and a pick of 12 coal is on its way into it, and a pick of 10 coal is next
- **THEN** the pick of 10 waits, because only 8 are left, and is sent once room for 10 is free

#### Scenario: A pick bigger than the room waits whole
- **WHEN** the To network buffer has room for 20 coal and the next pick carries 32 coal
- **THEN** the pick is not sent and reserves nothing until room for all 32 is free

### Requirement: The station sends from slots with picks on their way
A Packing Station SHALL send the items of its To network buffer to their shipments' targets also from slots where more picks of that item are still on their way, and those picks SHALL still find their room there when they arrive.

#### Scenario: Send while more coal comes
- **WHEN** a To network slot holds 6 coal of a granted shipment and a pick of 4 more coal for the same shipment is on its way into that slot
- **THEN** the station sends the 6 coal at its rate, and the 4 coal land in the slot when the pick arrives and are sent after them

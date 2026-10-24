## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Packets are put away
Packets delivered to a Packing Station SHALL enter through its packet inlet and land in its To storage buffer (example 9 slots, `inbound_buffer_slots` in `warehouse.json`) at no more than the station's conversion rate into storage, and only into the slots granted to their delivery; the warehouse SHALL put the buffer's items away by its zone rules like items in an Inbound Station, by shuttle into racks. Items a station has not granted SHALL stay at their source.

#### Scenario: Planks arrive
- **WHEN** 16 planks are delivered to a Packing Station of a warehouse with a rack that takes planks and a garage with a shuttle
- **THEN** a shuttle puts the planks into the rack and the To storage buffer is empty again

#### Scenario: To storage full
- **WHEN** the To storage buffer is full because no shuttle is free and a terminal sends 64 cobblestone
- **THEN** the cobblestone stays in the terminal's Input row until the buffer has a free slot again

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

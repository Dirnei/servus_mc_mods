## ADDED Requirements

### Requirement: Wireless Packing Station joins by rail
The Wireless Packing Station SHALL join a warehouse only through rails on its sides, like the Packing Station, SHALL hold no stock of the warehouse, SHALL put the contents of its To storage buffer away by the warehouse's zone rules (by shuttle for racks) and SHALL show the online and activity lamps. It SHALL NOT be a member of a data line and a data duct SHALL show no arm toward it. It SHALL remember the team of the player who placed it.

#### Scenario: On a rail
- **WHEN** a Wireless Packing Station stands beside a rail of an online warehouse
- **THEN** it belongs to that warehouse and its lamp is lit

#### Scenario: Not on the data line
- **WHEN** a duct with the data line touches a Wireless Packing Station
- **THEN** the duct shows no data arm toward it and the station is no member of that data network

### Requirement: Wireless line per team and dimension
All Wireless Packing Stations of one team in one dimension SHALL share one wireless line. A transfer SHALL only run between two stations of the same team in the same dimension.

#### Scenario: Other team
- **WHEN** the only Wireless Packing Station at a site belongs to another team
- **THEN** that site is not offered as a source for this team's transfers

### Requirement: Transfers between sites
A warehouse SHALL be able to send an amount of an item to a Wireless Packing Station of another site: it SHALL plan the picks like a shipment to a data member (instant from crates and foreign inventories, by shuttle from racks) into the To network buffer of its own Wireless Packing Station of the team with the shortest shuttle trip, never faster than that buffer has room. The sending station SHALL move the items to the receiving station's To storage buffer at no more than its own rate out and the receiver's rate in, only while that buffer has room and only as far as the receiver can pay, and the receiving warehouse SHALL put them away. The transfer SHALL report planned and delivered amounts and whether it ended. It SHALL be refused when the warehouse is offline, has no Wireless Packing Station of the team, or has none of the item.

#### Scenario: Iron from the outpost
- **WHEN** the outpost's crate holds 32 iron ingots and a transfer of 16 to the base's Wireless Packing Station is made, with enough FE at the base
- **THEN** 16 iron ingots leave the outpost and end up in the base's storage, and the transfer reports 16 delivered and ended

#### Scenario: From a rack
- **WHEN** the iron sits only in a Heavy Duty Rack at the outpost
- **THEN** a shuttle brings it to the outpost's Wireless Packing Station first, then it goes over the wireless line

### Requirement: Energy per item and distance
A Wireless Packing Station SHALL have an FE buffer of `energy_capacity` (example 100,000) that takes FE from any side through the standard energy capability, for example from the Utility Duct power line, and never gives FE out. Each item SHALL cost `fe_per_item` plus `fe_per_item_per_block` times the distance in blocks between the sending and the receiving station (examples 4 and 0.02), paid from the receiving station's buffer when the item moves. Without enough FE nothing SHALL move and the stations SHALL say so.

#### Scenario: Cost
- **WHEN** 10 items move between two stations 500 blocks apart with 4 FE per item and 0.02 FE per item and block
- **THEN** the receiving station's buffer loses 140 FE

#### Scenario: No power
- **WHEN** the receiving station's buffer is empty
- **THEN** no item moves and its status says it needs power

### Requirement: Wireless rate
A Wireless Packing Station SHALL convert at `items_per_second` (example 8, `wireless_packing_station.json`) per direction, multiplied by its Speed Upgrades like the Packing Station, its two directions independent.

#### Scenario: Eight per second
- **WHEN** 64 items are sent between two stations without upgrades and with enough FE and room
- **THEN** about 8 items arrive each second

### Requirement: Wireless Packing Station GUI
Right clicking the station SHALL open the Packing Station GUI (both buffers read-only, Speed Upgrade slots, rate) with a status line naming the transfer it sends (item and receiving site), or why it waits (no rail, offline, needs power with the FE stored, waiting for a shuttle, To storage full), and the FE stored while idle.

#### Scenario: Status while sending
- **WHEN** the outpost's station sends iron to the base
- **THEN** its status line names the iron and the base

### Requirement: Wireless Packing Station is obtainable
The Wireless Packing Station SHALL be crafted from a Packing Station, an ender eye, iron ingots, redstone and planks (example), recording the wood of the planks, drop itself with its wood and the contents of both buffers and its upgrades when broken, and be dismantled by sneaking with the Configuration Carrot. Its tooltip SHALL say it links the warehouse to the other sites of its team over a wireless line.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with a Packing Station in the middle
- **THEN** one Wireless Packing Station is produced

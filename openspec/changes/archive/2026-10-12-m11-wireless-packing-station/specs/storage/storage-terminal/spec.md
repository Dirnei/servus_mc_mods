## ADDED Requirements

### Requirement: Other sites in the terminal
The Storage Terminal's title SHALL be a site selector: "This site" shows the stock of the warehouses the terminal reaches as before; clicking cycles through the other sites of the team in the same dimension that have a Wireless Packing Station of the team, powered or not, by name, and back. The team SHALL be the team of the first warehouse the terminal reaches. An online site SHALL show its live stock; a sleeping site SHALL show its last known stock with every entry greyed out and a line saying when it can wake or that it cannot be ordered from. A site whose Wireless Packing Stations of the team are all unpowered SHALL show a line in the alert colour saying its Wireless Packing Station has no power. While another site is shown, the craft list, stock rules and pulling to the cursor SHALL apply to nothing; the open requests and the Input and Output rows stay those of this terminal. While another site is shown the Input row SHALL be closed: no click, shift click, carried stack or grid clear puts items into it, it is drawn greyed with a grey arrow, and hovering it says that it is closed while another site is shown, because what is handed in would be stored at this site.

#### Scenario: Pick the outpost
- **WHEN** a base terminal is open and the team's outpost has a Wireless Packing Station and the player clicks the title
- **THEN** the title reads the outpost's name and the grid shows the outpost's stock

#### Scenario: Input closed for another site
- **WHEN** the terminal shows the outpost and the player shift clicks logs from the inventory
- **THEN** the logs stay in the inventory and the Input row stays empty; back on "This site" the same shift click hands them in

#### Scenario: Sleeping outpost
- **WHEN** the outpost is asleep with 40 iron ingots last known
- **THEN** the grid shows 40 iron ingots greyed out

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items over the wireless line to a Wireless Packing Station of the warehouse this terminal reaches first, and they are put away there. The request SHALL be refused with the reason when this site has no Wireless Packing Station of the team or none of them is powered, when the other site has none, has only unpowered ones or sleeps without enough Chunk Loader Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL be listed with the open requests with its delivered and requested amounts and a state (waking, picking, sending, waiting for power, missing) and SHALL end when the items arrived at this site's Wireless Packing Station; clicking it SHALL cancel it like a request. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal and both sites have a Wireless Packing Station with power at the base
- **THEN** a transfer for 16 is listed, the iron arrives in the base's storage and the transfer leaves the list

#### Scenario: Wake for a transfer
- **WHEN** the outpost sleeps, has enough Chunk Loader Upgrades and the player requests iron from it
- **THEN** the outpost wakes, the transfer shows waking and then picking, and the outpost sleeps again after the transfer and its grace time

#### Scenario: Cannot be ordered
- **WHEN** the outpost sleeps without a Chunk Loader Upgrade
- **THEN** a request is refused with the reason that the site sleeps and cannot wake

## MODIFIED Requirements

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid. Stacks in the Input row SHALL be moved over the data line to the Packing Station on the terminal's data line nearest to the terminal, which puts them away like any intake (racks by shuttle, zones, crates and foreign inventories). Items SHALL leave the Input row only as fast as the Packing Station takes them: at most its conversion rate into storage and only while its To storage buffer has room, so the row shrinks over time and keeps what the station cannot take yet. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock, also when the terminal touches a storage node or the controller, SHALL never be a put-away target or a source for picks, and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while a Packing Station of the warehouse is on the line
- **THEN** the slot empties over a few seconds and the cobblestone ends in the warehouse after the put-away

#### Scenario: More than the station can hold
- **WHEN** the player fills all 9 input slots with cobblestone while the station's To storage buffer has room for one stack and no shuttle is free
- **THEN** one stack leaves the Input row and the other eight stay until the buffer has room

#### Scenario: No room
- **WHEN** the player puts bread into an input slot and no Packing Station is on the data line
- **THEN** the bread stays in the input slot and the message line says "No Packing Station on this data line"

#### Scenario: Racks only
- **WHEN** the warehouse has only Heavy Duty Racks with room and the player puts planks into an input slot
- **THEN** the planks end in a rack

#### Scenario: Terminal touching the controller
- **WHEN** the terminal stands directly beside the Warehouse Controller, its Output row holds 5 diamonds and the player puts 64 logs into the Input row of a warehouse whose only room is a rack
- **THEN** the stock shows no diamonds, all 64 logs end in the rack and the stock counts them once

#### Scenario: Nowhere to put it
- **WHEN** the player puts logs into the Input row and no storage of the warehouse has room for them
- **THEN** the Packing Station shows the blocked lamp and its status line says why, instead of the logs waiting silently

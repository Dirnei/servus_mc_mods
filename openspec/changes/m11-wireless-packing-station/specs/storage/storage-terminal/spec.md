## ADDED Requirements

### Requirement: Other sites in the terminal
The Storage Terminal's title SHALL be a site selector: "This site" shows the stock of the warehouses the terminal reaches as before; clicking cycles through the other sites of the team in the same dimension that have a Wireless Packing Station of the team, by name, and back. The team SHALL be the team of the first warehouse the terminal reaches. An online site SHALL show its live stock; a sleeping site SHALL show its last known stock with every entry greyed out and a line saying when it can wake or that it cannot be ordered from. While another site is shown, the craft list, stock rules and pulling to the cursor SHALL apply to nothing; the open requests and the Input and Output rows stay those of this terminal.

#### Scenario: Pick the outpost
- **WHEN** a base terminal is open and the team's outpost has a Wireless Packing Station and the player clicks the title
- **THEN** the title reads the outpost's name and the grid shows the outpost's stock

#### Scenario: Sleeping outpost
- **WHEN** the outpost is asleep with 40 iron ingots last known
- **THEN** the grid shows 40 iron ingots greyed out

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items over the wireless line to a Wireless Packing Station of the warehouse this terminal reaches first, and they are put away there. The request SHALL be refused with the reason when this site has no Wireless Packing Station of the team, when the other site has none or sleeps without enough Chunk Loader Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL be listed with the open requests with its delivered and requested amounts and a state (waking, picking, sending, missing) and SHALL end when the items arrived at this site's Wireless Packing Station; clicking it SHALL cancel it like a request. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal and both sites have a Wireless Packing Station with power at the base
- **THEN** a transfer for 16 is listed, the iron arrives in the base's storage and the transfer leaves the list

#### Scenario: Wake for a transfer
- **WHEN** the outpost sleeps, has enough Chunk Loader Upgrades and the player requests iron from it
- **THEN** the outpost wakes, the transfer shows waking and then picking, and the outpost sleeps again after the transfer and its grace time

#### Scenario: Cannot be ordered
- **WHEN** the outpost sleeps without a Chunk Loader Upgrade
- **THEN** a request is refused with the reason that the site sleeps and cannot wake

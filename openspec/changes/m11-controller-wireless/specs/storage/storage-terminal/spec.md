## MODIFIED Requirements

### Requirement: Other sites in the terminal
The Storage Terminal's title SHALL be a site selector only while the controller of the first warehouse the terminal reaches is wireless capable. "This site" shows the stock of the warehouses the terminal reaches as before; clicking cycles through the other sites of the same team in the same dimension whose controller holds a Wireless Upgrade, powered or not, by name, and back. While this site's controller is not wireless capable the title SHALL show only this site, clicking it SHALL do nothing, and a terminal that showed another site SHALL go back to "This site". An online site SHALL show its live stock; a sleeping site SHALL show its last known stock with every entry greyed out and a line saying when it can wake or that it cannot be ordered from. A site whose controller has no power SHALL show a line in the alert colour saying it has no power for wireless. While another site is shown, the craft list and stock rules SHALL apply to nothing; the open requests and the Input and Output rows stay those of this terminal. While another site is shown the Input row SHALL be closed: no click, shift click, carried stack or grid clear puts items into it, it is drawn greyed with a grey arrow, and hovering it says that it is closed while another site is shown, because what is handed in would be stored at this site. No text of the terminal SHALL use the word team.

#### Scenario: Pick the outpost
- **WHEN** a base terminal is open, the base controller is wireless capable, the outpost's controller holds a Wireless Upgrade and the player clicks the title
- **THEN** the title reads the outpost's name and the grid shows the outpost's stock

#### Scenario: This site only
- **WHEN** the base controller has no Wireless Upgrade and the player clicks the terminal's title
- **THEN** the title stays on this site and no other site is offered

#### Scenario: Power lost while viewing
- **WHEN** the terminal shows the outpost and the base controller runs out of FE
- **THEN** the terminal goes back to "This site" and the title offers no other site until FE arrives

#### Scenario: Input closed for another site
- **WHEN** the terminal shows the outpost and the player shift clicks logs from the inventory
- **THEN** the logs stay in the inventory and the Input row stays empty; back on "This site" the same shift click hands them in

#### Scenario: Sleeping outpost
- **WHEN** the outpost is asleep with 40 iron ingots last known
- **THEN** the grid shows 40 iron ingots greyed out

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items from one of its Packing Stations to a Packing Station of the warehouse this terminal reaches first, and they are put away there. The request SHALL be refused with the reason when this site's controller is not wireless capable, when this site has no Packing Station, when the other site's controller is not wireless capable, when the other site has no Packing Station or sleeps without enough Chunk Loader Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL be listed with the open requests with its delivered and requested amounts and a state (waking, picking, sending, waiting for power, missing) and SHALL end when the items arrived at this site's Packing Station; clicking it SHALL cancel it like a request. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal, both controllers are wireless capable and both sites have a Packing Station
- **THEN** a transfer for 16 is listed, the iron arrives in the base's storage and the transfer leaves the list

#### Scenario: Wake for a transfer
- **WHEN** the outpost sleeps, has enough Chunk Loader Upgrades and a wireless capable controller, and the player requests iron from it
- **THEN** the outpost wakes, the transfer shows waking and then picking, and the outpost sleeps again after the transfer and its grace time

#### Scenario: Cannot be ordered
- **WHEN** the outpost sleeps without a Chunk Loader Upgrade
- **THEN** a request is refused with the reason that the site sleeps and cannot wake

#### Scenario: Outpost without power
- **WHEN** the outpost's controller holds a Wireless Upgrade but no FE and the player requests iron from it
- **THEN** the request is refused with "That site has no power for wireless"

#### Scenario: No Packing Station here
- **WHEN** the base has no Packing Station on its rails and the player requests iron from the outpost
- **THEN** the request is refused with "This site has no Packing Station"

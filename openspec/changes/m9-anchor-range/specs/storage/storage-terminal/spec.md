## MODIFIED Requirements

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items from one of its Packing Stations to a Packing Station of the warehouse this terminal reaches first that reaches the terminal by data line, and that station passes them on as packets into the terminal's output slots like a request from this site's storage; they SHALL NOT be put away at this site. The request SHALL be refused with the reason when this site's controller is not wireless capable, when this site has no Packing Station, when the other site's controller is not wireless capable, when the other site has no Packing Station or sleeps without enough Anchor Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL be listed with the open requests with its delivered and requested amounts and a state (waking, picking, sending, waiting for power, missing) and SHALL end when the items arrived in the terminal's output slots; clicking it SHALL cancel it like a request. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal, both controllers are wireless capable and both sites have a Packing Station
- **THEN** a transfer for 16 is listed, the iron arrives in the terminal's output slots, the base's storage does not change and the transfer leaves the list

#### Scenario: Output full
- **WHEN** a transfer from the outpost arrives while the terminal's output slots are full
- **THEN** the items wait in the receiving Packing Station like packets of a local request and move on once the player empties the output

#### Scenario: Wake for a transfer
- **WHEN** the outpost sleeps, has enough Anchor Upgrades and a wireless capable controller, and the player requests iron from it
- **THEN** the outpost wakes, the transfer shows waking and then picking, and the outpost sleeps again after the transfer and its grace time

#### Scenario: Cannot be ordered
- **WHEN** the outpost sleeps without a Anchor Upgrade
- **THEN** a request is refused with the reason that the site sleeps and cannot wake

#### Scenario: Outpost without power
- **WHEN** the outpost's controller holds a Wireless Upgrade but no FE and the player requests iron from it
- **THEN** the request is refused with "That site has no power for wireless"

#### Scenario: No Packing Station here
- **WHEN** the base has no Packing Station on its rails and the player requests iron from the outpost
- **THEN** the request is refused with "This site has no Packing Station"

## ADDED Requirements

### Requirement: Controller lists the team's sites
The controller GUI SHALL list the other sites of its team in its dimension by name, each with its state: online, sleeping and able to wake, or sleeping and unable to wake. A site without a powered Wireless Packing Station of the team SHALL show its name in orange; hovering it SHALL say "No Wireless Packing Station" when it has none and "Wireless Packing Station has no power" when its stations of the team are all unpowered. A controller without a team SHALL list nothing.

#### Scenario: Two outposts
- **WHEN** the base's controller GUI is open and the team has an online outpost and a sleeping one without upgrades
- **THEN** the list shows both by name, the first online, the second sleeping and unable to wake

#### Scenario: Outpost without power
- **WHEN** the outpost's only Wireless Packing Station of the team has no FE
- **THEN** the base's list shows the outpost's name in orange and its hover says the Wireless Packing Station has no power

## MODIFIED Requirements

### Requirement: Sources are nodes and adjacent inventories
The index SHALL include every storage node of the network that holds stock and every block with an item handler adjacent to such a node that is not itself a node. Connector nodes such as rails SHALL NOT be sources and their neighbours SHALL NOT be discovered. Adjacent inventories SHALL be discovered when a node is placed and when a neighbour of a node changes. Servus blocks whose slots are no stock SHALL never be discovered as adjacent inventories, whatever item capability they expose: the Storage Terminal (Input and Output rows), the Crafting Station, and every station (Inbound, Outbound, Packing and Wireless Packing Station buffers), so they are never stock, a put-away target or a source for picks.

#### Scenario: Chest beside a crate
- **WHEN** a chest with 10 bread is placed next to a crate of an online network
- **THEN** the index soon reports 10 bread from that chest

#### Scenario: Chest removed
- **WHEN** that chest is broken
- **THEN** the bread leaves the index

#### Scenario: Chest beside a rail
- **WHEN** a chest with 10 bread is placed next to a rail of an online network and touches no stock holding node
- **THEN** the index does not report the bread

#### Scenario: Terminal beside the controller
- **WHEN** a Storage Terminal with 5 diamonds in its Output row stands directly beside the Warehouse Controller
- **THEN** the index does not report the diamonds and put-away never chooses the terminal

#### Scenario: Station beside a crate
- **WHEN** a Wireless Packing Station with emeralds in its buffer touches a crate without a rail between them
- **THEN** the index does not report the emeralds


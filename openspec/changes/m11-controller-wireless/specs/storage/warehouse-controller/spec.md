## ADDED Requirements

### Requirement: Wireless Upgrade slot
The controller SHALL have one slot that takes only the Wireless Upgrade, apart from its Chunk Loader Upgrade slots. A second Wireless Upgrade SHALL add nothing and does not fit. The upgrade SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slot.

#### Scenario: Insert the upgrade
- **WHEN** the player puts a Wireless Upgrade into the controller's wireless slot
- **THEN** the slot takes it and the controller reports that it holds a Wireless Upgrade

#### Scenario: Wrong item
- **WHEN** the player tries to put a Chunk Loader Upgrade or a Speed Upgrade into the wireless slot
- **THEN** the slot refuses it

#### Scenario: Dismantled
- **WHEN** a controller holding a Wireless Upgrade is dismantled with the Configuration Carrot
- **THEN** the Wireless Upgrade drops beside the controller item

### Requirement: Controller FE buffer
The controller SHALL have an FE buffer of `controller_energy_capacity` (example 100,000, `storage/power.json`) that takes FE from every side through the standard energy capability, for example from the Utility Duct power line, and never gives FE out. The FE stored SHALL be saved with the controller and SHALL NOT travel in a dismantled item. The controller GUI SHALL show the FE stored and the capacity.

#### Scenario: Power line
- **WHEN** a Utility Duct with the power line fed by a Coal Generator touches the controller
- **THEN** the duct shows a power arm toward the controller and its FE stored rises

#### Scenario: No FE out
- **WHEN** a machine next to the controller tries to pull FE from it
- **THEN** it gets none

### Requirement: Wireless capable controller
A controller SHALL be wireless capable while it holds a Wireless Upgrade and is powered: its buffer holds at least the cost of one item at the smallest cost, `wireless_fe_per_item` rounded up and at least 1 FE (example 4). A site whose controller is not wireless capable SHALL be offline for wireless: it neither sends nor receives. The state SHALL follow upgrade and FE changes, never a scan.

#### Scenario: Upgrade and power
- **WHEN** a controller holds a Wireless Upgrade and 500 FE with 4 FE per item
- **THEN** it is wireless capable

#### Scenario: Upgrade without power
- **WHEN** a controller holds a Wireless Upgrade and 2 FE with 4 FE per item
- **THEN** it is not wireless capable and its GUI says it needs 4 FE for wireless

#### Scenario: Power without upgrade
- **WHEN** a controller holds 50,000 FE and no Wireless Upgrade
- **THEN** it is not wireless capable

### Requirement: Transfers between sites
A wireless capable site SHALL be able to send an amount of an item to another wireless capable site of the same team in the same dimension: it SHALL plan the picks like a shipment (instant from crates and foreign inventories, by shuttle from racks) into the To network buffer of its Packing Station with the shortest shuttle trip, and that station SHALL move the items to a Packing Station of the receiving site, whose warehouse puts them away. The transfer SHALL report planned and delivered amounts and whether it ended. It SHALL be refused when either site is not wireless capable, either site has no Packing Station, or the sending site has none of the item.

#### Scenario: Iron from the outpost
- **WHEN** the outpost's crate holds 32 iron ingots and a transfer of 16 to the base is made, both controllers wireless capable and both sites with a Packing Station
- **THEN** 16 iron ingots leave the outpost and end up in the base's storage, and the transfer reports 16 delivered and ended

#### Scenario: From a rack
- **WHEN** the iron sits only in a Heavy Duty Rack at the outpost
- **THEN** a shuttle brings it to the outpost's Packing Station first, then it goes over the wireless line

#### Scenario: No Packing Station at the outpost
- **WHEN** the outpost's controller is wireless capable but the outpost has no Packing Station on its rails
- **THEN** the transfer is refused and nothing leaves the outpost

### Requirement: Wireless cost per item and distance
Each item a transfer moves SHALL cost `wireless_fe_per_item` plus `wireless_fe_per_item_per_block` times the straight distance in blocks between the two controllers (examples 4 and 0.02, `storage/power.json`), paid from the receiving site's controller buffer when the item moves. The sending site SHALL pay nothing for the wireless hop. Without enough FE at the receiver nothing SHALL move and the transfer SHALL wait.

#### Scenario: Cost
- **WHEN** 10 items move between two sites whose controllers are 500 blocks apart, with 4 FE per item and 0.02 FE per item and block
- **THEN** the receiving controller's buffer loses 140 FE and the sending controller's buffer is unchanged

#### Scenario: Receiver runs dry
- **WHEN** the receiving controller holds FE for 3 items of a transfer of 10
- **THEN** 3 items move and the rest waits until FE arrives

### Requirement: Running transfers wait for power
A transfer whose sending or receiving controller stops being wireless capable SHALL stop moving items, keep the items already picked in the sending Packing Station, stay open as waiting for power, and go on once both controllers are wireless capable again. It SHALL be cancellable meanwhile; cancelled items go back into the sending site's storage.

#### Scenario: Power lost during a transfer
- **WHEN** the base controller's FE is taken away while iron is being sent to it
- **THEN** no more iron moves, the transfer shows waiting for power, and once FE arrives again the rest of the iron arrives and the transfer ends

#### Scenario: Upgrade taken out
- **WHEN** the player takes the Wireless Upgrade out of the outpost's controller during a transfer
- **THEN** the transfer waits for power and goes on once the upgrade is back

### Requirement: Wireless Upgrade is obtainable
The Wireless Upgrade SHALL be a Storage item crafted at a crafting table from an ender eye, iron ingots, redstone and a copper ingot (example), with a tooltip saying it lets the warehouse trade with other sites over a wireless line for FE. It SHALL NOT be a Foundation multiplier upgrade.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with an ender eye in the middle
- **THEN** one Wireless Upgrade is produced

## MODIFIED Requirements

### Requirement: Controller lists the team's sites
The controller GUI SHALL have an "Other sites" panel. While the controller is wireless capable the panel SHALL list the other sites of its team in its dimension by name, each with its state: online, sleeping and able to wake, or sleeping and unable to wake. A site whose controller is not wireless capable SHALL show its name in orange. Hovering a site SHALL show only its name and its state. While the controller is not wireless capable the panel SHALL list nothing and show a short hint instead: "Insert a Wireless Upgrade to trade with other sites" without the upgrade, or that the controller needs FE for wireless with the upgrade but without power. A controller without a team SHALL list nothing. No text of the panel SHALL use the word team.

#### Scenario: Two outposts
- **WHEN** the base's controller is wireless capable, its GUI is open, and the team has an online outpost and a sleeping one without upgrades
- **THEN** the panel shows both by name, the first online, the second sleeping and unable to wake

#### Scenario: Outpost without power
- **WHEN** the outpost's controller has no Wireless Upgrade or no power
- **THEN** the base's panel shows the outpost's name in orange and its hover shows only the name and the state

#### Scenario: No upgrade here
- **WHEN** the base's controller has no Wireless Upgrade and its GUI is open
- **THEN** the panel lists no site and says "Insert a Wireless Upgrade to trade with other sites"

#### Scenario: Upgrade without power here
- **WHEN** the base's controller holds a Wireless Upgrade and no FE
- **THEN** the panel lists no site and says the controller needs FE for wireless

### Requirement: Sources are nodes and adjacent inventories
The index SHALL include every storage node of the network that holds stock and every block with an item handler adjacent to such a node that is not itself a node. Connector nodes such as rails SHALL NOT be sources and their neighbours SHALL NOT be discovered. Adjacent inventories SHALL be discovered when a node is placed and when a neighbour of a node changes. Servus blocks whose slots are no stock SHALL never be discovered as adjacent inventories, whatever item capability they expose: the Storage Terminal (Input and Output rows), the Crafting Station, and every station (Inbound, Outbound and Packing Station buffers), so they are never stock, a put-away target or a source for picks.

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
- **WHEN** a Packing Station with emeralds in its buffer touches a crate without a rail between them
- **THEN** the index does not report the emeralds

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the Chunk Loader Upgrade slots, the Wireless Upgrade slot, the FE stored with the capacity, the "Other sites" panel and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

#### Scenario: Energy shown
- **WHEN** a controller holds 12,000 of 100,000 FE and a player opens its GUI
- **THEN** the GUI shows 12,000 of 100,000 FE and the value follows while FE arrives

# Spec Delta

## MODIFIED Requirements

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the storage lines, the Anchor Upgrade slots, the Wireless Upgrade slot, the FE stored with the capacity, the "Other sites" panel and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The storage lines SHALL sit between the map row and the upgrade row: an Items line with the rack slots in use and the total, and a Fluids line with the mB in the Fluid Rack shelves and their capacity, each with a bar below it that turns to the alert colour when full. A warehouse without racks SHALL say so on the Items line, one without Fluid Racks on the Fluids line, both without a bar. The Items tooltip SHALL name the number of rack bays and the full zones; the Fluids tooltip SHALL name the number of Fluid Rack shelves. While the controller is not online the storage lines SHALL be left out. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

#### Scenario: Energy shown
- **WHEN** a controller holds 12,000 of 100,000 FE and a player opens its GUI
- **THEN** the GUI shows 12,000 of 100,000 FE and the value follows while FE arrives

#### Scenario: Storage shown
- **WHEN** a warehouse has 412 of 729 rack slots in use and 24,000 of 96,000 mB in its Fluid Racks and a player opens its controller GUI
- **THEN** the GUI shows Items 412 / 729 slots and Fluids 24k / 96k mB, each with a bar

#### Scenario: Storage follows
- **WHEN** the controller GUI is open and a shuttle puts a stack into an empty rack slot
- **THEN** the Items line counts one more slot in use within a few seconds

#### Scenario: Full zone in the tooltip
- **WHEN** the zone "Ores" is full and the player hovers the Items line
- **THEN** the tooltip names the rack bays and lists Ores as full

#### Scenario: No gantry
- **WHEN** a warehouse has racks but no Fluid Rack
- **THEN** the Fluids line says there is no fluid storage and shows no bar

#### Scenario: Fluid Racks in the tooltip
- **WHEN** a warehouse has two Fluid Rack shelves and the player hovers the Fluids line
- **THEN** the tooltip names 2 Fluid Rack shelves

### Requirement: Sources are the warehouse's own racks
The index SHALL include only the storage nodes of the network that hold stock, which are the Heavy Duty Racks joined through a rail in front of a bay of their shelf. No other block SHALL be a source: inventories touching a rack, the controller, a rail, a station or any other member (vanilla chests, inventories of other mods, Servus blocks such as the Storage Terminal or the Crafting Station) SHALL never be discovered, indexed, put-away targets or sources for picks. Connector nodes such as rails SHALL NOT be sources.

#### Scenario: Chest beside the controller
- **WHEN** a chest with 10 bread is placed next to the controller of an online network
- **THEN** the index does not report the bread and put-away never chooses the chest

#### Scenario: Chest beside a rack
- **WHEN** a chest with 10 bread touches the side of a rack that belongs to an online network
- **THEN** the index does not report the bread

#### Scenario: Chest beside a rail
- **WHEN** a chest with 10 bread is placed next to a rail of an online network
- **THEN** the index does not report the bread

#### Scenario: Terminal beside the controller
- **WHEN** a Storage Terminal with 5 diamonds in its Output row stands directly beside the Warehouse Controller
- **THEN** the index does not report the diamonds and put-away never chooses the terminal

#### Scenario: Rack joins by rail
- **WHEN** a rack holding 10 bread gets a rail of an online network in front of it
- **THEN** the index soon reports 10 bread from that rack, and breaking the rail removes them again

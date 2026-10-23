## MODIFIED Requirements

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the storage lines, the Anchor Upgrade slots, the Wireless Upgrade slot, the FE stored with the capacity, the "Other sites" panel and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The storage lines SHALL sit between the map row and the upgrade row: an Items line with the rack slots in use and the total, and a Fluids line with the mB in the Storage gantries and their capacity, each with a bar below it that turns to the alert colour when full. A warehouse without racks SHALL say so on the Items line, one without Storage gantries on the Fluids line, both without a bar. The Items tooltip SHALL name the number of rack bays and the full zones; the Fluids tooltip SHALL name the number of Storage gantries. While the controller is not online the storage lines SHALL be left out. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

#### Scenario: Energy shown
- **WHEN** a controller holds 12,000 of 100,000 FE and a player opens its GUI
- **THEN** the GUI shows 12,000 of 100,000 FE and the value follows while FE arrives

#### Scenario: Storage shown
- **WHEN** a warehouse has 412 of 729 rack slots in use and 24,000 of 96,000 mB in its Storage gantries and a player opens its controller GUI
- **THEN** the GUI shows Items 412 / 729 slots and Fluids 24k / 96k mB, each with a bar

#### Scenario: Storage follows
- **WHEN** the controller GUI is open and a shuttle puts a stack into an empty rack slot
- **THEN** the Items line counts one more slot in use within a few seconds

#### Scenario: Full zone in the tooltip
- **WHEN** the zone "Ores" is full and the player hovers the Items line
- **THEN** the tooltip names the rack bays and lists Ores as full

#### Scenario: No gantry
- **WHEN** a warehouse has racks but no Storage gantry
- **THEN** the Fluids line says there is no Storage gantry and shows no bar

## MODIFIED Requirements

### Requirement: Storage tab
The Storage tab SHALL show the warehouse the terminal's line belongs to, by site name. The warehouse SHALL have a header row with its name, its rack slots in use and total and a bar, then one row per zone as the storage fill orders them (full first, then the fullest), each with its slots and a bar, then a Fluids row with the mB in its Storage gantries and their capacity and a bar. Full rows SHALL use the alert colour. A warehouse without racks SHALL show a "no racks" row instead of zone rows; one without Storage gantries SHALL show no Fluids row. The header tooltip SHALL name the number of rack bays and Storage gantries. Without a warehouse the tab SHALL say that no warehouse is on the line; on a line in conflict it SHALL say that two warehouses are on this data line, in the alert colour. The tab SHALL follow the counts while open.

#### Scenario: One warehouse with zones
- **WHEN** a terminal reaches one warehouse with the zones Ores (81 of 81) and Food (30 of 81) and unzoned racks (301 of 567) and the player opens the Storage tab
- **THEN** the tab shows the warehouse header with 412 / 729, then Ores in the alert colour, (no zone), Food, and a Fluids row when it has Storage gantries

#### Scenario: Two warehouses
- **WHEN** the line reaches the base and an outpost warehouse
- **THEN** the tab shows no warehouse and says that two warehouses are on this data line, in the alert colour

#### Scenario: No warehouse
- **WHEN** the line has a CPU but no warehouse
- **THEN** the Storage tab says that no warehouse is on the line

## MODIFIED Requirements

### Requirement: Harvester module
The Harvester SHALL be a craftable tool module. It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's tool place. A shuttle with a Harvester SHALL be field only: it SHALL take field jobs and SHALL NOT take warehouse picks or put-aways. A shuttle without a Harvester SHALL NOT take field jobs. The Harvester SHALL be wide, and its tooltip SHALL say that it does not fit into lifts.

#### Scenario: Fit a Harvester
- **WHEN** a player puts a Mk1 shuttle into the workshop and a Harvester into the install slot
- **THEN** the shuttle has the Harvester and its tooltip lists it

#### Scenario: Plain shuttles stay home
- **WHEN** a warehouse has only shuttles without a tool and its field has ripe crops with a harvest due
- **THEN** no shuttle drives onto the field and the crops stay on the field

#### Scenario: Harvesters stay out of the warehouse
- **WHEN** a warehouse has only a Harvester shuttle and a player requests cobblestone from a rack on the rails
- **THEN** the request is refused because it needs a shuttle and the harvester stays in its garage

#### Scenario: Tooltip warns about lifts
- **WHEN** a player hovers over a Harvester item
- **THEN** the tooltip says it does not fit into lifts

### Requirement: Harvest, till and plant
For each plot the harvester works it SHALL:
- harvest a ripe age crop of any kind: the crop is broken without dropping items and its drops go into the cargo; a plot SHALL be harvested only when its drops, less one crop item kept to replant it, fit into the cargo;
- till a dirt or grass block one below an empty plot into farmland, but only while the field has a crop set; no other block is tilled, and only blocks under plots of the field;
- plant the field's crop on farmland one below an empty plot, including a plot it just harvested or tilled, when the field has a crop set and the harvester has the crop item.
In harvest only mode the harvester SHALL only harvest. Crops that are not ripe and plots with other blocks SHALL be left alone. From each rail the harvester SHALL work the plots of the field nearest to that rail, no farther out than the field radius.

#### Scenario: Harvest into storage
- **WHEN** a field with wheat set has ripe wheat and the warehouse has a rack with room on the harvester's floor
- **THEN** the harvester breaks the ripe wheat, the wheat ends up in the rack and the plots are planted with new wheat

#### Scenario: Till inside the strip only
- **WHEN** a field with wheat set and radius 2 has dirt under empty plots 2 blocks from a rail and dirt 3 blocks from the rail, and the warehouse holds wheat seeds
- **THEN** the dirt 2 blocks out becomes farmland with wheat planted on it and the dirt 3 blocks out stays dirt

#### Scenario: Harvest only does not plant
- **WHEN** a field in harvest only mode has ripe wheat
- **THEN** the harvester harvests it and the farmland stays empty

#### Scenario: Unripe crops stay
- **WHEN** a harvester works a rail whose plots hold ripe and unripe wheat
- **THEN** only the ripe wheat is harvested

### Requirement: Seeds
To plant, the harvester SHALL take the field's crop item from its cargo, which holds the harvest. When it puts the harvest away while plots remain to plant, it SHALL keep up to one stack of the crop item, no more than those plots need; when the kept seeds leave no room for the next harvest, the next put-away SHALL keep none. Only when the field has no ripe plot left to harvest and the harvester carries fewer crop items than there are plots to plant SHALL it pick the shortfall, up to one stack and as far as its cargo has room, from the warehouse before it drives to the field. When the warehouse holds none, the harvester SHALL plant only what the harvest provides and leave the rest of the farmland empty.

#### Scenario: Replant from the harvest
- **WHEN** a field with wheat set has only ripe wheat and the warehouse holds no wheat seeds
- **THEN** every harvested plot is planted again with seeds from the harvest and the spare seeds are put away

#### Scenario: Seeds from storage
- **WHEN** a hydrated field with wheat set has only empty farmland and a rack on the harvester's floor holds wheat seeds
- **THEN** the harvester picks seeds from the rack, plants the farmland and the rack holds fewer seeds

### Requirement: Harvest put-away
The harvest SHALL go into warehouse storage by the put-away routing of inbound stations: zoned racks listing the item first, then zoned racks matching by tag, then general storage. The harvester SHALL drive to the front rail of each rack it can reach and insert there. A rack whose rails the harvester reaches only by lift SHALL be skipped. When no rack it may use has room for some of the cargo, the harvester SHALL wait on the rail of its garage with that cargo and try again about once a second; it SHALL NOT go back to work meanwhile.

#### Scenario: Zones apply
- **WHEN** a warehouse has a rack zoned for wheat and an unzoned rack, and a harvester brings wheat
- **THEN** the wheat goes into the zoned rack

#### Scenario: Warehouse full
- **WHEN** the only rack of a warehouse is full and a harvester comes back with wheat
- **THEN** the harvester waits at its garage holding the wheat, and puts it away once the player empties the rack

#### Scenario: Storage upstairs is skipped
- **WHEN** the rack zoned for wheat faces a rail one floor up, reached only by the shaft, and an unzoned rack stands on the harvester's floor
- **THEN** the wheat goes into the rack on the harvester's floor

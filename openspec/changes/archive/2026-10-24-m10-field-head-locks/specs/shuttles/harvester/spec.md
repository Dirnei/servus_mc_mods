## MODIFIED Requirements

### Requirement: Field cargo
A harvester on a field run SHALL carry up to the stacks of its tier (example 2 for Mk1), each stack one item type. When the drops of the next ripe plot do not fit, the harvester SHALL stop working, put the harvest away and come back while work remains. When the drops of a plot do not fit even into an empty cargo, the harvester SHALL harvest it anyway, the part that does not fit SHALL ride along to be put away, and the harvester SHALL put the harvest away before it works on. A seed kept for replanting that cannot be planted SHALL ride along the same way. Nothing SHALL drop on the plot.

#### Scenario: Full cargo goes home
- **WHEN** a Mk1 harvester harvests a field with more ripe wheat than two stacks hold
- **THEN** it drives to storage with a full cargo, puts it away and returns to harvest the rest

#### Scenario: A bigger tier stays out longer
- **WHEN** a Mk2 harvester harvests the same field
- **THEN** it carries up to four stacks before it drives to storage

### Requirement: Seeds
To plant, the harvester SHALL take the field's crop item from its cargo, which holds the harvest. When it puts the harvest away while plots remain to plant, it SHALL keep up to one stack of the crop item, no more than those plots need; when the kept seeds leave no room for the next harvest, the next put-away SHALL keep none. Spare crop items SHALL first fill the head's seed buffer up to its level, less the room a supply run reserved, at the farm rail beside the head, before the rest is put away. Only when the field has no ripe plot left to harvest and the harvester carries fewer crop items than there are plots to plant SHALL it take the shortfall, up to one stack and as far as its cargo has room, from the head's seed buffer, never from racks: it SHALL lock those seeds in the buffer (OUT) when it decides to fetch them, out of the seeds no other job reserved, and take them at the head. When the buffer holds none it may take, the harvester SHALL plant only what the harvest provides and leave the rest of the farmland empty.

#### Scenario: Replant from the harvest
- **WHEN** a field with wheat set has only ripe wheat and the warehouse holds no wheat seeds
- **THEN** every harvested plot is planted again with seeds from the harvest, the spare seeds fill the head's seed buffer and the rest is put away

#### Scenario: Seeds from storage
- **WHEN** a hydrated field with wheat set has only empty farmland and the head's seed buffer holds wheat seeds
- **THEN** the harvester locks the seeds it needs in the buffer, takes them at the head, plants the farmland and the buffer holds fewer seeds

### Requirement: Harvest put-away
The harvest SHALL go into warehouse storage by the put-away rule of inbound stations (see `shuttles/zones`): zone tiers first, then racks already holding the item (a partial stack first), then the others by free slots, cheaper travel deciding within each, with the travel cost from where the harvester stands. Only racks the harvester can drive to count; a rack whose rails it reaches only by lift SHALL be skipped. The harvester SHALL lock the room it fills in the racks it chose (IN) when it plans the put-away, drive to the front rail of each and unload there as its job. What a rack does not take on arrival SHALL be routed again by the same rule from where the harvester stands. When no rack it may use has room for some of the cargo, or none can be reached, the harvester SHALL drive into its garage and park there with that cargo as a shuttle with cargo that has nowhere to go (see `shuttles/shuttle-garage`): the garage shows the error state, the harvester takes no other job and tries again when room may have appeared. Once its cargo is stored it SHALL go on with its run.

#### Scenario: Zones apply
- **WHEN** a warehouse has a rack zoned for wheat and an unzoned rack, and a harvester brings wheat
- **THEN** the wheat goes into the zoned rack

#### Scenario: Holding rack first
- **WHEN** an unzoned rack far away holds a partial stack of wheat and an empty unzoned rack stands next to the field, and a harvester brings wheat
- **THEN** the wheat goes into the rack that holds wheat

#### Scenario: Room locked on the way
- **WHEN** a harvester drives to a rack with 40 wheat
- **THEN** the rack slots it fills hold IN locks for 40 wheat naming its field job until it unloads there

#### Scenario: Warehouse full
- **WHEN** the only rack of a warehouse is full and a harvester comes back with wheat
- **THEN** the harvester parks in its garage holding the wheat, the garage lamp shows the error, and it puts the wheat away once the player empties the rack

#### Scenario: Storage upstairs is skipped
- **WHEN** the rack zoned for wheat faces a rail one floor up, reached only by the shaft, and an unzoned rack stands on the harvester's floor
- **THEN** the wheat goes into the rack on the harvester's floor

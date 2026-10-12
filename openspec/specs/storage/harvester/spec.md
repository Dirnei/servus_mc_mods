# storage/harvester Specification

## Purpose

Harvester shuttles tend the fields of their warehouse: they drive out over the farm rails, harvest ripe crops, till and plant, and bring the harvest home through put-away.

## Requirements

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

### Requirement: Field jobs
A warehouse SHALL look at the Field Head of every field whose farm rails are part of its rails every `field_check_interval` ticks (in `data/<ns>/storage/field.json`, example 40). It SHALL queue a field job for that field when it has a Harvester shuttle, no field job for that field is queued or running, and either the head signals harvest due, or the field has a crop set, is hydrated and has a plot to till or, while the warehouse holds the crop item, a plot to plant. At most one field job per field SHALL be queued or running at a time. A field job SHALL go to the free Harvester shuttle with the shortest way to a farm rail of the field that has work.

#### Scenario: Harvest due sends a harvester
- **WHEN** a field with wheat set has a harvest due and its warehouse has a free Harvester shuttle
- **THEN** within a few seconds the harvester leaves its garage and drives to the field

#### Scenario: Nothing to do
- **WHEN** a hydrated field with wheat set has every plot planted and nothing ripe
- **THEN** no field job is queued

#### Scenario: Planting needs seeds
- **WHEN** a hydrated field with wheat set has only empty farmland and the warehouse holds no wheat seeds
- **THEN** no field job is queued

### Requirement: Field run
On a field run the harvester SHALL drive over the rails to the nearest farm rail of the field whose plots have work, work those plots, and then drive to the next nearest farm rail with work, until no rail has work it can do or its cargo is full. A work segment in which nothing on the field changed (for example a crop that cannot grow on a dark plot) SHALL end the run. Each plot SHALL be worked from the farm rail nearest to it, so the harvester reaches at most 4 blocks, the strip of the field, to each side of the rail it stands on. Working a plot that needs something done SHALL take `work_ticks` ticks (in `field.json`, example 10), shortened by the shuttle's Handling upgrades like a stop and at least one tick; plots that need nothing SHALL cost no time. The harvester SHALL be drawn standing on the rail of the plots it works and moving between rails like other shuttles. After working a plot the head SHALL count the plot as it is now.

#### Scenario: Rail by rail
- **WHEN** a field of four farm rails in a row has ripe crops beside the first and the last rail
- **THEN** the harvester stops at the first rail, works those plots, drives to the last rail and works the plots beside it

#### Scenario: Head sees the work
- **WHEN** the harvester has harvested every ripe plot of a field in harvest only mode
- **THEN** the head counts no ripe plot

#### Scenario: Handling speeds up field work
- **WHEN** a harvester with two Handling upgrades works plots, with `work_ticks` 10 and a Handling factor of 0.8
- **THEN** each plot takes about 6 ticks instead of 10

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

### Requirement: Field cargo
A harvester on a field run SHALL carry up to the stacks of its tier (example 2 for Mk1), each stack one item type. When the drops of the next ripe plot do not fit, the harvester SHALL stop working, put the harvest away and come back while work remains. When the drops of a plot do not fit even into an empty cargo, the harvester SHALL harvest it anyway and the part that does not fit SHALL drop on the plot.

#### Scenario: Full cargo goes home
- **WHEN** a Mk1 harvester harvests a field with more ripe wheat than two stacks hold
- **THEN** it drives to storage with a full cargo, puts it away and returns to harvest the rest

#### Scenario: A bigger tier stays out longer
- **WHEN** a Mk2 harvester harvests the same field
- **THEN** it carries up to four stacks before it drives to storage

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

### Requirement: Runs that cannot go on
- When the Field Head is removed or no longer serves the field, the harvester SHALL stop working, put away what it carries and go home.
- When the field loses its water during a run, the harvester SHALL finish the rail it works on; it SHALL then go on only while ripe plots remain and SHALL NOT start more tilling or planting.
- When the rail the harvester stands on or drives over is removed, or its garage loses the way home, the harvester SHALL go back into its garage and its cargo SHALL drop at the garage.
- When its garage is removed or the player recalls the garage's shuttles, the cargo SHALL drop at the garage.
A field job that ends in one of these ways SHALL NOT be queued again by itself; the warehouse SHALL queue a new one at a later check if the field still has work.

#### Scenario: Head broken mid run
- **WHEN** the player breaks the Field Head while the harvester works the field with wheat in its cargo
- **THEN** the harvester stops, puts the wheat into storage and parks

#### Scenario: Rail broken under the harvester
- **WHEN** the player breaks the farm rail the harvester stands on
- **THEN** the harvester is back in its garage and its cargo lies on the ground at the garage

#### Scenario: Recall
- **WHEN** the player recalls the garage while its harvester works a field
- **THEN** the harvester is back in its garage and its cargo lies on the ground at the garage

### Requirement: Harvesters never ride a lift
A tool module SHALL be either wide or not; the Harvester SHALL be wide. A shuttle with a wide tool SHALL never call or ride the lift car of an elevator shaft: its ways SHALL leave out every step up or down a shaft. It SHALL still drive straight through a shaft on its own floor, over the floor's deck, like any shuttle that only crosses. A field, a source of seeds or a storage block that such a shuttle reaches only by lift SHALL count as out of its reach.

#### Scenario: Crossing a shaft on the floor
- **WHEN** a harvester's garage and its field lie on one floor with an elevator shaft between them on that floor
- **THEN** the harvester drives through the shaft to the field without calling the car

#### Scenario: Refusing the lift
- **WHEN** a harvester's garage stands on the floor below its field and the shaft is the only way up
- **THEN** the harvester stays in its garage and the field is not worked

#### Scenario: Plain shuttles still ride
- **WHEN** a plain shuttle has a pick from a rack one floor up
- **THEN** it rides the lift car as before

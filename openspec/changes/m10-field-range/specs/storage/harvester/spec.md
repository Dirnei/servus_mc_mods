## ADDED Requirements

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
- **WHEN** a warehouse has only a Harvester shuttle and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles and the harvester stays in its garage

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
- **WHEN** a field with wheat set has ripe wheat and the warehouse has a crate with room
- **THEN** the harvester breaks the ripe wheat, the wheat ends up in the crate and the plots are planted with new wheat

#### Scenario: Till inside the strip only
- **WHEN** a field with wheat set and radius 2 has dirt under empty plots 2 blocks from a rail and dirt 3 blocks from the rail, and the warehouse holds wheat seeds
- **THEN** the dirt 2 blocks out becomes farmland with wheat planted on it and the dirt 3 blocks out stays dirt

#### Scenario: Harvest only does not plant
- **WHEN** a field in harvest only mode has ripe wheat
- **THEN** the harvester harvests it and the farmland stays empty

#### Scenario: Unripe crops stay
- **WHEN** a harvester works a rail whose plots hold ripe and unripe wheat
- **THEN** only the ripe wheat is harvested

### Requirement: Harvest put-away
The harvest SHALL go into warehouse storage by the put-away routing of inbound stations: zoned blocks listing the item first, then zoned blocks matching by tag, then general storage. The harvester SHALL drive to a rail of each target it can reach and insert there; a target no rail of the warehouse reaches SHALL take its part at once. A target whose rails the harvester reaches only by lift SHALL be skipped. A target may be a Heavy Duty Rack the harvester can reach. When no block it may use has room for some of the cargo, the harvester SHALL wait on the rail of its garage with that cargo and try again about once a second; it SHALL NOT go back to work meanwhile.

#### Scenario: Zones apply
- **WHEN** a warehouse has a crate zoned for wheat and an unzoned crate, and a harvester brings wheat
- **THEN** the wheat goes into the zoned crate

#### Scenario: Warehouse full
- **WHEN** the only crate of a warehouse is full and a harvester comes back with wheat
- **THEN** the harvester waits at its garage holding the wheat, and puts it away once the player empties the crate

#### Scenario: Storage upstairs is skipped
- **WHEN** the crate zoned for wheat stands beside a rail one floor up, reached only by the shaft, and an unzoned crate stands on the harvester's floor
- **THEN** the wheat goes into the crate on the harvester's floor

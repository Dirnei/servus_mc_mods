## ADDED Requirements

### Requirement: Harvest unloads at the head
A harvester SHALL NOT drive to racks. When its cargo is full and when its run ends, it SHALL unload at the farm rail beside the Field Head: spare crop items into the seed buffer as before, everything else into the head's To storage buffer (see `shuttles/field-head`). When that buffer does not take everything, the harvester SHALL park in its garage with the rest as a shuttle with cargo that has nowhere to go, and unload it at the head once the buffer has room again.

#### Scenario: Full cargo unloads at the head
- **WHEN** a Mk1 harvester has two stacks of wheat aboard and ripe plots remain
- **THEN** it drives to the farm rail beside the head, puts the wheat into the To storage buffer and goes on harvesting

#### Scenario: Storage on another floor
- **WHEN** the only rack of the warehouse faces a rail one floor up, reached only by the shaft, and a harvester comes back with wheat
- **THEN** the wheat goes into the head's To storage buffer and not into storage on the harvester's floor

#### Scenario: Buffer full mid run
- **WHEN** the To storage buffer has room for one stack and the harvester carries two
- **THEN** one stack goes into the buffer, and the harvester parks in its garage with the other until a plain shuttle has collected from the buffer

## MODIFIED Requirements

### Requirement: Field jobs
A warehouse SHALL look at the Field Head of every field whose farm rails are part of its rails every `field_check_interval` ticks (in `data/<ns>/shuttles/field.json`, example 40). It SHALL queue a field job for that field when it has a Harvester shuttle, no field job for that field is queued or running, the head's To storage buffer has a free slot, and either the head signals harvest due, or the field has a crop set, is hydrated and has a plot to till or, while the warehouse holds the crop item, a plot to plant. At most one field job per field SHALL be queued or running at a time. A field job SHALL go to the free Harvester shuttle with the shortest way to a farm rail of the field that has work.

#### Scenario: Harvest due sends a harvester
- **WHEN** a field with wheat set has a harvest due and its warehouse has a free Harvester shuttle
- **THEN** within a few seconds the harvester leaves its garage and drives to the field

#### Scenario: Nothing to do
- **WHEN** a hydrated field with wheat set has every plot planted and nothing ripe
- **THEN** no field job is queued

#### Scenario: Planting needs seeds
- **WHEN** a hydrated field with wheat set has only empty farmland and the warehouse holds no wheat seeds
- **THEN** no field job is queued

#### Scenario: To storage buffer full
- **WHEN** a field has a harvest due and every slot of its head's To storage buffer holds items
- **THEN** no field job is queued until a plain shuttle has collected from the buffer

### Requirement: Field cargo
A harvester on a field run SHALL carry up to the stacks of its tier (example 2 for Mk1), each stack one item type. When the drops of the next ripe plot do not fit, the harvester SHALL stop working, unload at the head and come back while work remains. When the drops of a plot do not fit even into an empty cargo, the harvester SHALL harvest it anyway, the part that does not fit SHALL ride along to be unloaded, and the harvester SHALL unload at the head before it works on. A seed kept for replanting that cannot be planted SHALL ride along the same way. Nothing SHALL drop on the plot.

#### Scenario: Full cargo goes home
- **WHEN** a Mk1 harvester harvests a field with more ripe wheat than two stacks hold
- **THEN** it drives to the farm rail beside the head with a full cargo, unloads it there and returns to harvest the rest

#### Scenario: A bigger tier stays out longer
- **WHEN** a Mk2 harvester harvests the same field
- **THEN** it carries up to four stacks before it unloads at the head

### Requirement: Runs that cannot go on
- When the Field Head is removed or no longer serves the field, the harvester SHALL stop working, go home and its cargo SHALL drop at the garage.
- When the field loses its water during a run, the harvester SHALL finish the rail it works on; it SHALL then go on only while ripe plots remain and SHALL NOT start more tilling or planting.
- When the rail the harvester stands on or drives over is removed, or its garage loses the way home, the harvester SHALL go back into its garage and its cargo SHALL drop at the garage.
- When its garage is removed or the player recalls the garage's shuttles, the cargo SHALL drop at the garage.
A field job that ends in one of these ways SHALL NOT be queued again by itself; the warehouse SHALL queue a new one at a later check if the field still has work.

#### Scenario: Head broken mid run
- **WHEN** the player breaks the Field Head while the harvester works the field with wheat in its cargo
- **THEN** the harvester stops, goes home and the wheat lies on the ground at the garage

#### Scenario: Rail broken under the harvester
- **WHEN** the player breaks the farm rail the harvester stands on
- **THEN** the harvester is back in its garage and its cargo lies on the ground at the garage

#### Scenario: Recall
- **WHEN** the player recalls the garage while its harvester works a field
- **THEN** the harvester is back in its garage and its cargo lies on the ground at the garage

## REMOVED Requirements

### Requirement: Harvest put-away
**Reason**: Harvesters never ride a lift, so racks on another floor were out of reach and zones were ignored; the harvest now goes into the Field Head's To storage buffer and plain shuttles put it away by the zone rules.
**Migration**: None; harvests reach storage through the head.

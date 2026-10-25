## MODIFIED Requirements

### Requirement: Harvester module
The Harvester SHALL be a craftable tool module. It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's tool place. A shuttle with a Harvester SHALL be field only: it SHALL take the field jobs of Crops fields and SHALL NOT take warehouse picks, put-aways or the jobs of Trees fields. A shuttle without a Harvester SHALL NOT take the field jobs of Crops fields. The Harvester SHALL be wide, and its tooltip SHALL say that it does not fit into lifts.

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

#### Scenario: Harvesters leave trees alone
- **WHEN** a warehouse has a Harvester shuttle and a Trees field with grown trees
- **THEN** the harvester stays in its garage

### Requirement: Field jobs
A warehouse SHALL look at the Field Head of every field whose farm rails are part of its rails every `field_check_interval` ticks (in `data/<ns>/shuttles/field.json`, example 40). It SHALL queue a field job for a Crops field when it has a Harvester shuttle, no field job for that field is queued or running, the head's To storage buffer has a free slot, and either the head signals harvest due, or the field has a crop set, is hydrated and has a plot to till or, while the warehouse holds the crop item, a plot to plant. At most one field job per field SHALL be queued or running at a time. A field job SHALL go to the free Harvester shuttle with the shortest way to a farm rail of the field that has work. Trees fields get their jobs from Lumber shuttles (see `shuttles/lumber`).

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

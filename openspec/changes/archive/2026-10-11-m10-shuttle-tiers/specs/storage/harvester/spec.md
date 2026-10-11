## MODIFIED Requirements

### Requirement: Harvester module
The Harvester SHALL be a craftable tool module. It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's tool place. A shuttle with a Harvester SHALL be field only: it SHALL take field jobs and SHALL NOT take warehouse picks or put-aways. A shuttle without a Harvester SHALL NOT take field jobs.

#### Scenario: Fit a Harvester
- **WHEN** a player puts a Mk1 shuttle into the workshop and a Harvester into the install slot
- **THEN** the shuttle has the Harvester and its tooltip lists it

#### Scenario: Plain shuttles stay home
- **WHEN** a warehouse has only shuttles without a tool and its field has ripe crops with a harvest due
- **THEN** no shuttle drives onto the field and the crops stay on the field

#### Scenario: Harvesters stay out of the warehouse
- **WHEN** a warehouse has only a Harvester shuttle and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles and the harvester stays in its garage

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

### Requirement: Field cargo
A harvester on a field run SHALL carry up to the stacks of its tier (example 2 for Mk1), each stack one item type. When the drops of the next ripe plot do not fit, the harvester SHALL stop working, put the harvest away and come back while work remains. When the drops of a plot do not fit even into an empty cargo, the harvester SHALL harvest it anyway and the part that does not fit SHALL drop on the plot.

#### Scenario: Full cargo goes home
- **WHEN** a Mk1 harvester harvests a field with more ripe wheat than two stacks hold
- **THEN** it drives to storage with a full cargo, puts it away and returns to harvest the rest

#### Scenario: A bigger tier stays out longer
- **WHEN** a Mk2 harvester harvests the same field
- **THEN** it carries up to four stacks before it drives to storage

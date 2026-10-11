## ADDED Requirements

### Requirement: Open plots tracking
Besides planted and ripe plots, the head SHALL know which plots are open: a plot is to till when it is empty and stands on a dirt or grass block, and to plant when it is empty and stands on farmland. It SHALL learn this from the same events and passes as ripeness, and also from blocks broken or placed one below a plot and from blocks changed by a tool one below a plot. It SHALL NOT scan plots every tick.

#### Scenario: Dirt under an empty plot
- **WHEN** a field is loaded with dirt under one empty plot and farmland under another
- **THEN** the head counts one plot to till and one plot to plant

#### Scenario: Hoed by a player
- **WHEN** a player tills the dirt under an empty plot with a hoe
- **THEN** a tick later the head counts that plot to plant instead of to till

#### Scenario: Planted by a player
- **WHEN** a player plants wheat seeds on farmland under a plot to plant
- **THEN** a tick later the head counts that plot planted and not open

### Requirement: Field work status
The Field Head SHALL show in its GUI, and the field command SHALL print, what its served field is doing or the first thing that keeps a harvester away, checked in this order: rails not linked to a warehouse, warehouse without exactly one controller, no crop and no harvest only, no Harvester shuttle in a garage of that warehouse, a run queued or under way, harvest due, harvest only waiting for ripe crops, plots to work but no water, plots to work, farmland but no seeds in the warehouse, everything growing, no plot to till or plant.

#### Scenario: Field without a controller
- **WHEN** a served field's farm rails are linked to rails, a garage and racks but no Warehouse Controller
- **THEN** the Field Head GUI says the warehouse has no controller

#### Scenario: Ready field
- **WHEN** the warehouse is online, a Harvester shuttle sits in its garage, wheat is set, the head has water and grass plots lie in the strip
- **THEN** the GUI says work is ready and a harvester is coming


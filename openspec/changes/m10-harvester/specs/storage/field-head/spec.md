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

### Requirement: Plots in sight of the rails
A field's plots SHALL be the positions at rail level within 4 blocks of a farm rail that are joined to a rail through a chain of open plots, stepping north, east, south and west. A plot is open when it holds air, a crop or a replaceable plant such as short grass, over dirt, a grass block or farmland. A path, a block, water or a gap between rail and soil ends the field there; filling the gap brings the plots behind it back. The harvester clears a replaceable plant before it tills or plants.

#### Scenario: Dirt behind a gap
- **WHEN** dirt lies two plots beside a rail and the plot between holds no soil
- **THEN** the far dirt is not part of the field and is not counted to till

#### Scenario: Gap filled
- **WHEN** the player puts dirt into the gap
- **THEN** the far plot joins the field and counts to till

#### Scenario: Overgrown grass
- **WHEN** a grass block beside a rail has short grass on it
- **THEN** the plot counts to till and the harvester clears the grass and tills it

### Requirement: Harvest threshold per head
Each Field Head SHALL keep its own harvest threshold, the ripe share of the planted plots that makes a harvest due, set in its GUI with down and up buttons in steps of 10 percent from 10 to 100 percent and saved with the head. A head that was never set follows `harvest_share` from `field.json`. The GUI SHALL show the ripe and planted counts with a bar of the ripe share and a mark at the threshold; the bar turns green once the harvest is due.

#### Scenario: Lower threshold
- **WHEN** one of four planted plots is ripe and the player lowers the threshold from 50 to 20 percent
- **THEN** the harvest becomes due

#### Scenario: Default threshold
- **WHEN** a Field Head is placed and `harvest_share` is 0.5
- **THEN** its GUI shows "Harvest at 50%"


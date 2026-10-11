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

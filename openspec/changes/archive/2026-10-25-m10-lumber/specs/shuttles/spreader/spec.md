## MODIFIED Requirements

### Requirement: Dosing the field
On the field the Spreader SHALL drive to the nearest farm rail whose plots need a dose and work them, then on to the next nearest rail, until no rail has plots that need a dose or its fertilizer is used up. Each plot SHALL get up to `doses_per_plot` doses in one round, each dose one item of the fertilizer applied like bone meal on the crop, and working a plot SHALL take `work_ticks` shortened by Handling like the Harvester. A plot SHALL stop getting doses once its crop is ripe; ripe plots and plots without an age crop SHALL never be dosed. In a Trees field a sapling on a spot SHALL be dosed like a crop and SHALL stop getting doses once it has grown into a tree; grown trees SHALL never be dosed. Every dose SHALL let the head count the plot's growth like bone meal used by a player.

#### Scenario: Doses make crops grow
- **WHEN** the Spreader doses a wheat crop at its first age twice
- **THEN** the wheat is at least four ages older and the head counts it as it is now

#### Scenario: Ripe plots untouched
- **WHEN** a field set to fertilize until a ripe share has one ripe and one young wheat plot
- **THEN** only the young wheat gets doses

#### Scenario: Bone meal on saplings
- **WHEN** a Trees field set to fertilize after planting has a newly planted oak sapling and the Spreader doses it until it grows
- **THEN** the oak stands on the spot, the Spreader gives it no further dose and the head counts the spot ripe

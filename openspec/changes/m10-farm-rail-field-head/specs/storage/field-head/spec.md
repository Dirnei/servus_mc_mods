## Purpose

The Field Head waters a field of farm rails and keeps track of which plots are ripe, so harvester shuttles know when a run is worth it.

## ADDED Requirements

### Requirement: One head per field
A Field Head SHALL be a craftable block. It SHALL serve the field of a farm rail touching one of its four horizontal sides; when it touches farm rails of more than one field it SHALL serve the field of the first one in the order north, east, south, west. A field SHALL have at most one serving head: when several heads touch the same field, only one serves it and the others SHALL show that the field has another head. A field larger than the maximum size (`max_rails` in `data/<ns>/storage/field.json`, example 128 rails) SHALL NOT be served, and the head SHALL show that the field is too large. A head without a farm rail beside it SHALL show that it has no field.

#### Scenario: Head beside a run
- **WHEN** a Field Head is placed beside the end of a run of four farm rails
- **THEN** its GUI shows a field of four rails

#### Scenario: Second head
- **WHEN** a second Field Head is placed beside another rail of the same run
- **THEN** one of the two heads serves the field and the other shows that the field has another head

#### Scenario: No rail
- **WHEN** a Field Head stands with no farm rail beside it
- **THEN** its GUI shows that it has no field

### Requirement: Water buffer
A Field Head SHALL expose a fluid handler on every side that accepts water and refuses every other fluid, so the Foundation fluid pipe and other mods can fill it. Its buffer SHALL hold up to `capacity` mB (example 8000). It SHALL NOT give water back out. While it serves a field, the head SHALL use `water_per_rail` mB (example 1) per rail of its field every `water_interval` ticks (example 100). When the buffer holds less than one interval's use, the field SHALL stop being hydrated until enough water is in the buffer again. The water level SHALL be kept when the world is saved.

#### Scenario: Fill with water
- **WHEN** 1000 mB of water are offered to a Field Head
- **THEN** it takes them and its GUI shows the new level

#### Scenario: Lava refused
- **WHEN** lava is offered to a Field Head
- **THEN** it takes none

#### Scenario: Water through the pipe
- **WHEN** a Utility Duct with a fluid line runs from a pump on water to a Field Head
- **THEN** the head's buffer fills

#### Scenario: Water runs out
- **WHEN** a head serving a field of three rails holds exactly one interval's use and gets no more water
- **THEN** the field is hydrated for one interval and then dries

### Requirement: Hydration
While its field is hydrated, the head SHALL keep wet every farmland block in the 9 by 9 square centred on each farm rail of the field, one block below the rail (4 blocks to each side of every rail). Farmland in that square SHALL behave as if water were near it: it does not dry out and crops on it grow at the moistened rate. Hydration SHALL end when the buffer runs dry, when the head is removed or unloaded, or for the part of the field a removed rail took away. It SHALL resume after a world reload without any action from the player.

#### Scenario: Farmland stays wet
- **WHEN** a head with water serves a straight field and farmland lies four blocks beside a rail, one block below it
- **THEN** that farmland counts as near water

#### Scenario: Out of reach
- **WHEN** farmland lies five blocks beside the nearest rail of the field
- **THEN** the head does not keep it wet

#### Scenario: Rail removed
- **WHEN** the only rail beside a patch of farmland is broken
- **THEN** that farmland no longer counts as near water

### Requirement: Ripeness tracking
The plots of a field SHALL be the positions at the rail's level inside the hydrated squares, except the rail positions. The head SHALL know which plots hold an age crop (wheat, carrots, potatoes, beetroot and other crops of that kind) and which of those are ripe. It SHALL learn this from crop growth, bone meal, a block being broken or placed on a plot and farmland being trampled, and from one pass over all plots when the field is loaded, changed or rechecked by command. It SHALL NOT scan plots every tick. The ripe share SHALL be the ripe plots divided by the planted plots. The head SHALL signal harvest due when its field has a crop set or is in harvest only mode, at least one plot is planted and the ripe share reaches `harvest_share` (example 0.5).

#### Scenario: Crop grows ripe
- **WHEN** a wheat crop on a plot grows to its last stage
- **THEN** the head counts one more ripe plot

#### Scenario: Bone meal
- **WHEN** a player uses bone meal on a carrot crop of a plot until it is ripe
- **THEN** the head counts it ripe a tick later

#### Scenario: Harvested by hand
- **WHEN** a player breaks a ripe crop on a plot
- **THEN** the head counts one ripe plot less

#### Scenario: Due at the threshold
- **WHEN** a field with wheat set has four planted plots and the second one becomes ripe
- **THEN** the head signals harvest due

#### Scenario: Nothing set
- **WHEN** a field has neither a crop nor harvest only set and all its crops are ripe
- **THEN** the head does not signal harvest due

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI that shows the water level and capacity, the field size in rails and plots, the planted and ripe plot counts, whether the field is hydrated and whether a harvest is due, or why the head serves no field. The GUI SHALL have one ghost slot for the crop of the field: clicking it with a seed or crop item that plants an age crop sets that crop without taking the item, clicking it with an empty hand clears it, and other items are refused. A harvest only toggle SHALL set the field to harvest without planting; turning it on clears the crop and setting a crop turns it off. The crop and the mode SHALL be kept when the world is saved.

#### Scenario: Set wheat
- **WHEN** a player clicks the crop slot holding wheat seeds
- **THEN** the field's crop is wheat and the player keeps the seeds

#### Scenario: Refuse a non seed
- **WHEN** a player clicks the crop slot holding cobblestone
- **THEN** the crop does not change

#### Scenario: Harvest only clears the crop
- **WHEN** wheat is set and the player turns harvest only on
- **THEN** the crop slot is empty and the field is in harvest only mode

### Requirement: Field diagnostics
`/servus_warehouse field <pos>` SHALL print, for the Field Head at that position, its status, rails, plots, planted and ripe counts, water level, crop or mode and whether a harvest is due. `/servus_warehouse field <pos> recheck` SHALL run the pass over all plots again.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails and the head's water level

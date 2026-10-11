# storage/field-head Specification

## Purpose

The Field Head waters a field of farm rails and keeps track of which plots are ripe, so harvester shuttles know when a run is worth it.

## Requirements

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
While its field is hydrated, the head SHALL keep wet every farmland block within the field radius of each farm rail of the field, one block below the rail: the square of radius blocks to each side of every rail. Farmland in that square SHALL behave as if water were near it: it does not dry out and crops on it grow at the moistened rate. Hydration SHALL end when the buffer runs dry, when the head is removed or unloaded, or for the part of the field a removed rail or a smaller radius took away. It SHALL resume after a world reload without any action from the player.

#### Scenario: Farmland stays wet
- **WHEN** a head with water and radius 2 serves a straight field and farmland lies two blocks beside a rail, one block below it
- **THEN** that farmland counts as near water

#### Scenario: Out of reach
- **WHEN** farmland lies three blocks beside the nearest rail of a field with radius 2
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
Right clicking a Field Head SHALL open a GUI that shows the water level and capacity, the field size in rails and plots and its radius, the planted and ripe plot counts, whether the field is hydrated and whether a harvest is due, or why the head serves no field. The GUI SHALL have one ghost slot for the crop of the field: clicking it with a seed or crop item that plants an age crop sets that crop without taking the item, clicking it with an empty hand clears it, and other items are refused. The GUI SHALL have the Range Upgrade slot of the field radius. A harvest only toggle SHALL set the field to harvest without planting; turning it on clears the crop and setting a crop turns it off. The crop and the mode SHALL be kept when the world is saved. The GUI SHALL have the anti-corrosion upkeep slot and SHALL show the corrosion stage of the served field with the progress toward the next aging step, or, for a corroded field, that the rails are corroded and the field is not watered.

#### Scenario: Set wheat
- **WHEN** a player clicks the crop slot holding wheat seeds
- **THEN** the field's crop is wheat and the player keeps the seeds

#### Scenario: Refuse a non seed
- **WHEN** a player clicks the crop slot holding cobblestone
- **THEN** the crop does not change

#### Scenario: Harvest only clears the crop
- **WHEN** wheat is set and the player turns harvest only on
- **THEN** the crop slot is empty and the field is in harvest only mode

#### Scenario: Radius shown
- **WHEN** a player opens the GUI of a head holding one Range Upgrade with `radius` 2
- **THEN** the GUI shows radius 3

#### Scenario: Corroded field shown
- **WHEN** a player opens the GUI of a head whose field is corroded
- **THEN** the GUI says the rails are corroded and the field is not watered

#### Scenario: Honeycomb by hand
- **WHEN** a player shift clicks honeycomb in the head's GUI
- **THEN** the honeycomb moves into the anti-corrosion slot

### Requirement: Field diagnostics
`/servus_warehouse field <pos>` SHALL print, for the Field Head at that position, its status, rails, plots, planted and ripe counts, water level, crop or mode, whether a harvest is due, the corrosion stage and the water counted toward the next aging step. `/servus_warehouse field <pos> recheck` SHALL run the pass over all plots again. `/servus_warehouse field <pos> age <mB>` SHALL count that much water as used for aging, with the same upkeep and stage rules as water used by hydration, so a stage can be reached quickly for testing.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails, the head's water level and the corrosion stage

#### Scenario: Age by command
- **WHEN** an operator runs the age command with `corrosion_water` mB on a head of a copper field without anti-corrosion items
- **THEN** the field becomes exposed

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
The Field Head SHALL show in its GUI, and the field command SHALL print, what its served field is doing or the first thing that keeps a harvester away, checked in this order: rails not linked to a warehouse, warehouse without exactly one controller, no crop and no harvest only, no Harvester shuttle in a garage of that warehouse, no Harvester shuttle that can reach the field without riding a lift car, a run queued or under way, harvest due, harvest only waiting for ripe crops, plots to work but no water, plots to work, farmland but no seeds in the warehouse, everything growing, no plot to till or plant. The warehouse SHALL NOT send a run to a field no harvester can reach without a lift.

#### Scenario: Field without a controller
- **WHEN** a served field's farm rails are linked to rails, a garage and racks but no Warehouse Controller
- **THEN** the Field Head GUI says the warehouse has no controller

#### Scenario: Ready field
- **WHEN** the warehouse is online, a Harvester shuttle sits in its garage, wheat is set, the head has water and grass plots lie in the strip
- **THEN** the GUI says work is ready and a harvester is coming

#### Scenario: Field on another floor
- **WHEN** the only Harvester shuttle's garage stands on the floor below the field and an elevator shaft is the only way up
- **THEN** the GUI says no harvester can reach the field without a lift, and no harvester leaves its garage

### Requirement: Plots in sight of the rails
A field's plots SHALL be the positions at rail level within the field radius of a farm rail that the rails see in straight lines. Every rail looks straight out, at 90 degrees, on each side without a farm rail next to it; the first plot that is not open ends that line. Where a rail has two such sides at a right angle, at the end of a run or on the outside of a bend, it also sees the corner between them along an L: straight out on one side, then 90 degrees towards the other, every plot of the L open. Nothing is seen around a blocker. A plot is open when it holds air, a crop or a replaceable plant such as short grass, over dirt, a grass block or farmland; pipes, garages, the Field Head, paths, water and gaps without soil are not. Filling a gap brings the plots behind it back. The harvester clears a replaceable plant before it tills or plants.

#### Scenario: Dirt behind a gap
- **WHEN** dirt lies two plots beside a rail and the plot between holds no soil
- **THEN** the far dirt is not part of the field and is not counted to till

#### Scenario: Pipe beside the rail
- **WHEN** a pipe stands on the plot next to a middle rail of a run
- **THEN** the plots straight behind the pipe are out of sight, even when soil around the pipe would join them

#### Scenario: Corner at the end of a run
- **WHEN** the plots past the end rail and beside it are soil
- **THEN** the corner square between them is part of the field

#### Scenario: Gap filled
- **WHEN** the player puts dirt into the gap
- **THEN** the far plot joins the field and counts to till

#### Scenario: Overgrown grass
- **WHEN** a grass block beside a rail has short grass on it
- **THEN** the plot counts to till and the harvester clears the grass and tills it

#### Scenario: Beyond the radius
- **WHEN** a field has radius 2 and open soil lies three blocks beside a rail
- **THEN** that soil is not part of the field

### Requirement: Harvest threshold per head
Each Field Head SHALL keep its own harvest threshold, the ripe share of the planted plots that makes a harvest due, set in its GUI with down and up buttons in steps of 10 percent from 10 to 100 percent and saved with the head. A head that was never set follows `harvest_share` from `field.json`. The GUI SHALL show the ripe and planted counts with a bar of the ripe share and a mark at the threshold; the bar turns green once the harvest is due.

#### Scenario: Lower threshold
- **WHEN** one of four planted plots is ripe and the player lowers the threshold from 50 to 20 percent
- **THEN** the harvest becomes due

#### Scenario: Default threshold
- **WHEN** a Field Head is placed and `harvest_share` is 0.5
- **THEN** its GUI shows "Harvest at 50%"

### Requirement: Field radius
How far a field reaches to each side of its farm rails SHALL be the radius of its Field Head: `radius` from `field.json` (example 2), plus one for each Range Upgrade in the head, at most `max_radius` from `field.json` (example 4). The Range Upgrade SHALL be a crafted item. The Field Head GUI SHALL have one upgrade slot that takes only Range Upgrades, and only as many as the radius can still grow. Putting an upgrade in or taking one out SHALL change the field at once: its plots, hydration, ripe share and the plots the harvester works follow the new radius without the player touching a rail. The upgrades SHALL be kept when the world is saved and SHALL drop when the head is broken.

#### Scenario: A new head reaches two blocks
- **WHEN** a player places a Field Head beside a farm rail with no upgrade and `radius` is 2
- **THEN** the field reaches 2 blocks to each side of every rail

#### Scenario: Two upgrades reach four blocks
- **WHEN** a player puts two Range Upgrades into the head with `radius` 2 and `max_radius` 4
- **THEN** the field reaches 4 blocks to each side and soil 3 and 4 blocks out joins it

#### Scenario: No upgrade past the maximum
- **WHEN** the head holds two Range Upgrades with `radius` 2 and `max_radius` 4 and the player tries to put in a third
- **THEN** the slot refuses it

#### Scenario: Taking an upgrade out shrinks the field
- **WHEN** the head with two upgrades loses one
- **THEN** farmland 4 blocks out is no longer kept wet and its plots leave the field

#### Scenario: Upgrades drop with the head
- **WHEN** a player breaks a Field Head holding two Range Upgrades
- **THEN** the head and both upgrades drop

### Requirement: Field Head wood and facing
The Field Head SHALL look like a station: board panels in the wood of its recipe under the station iron frame. Its recipe SHALL take planks, the crafted item SHALL carry the wood picked from them, a placed head SHALL show that wood, and the drop of a broken head SHALL carry it again. The head SHALL have a horizontal facing, and its front SHALL face the player who places it.

#### Scenario: Wood from the recipe
- **WHEN** a player crafts a Field Head with birch planks
- **THEN** the item names birch as its wood, the placed head shows birch boards, and breaking it drops a Field Head with birch

#### Scenario: Front faces the player
- **WHEN** a player looking north places a Field Head
- **THEN** the head's front faces south, toward the player

### Requirement: Ripe share bar
The front of the Field Head SHALL show a ripe share bar in its lower panel: a small dark window that fills from the left with wheat gold in a few steps as the ripe share of the planted plots approaches the head's harvest threshold, and that is full and green while a harvest is due. The bar SHALL be empty when nothing is ripe or the head serves no field. The head SHALL change the bar only when its ripe count, planted count, crop, harvest only mode or threshold changes, never by looking at the field per tick.

#### Scenario: Bar follows the ripe share
- **WHEN** a head with wheat set and a threshold of 50 percent sees one of four planted plots ripe
- **THEN** its bar shows wheat gold, part way filled, and not green

#### Scenario: Bar turns green when due
- **WHEN** the ripe share reaches the head's threshold and a harvest is due
- **THEN** the bar is full and green

#### Scenario: Lowering the threshold
- **WHEN** the threshold is lowered in the GUI to the current ripe share
- **THEN** the bar turns green without any crop changing

### Requirement: Water window
Each side of the Field Head SHALL show a dark glass window in its upper panel, with the head's water drawn as real water (its still texture and the biome's water tint) from the bottom of the window up to the fill share of the buffer. An empty buffer SHALL show the dark glass only. The client SHALL learn the water level in a few steps of the window height (for example eighths), and only when the step changes, not on every mB.

#### Scenario: Water rises in the window
- **WHEN** a pipe fills an empty head to half of its capacity
- **THEN** the windows show water up to half their height

#### Scenario: Small changes stay quiet
- **WHEN** the field uses a few mB and the level stays within the same step
- **THEN** nothing is sent to the clients for the water

### Requirement: Crop icon on top
The top of the Field Head SHALL show a dark inset plate with the field's crop item drawn flat on it. The plate SHALL stay empty when no crop is set or harvest only is on. Players near the head SHALL see a new crop on the plate as soon as it is set.

#### Scenario: Crop shown on top
- **WHEN** a player sets carrots as the field's crop
- **THEN** the head's top shows a carrot on the plate

#### Scenario: Harvest only clears the plate
- **WHEN** harvest only is turned on
- **THEN** the plate is empty

### Requirement: Look steps are pure logic
The mapping from planted count, ripe count, threshold and due state to a bar step, and from water amount and capacity to a window level, SHALL be pure Java without Minecraft imports and covered by JUnit tests. Any water above nothing SHALL show at least the lowest level, and only a full buffer SHALL show the top level.

#### Scenario: Water levels
- **WHEN** a buffer of 8000 mB holds 1 mB, 4000 mB or 8000 mB
- **THEN** the window level is the lowest step, half, and full

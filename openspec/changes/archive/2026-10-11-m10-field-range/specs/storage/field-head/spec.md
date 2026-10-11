## ADDED Requirements

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

## MODIFIED Requirements

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

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI that shows the water level and capacity, the field size in rails and plots and its radius, the planted and ripe plot counts, whether the field is hydrated and whether a harvest is due, or why the head serves no field. The GUI SHALL have one ghost slot for the crop of the field: clicking it with a seed or crop item that plants an age crop sets that crop without taking the item, clicking it with an empty hand clears it, and other items are refused. The GUI SHALL have the Range Upgrade slot of the field radius. A harvest only toggle SHALL set the field to harvest without planting; turning it on clears the crop and setting a crop turns it off. The crop and the mode SHALL be kept when the world is saved.

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

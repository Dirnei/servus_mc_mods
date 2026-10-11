## MODIFIED Requirements

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

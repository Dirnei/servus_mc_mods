## MODIFIED Requirements

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with three tabs on the left edge of its panel and the player inventory below the page of the selected tab. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have one ghost slot for the crop of the field: clicking it with a seed or crop item that plants an age crop sets that crop without taking the item, clicking it with an empty hand clears it, and other items are refused. A harvest only toggle on it SHALL set the field to harvest without planting; turning it on clears the crop and setting a crop turns it off. The crop and the mode SHALL be kept when the world is saved. The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the harvest threshold with its buttons, whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL show the water level and capacity and whether the field is hydrated or dry. It SHALL have the anti-corrosion upkeep slot and SHALL show the corrosion stage of the served field with the progress toward the next aging step, or, for a corroded field, that the rails are corroded and the field is not watered.

The **Upgrades** tab SHALL have the Range Upgrade slot of the field radius and SHALL show the radius and the field size in rails and plots.

Shift clicking an anti-corrosion item or a Range Upgrade in the player inventory SHALL move it into its slot whatever tab is open, and shift clicking it in its slot SHALL move it back to the inventory.

A tab SHALL carry a red marker while its page holds a problem, so it is seen from every tab: the Field tab while the head serves no field or the work status is a warning, the Water tab while the served field is dry or corroded.

#### Scenario: Opens on the Field tab
- **WHEN** a player opens a Field Head
- **THEN** the Field tab is selected and shows the crop slot, while the Range Upgrade and anti-corrosion slots are hidden

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
- **WHEN** a player opens the Upgrades tab of a head holding one Range Upgrade with `radius` 2
- **THEN** the tab shows radius 3

#### Scenario: Corroded field shown
- **WHEN** a player opens the GUI of a head whose field is corroded
- **THEN** the Water tab has a red marker and its page says the rails are corroded and the field is not watered

#### Scenario: Honeycomb by hand
- **WHEN** a player shift clicks honeycomb in the head's GUI while the Field tab is open
- **THEN** the honeycomb moves into the anti-corrosion slot

#### Scenario: Range Upgrade back to the inventory
- **WHEN** a player shift clicks the Range Upgrade in its slot on the Upgrades tab
- **THEN** it moves back to the player inventory

#### Scenario: Dry field marked
- **WHEN** a head serving a field runs out of water while the player looks at the Upgrades tab
- **THEN** the Water tab shows a red marker

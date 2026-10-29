## ADDED Requirements

### Requirement: Stamping a Fluid Rack
Right clicking any bay of a Fluid Rack with a named Zone Label SHALL copy the label's name and filter onto every bay of that bay's section, and sneak right clicking SHALL clear the zone of the whole section. A bay that joins a zoned section SHALL take the section's zone. The zone SHALL survive reloads and the carrot dismantle and place cycle, and an unnamed label SHALL only tell the player to name it.

#### Scenario: Stamp a section
- **WHEN** a player right clicks the right bay of a three bay Fluid Rack section with a label named "Cooling"
- **THEN** all three bays belong to zone "Cooling"

#### Scenario: Clear a section
- **WHEN** a player sneaks and right clicks a zoned Fluid Rack with any label
- **THEN** every bay of its section belongs to no zone

### Requirement: Fluid filters on Fluid Racks
On a Fluid Rack a zone's filter SHALL be the fluids of the filled fluid containers in its ghost slots, for example a water bucket meaning water. A zoned Fluid Rack whose filter lists fluids SHALL take only those fluids; one whose filter lists none, and an unzoned Fluid Rack, SHALL take any fluid. Item tags and ghost items that hold no fluid SHALL be ignored on Fluid Racks and SHALL keep their meaning on Heavy Duty Racks.

#### Scenario: Water only
- **WHEN** a Fluid Rack is zoned "Cooling" with a water bucket in a ghost slot and lava is offered to it
- **THEN** it takes none

#### Scenario: Name only zone takes anything
- **WHEN** a Fluid Rack is zoned "Back" with a filter of bread and the tag `c:ores`
- **THEN** it takes any fluid

#### Scenario: Same label on both racks
- **WHEN** a label named "Mixed" carries a water bucket and bread and stamps a Heavy Duty Rack and a Fluid Rack
- **THEN** the Heavy Duty Rack takes only water buckets and bread, and the Fluid Rack takes only water

### Requirement: Zone shown on the Fluid Rack
A zoned Fluid Rack section SHALL show its zone name once, small, on the top beam of its port bay, to every player who can see it. An unzoned section SHALL show nothing there.

#### Scenario: Looking at a zoned section
- **WHEN** a player looks at a three bay section in zone "Cooling"
- **THEN** "Cooling" is drawn on the top beam of the middle bay

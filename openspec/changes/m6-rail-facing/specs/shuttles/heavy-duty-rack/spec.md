# Spec Delta

## ADDED Requirements

### Requirement: Rack facing on placement
A rack SHALL take its facing on placement in this order: placed by clicking any face of a Heavy Duty Rack, it SHALL take that rack's facing; placed directly on top of a Heavy Duty Rack, it SHALL take the facing below; placed while sneaking, its front SHALL face the player; otherwise it SHALL turn its front to a rail by the shared rail facing rule (see `shuttles/rail-facing`). The wood of the other rack SHALL NOT matter.

#### Scenario: Placed against a rack's side
- **WHEN** a player clicks the east side of a rack facing north with another rack
- **THEN** the new rack stands east of it and faces north

#### Scenario: Placed against a rack's front
- **WHEN** a player clicks the front of a rack facing north with a rack, so the new one stands in front of it
- **THEN** the new rack faces north as well

#### Scenario: Placed from the aisle
- **WHEN** a player standing in the aisle looks north across a rail and places a rack on the floor beyond it, with no rack to copy
- **THEN** the rack's front faces south, towards the rail

### Requirement: Section layer on top
Clicking the top face of a rack with a Heavy Duty Rack item, without sneaking, SHALL place a rack on top of every bay of the clicked rack's section when every spot above the section is free and the held stack holds a rack item for every bay (always in creative), using one item per bay. The new racks SHALL take the facing and the section boundaries of the section below. Otherwise one rack SHALL be placed as usual.

#### Scenario: Layer over a section of three
- **WHEN** a player holding 10 racks clicks the top of the middle bay of a three bay section
- **THEN** three racks stand on top of the section, form one section of three with the same facing, and the player holds 7 racks

#### Scenario: Not enough racks
- **WHEN** a player holding 2 racks clicks the top of a three bay section
- **THEN** one rack is placed on the clicked bay

#### Scenario: A spot is taken
- **WHEN** a block stands above one bay of the section
- **THEN** one rack is placed on the clicked bay

#### Scenario: Only the clicked section
- **WHEN** a row has two sections of three side by side and the player clicks the top of the left one
- **THEN** only the left section gets a layer

### Requirement: Sneak extends a rack row
Sneaking while looking at a Heavy Duty Rack with a Heavy Duty Rack item SHALL place the rack at the first free spot past the end of the clicked rack's row of racks with the same facing, through the Foundation placement assist with its ghost, arrow and reach. It SHALL take the row's facing; a row that ends at a block that cannot be replaced or runs past the reach SHALL place nothing.

#### Scenario: Walk towards a clicked side
- **WHEN** a player sneaks and looks at the exposed west side face of the rack at the west end of a row facing north
- **THEN** the walk goes west and the rack lands right beside it, facing north

#### Scenario: Extend to the right
- **WHEN** a player sneaks and looks at the right half of the front of the first rack in a row of four facing north
- **THEN** the walk goes towards the half looked at, the ghost shows a rack past the fourth rack, and the click places it there facing north

#### Scenario: Row ends at a wall
- **WHEN** the spot past the end of the row is stone
- **THEN** no ghost shows and the click places nothing

#### Scenario: Unsneaked click
- **WHEN** a player clicks the side of a rack without sneaking
- **THEN** the rack is placed against the clicked face as usual

### Requirement: Sneak stacks a rack column
Sneaking while looking at the top or bottom of a Heavy Duty Rack or Fluid Rack, or near the top or bottom edge of its front or back, with a rack item of the same kind SHALL place the rack at the first free spot above or below the column of racks of that kind with the same facing, through the Foundation placement assist with its ghost, an up or down arrow and reach, taking the column's facing.

#### Scenario: Stack up from the front
- **WHEN** a player sneaks and looks near the top edge of the front of a rack in a row facing north
- **THEN** the ghost and an up arrow show a rack on top of it, and the click places it there facing north

#### Scenario: Stack up a column
- **WHEN** a player sneaks and looks at the top of the lower rack of a column of two facing north
- **THEN** the ghost and an up arrow show a rack on top of the column, and the click places it there facing north

### Requirement: Sneak up lays a layer
A sneak placement that walks up a column SHALL place a whole layer over the section of the column's top rack, under the same conditions as the top click layer (every spot free, an item per bay or creative); otherwise it SHALL place the one rack on the column.

#### Scenario: Layer on top of a wall
- **WHEN** a player holding ten racks sneaks and looks near the top edge of the front of a bay of a section of three, twice
- **THEN** a layer of three goes on top each time, rows two and three, and six racks are used

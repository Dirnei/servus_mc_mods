## MODIFIED Requirements

### Requirement: Shuttle size
A shuttle SHALL be drawn a little below block scale (example 0.8, so a body of about 10 pixels), bigger than the first carts, with its wheels on the rail top, and SHALL stay inside the block of the rail it stands on, so it does not reach into a rack or a station beside the rail. Only a tool MAY reach past the body to the sides. A shuttle standing on a lift car SHALL fit on the car. Its wheels SHALL stand on the two rail heads, on a rail and on a lift car.

#### Scenario: Passing a rack
- **WHEN** a Mk3 shuttle drives along the rail in front of a rack
- **THEN** no part of the shuttle is drawn inside the rack's block

#### Scenario: Wheels on the heads
- **WHEN** a shuttle drives along a straight rail or rides a lift car
- **THEN** its wheels touch the tops of the rail heads and do not sink into them

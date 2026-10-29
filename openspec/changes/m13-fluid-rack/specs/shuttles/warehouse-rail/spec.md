## MODIFIED Requirements

### Requirement: Free rails bridge short gaps
Free rails that link to each other SHALL form a gap. The span of a gap SHALL be its number of free rails plus the number of hung rails linked to them. A gap SHALL hold when its span is at most the maximum span (for example 3, set in the warehouse datapack config) and it links to at least two hung rails. A gap of a single free rail SHALL also hold when it links to one hung rail, an overhang of one. A gap that does not hold SHALL collapse: its free rails and the hung rails linked to them drop as items. A gap SHALL be checked again when a rail is placed into it or beside it, when a rail, anchor or floor block it relies on is removed, and when one of its rails loses its floor. Gaps SHALL NOT be checked every tick.

#### Scenario: Bridge between two elevators
- **WHEN** two elevator shafts stand with a three block gap between them and the player fills it with three rails, one at each shaft first and the middle one last
- **THEN** all three rails stay and the shafts are linked through them

#### Scenario: Bridge between two racks
- **WHEN** two Heavy Duty Racks face each other in the air across a three block gap and the player fills it with three rails, ends first
- **THEN** all three rails stay

#### Scenario: Fourth rail collapses the bridge
- **WHEN** a three rail bridge spans two racks and the player places a fourth rail beside its middle rail
- **THEN** all four rails drop as items

#### Scenario: Four block gap cannot be bridged
- **WHEN** two racks face each other across a four block gap with a rail hung on each front, and the player places a rail beside one of them and then a rail beside the other
- **THEN** the first rail stays as an overhang, and the second closes a gap of span four, so all four rails drop as items

#### Scenario: Overhang of one
- **WHEN** a rail hangs on a rack front and the player places a free rail beside it that touches nothing else
- **THEN** both rails stay

#### Scenario: Overhang collapses
- **WHEN** a rail hangs on a rack front with one free rail beyond it, and the player places a second free rail beyond that
- **THEN** both free rails and the hung rail drop as items

#### Scenario: Bridge between floors
- **WHEN** two standing rails end at a drop three blocks wide and the player hangs a rail off each and then fills the middle
- **THEN** the three rails stay

#### Scenario: One rail off a floor
- **WHEN** a standing rail ends at a drop and the player places one rail beyond it
- **THEN** the rail stays, hung off the standing rail

#### Scenario: Rack row to elevator
- **WHEN** a row of rails hangs along an upper rack row and one free rail links its last rail to a rail hung on an elevator segment
- **THEN** every rail stays

#### Scenario: Anchor removed
- **WHEN** a three rail bridge spans two racks and one rack is broken
- **THEN** all three rails drop as items

#### Scenario: Floor removed under a bridge end
- **WHEN** a rail hung on a rack front links through a free rail to a rail hung off a standing rail, and the block under the standing rail is broken
- **THEN** all four rails drop as items

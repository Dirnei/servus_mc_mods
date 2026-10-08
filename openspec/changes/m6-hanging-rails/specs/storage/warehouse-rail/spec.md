## ADDED Requirements

### Requirement: Rail stands or hangs
The Warehouse Rail SHALL be a flat block in the bottom of its block space, one block wide, with a selection shape a few pixels high so players walk over it. A rail with a block with a solid top under it SHALL be standing. A rail without one SHALL be hung when one of its horizontal neighbours is an anchor and free otherwise. An anchor SHALL be the front side of a Heavy Duty Rack, any side of a Rail Elevator segment or a standing rail; crates, controllers, terminals, stations and garages SHALL NOT be anchors. Standing and hung rails SHALL hold on their own. A rail SHALL be placeable on any block with a solid top, and in open air only when one of its horizontal neighbours is an anchor or another rail. Hung and free rails SHALL look the same as standing rails.

#### Scenario: Walk over
- **WHEN** a player walks along an aisle paved with rails
- **THEN** the player is not blocked and does not climb

#### Scenario: Placement on a floor
- **WHEN** a player places a rail on a stone floor
- **THEN** the rail is placed with its flat side on the floor

#### Scenario: Hung on a rack front
- **WHEN** a player clicks the front of a Heavy Duty Rack on the second row of a stack, with air below the space in front of it
- **THEN** a rail is placed in front of the rack and stays there

#### Scenario: Row along an upper rack row
- **WHEN** five Heavy Duty Racks stand in a row on the second level and the player hangs a rail in front of each
- **THEN** all five rails stay and link to each other

#### Scenario: Open air
- **WHEN** a player tries to place a rail in the air with no rail, rack front or elevator segment beside it
- **THEN** no rail is placed

#### Scenario: Rack side is no anchor
- **WHEN** a player tries to place a rail in the air beside the side of a Heavy Duty Rack with nothing else beside it
- **THEN** no rail is placed

#### Scenario: Crate is no anchor
- **WHEN** a player tries to place a rail in the air beside a crate with nothing else beside it
- **THEN** no rail is placed

### Requirement: Free rails bridge short gaps
Free rails that link to each other SHALL form a gap. The span of a gap SHALL be its number of free rails plus the number of hung rails linked to them. A gap SHALL hold when its span is at most the maximum span (for example 3, set in the warehouse datapack config) and it links to at least two hung rails. A gap that does not hold SHALL collapse: its free rails and the hung rails linked to them drop as items. A gap SHALL be checked again when a rail is placed into it or beside it, when a rail, anchor or floor block it relies on is removed, and when one of its rails loses its floor. Gaps SHALL NOT be checked every tick.

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
- **WHEN** two racks face each other across a four block gap with a rail hung on each front and the player places a rail beside one of them
- **THEN** that rail and the hung rail it touches drop as items

#### Scenario: Overhang collapses
- **WHEN** a rail hangs on a rack front and the player places a free rail beside it that touches nothing else
- **THEN** both rails drop as items

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

## MODIFIED Requirements

### Requirement: Rail is obtainable and dismantled
The rail SHALL be craftable from vanilla materials in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot. When the block under a rail is removed the rail SHALL become hung or free and SHALL drop itself only when it collapses with its gap.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and a stick in the rail pattern (for example two rows of three ingots around a stick)
- **THEN** the result is several rails (for example eight)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed
- **WHEN** the block under a rail is broken and the rail touches no anchor and no other rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed beside a rack
- **WHEN** the block under a rail in front of a Heavy Duty Rack is broken and no other rail touches it
- **THEN** the rail stays, hung on the rack front

## REMOVED Requirements

### Requirement: Floor rail block
**Reason**: Rails may now hang on rack fronts and elevator segments and bridge short gaps; the requirement is replaced by "Rail stands or hangs" and "Free rails bridge short gaps".
**Migration**: None needed; existing floor rails are standing rails and behave as before.

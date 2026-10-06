# Spec Delta

## ADDED Requirements

### Requirement: Sides climb like a ladder
A player touching an exposed side of a rack (a face at a right angle to the rack's facing) SHALL climb it like a vanilla ladder: pushing towards the side moves the player up, hanging against it without input slides the player down slowly, sneaking against it holds the position, and the player takes no fall damage from a fall that ends against a side. The front and the back of a rack SHALL NOT be climbable. Only players climb; other entities treat the rack as a full block.

#### Scenario: Climb a stack
- **WHEN** a player standing beside the side posts of three stacked racks walks into them and keeps pushing
- **THEN** the player rises along the posts and steps onto the top rack

#### Scenario: Front and back stay walls
- **WHEN** a player pushes against the front or the back of a rack
- **THEN** the player stays on the ground and does not rise

#### Scenario: Slide down
- **WHEN** a player steps off the top of a stack, stays in contact with its side and gives no movement input
- **THEN** the player slides down slowly and lands without fall damage

#### Scenario: Hold position
- **WHEN** a player climbing a side stops pushing and sneaks
- **THEN** the player stays at the current height

#### Scenario: Only the outer posts of a section
- **WHEN** a row of three bays is joined into one section
- **THEN** the two outer sides of the section are climbable and the inner joins, being covered by the neighbouring bays, offer nothing to climb

#### Scenario: Mob at a rack
- **WHEN** a zombie walks against the side of a rack
- **THEN** the zombie stays on the ground

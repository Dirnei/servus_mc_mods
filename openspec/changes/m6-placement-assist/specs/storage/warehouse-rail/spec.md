# Spec Delta

## ADDED Requirements

### Requirement: Sneak placement extends a rail run
The rail item SHALL use the placement assist. Its run SHALL be the rails in a straight line from the clicked rail. On a hit on the top or bottom face, the direction SHALL be the horizontal side whose edge is nearest to the hit point. On a hit on a side face, it SHALL be that face. The placed rail SHALL follow the normal stand, hang and gap rules, with no special handling for rails placed this way.

#### Scenario: Quick straight on a floor
- **WHEN** four rails lie on a floor from west to east and a sneaking player clicks the top of the first rail near its east edge while holding a rail
- **THEN** a rail is placed east of the fourth rail

#### Scenario: Direction from the nearest edge
- **WHEN** a sneaking player clicks the top of a lone rail near its north edge
- **THEN** the ghost and the placed rail are north of it

#### Scenario: Side face hit
- **WHEN** a sneaking player clicks the south side face of a rail
- **THEN** the rail is placed past the run to the south

#### Scenario: Run passes a junction
- **WHEN** a straight line of rails eastwards has a branch to the north at its second rail and a sneaking player clicks the first rail towards the east
- **THEN** the walk passes the junction and the rail is placed past the last rail of the line

#### Scenario: Bridge by extending both ends
- **WHEN** two Heavy Duty Racks face each other across a three block gap, a rail hangs on each front, and a sneaking player clicks one of them towards the other
- **THEN** the rail is placed in the middle of the gap and all three rails stay

#### Scenario: Overhang still collapses
- **WHEN** a rail hangs on a rack front with nothing else around and a sneaking player clicks it away from the rack
- **THEN** a rail is placed beyond it and both rails drop as items, as they would when placed by hand

#### Scenario: Normal click still turns
- **WHEN** a player who is not sneaking clicks the north side face of a rail while holding a rail
- **THEN** a rail is placed against that face as before

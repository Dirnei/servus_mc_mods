# Spec Delta

## MODIFIED Requirements

### Requirement: Rack is a storage block
A rack SHALL be a stock holding node of the warehouse network: its contents are indexed, it joins through touching storage blocks on any side and through rails on its front side only, and rails dock onto its front only. A rack SHALL be picked from and filled by the warehouse only through a shuttle at its front rail, never instantly. A rack SHALL NOT carry a connection lamp.

#### Scenario: Rack beside a rail
- **WHEN** a rack is placed with a rail of an online warehouse in front of it
- **THEN** the rail shows a dock towards it and the terminal lists its contents

#### Scenario: Rail at the side
- **WHEN** a rail of an online warehouse runs along the side or the back of a rack and nothing else touches it
- **THEN** the rail shows no dock, and the rack's contents are not listed

#### Scenario: Rack without a shuttle
- **WHEN** a rack touches the controller directly, holds bread, and the warehouse has no garage
- **THEN** the terminal lists the bread but a request for it is refused as needing a shuttle

#### Scenario: Rack by shuttle
- **WHEN** the same rack stands with its front on a rail and a garage with one shuttle is on that rail
- **THEN** a request for bread sends the shuttle to the rack's front and the bread lands in the terminal's output slots

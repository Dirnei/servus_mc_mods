## ADDED Requirements

### Requirement: Floors join through a shaft
Every elevator segment SHALL be a node of the warehouse network. Segments SHALL link to the segments directly above and below them and to rails beside them, so rails on different floors linked by a shaft form one warehouse. A storage block touching a segment SHALL NOT join the warehouse through that segment; only rails carry membership.

#### Scenario: Two floors one controller
- **WHEN** a rack faces a rail on the ground floor, the rail meets a shaft, and three segments up another rail leads to a controller, and a terminal reaches the warehouse over the data line
- **THEN** the warehouse is online and the terminal lists the rack's contents

#### Scenario: Broken segment
- **WHEN** the middle segment of that shaft is broken
- **THEN** the ground floor leaves the controller's network and the rack's items leave the index

#### Scenario: Rack against the shaft
- **WHEN** a rack touches a segment of an online warehouse and no rail stands in front of it
- **THEN** the rack is not part of the warehouse and its items are not listed

## REMOVED Requirements

### Requirement: Rail levels join through a shaft
**Reason**: The requirement named crates and direct contact between storage blocks, which no longer carry membership.
**Migration**: See "Floors join through a shaft".

## MODIFIED Requirements

### Requirement: Floors join through a shaft
Every elevator segment SHALL be a node of the warehouse network. Segments SHALL link to the segments directly above and below them, to rails beside them and to a Warehouse Controller beside them, so rails on different floors linked by a shaft form one warehouse and a controller touching a shaft from the side belongs to it. A segment SHALL NOT link to a controller directly above or below it. A storage block, Fluid Rack, station, fluid station or garage touching a segment SHALL NOT join the warehouse through that segment; only rails carry their membership. A controller beside a segment SHALL NOT change how the segment looks, and shuttles SHALL NOT drive into the controller.

#### Scenario: Two floors one controller
- **WHEN** a rack faces a rail on the ground floor, the rail meets a shaft, and three segments up another rail leads to a controller, and a terminal reaches the warehouse over the data line
- **THEN** the warehouse is online and the terminal lists the rack's contents

#### Scenario: Broken segment
- **WHEN** the middle segment of that shaft is broken
- **THEN** the ground floor leaves the controller's network and the rack's items leave the index

#### Scenario: Rack against the shaft
- **WHEN** a rack touches a segment of an online warehouse and no rail stands in front of it
- **THEN** the rack is not part of the warehouse and its items are not listed

#### Scenario: Controller against the shaft
- **WHEN** a controller touches the side of a segment, no rail touches the controller, and a rack faces a rail that meets the same shaft on another floor
- **THEN** the warehouse is online and the rack's items are indexed

#### Scenario: Controller on top of the shaft
- **WHEN** a controller sits directly on the top segment of a shaft and touches nothing else
- **THEN** the controller is not part of the shaft's network

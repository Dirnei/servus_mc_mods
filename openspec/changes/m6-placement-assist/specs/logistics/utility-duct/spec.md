# Spec Delta

## MODIFIED Requirements

### Requirement: Channels are placed as items
An Item Pipe, Power Cable or Fluid Pipe item SHALL place a Utility Duct carrying that line, or add the line to a clicked duct that lacks it. Clicking a duct that already carries the line SHALL place a new duct against the clicked face like any block. Sneaking SHALL use the placement assist: the direction is the clicked face, and the run is the ducts that carry the held line. When the spot beside the clicked duct is free, sneaking places there as before. A duct SHALL drop its line items when broken or dismantled, and removing the last line SHALL remove the duct.

#### Scenario: Add a second channel
- **WHEN** a player uses a Power Cable on a duct that carries only an Item Pipe
- **THEN** the duct carries both and one Power Cable is consumed

#### Scenario: Add the fluid line
- **WHEN** a player uses a Fluid Pipe on a duct that carries an Item Pipe and a Power Cable
- **THEN** the duct carries all three lines and one Fluid Pipe is consumed

#### Scenario: Extend a run
- **WHEN** a player uses an Item Pipe on the east face of a duct that already carries an Item Pipe
- **THEN** a new Item Pipe duct is placed east of it

#### Scenario: Sneak places
- **WHEN** a sneaking player uses a Fluid Pipe on the top of a duct that lacks the Fluid Pipe
- **THEN** a new Fluid Pipe duct is placed above it and the clicked duct stays unchanged

#### Scenario: Sneak extends a run
- **WHEN** six ducts carrying an Item Pipe stand in a line from west to east and a sneaking player uses an Item Pipe on the east face of the second one
- **THEN** a new Item Pipe duct is placed east of the sixth duct

#### Scenario: Sneak extends upwards
- **WHEN** a column of four Power Cable ducts rises from the floor and a sneaking player uses a Power Cable on the top face of the lowest duct
- **THEN** a new Power Cable duct is placed on top of the column

#### Scenario: Run of another line stops the walk
- **WHEN** a sneaking player uses a Fluid Pipe on the east face of a Fluid Pipe duct whose east neighbour is a duct carrying only an Item Pipe
- **THEN** nothing is placed

#### Scenario: Broken duct
- **WHEN** a duct with all three lines is broken
- **THEN** one Item Pipe, one Power Cable and one Fluid Pipe drop

#### Scenario: Old item ids
- **WHEN** a command refers to `servus_logistics:item_channel` or `servus_logistics:energy_channel`
- **THEN** no such item exists; the items are `servus_logistics:item_pipe`, `servus_logistics:power_cable` and `servus_logistics:fluid_pipe`

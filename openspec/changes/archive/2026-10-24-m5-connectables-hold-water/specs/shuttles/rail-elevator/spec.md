# Spec Delta

## ADDED Requirements

### Requirement: Segments hold back fluids
An elevator shaft segment SHALL stop flowing water and lava like a solid wall: the fluid SHALL neither flow into the segment's block nor wash the segment away. A bucket used on a segment SHALL place its fluid in the block in front of the clicked face, as on stone. A segment SHALL never be waterlogged.

#### Scenario: Water flows down a shaft
- **WHEN** water flows onto the top segment of a shaft
- **THEN** every segment stays and the water does not enter the shaft

### Requirement: Segments are not placed into fluid sources
Placing a shaft segment into a block holding a water or lava source SHALL fail without a message, and the source SHALL stay. Placing a segment into flowing water or lava SHALL succeed, and the segment SHALL then dam the flow.

#### Scenario: Segment into a water source
- **WHEN** a player places a shaft segment into a water source block
- **THEN** no segment is placed and the water source stays

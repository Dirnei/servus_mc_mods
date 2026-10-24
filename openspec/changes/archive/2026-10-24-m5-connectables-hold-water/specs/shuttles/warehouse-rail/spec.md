# Spec Delta

## ADDED Requirements

### Requirement: Rails hold back fluids
A Warehouse Rail, and with it a Farm Rail, SHALL stop flowing water and lava like a solid wall: the fluid SHALL neither flow into the rail's block nor wash the rail away. A bucket used on a rail SHALL place its fluid in the block in front of the clicked face, as on stone. A rail SHALL never be waterlogged.

#### Scenario: Water flows against a rail
- **WHEN** water flows toward a rail that is part of a warehouse
- **THEN** the rail stays, no item drops and the warehouse keeps the rail

#### Scenario: Water flows against a farm rail
- **WHEN** water from a field's irrigation flows toward a farm rail
- **THEN** the farm rail stays and the water does not enter its block

### Requirement: Rails are not placed into fluid sources
Placing a rail into a block holding a water or lava source SHALL fail without a message, and the source SHALL stay. Placing a rail into flowing water or lava SHALL succeed, and the rail SHALL then dam the flow.

#### Scenario: Rail into a water source
- **WHEN** a player places a rail into a water source block
- **THEN** no rail is placed and the water source stays

#### Scenario: Rail into flowing water
- **WHEN** a player places a rail into flowing water
- **THEN** the rail is placed and the water stops at it

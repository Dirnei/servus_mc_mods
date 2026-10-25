## MODIFIED Requirements

### Requirement: Fluid room in Storage gantries
A warehouse SHALL count its fluid room as the mB in the tanks of its Storage gantries and in its Fluid Rack sections against their capacity, each gantry tank's capacity as its Capacity Upgrades make it and each section's capacity as its bays make it. Import and Export gantries SHALL NOT count.

#### Scenario: Two Storage gantries
- **WHEN** a warehouse has two Storage gantries with tanks of 16,000 mB each and holds 24,000 mB of water
- **THEN** its fluid fill is 24,000 of 64,000 mB

#### Scenario: Import gantry left out
- **WHEN** an Import gantry of the warehouse holds 8,000 mB of lava
- **THEN** the fluid fill and capacity do not change

#### Scenario: Fluid Rack counts
- **WHEN** a warehouse has no gantry and one docked three bay Fluid Rack section of 16,000 mB per bay holding 12,000 mB of water
- **THEN** its fluid fill is 12,000 of 48,000 mB

# Spec Delta

## MODIFIED Requirements

### Requirement: Fluid room in Fluid Racks
A warehouse SHALL count its fluid room as the mB in its Fluid Rack shelves against their capacity, each shelf's capacity as its bays make it. Fluid Inbounds and Fluid Outbounds SHALL NOT count.

#### Scenario: Fluid Rack counts
- **WHEN** a warehouse has one docked three bay Fluid Rack shelf of 16,000 mB per bay holding 12,000 mB of water
- **THEN** its fluid fill is 12,000 of 48,000 mB

#### Scenario: Stations left out
- **WHEN** a Fluid Inbound of the warehouse holds 8,000 mB of lava
- **THEN** the fluid fill and capacity do not change

# shuttles/storage-fill Specification

## Purpose
TBD - created by archiving change m8-storage-fill. Update Purpose after archive.

## Requirements

### Requirement: Item room in rack slots
A warehouse SHALL count its item room in Heavy Duty Rack slots: the slots of all its rack bays as the total and the slots in use as the fill. A slot SHALL count as in use when it holds items or a job has locked it. Slots of Inbound, Outbound and Packing Station buffers, terminal output slots and CPU buffers SHALL NOT count. A rack bay SHALL count with its slots as last read by the warehouse's stock poll, so a bay whose chunk is not loaded keeps its last count until the warehouse drops it from its stock.

#### Scenario: Partly full
- **WHEN** a warehouse has three rack bays of 27 slots and 30 slots hold items
- **THEN** its item fill is 30 of 81 slots

#### Scenario: Reserved room counts as used
- **WHEN** a put-away has locked two empty slots of a bay for its cargo
- **THEN** both slots count as in use before the shuttle arrives

#### Scenario: Station buffers do not count
- **WHEN** an Inbound Station of the warehouse holds 9 stacks in its buffer
- **THEN** the item fill and total are the same as without the station

### Requirement: Fluid room in Storage gantries
A warehouse SHALL count its fluid room as the mB in the tanks of its Storage gantries against their capacity, each tank's capacity as its Capacity Upgrades make it. Import and Export gantries SHALL NOT count.

#### Scenario: Two Storage gantries
- **WHEN** a warehouse has two Storage gantries with tanks of 16,000 mB each and holds 24,000 mB of water
- **THEN** its fluid fill is 24,000 of 64,000 mB

#### Scenario: Import gantry left out
- **WHEN** an Import gantry of the warehouse holds 8,000 mB of lava
- **THEN** the fluid fill and capacity do not change

### Requirement: Fill per zone
The item fill SHALL break down per zone: one row per zone name the warehouse's racks carry, with the slots in use and the total of the racks of that zone, and one "(no zone)" row for racks without a zone. The rows SHALL be ordered full first, then by the share in use from the fullest down, then by name, the "(no zone)" row after zones of the same share. A zone counts as full when all its slots are in use. Zone rows SHALL add up to the warehouse's item fill.

#### Scenario: Full zone first
- **WHEN** the racks of zone "Ores" have 81 of 81 slots in use, zone "Food" 30 of 81 and the unzoned racks 300 of 567
- **THEN** the rows are Ores (full), (no zone) 300 of 567, Food 30 of 81

#### Scenario: No zones
- **WHEN** no rack of the warehouse carries a zone
- **THEN** there is one row, (no zone), with the warehouse's whole item fill

### Requirement: Fill counting is pure
Counting the slots in use, adding up bays and tanks, and ordering the zone rows SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover in use counting with locks, zone rows and their order, and the fluid sum

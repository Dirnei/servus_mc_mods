# Spec Delta

## MODIFIED Requirements

### Requirement: Steps run over the data line
The producers of a CPU SHALL be the members of its data line with the producer role. A step SHALL run on the producer whose reported recipe it uses, or, for a step planned from a pattern, on a producer of the line that can execute that pattern; when it plans, a CPU SHALL prefer producers no step occupies. Before the inputs of a pattern step on a Crafting Station are shipped, the CPU SHALL hand the station the step's pattern and batches as its job; a station that refuses keeps the step waiting for the station. Its inputs SHALL be shipped by the warehouses of the line through an Access Point as packets into the producer, the warehouse holding the most of an input first. While the step delivers or produces, outputs of its recipe in the producer SHALL be moved as packets to an Access Point of the line, where they are put away; what arrives there SHALL count for the step. A step whose shipment ends short SHALL ship the rest again. When a step is done or its order is cancelled, the CPU SHALL end the station's job. Each producer SHALL run one step at a time over all CPUs of the line, orders in creation order.

#### Scenario: Sticks over the line
- **WHEN** a CPU runs a sticks step for 2 batches on a Crafting Station on the line, and the warehouse has planks and an Access Point on the line
- **THEN** the station gets a sticks job for 2 crafts, 4 planks arrive in the station as packets, it crafts twice, the 8 sticks travel back to the Access Point and into storage, the step is done and the station is idle again

#### Scenario: Producer busy
- **WHEN** two CPUs each have an order that needs the same Crafting Station
- **THEN** the older order's step runs first and the other waits for the station

#### Scenario: Two stations in parallel
- **WHEN** a line has two idle Crafting Stations and a sticks pattern, and two orders for sticks are made one after the other
- **THEN** each order's step runs on a different station at the same time

#### Scenario: Step from a pattern
- **WHEN** a CPU plans sticks from a sticks pattern of the line and a Crafting Station can execute it
- **THEN** the step runs on that station and counts the sticks that come back like any step

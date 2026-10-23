## ADDED Requirements

### Requirement: Aisle traffic costs time
A step of a way SHALL cost more for the shuttles on it, read when the way is planned: for every other shuttle on that step with the same heading the traffic weight per shuttle (`traffic_per_shuttle` in `travel_costs` of `data/<ns>/shuttles/warehouse.json`, example 4), and for a shuttle standing at the end of that step with the same heading also the ticks left of its stop. A shuttle SHALL NOT count itself. Shuttles inside their garage and shuttles riding a lift car SHALL NOT count.

#### Scenario: Busy aisle
- **WHEN** two aisles of equal length lead to the same rail and three shuttles drive along the first in the same direction
- **THEN** a shuttle planned now takes the second aisle

#### Scenario: Oncoming traffic is free
- **WHEN** three shuttles drive along an aisle the other way
- **THEN** the aisle costs a planned shuttle no more than an empty one

# Spec Delta

## ADDED Requirements

### Requirement: Bandwidth along the path
A delivery SHALL move per tick no more than its gates allow and no more than the remaining budget of every duct on its path in its direction. Deliveries that cross the same duct in the same tick SHALL share that duct's budget in creation order. A delivery SHALL take the path with the most bandwidth among the shortest paths when several exist.

#### Scenario: Slowest duct decides
- **WHEN** a delivery leaves a Packing Station of rate 64 over two Fast Data Cable ducts of 128 and one Data Cable duct of 32
- **THEN** it moves about 32 items per second

#### Scenario: Shared duct
- **WHEN** two deliveries in the same direction cross the same duplex duct of bandwidth 32 and nothing else limits them
- **THEN** together they move about 32 items per second through it

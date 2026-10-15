# Spec Delta

## MODIFIED Requirements

### Requirement: One lift car per shaft
Every shaft SHALL have exactly one lift car. The car SHALL be drawn as a platform at rail height with a sleeve around each corner post, and SHALL move only along its shaft at `lift_speed` (in `data/<ns>/shuttles/warehouse.json`, for example 0.1 blocks per tick). The car SHALL stay at the last floor it served and SHALL start at the lowest segment when the shaft is built or the world is loaded.

#### Scenario: New shaft
- **WHEN** a player builds a shaft of four segments
- **THEN** the car stands in the lowest segment

#### Scenario: Ride time
- **WHEN** the car moves three segments up at 0.1 blocks per tick
- **THEN** it reaches the target floor after about 30 ticks

#### Scenario: Parks where it stopped
- **WHEN** the car has carried a shuttle to the top floor and no shuttle calls it
- **THEN** it stays at the top floor

#### Scenario: Shaft grows
- **WHEN** a segment is added on top of a shaft
- **THEN** the shaft still has one car, at the floor where it was

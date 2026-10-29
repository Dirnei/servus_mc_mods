## MODIFIED Requirements

### Requirement: A standing shuttle holds the shuttles behind it
A shuttle that stands on a rail, for its stop at a bay, station, fluid station or fluid port, for field work at a plot, for the lift car, for a deck or for power, SHALL hold every shuttle that would share its lane there: they stop the follow gap behind it and drive on once it has left. Shuttles that pass it with an opposite heading or across its lane SHALL NOT wait.

#### Scenario: Waiting behind a loading shuttle
- **WHEN** a shuttle stands at a bay for its stop and another shuttle comes along the same aisle in the same direction
- **THEN** the second stops one block behind it and drives on after the first has left

#### Scenario: Passing a loading shuttle the other way
- **WHEN** a shuttle stands at a bay for its stop, heading east, and another shuttle drives west along that aisle
- **THEN** the second drives past without stopping

#### Scenario: Waiting behind a pumping tanker
- **WHEN** a Tanker shuttle stands at a Fluid Rack's port with its coupling out and another shuttle comes along the same lane in the same direction
- **THEN** the second stops behind it and drives on after the tanker has left

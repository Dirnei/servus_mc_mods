## ADDED Requirements

### Requirement: Keep level on Export tanks
Each tank of an Export gantry SHALL have a keep level, 0 by default, set in the GUI under its gauge with buttons for -1 B, +1 B, -10 B and +10 B and limited to the tank's capacity. A keep level SHALL only apply while the tank is locked to a fluid; on Any the GUI SHALL show the keep level greyed out with a hint to lock the tank. Keep levels SHALL be kept with the gantry and on its dropped item. Storage and Import gantries SHALL NOT show keep levels.

#### Scenario: Set a keep level
- **WHEN** a player sets the left tank of an Export gantry locked to water to 8 B
- **THEN** the left gauge shows a keep mark at 8000 mB

#### Scenario: Keep needs a lock
- **WHEN** the right tank of an Export gantry is on Any
- **THEN** its keep level is greyed out and the GUI says to lock the tank

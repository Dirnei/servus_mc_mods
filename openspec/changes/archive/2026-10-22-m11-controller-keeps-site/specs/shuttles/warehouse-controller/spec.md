# Spec Delta

## MODIFIED Requirements

### Requirement: Controller is a named site
Every Warehouse Controller SHALL be a site with a name of at most 32 characters, set in its GUI. A controller without a name SHALL be shown as "Warehouse at <x>, <z>" with its block position. The name SHALL be kept when the world is saved and SHALL travel in the dropped item when the controller is mined or dismantled with the Configuration Carrot. The controller SHALL remember the team of the player who placed it.

#### Scenario: Unnamed
- **WHEN** a controller is placed at 120, 64, -40 and nobody names it
- **THEN** its GUI title reads "Warehouse at 120, -40"

#### Scenario: Named
- **WHEN** the player types "North Mine" into the name field and presses Enter
- **THEN** the GUI title reads "North Mine" and the name is still there after a reload

#### Scenario: Carrot dismantle
- **WHEN** a controller named "North Mine" is dismantled with the carrot and placed again
- **THEN** it is still named "North Mine"

#### Scenario: Mined
- **WHEN** a controller named "North Mine" is mined with a pickaxe and its item placed again
- **THEN** it is still named "North Mine"

### Requirement: Controller FE buffer
The controller SHALL have an FE buffer of `controller_energy_capacity` (example 100,000, `shuttles/power.json`) that takes FE from every side through the standard energy capability, for example from the Utility Duct power line, and never gives FE out. The FE stored SHALL be saved with the controller and SHALL travel in the dropped item when the controller is mined or dismantled with the Configuration Carrot; a placed controller SHALL start with the FE its item carries, at most its capacity. The controller GUI SHALL show the FE stored and the capacity.

#### Scenario: Power line
- **WHEN** a Utility Duct with the power line fed by a Coal Generator touches the controller
- **THEN** the duct shows a power arm toward the controller and its FE stored rises

#### Scenario: No FE out
- **WHEN** a machine next to the controller tries to pull FE from it
- **THEN** it gets none

#### Scenario: Mined with FE
- **WHEN** a controller holding 12,000 FE is mined and its item placed again
- **THEN** the placed controller holds 12,000 FE

#### Scenario: Dismantled with FE
- **WHEN** a controller holding 12,000 FE is dismantled with the carrot and placed again
- **THEN** the placed controller holds 12,000 FE

#### Scenario: More than the capacity
- **WHEN** a controller item carries 150,000 FE and the capacity is 100,000
- **THEN** the placed controller holds 100,000 FE

#### Scenario: New controller
- **WHEN** a freshly crafted controller is placed
- **THEN** it holds no FE

## ADDED Requirements

### Requirement: Controller item tooltip
A Warehouse Controller item that carries a site name SHALL show the name in its tooltip, and one that carries FE SHALL show the FE it holds. An item that carries neither SHALL show neither line.

#### Scenario: Named item with FE
- **WHEN** a player hovers a controller item that was mined from "North Mine" holding 12,000 FE
- **THEN** the tooltip shows "North Mine" and 12,000 FE

#### Scenario: Fresh item
- **WHEN** a player hovers a freshly crafted controller item
- **THEN** the tooltip shows no site name and no FE line

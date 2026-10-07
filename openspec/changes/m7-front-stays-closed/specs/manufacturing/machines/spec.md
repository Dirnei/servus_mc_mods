# Spec Delta

## MODIFIED Requirements

### Requirement: Inventory sides
A machine SHALL expose its inventory through the item handler capability on every side but the front, which is fixed to Off and exposes nothing. Input and fuel slots accept insertion only, output slots allow extraction only, and automation SHALL never extract inputs or insert into outputs.

#### Scenario: Hopper feeds fuel
- **WHEN** a hopper above a burner machine holds coal
- **THEN** the coal moves into the machine's fuel slot

#### Scenario: Hopper pulls output
- **WHEN** a hopper below a machine with an output item runs
- **THEN** it extracts only the output, never fuel or inputs

#### Scenario: Front is closed
- **WHEN** a duct or hopper offers coal to the front of a burner machine
- **THEN** nothing goes in, nothing comes out, and the front cannot be set to another mode

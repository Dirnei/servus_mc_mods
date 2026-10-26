# Spec Delta

## MODIFIED Requirements

### Requirement: Blocks of other mods give potato reports
Any Servus block SHALL be able to answer the Data Potato with its own chat report, also when it lives in a mod that Foundation does not know. Using the potato on such a block SHALL print that report and SHALL NOT open its GUI or change it. Using the potato on a named machine SHALL print its label first, also when its block gives no report of its own, and SHALL NOT open its GUI. Using the potato on a block that gives no report and is no named machine SHALL do what any other item does there.

#### Scenario: Report from another mod
- **WHEN** Servus: Shuttles is installed and a player uses the Data Potato on a Heavy Duty Rack
- **THEN** the rack's report is printed into the player's chat and no GUI opens

#### Scenario: Block without a report
- **WHEN** a player uses the Data Potato on a vanilla chest
- **THEN** no report is printed and the potato acts like a plain item

#### Scenario: Named machine without a report
- **WHEN** a player uses the Data Potato on Shuttle Garage "Main Garage (Garage #1)"
- **THEN** chat shows "Main Garage (Garage #1)" and the garage GUI does not open

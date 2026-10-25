## MODIFIED Requirements

### Requirement: Tool modules mark field work
A tool module SHALL be able to mark its shuttle as field only. A field only shuttle SHALL never take a warehouse pick or put-away. A shuttle with no tool module, or with a tool module that is not field only, SHALL take warehouse jobs. The Harvester, the Spreader and the Lumber module are tool modules and are field only; a field only shuttle SHALL take only the field jobs of its own tool.

#### Scenario: No tool module
- **WHEN** a garage holds a shuttle with only Speed upgrades
- **THEN** that shuttle takes warehouse picks and put-aways like any shuttle

#### Scenario: Harvester shuttle
- **WHEN** a garage holds a shuttle with a Harvester and two Speed upgrades
- **THEN** that shuttle takes no warehouse pick or put-away

#### Scenario: Spreader shuttle
- **WHEN** a garage holds a shuttle with a Spreader
- **THEN** that shuttle takes no warehouse pick or put-away and no harvest run

#### Scenario: Lumber shuttle
- **WHEN** a garage holds a shuttle with a Lumber module
- **THEN** that shuttle takes no warehouse pick or put-away, no crop field job and no fertilize run

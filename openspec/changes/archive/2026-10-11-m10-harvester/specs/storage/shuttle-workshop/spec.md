## MODIFIED Requirements

### Requirement: Tool modules mark field work
A tool module SHALL be able to mark its shuttle as field only. A field only shuttle SHALL never take a warehouse pick or put-away. A shuttle with no tool module, or with a tool module that is not field only, SHALL take warehouse jobs. The Harvester is a tool module and is field only.

#### Scenario: No tool module
- **WHEN** a garage holds a shuttle with only a Cargo Hold
- **THEN** that shuttle takes warehouse picks and put-aways like a plain shuttle

#### Scenario: Harvester is field only
- **WHEN** a garage holds a shuttle with a Harvester and a Cargo Hold
- **THEN** that shuttle takes no warehouse pick or put-away

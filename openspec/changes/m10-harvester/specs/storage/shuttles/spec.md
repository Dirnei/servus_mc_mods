## MODIFIED Requirements

### Requirement: Jobs go to shuttles whose modules fit
Every job SHALL name the modules a shuttle needs for it and whether it is field work. Warehouse picks and put-aways SHALL need no module and SHALL NOT be field work. A free shuttle SHALL take a job only when it has every module the job needs and, for a job that is not field work, is not field only. A warehouse job that no shuttle of the warehouse fits SHALL be served as if the warehouse had no shuttle; a field job that no shuttle fits, or that no free fitting shuttle can drive to, SHALL be dropped, because field work needs a shuttle. A job that only busy shuttles fit SHALL wait in the queue while later jobs that free shuttles fit go ahead.

#### Scenario: Only field only shuttles
- **WHEN** every shuttle of a warehouse is field only and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles

#### Scenario: Plain and Cargo Hold shuttles share the queue
- **WHEN** a warehouse has a plain shuttle and a Cargo Hold shuttle and two picks are made
- **THEN** each takes one of them

#### Scenario: Field job without a harvester
- **WHEN** a field job is queued and the warehouse's only Harvester shuttle is taken out of its garage
- **THEN** the field job is dropped and nothing on the field changes

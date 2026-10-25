## ADDED Requirements

### Requirement: Any pattern jobs craft the chosen variant
A job for an Any pattern SHALL carry the items its CPU step chose. The station SHALL take those items from its inputs, craft the recipe with them and give the result the recipe makes from them; it SHALL NOT wait for the pattern's own items. The chosen items SHALL be saved with the job.

#### Scenario: Spruce sticks job
- **WHEN** a CPU hands a station a job for an Any sticks pattern saved with oak planks, with spruce planks chosen, and spruce planks arrive
- **THEN** the station crafts sticks from the spruce planks

#### Scenario: Restart keeps the variant
- **WHEN** the station has that job with crafts left and the server restarts
- **THEN** the job still takes spruce planks

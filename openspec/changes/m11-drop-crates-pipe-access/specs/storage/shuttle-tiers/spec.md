## MODIFIED Requirements

### Requirement: Speed between stops
A shuttle SHALL travel at the base speed plus a step per Speed count, from `shuttle.json` (examples: base 0.15 blocks per tick, plus 0.04 per count). The lift car's speed SHALL NOT change with Speed: a shuttle riding a car moves at the car's speed.

#### Scenario: Faster with upgrades
- **WHEN** one shuttle has no Speed upgrade and another has two, with the example numbers
- **THEN** the first drives 0.15 blocks per tick and the second 0.23, and the second reaches a rack twelve rails away in about 52 ticks instead of 80

# Spec Delta

## MODIFIED Requirements

### Requirement: Seeds
To plant, the harvester SHALL take the field's crop item from its cargo, which holds the harvest. When it puts the harvest away while plots remain to plant, it SHALL keep up to one stack of the crop item, no more than those plots need; when the kept seeds leave no room for the next harvest, the next put-away SHALL keep none. Spare crop items SHALL first fill the head's seed buffer up to its level, at the farm rail beside the head, before the rest is put away. Only when the field has no ripe plot left to harvest and the harvester carries fewer crop items than there are plots to plant SHALL it take the shortfall, up to one stack and as far as its cargo has room, from the head's seed buffer, never from racks. When the buffer holds none, the harvester SHALL plant only what the harvest provides and leave the rest of the farmland empty.

#### Scenario: Replant from the harvest
- **WHEN** a field with wheat set has only ripe wheat and the warehouse holds no wheat seeds
- **THEN** every harvested plot is planted again with seeds from the harvest, the spare seeds fill the head's seed buffer and the rest is put away

#### Scenario: Seeds from storage
- **WHEN** a hydrated field with wheat set has only empty farmland and the head's seed buffer holds wheat seeds
- **THEN** the harvester takes seeds from the buffer, plants the farmland and the buffer holds fewer seeds

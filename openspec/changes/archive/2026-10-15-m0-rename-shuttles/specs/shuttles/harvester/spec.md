# Spec Delta

## MODIFIED Requirements

### Requirement: Field jobs
A warehouse SHALL look at the Field Head of every field whose farm rails are part of its rails every `field_check_interval` ticks (in `data/<ns>/shuttles/field.json`, example 40). It SHALL queue a field job for that field when it has a Harvester shuttle, no field job for that field is queued or running, and either the head signals harvest due, or the field has a crop set, is hydrated and has a plot to till or, while the warehouse holds the crop item, a plot to plant. At most one field job per field SHALL be queued or running at a time. A field job SHALL go to the free Harvester shuttle with the shortest way to a farm rail of the field that has work.

#### Scenario: Harvest due sends a harvester
- **WHEN** a field with wheat set has a harvest due and its warehouse has a free Harvester shuttle
- **THEN** within a few seconds the harvester leaves its garage and drives to the field

#### Scenario: Nothing to do
- **WHEN** a hydrated field with wheat set has every plot planted and nothing ripe
- **THEN** no field job is queued

#### Scenario: Planting needs seeds
- **WHEN** a hydrated field with wheat set has only empty farmland and the warehouse holds no wheat seeds
- **THEN** no field job is queued

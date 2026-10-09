# Spec Delta

## ADDED Requirements

### Requirement: Packing Stations and line load in the Machines tab
The Machines tab SHALL list every Packing Station on the data line with its rate use per direction (items per second used of its rate, to storage and to network) and SHALL name the busiest data duct of the line with its load out of its bandwidth. A Packing Station using its full rate in a direction SHALL be marked busy.

#### Scenario: Station at full rate
- **WHEN** a Packing Station of rate 8 sends 8 items per second to the network and receives none
- **THEN** its row shows 8 of 8 to network marked busy and 0 of 8 to storage

#### Scenario: Busiest duct
- **WHEN** one Data Cable duct of bandwidth 32 carries 30 items per second and every other duct less
- **THEN** the tab names that duct with 30 of 32 items/s

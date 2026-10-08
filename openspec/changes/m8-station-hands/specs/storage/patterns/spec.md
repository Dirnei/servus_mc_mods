# Spec Delta

## MODIFIED Requirements

### Requirement: CPU plans with patterns
A Crafting CPU SHALL plan with the patterns of its data line in addition to the recipes producers report. A pattern SHALL be usable when a producer on the line can execute it; every Crafting Station on the line can execute every crafting pattern. A pattern step SHALL run on such a producer like any step, with the pattern's items as inputs and its result as output, stations that are not busy preferred.

#### Scenario: Pattern with a matching station
- **WHEN** the line has a sticks pattern and a Crafting Station without any job, and a terminal requests 8 sticks that are not in stock
- **THEN** the CPU makes an order whose step runs the sticks pattern on that station

#### Scenario: Pattern without a producer
- **WHEN** the line has a sticks pattern and no Crafting Station
- **THEN** the pattern does not make sticks orderable

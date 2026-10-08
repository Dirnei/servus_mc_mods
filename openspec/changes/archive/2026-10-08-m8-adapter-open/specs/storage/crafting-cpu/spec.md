# Spec Delta

## MODIFIED Requirements

### Requirement: Order slots
A formed CPU SHALL run at most `order_slots` open orders at a time (example 2, from `data/<ns>/storage/cpu.json`), the oldest first. Further orders on that CPU SHALL wait and show "waiting for CPU" until a slot is free. Right clicking a formed CPU block with an empty hand or with any item except the Configuration Carrot SHALL show in the action bar how many slots are in use and how many orders wait; a sneaking player holding an item uses that item instead.

#### Scenario: Third order waits
- **WHEN** one CPU with 2 slots holds three open orders
- **THEN** the two oldest run or are blocked and the third waits for the CPU

#### Scenario: Summary
- **WHEN** a player right clicks that CPU with an empty hand
- **THEN** the action bar says 2 of 2 slots in use and 1 waiting

#### Scenario: Summary while holding a block
- **WHEN** a player holding cobblestone right clicks a formed CPU block without sneaking
- **THEN** the action bar shows the slot summary and no cobblestone is placed

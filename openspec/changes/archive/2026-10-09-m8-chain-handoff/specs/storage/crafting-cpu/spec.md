# Spec Delta

## ADDED Requirements

### Requirement: Intermediates go straight to the next step
When the CPU collects outputs of a step that a later step of the same order still needs, and that later step is delivering on another producer, it SHALL send them as packets straight from the step's producer to the later step's producer, up to what the later step still needs, counted as fed inputs of the later step and as collected outputs of the step. Only outputs beyond that go to an Access Point. Storage SHALL ship the later step only the inputs it still needs after that.

#### Scenario: Planks to sticks on two stations
- **WHEN** an order for 8 sticks has a planks step on one Crafting Station and a sticks step on a second station
- **THEN** the planks go from the first station straight to the second, the warehouse never holds planks, and 8 sticks reach the terminal

#### Scenario: Smelt then craft
- **WHEN** an order needs stone smelted in a furnace with a Machine Adapter and then crafted on a Crafting Station
- **THEN** the stone goes from the furnace straight to the station and never enters the warehouse

#### Scenario: Surplus still stored
- **WHEN** a step makes 4 planks and the later step needs 2
- **THEN** 2 planks go to the later step and 2 to an Access Point

### Requirement: Waiting for the next step
When a later step of the same order still needs a step's outputs but is not delivering yet because its producer is busy with another order, the outputs SHALL wait at their producer for up to `handoff_wait_ticks` (example 100, from `data/<ns>/storage/cpu.json`) and then go to an Access Point as before. When the later step runs on the same producer as the step, the outputs SHALL go to an Access Point without waiting.

#### Scenario: Next producer stays busy
- **WHEN** the only producer for the later step is busy with another order for longer than the wait
- **THEN** the outputs go to storage and the order still completes once the producer is free

#### Scenario: Same producer
- **WHEN** a chain runs both steps on one Crafting Station
- **THEN** the intermediate goes to storage right away and the order completes

### Requirement: Waiting steps move to a free producer
A step waiting for its producer because another step occupies it SHALL move to another producer of the CPU's data line that offers the same recipe with the same inputs and is free, and start there.

#### Scenario: Second station takes over
- **WHEN** two Crafting Stations are on the line and an order's second step was planned on the station its first step runs on
- **THEN** the second step starts on the other station

### Requirement: No missing report for inputs still being made
A delivering step SHALL NOT report an input as missing while an earlier step of the same order that makes that input is not done.

#### Scenario: Waiting for planks
- **WHEN** the sticks step delivers while the planks step still crafts
- **THEN** the order is not blocked with planks missing

### Requirement: Handoffs survive reloads
A handoff SHALL be booked in the production ledger like other transfers: the step counts what landed as collected, the later step counts it as fed, and a handoff the ledger forgot counts as landed for the step and as nothing for the later step, which is then fed again from storage. Nothing SHALL be lost or counted twice across a reload.

#### Scenario: Reload mid handoff
- **WHEN** the CPU and both producers are reloaded while planks travel from one station to the other
- **THEN** the order completes with 8 sticks at the terminal and no planks lost

# Spec Delta

## ADDED Requirements

### Requirement: Controller exposes stock
A Warehouse Controller SHALL expose the Lib stock provider and stock sink capabilities on every side for its online warehouse: counts come from the index, taking uses the warehouse's instant extract, handing in uses the warehouse's insert with its zone routing. An offline controller SHALL report no stock and accept nothing.

#### Scenario: Another mod counts stock
- **WHEN** a mod asks the controller's stock provider for cobblestone while crates hold 80
- **THEN** it gets 80

#### Scenario: Another mod hands items in
- **WHEN** a mod hands 20 bread to the controller's stock sink while an empty crate is in the warehouse
- **THEN** the bread lands in the crate and nothing is returned

#### Scenario: Offline
- **WHEN** the network has two controllers
- **THEN** the stock provider reports zero and the sink returns the whole stack

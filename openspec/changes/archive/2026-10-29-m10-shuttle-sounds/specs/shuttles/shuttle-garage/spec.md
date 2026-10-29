# Spec Delta

## ADDED Requirements

### Requirement: Garage Muffler slot
The garage GUI SHALL have one Muffler slot that takes only a Muffler, one item. While it holds a Muffler, every shuttle of the garage SHALL be muffled, inside or out on a job. The Muffler SHALL be saved with the garage, SHALL drop as an item when the garage is broken or dismantled with the Configuration Carrot. The Muffler slot SHALL never hold a shuttle, and taking the Muffler out SHALL always be allowed.

#### Scenario: Put a Muffler in
- **WHEN** a player puts a Muffler into the garage's Muffler slot
- **THEN** every shuttle of the garage plays the damped drive sound

#### Scenario: Only a Muffler fits
- **WHEN** a player tries to put a Speed Upgrade into the Muffler slot
- **THEN** it does not go in

#### Scenario: Saved with the garage
- **WHEN** a garage with a Muffler is reloaded with the world
- **THEN** it still holds the Muffler and its shuttles are still muffled

#### Scenario: Broken empty garage
- **WHEN** a player breaks an empty garage that holds a Muffler
- **THEN** the garage and the Muffler drop as items

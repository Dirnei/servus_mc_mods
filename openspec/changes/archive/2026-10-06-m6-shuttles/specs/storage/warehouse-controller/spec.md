# Spec Delta

## ADDED Requirements

### Requirement: Controller holds the fleet
The controller SHALL remember how many shuttles its warehouse has, keep that number across reloads, and SHALL hand the fleet to whichever network it belongs to. When the controller leaves a network, shuttles in flight SHALL drop their cargo at the controller.

#### Scenario: Reload
- **WHEN** a controller with three shuttles is unloaded and loaded again
- **THEN** the warehouse has three shuttles parked on the home rail

#### Scenario: Controller broken mid pick
- **WHEN** the controller is broken while a shuttle carries cobblestone
- **THEN** the cobblestone and three shuttle items drop at the controller

### Requirement: Summary reports shuttles
The controller's summary SHALL state the shuttle count, how many are busy, and whether a home rail exists.

#### Scenario: Busy fleet
- **WHEN** an op right clicks a controller with four shuttles of which one is on a job
- **THEN** the summary says four shuttles, one busy

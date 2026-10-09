# Spec Delta

## ADDED Requirements

### Requirement: Athanor crafts count
The Athanor SHALL report every finished craft to the journal of its owner's team as an `athanor` event for its result, and the event SHALL carry the Athanor's tier name so a task can filter on it with `tier:<name>`.

#### Scenario: Schwaz task
- **WHEN** a team finishes a craft on an Albedo Athanor while Schwaz is open
- **THEN** Schwaz's task "craft on an Albedo Athanor" is done

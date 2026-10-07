# Spec Delta

## ADDED Requirements

### Requirement: Fixed sides
A block MAY fix single relative sides to one mode. A fixed side SHALL keep that mode through cycling, setting, the carrot and loading, SHALL count as default, SHALL be drawn greyed in the side panel and SHALL NOT react to clicks there.

#### Scenario: Cycling a fixed side
- **WHEN** a machine's front is fixed to Off and the player clicks its cell or uses the carrot on the front
- **THEN** the front stays Off and the cell stays greyed

#### Scenario: Loading
- **WHEN** a saved configuration holds Input for a side the block fixes to Off
- **THEN** the side is Off after loading

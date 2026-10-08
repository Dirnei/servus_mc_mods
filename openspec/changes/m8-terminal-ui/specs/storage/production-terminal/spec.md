# Spec Delta

## ADDED Requirements

### Requirement: Screen layout
The Production Terminal screen SHALL show the title on its own row, the tab bar on the row below it and the list below the tab bar, so the title, the tabs and the rows never overlap; the pattern editor SHALL cover the list area with the player inventory below it, inside the panel. The panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Title and tabs
- **WHEN** the screen opens with all three tabs
- **THEN** the title and the tabs do not overlap

## MODIFIED Requirements

### Requirement: Live view
The terminal SHALL send its full content as soon as the screen opens, without waiting for the periodic update, and the screen SHALL show a loading hint until it arrives. While the screen is open the terminal SHALL send the current CPUs, machines, orders, patterns and pattern capacity every half second when something changed, and nothing when nothing changed.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from delivering to producing while the screen is open
- **THEN** the screen shows producing within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player saves a pattern from a second Production Terminal on the same line while the screen is open
- **THEN** the Patterns tab lists it within half a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** its content is sent in the same tick the screen opens

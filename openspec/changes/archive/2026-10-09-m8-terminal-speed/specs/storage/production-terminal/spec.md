# Spec Delta

## MODIFIED Requirements

### Requirement: Live view
The terminal SHALL send its full content when the game opens the screen, without waiting for the periodic update. The client SHALL keep the last content of each terminal for the session and SHALL show it at once when that terminal is opened again, replacing it when the fresh content arrives; the screen SHALL show a loading hint only while it has no content for that terminal yet. While the screen is open the terminal SHALL send the current CPUs, machines, orders, patterns and pattern capacity every half second when something changed, and nothing when nothing changed.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from delivering to producing while the screen is open
- **THEN** the screen shows producing within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player saves a pattern from a second Production Terminal on the same line while the screen is open
- **THEN** the Patterns tab lists it within half a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** its content is sent in the same tick the screen opens

#### Scenario: Opened again
- **WHEN** the player closes the terminal and opens it again in the same session
- **THEN** the last content shows at once without a loading hint and is replaced by the fresh content

# Spec Delta

## MODIFIED Requirements

### Requirement: Live view
While the screen is open the terminal SHALL send the current CPUs, machines, orders, patterns and pattern capacity every half second when something changed, and nothing when nothing changed.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from delivering to producing while the screen is open
- **THEN** the screen shows producing within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player saves a pattern from a second Production Terminal on the same line while the screen is open
- **THEN** the Patterns tab lists it within half a second

### Requirement: Patterns tab
The Patterns tab SHALL show a capacity bar with the used and total pattern places of the line, a plus button, and every pattern of the line with its result, its inputs and the position of its Pattern Storage. Plus SHALL open the recipe editor with an empty grid; clicking a pattern SHALL open the editor prefilled with its template and offer Delete. While the editor is open the player inventory SHALL be shown so items can be put into the ghost grid, and the rest of the screen SHALL be dimmed and inert.

#### Scenario: Capacity bar
- **WHEN** the line has one Pattern Storage with 9 places holding 2 patterns
- **THEN** the bar reads 2 of 9

#### Scenario: Edit a pattern
- **WHEN** the player clicks the sticks pattern, changes the grid to a torch layout and presses Save
- **THEN** that pattern now holds the torch recipe

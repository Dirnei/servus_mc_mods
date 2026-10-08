# Spec Delta

## MODIFIED Requirements

### Requirement: Tab bar
The screen SHALL show a tab bar with an Orders tab, a Machines tab and a Patterns tab, the Orders tab selected when the screen opens. Clicking a tab SHALL show its content. The bar SHALL leave room for further tabs.

#### Scenario: Switch tab
- **WHEN** the player clicks the Machines tab
- **THEN** the screen shows the producers of the line

#### Scenario: Patterns tab
- **WHEN** the player clicks the Patterns tab
- **THEN** the screen shows the patterns of the line and the capacity bar

### Requirement: Live view
While the screen is open the terminal SHALL send the current CPUs, machines, orders, patterns and pattern capacity every half second when something changed, and nothing when nothing changed.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from delivering to producing while the screen is open
- **THEN** the screen shows producing within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player puts an encoded Pattern into a Pattern Storage of the line while the screen is open
- **THEN** the Patterns tab lists it within half a second

## ADDED Requirements

### Requirement: Patterns tab
The Patterns tab SHALL show a capacity bar with the used and total pattern slots of the line, a plus button, and every pattern of the line with its result, its inputs and the position of its Pattern Storage. Plus SHALL open the recipe editor with an empty grid; clicking a pattern SHALL open the editor prefilled with its template and offer Delete. While the editor is open the player inventory SHALL be shown so items can be put into the ghost grid, and the rest of the screen SHALL be dimmed and inert.

#### Scenario: Capacity bar
- **WHEN** the line has one Pattern Storage with 9 slots holding 2 patterns
- **THEN** the bar reads 2 of 9

#### Scenario: Edit a pattern
- **WHEN** the player clicks the sticks pattern, changes the grid to a torch layout and presses Save
- **THEN** that pattern now holds the torch recipe and no Blank Pattern is used

### Requirement: Recipe viewer transfer into the terminal
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the open Production Terminal prefilled with that recipe's layout. Without JEI the terminal SHALL work unchanged.

#### Scenario: Plus button in JEI
- **WHEN** the terminal screen is open and the player presses the transfer button on the stick recipe in JEI
- **THEN** the editor opens with a plank above a plank and the preview shows four sticks

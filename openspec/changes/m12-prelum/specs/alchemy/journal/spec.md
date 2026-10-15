## ADDED Requirements

### Requirement: Pressing counts
Every result a Prelum presses SHALL count as a `press` event for the Prelum's owning team, with the result item as the event's item, so a task can filter by item id or `#tag`.

#### Scenario: Scrap counts
- **WHEN** a Prelum placed by a team member presses one Netherite Scrap
- **THEN** a `press` event for Netherite Scrap counts once for the team

### Requirement: Basel and Salzburg ends at the Prelum
The Basel and Salzburg chapter SHALL have a fourth task "for the first time": press one Netherite Scrap (`press` with filter `minecraft:netherite_scrap`). Its task page SHALL list it.

#### Scenario: First scrap
- **WHEN** a team with Basel and Salzburg open presses its first Netherite Scrap
- **THEN** the task "press Netherite Scrap" of Basel and Salzburg is done

#### Scenario: Chapter finished
- **WHEN** a team with Basel and Salzburg open finishes a Rubedo craft, binds a gnome to a diamond vein, finishes sixteen Athanor crafts and presses a Netherite Scrap
- **THEN** Basel and Salzburg is finished

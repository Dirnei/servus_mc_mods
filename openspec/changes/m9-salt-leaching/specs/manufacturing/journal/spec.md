# Spec Delta

## MODIFIED Requirements

### Requirement: Tasks count events
A task SHALL name an event (`calcine`, `principle`, `coagulate`, `purify`), an optional item or tag filter (for `principle`, a principle name) and a count. Machines SHALL report their work as events for their owner's team; a task SHALL add the event's amount while its chapter is open and its parent chapter is finished, and SHALL be done when it reaches its count. A task with count 1 is a "first time" task.

#### Scenario: Calcine one hundred things
- **WHEN** a team's Calciner calcines 100 items while Villach is open
- **THEN** the task "calcine 100 things" of Villach is done

#### Scenario: First Salt Crystal
- **WHEN** a team's Coagulator makes its first Salt Crystal
- **THEN** the task "coagulate a Salt Crystal" is done

#### Scenario: First purification
- **WHEN** a team's Purifier finishes its first cycle while Villach is open
- **THEN** the task "purify Ash" of Villach is done

#### Scenario: Before the chapter opens
- **WHEN** a team calcines items while the parent of a chapter is not finished
- **THEN** nothing counts for that chapter

### Requirement: Workshop tutorial
The journal SHALL have a Workshop category beside the story with an entry per alchemy machine and topic: the Principles, the Calciner, the Purifier, the Coagulator, the Athanor, pipes and fluid sides, and for Albedo the Salt Crystal Block, the Elixir of Sight and the Gnome Hollow. A machine entry SHALL show the machine, its crafting recipe and how to use it. Nigredo entries SHALL be open from the start; Albedo entries SHALL unlock with the Schwaz chapter.

#### Scenario: Calciner entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Calciner entry shows the Calciner, its crafting recipe and how to fuel and empty it

#### Scenario: Purifier entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Purifier entry shows the Purifier, its crafting recipe, how Ash and water become Salt, and the purification recipes

#### Scenario: Albedo entries locked
- **WHEN** a player has not finished Villach
- **THEN** the Salt Crystal Block, Elixir of Sight and Gnome Hollow entries are locked

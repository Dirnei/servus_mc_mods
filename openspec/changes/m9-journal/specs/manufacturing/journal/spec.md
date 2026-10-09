# Spec Delta

## Purpose

Paracelsus' journal is Manufacturing's tech tree: chapters of his life, each with tasks a team completes by doing alchemy, unlock the next chapter, its story and its recipes. There is no research lab and no pack.

## ADDED Requirements

### Requirement: Chapters are data
A journal chapter SHALL be a JSON file at `data/<namespace>/servus_manufacturing/journal/<chapter>.json` with an optional parent chapter, a sort order and a list of tasks. The chapter id is `<namespace>:<chapter>`. Chapters SHALL reload with `/reload`.

#### Scenario: Villach
- **WHEN** the shipped data holds `villach.json` without a parent and three tasks
- **THEN** Villach is the first chapter and every team starts with it open

### Requirement: Tasks count events
A task SHALL name an event (`calcine`, `principle`, `coagulate`), an optional item or tag filter (for `principle`, a principle name) and a count. Machines SHALL report their work as events for their owner's team; a task SHALL add the event's amount while its chapter is open and its parent chapter is finished, and SHALL be done when it reaches its count. A task with count 1 is a "first time" task.

#### Scenario: Calcine one hundred things
- **WHEN** a team's Calciner calcines 100 items while Villach is open
- **THEN** the task "calcine 100 things" of Villach is done

#### Scenario: First Salt Crystal
- **WHEN** a team's Coagulator makes its first Salt Crystal
- **THEN** the task "coagulate a Salt Crystal" is done

#### Scenario: Before the chapter opens
- **WHEN** a team calcines items while the parent of a chapter is not finished
- **THEN** nothing counts for that chapter

### Requirement: Team progress persists
Task counts and finished chapters SHALL be kept per team (the Foundation team of the player: an FTB team when present, else the player) in world data and SHALL survive a restart.

#### Scenario: Restart
- **WHEN** a team has calcined 40 of 100 items and the server restarts
- **THEN** the task still shows 40 of 100

### Requirement: Finished chapters grant advancements
When every task of a chapter is done the chapter SHALL be finished for the team, and every member SHALL receive the advancement `<namespace>:journal/<chapter>`: online members at once, others at their next login.

#### Scenario: Teammate offline
- **WHEN** a team finishes Villach while one member is offline
- **THEN** that member gets the Villach advancement when they log in

### Requirement: Chapter recipes stay locked
A coagulation recipe MAY name a `chapter`. A Coagulator SHALL only select such a recipe when the team of its owner has finished that chapter, and its GUI SHALL show such recipes as locked until then.

#### Scenario: Locked recipe
- **WHEN** a player clicks a recipe whose chapter their team has not finished
- **THEN** the selection does not change and the list marks the recipe as locked

### Requirement: The journal book
Manufacturing SHALL ship a Modonomicon book `servus_manufacturing:journal`, "Paracelsus' Journal", with one entry per chapter that tells the chapter's part of the story, lists its tasks and what it unlocks. The entry of a chapter with a parent SHALL stay locked until the reader has the parent's advancement.

#### Scenario: Schwaz opens
- **WHEN** a player receives the Villach advancement
- **THEN** the Schwaz entry of their journal unlocks

### Requirement: Getting the journal
Every player SHALL receive the journal once at their first login, and SHALL be able to craft it from a book and a piece of coal.

#### Scenario: First login
- **WHEN** a player joins a world for the first time
- **THEN** the journal is in their inventory, and a later login gives no second copy

### Requirement: Journal command
`/servus_manufacturing journal` SHALL list for the player's team every open or finished chapter with each task's progress.

#### Scenario: Progress
- **WHEN** a player runs the command after calcining 40 items
- **THEN** it shows Villach with "calcine: 40 / 100"

# manufacturing/journal Specification

## Purpose
Paracelsus' journal is Manufacturing's tech tree: chapters of his life, each with tasks a team completes by doing alchemy, unlock the next chapter, its story and its recipes. There is no research lab and no pack.

## Requirements

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
A coagulation recipe MAY name a `chapter`. A Coagulator SHALL only select such a recipe when the team of its owner has finished that chapter, and its GUI SHALL name the needed chapter in the recipe's tooltip.

#### Scenario: Locked recipe
- **WHEN** a player clicks a recipe whose chapter their team has not finished
- **THEN** the selection does not change and the tooltip names the chapter the recipe needs

### Requirement: The journal book
Manufacturing SHALL ship a Modonomicon book `servus_manufacturing:journal`, "Paracelsus' Journal", with one entry per chapter that tells the chapter's part of the story, lists its tasks and what it unlocks. The entry of a chapter with a parent SHALL stay locked until the reader has the parent's advancement.

#### Scenario: Schwaz opens
- **WHEN** a player receives the Villach advancement
- **THEN** the Schwaz entry of their journal unlocks

### Requirement: Getting the journal
The journal SHALL be Manufacturing's own item `servus_manufacturing:paracelsus_journal`, named "Paracelsus' Journal", shown as a Servus: Manufacturing item and opening the journal book. Every player SHALL receive it once at their first login, and SHALL be able to craft it from a book and a piece of coal.

#### Scenario: Own item
- **WHEN** a player hovers the journal in an inventory
- **THEN** it reads "Paracelsus' Journal" from Servus: Manufacturing, and using it opens the journal

#### Scenario: First login
- **WHEN** a player joins a world for the first time
- **THEN** the journal is in their inventory, and a later login gives no second copy

### Requirement: Journal command
`/servus_manufacturing journal` SHALL list for the player's team every open or finished chapter with each task's progress.

#### Scenario: Progress
- **WHEN** a player runs the command after calcining 40 items
- **THEN** it shows Villach with "calcine: 40 / 100"

### Requirement: Workshop tutorial
The journal SHALL have a Workshop category beside the story with an entry per alchemy machine and topic: the Principles, the Calciner, the Coagulator, the Athanor, pipes and fluid sides, and for Albedo the Salt Crystal Block, the Elixir of Sight and the Gnome Hollow. A machine entry SHALL show the machine, its crafting recipe and how to use it. Nigredo entries SHALL be open from the start; Albedo entries SHALL unlock with the Schwaz chapter.

#### Scenario: Calciner entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Calciner entry shows the Calciner, its crafting recipe and how to fuel and empty it

#### Scenario: Albedo entries locked
- **WHEN** a player has not finished Villach
- **THEN** the Salt Crystal Block, Elixir of Sight and Gnome Hollow entries are locked

### Requirement: Recipe lists from the shipped recipes
The Workshop entries of the Calciner, Coagulator and Athanor SHALL list every calcination, coagulation and Athanor recipe Manufacturing ships, one line per recipe in the form "Cobblestone: 100 Salt, 10 Mercury", generated from the same data that generates the recipes, so the book always matches the shipped recipes. Formatting the lines SHALL be pure Java covered by JUnit.

#### Scenario: Calcination list
- **WHEN** the player reads the Calciner entry
- **THEN** a page lists "Cobblestone: 100 Salt, 10 Mercury" among the other calcination recipes

#### Scenario: Principles omitted when zero
- **WHEN** a recipe uses only Salt
- **THEN** its line names only Salt

### Requirement: Athanor crafts count
The Athanor SHALL report every finished craft to the journal of its owner's team as an `athanor` event for its result, and the event SHALL carry the Athanor's tier name so a task can filter on it with `tier:<name>`.

#### Scenario: Schwaz task
- **WHEN** a team finishes a craft on an Albedo Athanor while Schwaz is open
- **THEN** Schwaz's task "craft on an Albedo Athanor" is done

### Requirement: Drinking the elixir counts
Drinking an Elixir of Sight SHALL report a `sight` event to the journal of the drinker's team. Schwaz SHALL ask for it once.

#### Scenario: First sight
- **WHEN** a player whose team has Schwaz open drinks an Elixir of Sight
- **THEN** Schwaz's task "drink the Elixir of Sight" is done

### Requirement: Binding a gnome counts
Binding a gnome SHALL report a `bind` event for the vein's yield to the journal of the Hollow's team. Schwaz SHALL ask for one bound gnome.

#### Scenario: First gnome
- **WHEN** a team with Schwaz open binds its first gnome
- **THEN** Schwaz's task "bind a gnome" is done

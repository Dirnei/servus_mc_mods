# Spec Delta

## MODIFIED Requirements

### Requirement: Chapters are data
A journal chapter SHALL be a JSON file at `data/<namespace>/servus_alchemy/journal/<chapter>.json` with an optional parent chapter, a sort order and a list of tasks. The chapter id is `<namespace>:<chapter>`. Chapters SHALL reload with `/reload`.

#### Scenario: Villach
- **WHEN** the shipped data holds `villach.json` without a parent and three tasks
- **THEN** Villach is the first chapter and every team starts with it open

### Requirement: The journal book
Alchemy SHALL ship a Modonomicon book `servus_alchemy:journal`, "Paracelsus' Journal", with one entry per chapter that tells the chapter's part of the story, lists its tasks and what it unlocks. The entry of a chapter with a parent SHALL stay locked until the reader has the parent's advancement.

#### Scenario: Schwaz opens
- **WHEN** a player receives the Villach advancement
- **THEN** the Schwaz entry of their journal unlocks

### Requirement: Getting the journal
The journal SHALL be Alchemy's own item `servus_alchemy:paracelsus_journal`, named "Paracelsus' Journal", shown as a Servus: Alchemy item and opening the journal book. Every player SHALL receive it once at their first login, and SHALL be able to craft it from a book and a piece of coal.

#### Scenario: Own item
- **WHEN** a player hovers the journal in an inventory
- **THEN** it reads "Paracelsus' Journal" from Servus: Alchemy, and using it opens the journal

#### Scenario: First login
- **WHEN** a player joins a world for the first time
- **THEN** the journal is in their inventory, and a later login gives no second copy

### Requirement: Journal command
`/servus_alchemy journal` SHALL list for the player's team every open or finished chapter with each task's progress.

#### Scenario: Progress
- **WHEN** a player runs the command after calcining 40 items
- **THEN** it shows Villach with "calcine: 40 / 100"

### Requirement: Recipe lists from the shipped recipes
The Workshop entries of the Calciner, Coagulator and Athanor SHALL list every calcination, coagulation and Athanor recipe Alchemy ships, one line per recipe in the form "Cobblestone: 100 Salt, 10 Mercury", generated from the same data that generates the recipes, so the book always matches the shipped recipes. Formatting the lines SHALL be pure Java covered by JUnit.

#### Scenario: Calcination list
- **WHEN** the player reads the Calciner entry
- **THEN** a page lists "Cobblestone: 100 Salt, 10 Mercury" among the other calcination recipes

#### Scenario: Principles omitted when zero
- **WHEN** a recipe uses only Salt
- **THEN** its line names only Salt

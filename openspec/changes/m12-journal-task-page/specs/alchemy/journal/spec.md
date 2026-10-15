## ADDED Requirements

### Requirement: Task page with live progress
Every chapter entry of the journal book SHALL show a task page as the first page after its title. It SHALL list each task of the chapter with a short name, the team's progress against the task's count (for example "40 / 100") and a mark once the task is done. When the team has finished the chapter, the page SHALL say so. The page SHALL show the reader's team progress as it is now, also while the book stays open.

#### Scenario: Progress shows in the book
- **WHEN** a player whose team has calcined 40 of 100 items opens the Villach entry
- **THEN** the task page shows "Calcine 100 things" with "40 / 100" and no mark

#### Scenario: Task done
- **WHEN** the team coagulates its first Salt Crystal while the player reads the Villach entry
- **THEN** "Coagulate a Salt Crystal" shows "1 / 1" with a mark

#### Scenario: Finished chapter
- **WHEN** the team has finished Villach
- **THEN** the Villach task page shows every task done and says the chapter is finished

#### Scenario: Teammate's work
- **WHEN** a teammate binds a gnome while Schwaz is open
- **THEN** the other online members see "Bind a gnome" done on the Schwaz task page

### Requirement: Task names are translated
Every task of every shipped chapter SHALL have a lang key `journal.<namespace>.<chapter>.task.<task id>`; the task page SHALL fall back to the task id when a key is missing.

#### Scenario: Shipped tasks named
- **WHEN** the shipped chapters load
- **THEN** every task has a translated name in `en_us`

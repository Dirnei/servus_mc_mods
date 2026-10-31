## MODIFIED Requirements

### Requirement: Journal command
`/servus alchemy journal [player]` SHALL list for the team of the given player, or of the player who runs it when left out, every open or finished chapter with each task's progress. Every player SHALL be able to run it for themselves; naming another player SHALL need operator permission (level 2). Run without a player from a source that is not a player, such as RCON, it SHALL fail with a message asking for a player.

#### Scenario: Progress
- **WHEN** a player runs the command after calcining 40 items
- **THEN** it shows Villach with "calcine: 40 / 100"

#### Scenario: Over RCON
- **WHEN** `/servus alchemy journal Alex` arrives over RCON
- **THEN** it shows the chapters of Alex's team

### Requirement: Finishing a chapter by command
`/servus alchemy journal finish <player> <chapter>` SHALL need operator permission (level 2) and SHALL finish the named chapter and every unfinished chapter on its parent chain for the team of the given player (`@s` for the player who runs it): every task of those chapters is set to its count, the chapters count as finished, and every online member of the team receives their advancements. The chapter argument SHALL suggest the loaded chapter ids. An unknown chapter SHALL fail with a message and change nothing.

#### Scenario: Jump to Basel and Salzburg
- **WHEN** an operator whose team has finished nothing runs `/servus alchemy journal finish @s servus_alchemy:wandering_years`
- **THEN** Villach, Schwaz and Wandering years are finished for the team, Basel and Salzburg is open, and the journal unlocks the entries of those chapters

#### Scenario: Already finished
- **WHEN** an operator runs the command for a chapter their team already finished
- **THEN** nothing changes and the message says the chapter was already finished

#### Scenario: Not an operator
- **WHEN** a player without operator permission types `/servus alchemy journal finish`
- **THEN** the command is not available to them

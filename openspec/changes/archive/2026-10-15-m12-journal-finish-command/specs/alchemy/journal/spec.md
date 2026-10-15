## ADDED Requirements

### Requirement: Finishing a chapter by command
`/servus_alchemy journal finish <chapter>` SHALL need operator permission (level 2) and SHALL finish the named chapter and every unfinished chapter on its parent chain for the team of the player who runs it: every task of those chapters is set to its count, the chapters count as finished, and every online member of the team receives their advancements. The chapter argument SHALL suggest the loaded chapter ids. An unknown chapter SHALL fail with a message and change nothing.

#### Scenario: Jump to Basel and Salzburg
- **WHEN** an operator whose team has finished nothing runs `/servus_alchemy journal finish servus_alchemy:wandering_years`
- **THEN** Villach, Schwaz and Wandering years are finished for the team, Basel and Salzburg is open, and the journal unlocks the entries of those chapters

#### Scenario: Already finished
- **WHEN** an operator runs the command for a chapter their team already finished
- **THEN** nothing changes and the message says the chapter was already finished

#### Scenario: Not an operator
- **WHEN** a player without operator permission types `/servus_alchemy journal finish`
- **THEN** the command is not available to them

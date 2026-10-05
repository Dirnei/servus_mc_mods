# manufacturing/tier1-balance Specification

## Purpose

Tier 1 machines are balanced so a documented reference layout yields one Red Pack per 5 seconds. Players can plan builds around that ratio, and the GameTest keeps it true.

## Requirements

### Requirement: Reference block produces one Red Pack per 5 seconds
With default stats and recipes, the reference block (two Burner Extractors and two Burner Smelters on iron, one extractor and one smelter on copper, one extractor on coal, three Assemblers set to iron gear, copper wire and red pack, generators for power, ducts between them, and one Research Lab collecting packs) SHALL produce one Red Pack every 5 seconds once the pipeline is filled.

#### Scenario: Sixty seconds
- **WHEN** the reference block runs fueled and powered for 60 seconds from empty
- **THEN** the lab holds between 7 and 13 Red Packs

### Requirement: The balance is covered by a GameTest
A GameTest SHALL build the reference block and check the rate, so the test server run fails when a change breaks the balance.

#### Scenario: Test run
- **WHEN** `gradlew :manufacturing:runGameTestServer` runs
- **THEN** the reference block test passes and the server exits with status 0

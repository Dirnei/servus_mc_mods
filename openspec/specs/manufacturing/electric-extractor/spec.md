# manufacturing/electric-extractor Specification

## Purpose

The Electric Extractor is the first research reward: a faster, powered replacement for the Burner Extractor that keeps the same deposit rules.

## Requirements

### Requirement: Extracts twice as fast on power
An Electric Extractor SHALL produce the yield of the deposit below it once per work period while it has energy and output room, spending its energy per tick while working. Without a deposit it SHALL idle and spend nothing.

#### Scenario: On iron deposit
- **WHEN** an electric extractor stands on an iron deposit with energy available
- **THEN** one raw iron appears in its output every 2 seconds (40 ticks by default, retunable in data)

#### Scenario: No deposit
- **WHEN** it stands on dirt
- **THEN** nothing is produced and no energy is spent

#### Scenario: No power
- **WHEN** the energy buffer is empty
- **THEN** progress pauses and resumes when energy arrives

### Requirement: Obtained through research
The Electric Extractor SHALL be made in the assembler from a Burner Extractor, iron gears and copper wire, and the recipe SHALL require the electric_extractor tech.

#### Scenario: Before research
- **WHEN** a player whose team has not researched electric_extractor adds the recipe to an assembler
- **THEN** the assembler refuses it with the research message

#### Scenario: After research
- **WHEN** the tech is completed
- **THEN** the recipe can be added and the assembler produces Electric Extractors

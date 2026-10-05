# manufacturing/stone-processing Specification

## Purpose

Stone Processing makes the stone deposit worth mining: assemblers turn cobblestone and sand into stone, stone bricks and glass once the tech is researched.

## Requirements

### Requirement: Stone processing recipes
Assembling recipes SHALL exist that turn cobblestone into stone, stone into stone bricks and sand into glass, each requiring the stone_processing tech (example values: two to two in 20 ticks for stone and bricks, one to one in 40 ticks for glass).

#### Scenario: After research
- **WHEN** a team has completed stone_processing and configures the stone recipe on an assembler with cobblestone in the inputs
- **THEN** stone is produced at the recipe time

#### Scenario: Before research
- **WHEN** a team without the tech adds the glass recipe
- **THEN** the assembler refuses it with the research message

#### Scenario: Listed in JEI
- **WHEN** the player looks up stone bricks in JEI
- **THEN** the Assembling category shows the stone to stone bricks recipe

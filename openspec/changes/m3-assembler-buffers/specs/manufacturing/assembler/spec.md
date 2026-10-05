# Spec Delta

## MODIFIED Requirements

### Requirement: Crafting the selected recipe
The assembler SHALL craft any of its configured recipes whose required items are all present in the input slots and whose result and remainders fit the output slots, taking turns between craftable recipes. A crafting recipe takes the machine's work time; an assembling recipe takes its own time. Crafting remainders such as empty buckets go to the output slots together with the result.

#### Scenario: Vanilla recipe
- **WHEN** the stick recipe is configured and two planks are in the inputs with energy available
- **THEN** after the machine's work time four sticks are in the output and the planks are gone

#### Scenario: Round robin
- **WHEN** the gear and wire recipes are configured and both iron and copper ingots are present
- **THEN** crafts alternate between gears and wire

#### Scenario: Missing ingredient
- **WHEN** a configured recipe lacks an ingredient
- **THEN** it is skipped and no energy is spent on it

#### Scenario: Iron gear
- **WHEN** iron gear is configured, four iron ingots are in the inputs, the machine has energy
- **THEN** after 20 ticks one iron gear is in the output and two ingots remain, and after 20 more ticks a second gear is produced

#### Scenario: No power
- **WHEN** a configured recipe could be crafted but the energy buffer is empty
- **THEN** progress does not advance and the inputs are untouched

#### Scenario: Remainders
- **WHEN** the cake recipe is configured and three milk buckets, two sugar, an egg and three wheat are in the inputs
- **THEN** after one craft the outputs hold one cake and three empty buckets

#### Scenario: Outputs full
- **WHEN** the result or a remainder would not fit the output slots
- **THEN** the craft waits and the inputs are untouched

## ADDED Requirements

### Requirement: Buffers
The Assembler Mk1 SHALL have 18 input slots and 4 output slots, so two configured recipes can be buffered and results and remainders can accumulate.

#### Scenario: Slot counts
- **WHEN** the player opens the assembler GUI
- **THEN** three rows of six input slots and a two by two block of output slots are shown

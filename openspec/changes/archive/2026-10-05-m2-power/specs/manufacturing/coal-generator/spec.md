# Spec Delta

## Purpose

The Coal Generator is the Tier 1 power source: solid fuel in, FE out, works with any FE consumer.

## ADDED Requirements

### Requirement: Generates FE from furnace fuel
The generator SHALL burn any furnace fuel and produce a fixed amount of FE per tick while burning. One burn SHALL last the item's furnace burn time divided by the generator's fuel divisor.

#### Scenario: Coal
- **WHEN** one coal is in the fuel slot and the buffer has room
- **THEN** the generator produces 40 FE/t for 400 ticks (20 seconds) with the default stats

#### Scenario: Buffer full
- **WHEN** the internal buffer is full
- **THEN** no new fuel is consumed, and a burn in progress keeps counting down without adding energy

### Requirement: Energy is offered and pushed to neighbours
The generator SHALL expose its buffer through the energy capability so neighbours can extract, and SHALL push energy into any adjacent energy receiver every tick up to its output rate.

#### Scenario: Consumer beside the generator
- **WHEN** a machine that accepts FE is adjacent to a running generator
- **THEN** the machine's buffer fills without any cable

#### Scenario: Generator never receives
- **WHEN** another generator or cable offers energy to this generator
- **THEN** it accepts none

### Requirement: GUI and crafting
The generator SHALL have a GUI with the fuel slot, a flame while burning and an energy bar, and SHALL be craftable from vanilla materials.

#### Scenario: Open GUI
- **WHEN** the player right clicks the generator
- **THEN** the GUI shows the fuel slot, the burn flame and the stored energy

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots, copper ingots and a furnace in the generator pattern
- **THEN** the result is one Coal Generator

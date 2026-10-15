## MODIFIED Requirements

### Requirement: Elixir of Sight
The Elixir of Sight SHALL be drunk like a potion, over a short time, SHALL give the drinker Elemental Sight for its duration (example 3,600 ticks), and SHALL leave a glass bottle. It SHALL stack to 16. Every drink SHALL also roll a poison chance (see "The dose makes the poison"); Elemental Sight SHALL be granted whether the roll poisons or not.

#### Scenario: Drink
- **WHEN** a player finishes drinking an Elixir of Sight and the roll does not poison
- **THEN** they have Elemental Sight for three minutes and hold a glass bottle

## ADDED Requirements

### Requirement: The dose makes the poison
Each finished drink of an Elixir of Sight SHALL poison the drinker with a chance read from datapack JSON (example 15 %). A poisoned drinker SHALL get Poison (example 200 ticks) and Nausea (example 200 ticks), each skipped when its duration is 0. The roll SHALL use the level's random source. Without a data file for the elixir it SHALL never poison.

#### Scenario: Chance one
- **WHEN** the poison chance is 1 and a player drinks the elixir
- **THEN** they have Poison, Nausea and Elemental Sight

#### Scenario: Chance zero
- **WHEN** the poison chance is 0 and a player drinks the elixir many times
- **THEN** they never have Poison or Nausea and always have Elemental Sight

#### Scenario: Shipped chance
- **WHEN** the shipped data is loaded
- **THEN** the chance is 15 % with 10 seconds of Poison and Nausea

### Requirement: Dose stacking
When the drinker still has Elemental Sight as they finish the drink, the poison chance SHALL be raised by an extra chance from the same data (example 15 percentage points), capped at 1.

#### Scenario: Drinking again under the sight
- **WHEN** the chance is 15 %, the extra chance 15 % and a player with Elemental Sight drinks again
- **THEN** that drink poisons with a chance of 30 %

### Requirement: Paracelsus scolds the poisoned
A poisoned player SHALL see a short action bar line in Paracelsus' voice about the dose. Nothing SHALL be said when the drink does not poison.

#### Scenario: Poisoned message
- **WHEN** a drink poisons a player
- **THEN** their action bar shows Paracelsus' line about the dose

### Requirement: The journal warns about the dose
The journal's Schwaz chapter and its Workshop entry for the Elixir of Sight SHALL say that the dose makes the poison and that the elixir can poison its drinker.

#### Scenario: Reading Schwaz
- **WHEN** a player reads the Schwaz chapter
- **THEN** it mentions that the dose makes the poison

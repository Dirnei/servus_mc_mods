# alchemy/calciner-eternal-fire Specification

## Purpose

Ignis Perpetuus set into the Firebox of a Calciner is an eternal flame: the Calciner then burns without fuel or upkeep and calcines faster.

## Requirements

### Requirement: Setting the eternal flame
Using Ignis Perpetuus on the Firebox of a formed Calciner that has none SHALL take one Ignis Perpetuus from the player's hand and set it into the Firebox. A Calciner SHALL hold at most one. Any fuel left in the fuel slot SHALL go to the player at the same time. Using Ignis Perpetuus on any other block of the structure, or on an unformed Firebox, SHALL do nothing.

#### Scenario: Set by hand
- **WHEN** a player holding two Ignis Perpetuus uses them on the Firebox of a formed Calciner without one
- **THEN** the player keeps one Ignis Perpetuus and the Calciner holds one

#### Scenario: Coal comes back
- **WHEN** a player sets Ignis Perpetuus into the Firebox of a formed Calciner with 5 coal in its fuel slot
- **THEN** the player receives the 5 coal and the fuel slot is empty

#### Scenario: Only one
- **WHEN** a player uses Ignis Perpetuus on the Firebox of a Calciner that already holds one
- **THEN** the player keeps it and nothing changes

#### Scenario: Not at the mouth
- **WHEN** a player uses Ignis Perpetuus on the Calciner block of a formed Calciner
- **THEN** nothing is set and the Ignis is not put into the input

#### Scenario: Not by pipe
- **WHEN** a hopper or Item Pipe offers Ignis Perpetuus to the Firebox of a formed Calciner
- **THEN** the Firebox takes nothing

### Requirement: The eternal flame burns without fuel
A formed Calciner holding Ignis Perpetuus SHALL burn whenever it has work, without using fuel and without ever going out. It SHALL take no fuel by hand or through any port and SHALL report no upkeep, so no data line sends it fuel. All other rules of a cycle (formed, recipe, room in the tanks and the Ash Pan) SHALL stay the same.

#### Scenario: No coal needed
- **WHEN** a formed Calciner holding Ignis Perpetuus and no fuel holds 64 cobblestone and its tanks and Ash Pan have room
- **THEN** it calcines cobblestone until the Ash Pan is full, and the Ignis Perpetuus is still there

#### Scenario: Coal refused
- **WHEN** a player uses coal on the Firebox of a formed Calciner holding Ignis Perpetuus
- **THEN** the player keeps the coal

#### Scenario: No upkeep
- **WHEN** a Machine Adapter touches the Firebox of a formed Calciner holding Ignis Perpetuus
- **THEN** the Calciner asks for no upkeep fuel

#### Scenario: Waits on full tanks
- **WHEN** the Mercury tank of a formed Calciner holding Ignis Perpetuus is full
- **THEN** the Calciner waits like a Calciner burning coal

### Requirement: The eternal flame calcines faster
A formed Calciner holding Ignis Perpetuus SHALL take a recipe's time divided by a speed factor from datapack JSON for each cycle, rounded down and at least one tick. The factor SHALL be at least 1. Without the file the factor SHALL be 1.

#### Scenario: Twice as fast
- **WHEN** the shipped data sets the speed factor to 2 and a Calciner holding Ignis Perpetuus calcines cobblestone whose recipe takes 80 ticks
- **THEN** each cobblestone takes 40 ticks

#### Scenario: Coal keeps the recipe time
- **WHEN** the same Calciner burns coal without Ignis Perpetuus
- **THEN** each cobblestone takes 80 ticks

### Requirement: Speed of the eternal flame is pure logic
The time of one cycle under the eternal flame SHALL be computed by pure Java from the recipe time and the speed factor, covered by JUnit.

#### Scenario: Never below one tick
- **WHEN** a recipe takes 1 tick and the speed factor is 4
- **THEN** the cycle takes 1 tick

#### Scenario: Odd time
- **WHEN** a recipe takes 81 ticks and the speed factor is 2
- **THEN** the cycle takes 40 ticks

### Requirement: Taking the eternal flame back
Using the Firebox of a formed Calciner with an empty hand while sneaking SHALL give its Ignis Perpetuus to the player; the Calciner then needs fuel again. A Calciner that is unformed while its Calciner block and Firebox stay in place SHALL keep its Ignis Perpetuus, SHALL NOT work, and SHALL burn with it again once formed.

#### Scenario: Take back
- **WHEN** a player sneaks and uses the Firebox of a formed Calciner holding Ignis Perpetuus with an empty hand
- **THEN** the player receives the Ignis Perpetuus and the Calciner stops until it gets fuel

#### Scenario: Kept while unformed
- **WHEN** a Stone Bricks block of a formed Calciner holding Ignis Perpetuus is broken and put back, and the Calciner is formed again
- **THEN** the Calciner still holds the Ignis Perpetuus and burns without fuel

### Requirement: The eternal flame shows
A formed Calciner holding Ignis Perpetuus SHALL show the Ignis Perpetuus glowing in the Firebox grate at all times, also while idle. Fire, light and smoke while working SHALL be the same as with fuel.

#### Scenario: Seen while idle
- **WHEN** a formed Calciner holds Ignis Perpetuus and nothing to calcine
- **THEN** the Ignis Perpetuus is seen glowing in the Firebox grate and no smoke rises

#### Scenario: Gone when taken
- **WHEN** the player takes the Ignis Perpetuus back
- **THEN** the grate no longer shows it

### Requirement: The Journal tells of the eternal flame
The Journal's Ignis Perpetuus page and its Calciner page SHALL say that Ignis Perpetuus set into a Calciner's Firebox burns it forever and makes it calcine faster, and how to take it back.

#### Scenario: Ignis page
- **WHEN** a player reads the Ignis Perpetuus page of the Journal
- **THEN** it says that it fires a Calciner forever and faster when set into the Firebox

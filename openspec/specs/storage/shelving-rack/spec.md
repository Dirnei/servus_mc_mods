# storage/shelving-rack Specification

## Purpose

The shelving rack is the warehouse's visible shelf: a few stacks of anything, each on display.

## Requirements

### Requirement: Four compartments
A Shelving Rack SHALL have four compartments arranged two by two on its front face, each holding one stack of any item up to that item's maximum stack size.

#### Scenario: Different items per compartment
- **WHEN** a player places iron ingots in one compartment and bread in another
- **THEN** both stay in their own compartments

### Requirement: Hand interaction by compartment
Right clicking a compartment while holding an item SHALL put the held stack into that compartment, merging with a matching stack and leaving the rest in hand. Right clicking a filled compartment with an empty hand SHALL take its stack; sneaking takes one item.

#### Scenario: Put and take
- **WHEN** a player right clicks the top left compartment with 10 bread, then right clicks it with an empty hand
- **THEN** the compartment held 10 bread and the player gets the 10 bread back

#### Scenario: Mismatch stays in hand
- **WHEN** a player right clicks a compartment holding bread while holding stone
- **THEN** nothing moves

### Requirement: Automation access
A rack SHALL expose a four slot item handler on every side. Insertion fills the first compatible compartment; extraction takes from any filled one.

#### Scenario: Hopper fills a rack
- **WHEN** a hopper above a rack offers 100 cobblestone
- **THEN** compartments fill to 64 and 36 and the rest waits in the hopper

### Requirement: Visible contents
Each filled compartment SHALL show its item on the front face, with the count beside it when above one, within a second of a change.

#### Scenario: Looking at a rack
- **WHEN** a compartment holds 12 bread
- **THEN** the front shows bread with the number 12 in that compartment's quarter

### Requirement: Breaking and dismantling
Breaking a rack SHALL spill its contents. Sneak wrench dismantling SHALL keep them on the dropped item.

#### Scenario: Dismantle
- **WHEN** a player sneak wrenches a rack with two stacks and places it again
- **THEN** the new rack holds the same two stacks

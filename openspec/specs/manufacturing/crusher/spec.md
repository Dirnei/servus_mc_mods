# manufacturing/crusher Specification

## Purpose

The Crusher multiplies ore yield: raw ore in, two grit out, grit smelts to ingots. Crushing recipes are data so packs can extend the chain.

## Requirements

### Requirement: Crushing recipes are data
A crushing recipe SHALL be a JSON file of type `servus_manufacturing:crushing` with one counted input, a result stack and a time in ticks. Built in recipes SHALL turn one raw iron into two iron grit and one raw copper into two copper grit (example values).

#### Scenario: Built in recipes
- **WHEN** the server loads
- **THEN** crushing recipes exist for raw iron, raw copper, cobblestone and gravel

#### Scenario: Datapack recipe
- **WHEN** a datapack adds a crushing recipe for another ore
- **THEN** after `/reload` the crusher processes it and JEI lists it

### Requirement: Crusher runs the recipe of its input
A Crusher SHALL accept only items with a crushing recipe, SHALL produce the recipe's result once per recipe time while powered and the output fits, spending its energy per tick while working.

#### Scenario: Raw iron
- **WHEN** raw iron is in the input with energy available
- **THEN** every recipe time two iron grit are added to the output and one raw iron is consumed

#### Scenario: Unknown item
- **WHEN** a hopper offers a stick
- **THEN** the crusher rejects it

### Requirement: Grit smelts to ingots
Iron grit and copper grit SHALL smelt and blast into the matching ingot, so the chain ore, crusher, smelter yields two ingots per ore.

#### Scenario: Smelter
- **WHEN** iron grit is in a Burner Smelter with fuel
- **THEN** iron ingots come out at the smelter's fixed rate

### Requirement: Obtained through research
The Crusher SHALL be assembled from an Electric Extractor, iron gears and copper wire, and the recipe SHALL require the crusher tech.

#### Scenario: Gated
- **WHEN** a team without the crusher tech adds the recipe to an assembler
- **THEN** it is refused with the research message

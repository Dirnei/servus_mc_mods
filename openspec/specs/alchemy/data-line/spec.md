# alchemy/data-line Specification

## Purpose
Alchemy machines take part in on-demand production over the data line: burner machines get their fuel kept stocked as upkeep, and the Burner Smelter and Crusher run order steps for the Crafting CPU like the Assembler.

## Requirements

### Requirement: Calciner is no producer
The Calciner SHALL NOT expose the production capability: it makes no item a Crafting CPU could order. It is fed by pipes, hoppers or by hand.

#### Scenario: Not on the order list
- **WHEN** a duct with the data line touches a Calciner
- **THEN** the Calciner is no producer member of the data network

### Requirement: Coagulator is a producer
The Coagulator SHALL expose the production capability with exactly the recipe its seed grows, when the owner's team may grow it: the item ingredient (if any) and one seed in, what that seed grows out (the result, plus one for a seed that is the result itself) and the recipe time as ticks. Without a seed, or with a locked one, it SHALL report no recipe. Principles are not part of the reported inputs; they reach the Coagulator by pipe.

#### Scenario: Order Salt Crystals
- **WHEN** a Crafting CPU plans 4 Salt Crystals on a line with a Coagulator holding a Kandis seed and enough Salt piped into it
- **THEN** the step runs on the Coagulator and the 4 crystals are collected into storage

#### Scenario: No seed, no producer
- **WHEN** the seed is taken out of a Coagulator on a data line
- **THEN** it reports no recipe to the Crafting CPU

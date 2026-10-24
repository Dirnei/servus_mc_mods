## MODIFIED Requirements

### Requirement: Coagulator is a producer
The Coagulator SHALL expose the production capability with exactly the recipe its seed grows, when the owner's team may grow it: the item ingredient (if any) and one seed in, what that seed grows out (the result, plus one for a seed that is the result itself) and the recipe time as ticks. Without a seed, or with a locked one, it SHALL report no recipe. Principles are not part of the reported inputs; they reach the Coagulator by pipe.

#### Scenario: Order Salt Crystals
- **WHEN** a Crafting CPU plans 4 Salt Crystals on a line with a Coagulator holding a Kandis seed and enough Salt piped into it
- **THEN** the step runs on the Coagulator and the 4 crystals are collected into storage

#### Scenario: No seed, no producer
- **WHEN** the seed is taken out of a Coagulator on a data line
- **THEN** it reports no recipe to the Crafting CPU

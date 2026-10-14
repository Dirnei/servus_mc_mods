# Spec Delta

## MODIFIED Requirements

### Requirement: Athanor crafts are data
An Athanor craft SHALL be a datapack recipe of type `servus_alchemy:athanor` with one counted item input, the principles it takes, a minimum tier (default 0), one result and a time in ticks. The item in the Athanor's input slot SHALL pick the craft.

#### Scenario: Blaze Powder
- **WHEN** the shipped data holds a craft of one gunpowder and 200 mB Sulfur for one Blaze Powder in 100 ticks at tier 0
- **THEN** an Athanor with gunpowder on it and 200 mB Sulfur in its tank makes one Blaze Powder in 5 seconds

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_alchemy/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0. The shipped Albedo tier SHALL be twelve Salt Crystal Blocks in the hearth layer, filling the corners of the 5 by 5 square around the diamond hearth, so nothing stands next to the Athanor on its own level.

#### Scenario: Albedo ring
- **WHEN** twelve Salt Crystal Blocks fill the hearth layer of a formed Athanor out to a 5 by 5 square
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

### Requirement: Salt Crystal Block
Alchemy SHALL add a Salt Crystal Block. It SHALL be grown only on the Athanor, from one Salt Crystal and Salt from the tanks of its build (example 1,000 mB) at tier 0. It SHALL NOT be crafted from Salt Crystals at a crafting table and SHALL NOT be crafted back into Salt Crystals.

#### Scenario: Block recipe
- **WHEN** a Salt Crystal lies on a formed Athanor whose tanks hold the Salt
- **THEN** one Salt Crystal Block is made

#### Scenario: Not at the table
- **WHEN** a player puts nine Salt Crystals into a crafting table
- **THEN** no Salt Crystal Block is offered

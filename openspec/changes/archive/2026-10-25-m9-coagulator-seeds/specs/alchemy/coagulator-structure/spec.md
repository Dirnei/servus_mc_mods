## MODIFIED Requirements

### Requirement: Items at the Coagulator
A formed Coagulator SHALL take, through the item capability on every face of the Coagulator block, the ingredient of its seed's recipe into the ingredient slot and seeds into the seed slot, any seed into an empty slot and more of the same item up to its stack size into a filled one, and give its products from there. A different seed SHALL be refused while the seed slot holds one. An item that the seed's recipe takes as its ingredient SHALL go into the ingredient slot first. It SHALL take nothing into the output and give nothing from the ingredient or the seed slot; the seed comes out only by hand. Using seed items on a formed Coagulator SHALL put in as many of them as the seed slot takes; sneaking with an empty hand on a formed Coagulator SHALL give all seeds back. The other blocks SHALL expose no item capability.

#### Scenario: Hopper takes the product
- **WHEN** a hopper under the Coagulator of a formed structure and a Salt Crystal in its output
- **THEN** the hopper receives the Salt Crystal

#### Scenario: Pipe sets the seed
- **WHEN** a pipe pushes 16 Kandis into a formed Coagulator without a seed
- **THEN** all 16 Kandis go into the seed slot

#### Scenario: Pipe adds seeds
- **WHEN** a pipe pushes 4 Kandis into a formed Coagulator whose seed slot holds 16 Kandis
- **THEN** the seed slot holds 20 Kandis

#### Scenario: Another seed refused
- **WHEN** a pipe pushes Brimstone into a formed Coagulator whose seed slot holds Kandis
- **THEN** the pipe keeps the Brimstone

#### Scenario: Pipes never take the seed
- **WHEN** a hopper under a formed Coagulator pulls items
- **THEN** it never takes the seed

#### Scenario: Seed by hand
- **WHEN** a player sneaks and uses a formed Coagulator holding 3 Cinnabar seeds with an empty hand
- **THEN** the player receives the 3 Cinnabar and the seed slot is empty

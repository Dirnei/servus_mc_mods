## MODIFIED Requirements

### Requirement: Athanor schematics
The Athanor entry SHALL show the Athanor on its hearth with its step, its three Fluid Tanks three high and its three Copper Ducts as a multiblock page with Visualize and SHALL tell the player to form it with the Journal. Every shipped tier SHALL have its own multiblock page, on the entry of its stage (Albedo on the Salt Crystal Block entry, Citrinitas on the Amethyst ring entry, Rubedo on the Crying Obsidian entry), showing the hearth, the Athanor, its tanks, its Copper Ducts and the blocks of that tier and the tiers below. A tier grown by a ritual SHALL show the blocks the player places, and its entry SHALL tell the player which offering casts the ritual. The schematics SHALL stand on the clicked ground.

#### Scenario: Albedo page
- **WHEN** a player opens the Salt Crystal Block entry and presses Visualize
- **THEN** a ghost of the salt ring with the hearth, the Athanor, its tanks and its ducts on it stands on the clicked ground

#### Scenario: Citrinitas page
- **WHEN** a player opens the Amethyst ring entry
- **THEN** it shows the Albedo build with an empty Principle Vat beside each tank and tells the player to use an Amethyst Block on the Athanor

#### Scenario: Rubedo page
- **WHEN** a player opens the Crying Obsidian entry and presses Visualize
- **THEN** a ghost of the whole build with the salt ring, the vats beside the tanks and Obsidian on the four pillar cells stands on the clicked ground, and the entry tells the player to use a Ghast Tear on the Athanor

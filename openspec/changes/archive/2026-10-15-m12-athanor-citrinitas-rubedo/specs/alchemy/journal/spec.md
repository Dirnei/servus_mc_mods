## MODIFIED Requirements

### Requirement: Workshop tutorial
The journal SHALL have a Workshop category beside the story with an entry per alchemy machine and topic: the Principles, the Calciner, the Purifier, the Coagulator, the Athanor, pipes and fluid sides, for Albedo the Salt Crystal Block, the Elixir of Sight and the Gnome Hollow, for Citrinitas the Amethyst ring with the noble veins, and for Rubedo the Crying Obsidian pillars. A machine entry SHALL show the machine, its crafting recipe and how to use it. Nigredo entries SHALL be open from the start; Albedo entries SHALL unlock with the Schwaz chapter, Citrinitas entries with the Wandering years chapter and Rubedo entries with the Basel and Salzburg chapter.

#### Scenario: Calciner entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Calciner entry shows the Calciner, its crafting recipe and how to fuel and empty it

#### Scenario: Purifier entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Purifier entry shows the Purifier build, the crafting recipes of the Vat, the Basin and the Residue Crock, how Ash and water become Salt and Caput Mortuum, and the purification recipes

#### Scenario: Albedo entries locked
- **WHEN** a player has not finished Villach
- **THEN** the Salt Crystal Block, Elixir of Sight and Gnome Hollow entries are locked

#### Scenario: Citrinitas entry opens
- **WHEN** a player receives the Schwaz advancement
- **THEN** the Workshop entry of the Amethyst ring unlocks, and the Crying Obsidian entry stays locked

### Requirement: Athanor schematics
The Athanor entry SHALL show the Athanor on its hearth with its three Fluid Tanks and six Copper Ducts as a multiblock page with Visualize and SHALL tell the player to form it with the Journal. Every shipped tier SHALL have its own multiblock page, on the entry of its stage (Albedo on the Salt Crystal Block entry, Citrinitas on the Amethyst ring entry, Rubedo on the Crying Obsidian entry), showing the hearth, the Athanor, its tanks, its Copper Ducts and the blocks of that tier and the tiers below. The schematics SHALL stand on the clicked ground.

#### Scenario: Albedo page
- **WHEN** a player opens the Salt Crystal Block entry and presses Visualize
- **THEN** a ghost of the diamond hearth with the twelve Salt Crystal Blocks around it, the Athanor and its tanks stands on the clicked ground

#### Scenario: Rubedo page
- **WHEN** a player opens the Crying Obsidian entry and presses Visualize
- **THEN** a ghost of the whole build with the salt ring, the Amethyst ring and the four Crying Obsidian pillars stands on the clicked ground

## ADDED Requirements

### Requirement: Citrinitas and Rubedo chapters
Alchemy SHALL ship two chapters after Schwaz, each with tasks of the two kinds "do N times" and "for the first time" counted from existing events:
- Wandering years (Citrinitas), parent Schwaz: one craft on a Citrinitas Athanor, bind a gnome to a gold vein, bind four gnomes.
- Basel and Salzburg (Rubedo), parent Wandering years: one craft on a Rubedo Athanor, bind a gnome to a diamond vein, finish sixteen Athanor crafts.
Each SHALL grant its advancement when finished and SHALL have a journal entry that unlocks with the parent's advancement and tells its story, lists its tasks and what it opens.

#### Scenario: Citrinitas craft counts
- **WHEN** a team with Wandering years open finishes a craft on a Citrinitas Athanor
- **THEN** the task "craft on a Citrinitas Athanor" of Wandering years is done

#### Scenario: Not before Schwaz
- **WHEN** a team that has not finished Schwaz finishes a craft on a Citrinitas Athanor
- **THEN** nothing counts for Wandering years

#### Scenario: Wandering years finished
- **WHEN** a team with Wandering years open crafts on a Citrinitas Athanor, binds a gnome to a gold vein and has bound four gnomes in the chapter
- **THEN** Wandering years is finished and Basel and Salzburg opens

#### Scenario: Entry unlocks
- **WHEN** a player receives the Wandering years advancement
- **THEN** the Basel and Salzburg entry of their journal unlocks

### Requirement: Story pages are placeholders
The story pages of Wandering years and Basel and Salzburg SHALL be short texts in Paracelsus' voice, a confused and ranting diary, written as language keys through datagen. Until the storyteller replaces them, each story text SHALL start with `[storyteller]` so it reads as a placeholder in game. Task and unlock pages SHALL be plain instructions without the marker.

#### Scenario: Placeholder
- **WHEN** a player opens the Wandering years entry
- **THEN** its story page starts with "[storyteller]" and its task page lists the three tasks

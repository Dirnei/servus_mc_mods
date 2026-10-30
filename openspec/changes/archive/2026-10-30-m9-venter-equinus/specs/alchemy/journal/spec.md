# Spec Delta

## MODIFIED Requirements

### Requirement: Workshop tutorial
The journal SHALL have a Workshop category beside the story with an entry per alchemy machine and topic: the Principles, the Calciner, the Purifier, the Venter Equinus, the Coagulator, the Athanor, pipes and fluid sides, for Albedo the Salt Crystal Block, the Elixir of Sight and the Gnome Hollow, for Citrinitas the Amethyst ring with the noble veins, and for Rubedo the Crying Obsidian pillars. A machine entry SHALL show the machine, its crafting recipe and how to use it. Nigredo entries SHALL be open from the start; Albedo entries SHALL unlock with the Schwaz chapter, Citrinitas entries with the Wandering years chapter and Rubedo entries with the Basel and Salzburg chapter.

#### Scenario: Calciner entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Calciner entry shows the Calciner, its crafting recipe and how to fuel and empty it

#### Scenario: Purifier entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Purifier entry shows the Purifier build, the crafting recipes of the Vat, the Basin and the Residue Crock, how Ash and water become Salt and Caput Mortuum, and the purification recipes

#### Scenario: Venter Equinus entry
- **WHEN** a new player opens the Workshop category
- **THEN** the Venter Equinus entry is open and shows the bed, the crafting recipes of the Cucurbit, the Dung Bed and the Bed Hatch, how rot and Hay Bales go in and Humus comes out, the putrefaction recipes and the Dirt recipes for the Mixer

#### Scenario: Albedo entries locked
- **WHEN** a player has not finished Villach
- **THEN** the Salt Crystal Block, Elixir of Sight and Gnome Hollow entries are locked

#### Scenario: Citrinitas entry opens
- **WHEN** a player receives the Schwaz advancement
- **THEN** the Workshop entry of the Amethyst ring unlocks, and the Crying Obsidian entry stays locked

### Requirement: Forming shows itself
When Paracelsus' Journal forms an alchemy build (Calciner, Purifier, Venter Equinus, Coagulator, Athanor), a sound SHALL play at the build's controller and particles SHALL rise from every block of the build. A failed attempt SHALL show no effect, only the message naming the missing block.

#### Scenario: Formed with effect
- **WHEN** a player right clicks the controller of a complete build with the Journal
- **THEN** the build forms, a sound plays and particles rise from every block of it

#### Scenario: Missing block
- **WHEN** a player right clicks the controller of an incomplete build with the Journal
- **THEN** no sound plays, no particles rise and the player sees the missing block

#### Scenario: Bed forms with effect
- **WHEN** a player forms a Venter Equinus with the Journal
- **THEN** particles rise from all nine blocks of the bed

## ADDED Requirements

### Requirement: The horse's belly in Paracelsus' voice
The Venter Equinus entry SHALL tell, in Paracelsus' confused diary voice, that alchemists bedded sealed vessels in warm horse dung for putrefaction, and SHALL hint that he used the horse's belly for other, secret work without naming the homunculus or what grew there.

#### Scenario: No secret told
- **WHEN** a player reads every page of the Venter Equinus entry
- **THEN** the word "homunculus" does not appear and no page says what Paracelsus putrefied in secret

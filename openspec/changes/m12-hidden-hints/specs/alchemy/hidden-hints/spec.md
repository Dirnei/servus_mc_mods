## ADDED Requirements

### Requirement: Knock sound
Alchemy SHALL register the sound event `servus_alchemy:hint.knock`, made from vanilla stone sounds at a low pitch, with the subtitle "Something knocks in the rock".

#### Scenario: Subtitle
- **WHEN** a player with subtitles on hears the knock sound
- **THEN** the subtitle reads "Something knocks in the rock"

### Requirement: Working gnome knocks
A Gnome Hollow whose gnome is bound SHALL now and then play the knock sound softly at its position for every nearby player, with or without Elemental Sight. The sound SHALL be played by the client from the block's animation tick, so no server work and no block scan is involved. A Hollow without a bound gnome SHALL stay silent.

#### Scenario: Bound gnome
- **WHEN** a player stands for a minute next to a Hollow with a bound gnome
- **THEN** they hear soft knocking from the Hollow now and then

#### Scenario: No gnome
- **WHEN** a Hollow has no bound gnome
- **THEN** it never knocks

### Requirement: Salamander flame dances
A wild salamander SHALL now and then show, besides its single flame, a short dancing flame: a few flame particles circling up above it, visible without Elemental Sight. A befriended salamander SHALL show neither.

#### Scenario: Dancing flame
- **WHEN** a player without the sight watches a wild salamander in lava for a while
- **THEN** a flame now and then circles up from the lava where it swims

### Requirement: Vas liquid stirs
The liquid of a Vas Hermeticum whose homunculus is not freed SHALL now and then stir by itself: a few bubbles and a small splash on its surface, visible to every player with or without Elemental Sight. A freed Vas SHALL show nothing.

#### Scenario: Stirring
- **WHEN** a player without the sight watches a Vas with an unfreed homunculus
- **THEN** its liquid bubbles now and then

### Requirement: Old gnome settings
The old gnome's numbers SHALL be datapack data in `data/<namespace>/servus_alchemy/old_gnome/old_gnome.json`: wake chance per broken block, wake chapter, the height below which deepslate wakes him, knocking range and interval, dust range, trace range and trace chance. Without the file the built in defaults SHALL apply. Shipped examples: chance 0.003, chapter `servus_alchemy:schwaz`, below Y 0, knocking within 24 blocks every 100 ticks, dust within 6 blocks, traces within 8 blocks with chance 0.15.

#### Scenario: Data changes the chance
- **WHEN** a datapack sets the wake chance to 1
- **THEN** the first fitting deepslate block a team breaks after its wake chapter wakes the old gnome

### Requirement: Old gnome wakes while mining
When a player breaks a block in the tag `servus_alchemy:wakes_old_gnome` (Deepslate) below the configured height, and the player's team has finished the wake chapter and has no old gnome yet, the break SHALL with the wake chance wake the old gnome. The mod SHALL check a fixed, ordered list of positions within a few blocks of the broken block (the start direction chosen at random) and turn the first one that holds a block of the same tag and has air on a horizontal side into an Ancient Hollow open toward that air. If no position fits, nothing SHALL happen and a later break may try again. Nothing SHALL scan the world.

#### Scenario: Wakes after the chapter
- **WHEN** a team that finished Schwaz breaks deepslate in a tunnel below Y 0 and the chance hits
- **THEN** a deepslate block in the tunnel wall a few blocks away becomes an Ancient Hollow open toward the tunnel

#### Scenario: Not before the chapter
- **WHEN** a team that has not finished Schwaz breaks deepslate with a chance of 1
- **THEN** no Ancient Hollow appears

#### Scenario: Not above the height
- **WHEN** a team that finished Schwaz breaks deepslate above Y 0
- **THEN** no Ancient Hollow appears

### Requirement: One old gnome per team
Each team SHALL have at most one Ancient Hollow, stored in world data with its position and dimension. While it stands, further breaks SHALL NOT wake another one. Once the team has found the old gnome, he SHALL never wake again, even if the block is broken. If the Ancient Hollow is broken before he is found, the team SHALL be able to wake him again.

#### Scenario: Only once
- **WHEN** a team with an Ancient Hollow breaks more deepslate with a chance of 1
- **THEN** no second Ancient Hollow appears

#### Scenario: Lost before found
- **WHEN** the Ancient Hollow is broken before the team found the old gnome
- **THEN** a later break may wake him again in a new place

#### Scenario: Found stays found
- **WHEN** a team found the old gnome and the block is broken
- **THEN** no break ever wakes him again for that team

### Requirement: Ancient Hollow looks like deepslate
The Ancient Hollow SHALL look like Deepslate to everyone, show as Deepslate in Jade and give Deepslate on pick block to a player without Elemental Sight, mine like Deepslate and drop Cobbled Deepslate. It has no item, screen or capability.

#### Scenario: Without the sight
- **WHEN** a player without the sight looks at an Ancient Hollow
- **THEN** it looks like Deepslate and Jade names it Deepslate

### Requirement: Ancient Hollow knocks and drops dust
While the old gnome is not found, the Ancient Hollow SHALL knock (three knocks of the knock sound at its position, so the sound comes from its direction) about once per knocking interval while a player is within knocking range, and SHALL let deepslate dust fall in front of its open face for every player within the dust range.

#### Scenario: Knocking
- **WHEN** a player stands within 24 blocks of an Ancient Hollow
- **THEN** they hear knocking from its direction every few seconds

#### Scenario: Dust
- **WHEN** a player comes within 6 blocks of the Ancient Hollow
- **THEN** dust trickles from the one block

#### Scenario: Quiet once found
- **WHEN** the team found the old gnome
- **THEN** the Ancient Hollow neither knocks nor drops dust

### Requirement: Traces
Alchemy SHALL add three trace items, a Tiny Pickaxe, a Scrap of Red Felt and a Bowl of Salt, in the item tag `servus_alchemy:hint_traces`. When the old gnome wakes, one trace SHALL lie in front of the Ancient Hollow's open face. While he is not found, each block of `servus_alchemy:wakes_old_gnome` a player of his team breaks within the trace range of the Ancient Hollow SHALL with the trace chance drop one random trace. Picking up a trace SHALL record a `find` event with the trace item for the player's team and show the journal line "They are near. Drink, and look."

#### Scenario: A trace when he wakes
- **WHEN** the old gnome wakes
- **THEN** a trace item lies in front of the Ancient Hollow

#### Scenario: Picking one up
- **WHEN** a player picks up a Scrap of Red Felt
- **THEN** a `find` event counts for their team and the line "They are near. Drink, and look." shows

### Requirement: The old gnome under the sight
Under Elemental Sight the Ancient Hollow SHALL show the old gnome in front of its open face: the red cap of the Hollow gnome and a long white beard. Using Paracelsus' Journal on it under the sight SHALL show a dialogue line marked as a placeholder (`[storyteller] ...`), record the `find` event `servus_alchemy:old_gnome` for the player's team and mark the owning team's old gnome as found. Without the sight the journal SHALL do nothing special on it.

#### Scenario: Finding him
- **WHEN** a player under the sight uses the journal on the Ancient Hollow
- **THEN** the placeholder dialogue line shows, the `find` event `servus_alchemy:old_gnome` counts and the old gnome is found

#### Scenario: Without the sight
- **WHEN** a player without the sight uses the journal on it
- **THEN** no dialogue shows and nothing is found

### Requirement: Workshop entry on the hints
The Workshop SHALL have an entry "Knocking in the Rock", open with the Albedo entries, that explains the three steps of hints (knocking, dust, traces) and which hidden thing shows which hint.

#### Scenario: Entry
- **WHEN** a player with Villach finished opens the Workshop
- **THEN** the entry "Knocking in the Rock" is there

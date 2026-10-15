## Purpose

The salamander is Alchemy's fire elemental: a hidden pet from the Nether's lava that turns gold into Sal Ammoniac, the ingredient of the eternal flame Ignis Perpetuus.

## ADDED Requirements

### Requirement: Where the salamander lives
Alchemy SHALL add a salamander creature that spawns mainly in the Crimson Forest, a bit less in the Warped Forest and rarely on lava in the Nether Wastes and the Basalt Deltas. In the forests it SHALL spawn only on the ground within a home distance from data (example 4 blocks) of a block in the block tag `servus_alchemy:salamander_home` (crimson and warped stems, both wart blocks, the Nether Wart plant). On lava it SHALL spawn only on a lava block whose lava column is topped by air. Weights and group sizes SHALL be data in a biome modifier.

#### Scenario: Among the crimson stems
- **WHEN** the Nether spawns creatures on Crimson Nylium two blocks from a Crimson Stem
- **THEN** a salamander may spawn there, about as often as a Strider on a lava lake

#### Scenario: Warped Forest
- **WHEN** the Nether spawns creatures on Warped Nylium next to a Warped Stem
- **THEN** a salamander may spawn there, a bit less often than in the Crimson Forest

#### Scenario: Far from the trees
- **WHEN** the Nether tries to spawn a salamander on Crimson Nylium with no home block within the home distance
- **THEN** no salamander spawns there

#### Scenario: Spawning on a lava lake
- **WHEN** the Nether spawns creatures near a lava lake in the Nether Wastes
- **THEN** a salamander may spawn on the lava, rarer than in the forests and rarer than a Strider

#### Scenario: Not in the Overworld
- **WHEN** creatures spawn in an Overworld biome
- **THEN** no salamander spawns

### Requirement: Not overcrowded
A wild salamander SHALL NOT spawn when a crowd limit from data (example 3) of wild salamanders already lives within a crowd radius from data (example 32 blocks) of the spawn spot. Befriended salamanders SHALL NOT count.

#### Scenario: Crowd full
- **WHEN** three wild salamanders live within 32 blocks of a spawn spot in the Crimson Forest
- **THEN** no further salamander spawns there

#### Scenario: Pets do not count
- **WHEN** three befriended salamanders sit within 32 blocks of the spot
- **THEN** a wild salamander may still spawn there

#### Scenario: Pure crowd check
- **WHEN** the crowd check gets 2 wild salamanders in range and a limit of 3
- **THEN** it allows the spawn, and with 3 it refuses, computed by pure Java covered by JUnit

### Requirement: Fire immune and harmless
A salamander SHALL be immune to fire and lava damage, SHALL walk on the lava surface and on land, SHALL never attack, and SHALL not burn.

#### Scenario: In lava
- **WHEN** a salamander stands in lava or in fire for a while
- **THEN** it takes no damage and is not on fire

#### Scenario: Harmless
- **WHEN** a player walks up to or hits a salamander
- **THEN** the salamander does not attack

### Requirement: Hidden until befriended
A wild salamander SHALL not be drawn for a player without Elemental Sight: no model, no shadow, no name. Jade SHALL not name a wild salamander for such a player. A wild salamander SHALL now and then give off a flame particle that every player sees. A befriended salamander SHALL be drawn for every player.

#### Scenario: Without the sight
- **WHEN** a player without Elemental Sight looks at a wild salamander among the crimson stems or on the lava
- **THEN** they see no salamander, only a flame flickering there now and then

#### Scenario: Under the sight
- **WHEN** a player with Elemental Sight looks at the same salamander
- **THEN** they see the salamander

#### Scenario: Befriended
- **WHEN** a player without Elemental Sight looks at a befriended salamander
- **THEN** they see it

### Requirement: Befriending with gold
Right clicking a wild salamander with a Gold Ingot SHALL use up one ingot and befriend the salamander with a chance from data (example 1 in 3). On success the player SHALL become its owner and hearts SHALL show; on failure smoke SHALL show. The chance SHALL be data.

#### Scenario: Lucky ingot
- **WHEN** a player right clicks a wild salamander with a Gold Ingot and the roll succeeds
- **THEN** the ingot is used up, the salamander is befriended and owned by that player, and hearts show

#### Scenario: Unlucky ingot
- **WHEN** the roll fails
- **THEN** the ingot is used up, the salamander stays wild and smoke shows

### Requirement: A befriended salamander is a pet
A befriended salamander SHALL follow its owner and teleport near them when it falls far behind, SHALL sit or stand up when its owner right clicks it with an empty hand, SHALL never despawn, and when idle SHALL walk to a lit campfire nearby (example 8 blocks) and lie in it. A wild salamander SHALL despawn like other wild creatures far from players.

#### Scenario: Sit
- **WHEN** the owner right clicks their standing salamander with an empty hand
- **THEN** it sits and stays; a second click makes it stand and follow again

#### Scenario: Stranger
- **WHEN** another player right clicks the salamander with an empty hand
- **THEN** nothing changes

#### Scenario: Campfire
- **WHEN** a befriended salamander that is not sitting idles near a lit campfire
- **THEN** it walks into the campfire and lies there

#### Scenario: No despawn
- **WHEN** the owner moves far away from their salamander
- **THEN** it does not despawn

### Requirement: Digesting gold into Sal Ammoniac
The owner SHALL feed a befriended salamander a Gold Ingot by right clicking it. The salamander SHALL take one ingot only while it digests none, and after a digest time from data (example 60 s) SHALL drop one Sal Ammoniac. Digestion SHALL continue across saving and loading.

#### Scenario: Feeding
- **WHEN** the owner right clicks their idle salamander with a Gold Ingot
- **THEN** one ingot is used up, and after the digest time the salamander drops one Sal Ammoniac

#### Scenario: One at a time
- **WHEN** the owner right clicks it with a Gold Ingot while it still digests
- **THEN** the ingot is not used up

### Requirement: Salamander numbers are data
The befriend chance, the digest time, the home distance, the crowd radius and the crowd limit SHALL come from a datapack file per entity type. Without a file the salamander SHALL use built in defaults.

#### Scenario: Pack changes the chance
- **WHEN** a datapack sets the befriend chance to 1
- **THEN** every Gold Ingot befriends a wild salamander

#### Scenario: Pack changes the crowd
- **WHEN** a datapack sets the crowd limit to 1
- **THEN** no wild salamander spawns within the crowd radius of another wild one

### Requirement: Ignis Perpetuus
Alchemy SHALL add Ignis Perpetuus, an item made on an Athanor of at least the Citrinitas tier from one Sal Ammoniac and Sulfur. It SHALL be a building part and SHALL not burn as a furnace fuel.

#### Scenario: Citrinitas craft
- **WHEN** Sal Ammoniac lies on a Citrinitas Athanor with enough Sulfur in its tanks
- **THEN** the Athanor makes one Ignis Perpetuus and burns the Sulfur

#### Scenario: Not below Citrinitas
- **WHEN** Sal Ammoniac lies on an Albedo Athanor with the same principles
- **THEN** nothing is made

#### Scenario: Not a fuel
- **WHEN** a player puts Ignis Perpetuus into a furnace's fuel slot
- **THEN** it does not burn

### Requirement: Paracelsus' tooltips
Sal Ammoniac and Ignis Perpetuus SHALL show a line in Paracelsus' voice in their tooltips.

#### Scenario: Ignis Perpetuus tooltip
- **WHEN** a player hovers over Ignis Perpetuus
- **THEN** the tooltip says it burns forever and not to ask what it is

### Requirement: Spawn egg
Alchemy SHALL have a Salamander spawn egg that spawns a wild salamander.

#### Scenario: Egg
- **WHEN** a creative player uses the Salamander spawn egg on the ground
- **THEN** a wild salamander appears

### Requirement: Workshop entry
The Workshop SHALL have a salamander entry that unlocks with the Wandering years chapter and tells where salamanders live, that only Elemental Sight shows them, how gold befriends and feeds them, and how Sal Ammoniac and Sulfur make Ignis Perpetuus on a Citrinitas Athanor. Befriending a salamander SHALL stay optional for the chapters.

#### Scenario: Entry unlocks
- **WHEN** a player receives the Schwaz advancement
- **THEN** the salamander entry of the Workshop unlocks

## ADDED Requirements

### Requirement: Loaded while powered
An active Ender Anchor SHALL keep its own chunk loaded and ticking and SHALL spend its energy per tick while active. Without energy it SHALL release the chunk until energy returns.

#### Scenario: Overnight
- **WHEN** a powered Ender Anchor stands in a chunk with producing machines and no player is nearby
- **THEN** the machines keep producing

#### Scenario: Power lost
- **WHEN** the anchor's buffer runs dry
- **THEN** the chunk is released and machines stop until power returns

### Requirement: Per team cap
A team SHALL have at most `maxEnderAnchorsPerTeam` active anchors (config, default 4). An anchor beyond the cap SHALL stay inactive and show the reason in its GUI.

#### Scenario: Fifth anchor
- **WHEN** a team already has four active anchors and places a fifth
- **THEN** the fifth stays inactive with the status "team limit reached"

### Requirement: FTB Chunks respected
When FTB Chunks is installed and `enderAnchorRespectsFtbChunks` is true (default), Ender Anchors SHALL stay inactive and show a status pointing to FTB Chunks.

#### Scenario: FTB Chunks present
- **WHEN** FTB Chunks is installed with the default config
- **THEN** a placed Ender Anchor is inactive and its GUI says to use FTB Chunks

### Requirement: Tickets survive and are cleaned up
Chunk tickets SHALL survive a restart while the anchor stays active, and SHALL be released when the anchor is broken or dismantled.

#### Scenario: Removed
- **WHEN** an active anchor is broken
- **THEN** its chunk is no longer force loaded by it

### Requirement: Crafting
The Ender Anchor SHALL be crafted at a crafting table from an ender pearl, iron ingots, copper ingots, redstone and obsidian, with obsidian in the two bottom corners, and SHALL need no research.

#### Scenario: Recipe
- **WHEN** the player crafts the Ender Anchor pattern at a crafting table
- **THEN** one Ender Anchor is produced, without an assembler

#### Scenario: Old name gone
- **WHEN** a player searches the creative inventory or JEI for "Chunk Loader"
- **THEN** only the Storage Chunk Loader Upgrade is found, and the block is listed as "Ender Anchor"

### Requirement: Ender Anchor look
The Ender Anchor SHALL be a full block with obsidian panels in a dark steel frame, an ender eye lens in the middle of its front, a chunk grid engraved on its sides and purple accents on its top that brighten while the anchor is active. It SHALL show no coloured bar and no yellow window. The item SHALL show the block with the eye shut.

#### Scenario: Placed
- **WHEN** a player places an Ender Anchor
- **THEN** it shows the obsidian panels, the eye lens on the side facing the player and the chunk grid on the other sides

#### Scenario: Item
- **WHEN** the player looks at an Ender Anchor in the inventory
- **THEN** it shows the anchor with the eye shut

### Requirement: Eye shows whether the chunk is held
The eye SHALL be shut while the anchor does not hold its chunk, whatever the reason (no power, team cap, FTB Chunks, no owner). When the anchor becomes active the eye SHALL open with a short animation of about a second; when it stops being active the eye SHALL close the same way. While active, the eye SHALL blink now and then at irregular intervals of several seconds, each blink short, and anchors side by side SHALL NOT blink in step. The eye is look only and does not change what the anchor does.

#### Scenario: Placed with power
- **WHEN** a player places an Ender Anchor next to a powered cable within the team cap
- **THEN** the eye opens within about a second and stays open

#### Scenario: Power runs out
- **WHEN** the anchor's energy runs out and it releases the chunk
- **THEN** the eye closes

#### Scenario: Over the team cap
- **WHEN** an anchor stays inactive because the team cap is reached
- **THEN** its eye stays shut

#### Scenario: Blinking
- **WHEN** a player watches an active anchor for half a minute
- **THEN** the eye blinks several times at uneven intervals

#### Scenario: Not in step
- **WHEN** two active anchors stand side by side
- **THEN** they do not blink at the same moments

### Requirement: Eye follows the nearest player
While the anchor is active, the iris SHALL turn toward the nearest player within a few blocks (example 8) who is in front of the eye, moving smoothly and staying inside the lens. With no such player the iris SHALL drift back to the centre. While the eye is shut nothing follows.

#### Scenario: Walking past
- **WHEN** a player walks from the left to the right in front of an active anchor at 4 blocks
- **THEN** the iris follows from the left to the right side of the lens

#### Scenario: Nobody near
- **WHEN** the nearest player is 20 blocks away
- **THEN** the iris rests in the centre

#### Scenario: Behind the anchor
- **WHEN** the only nearby player stands behind the anchor
- **THEN** the iris rests in the centre

### Requirement: Portal particles while active
While the anchor is active, a few faint purple portal particles SHALL drift around the block; while it is not active none SHALL show. The particles are look only.

#### Scenario: Active
- **WHEN** an anchor holds its chunk
- **THEN** a few purple particles drift around it now and then

#### Scenario: Inactive
- **WHEN** the anchor has no power
- **THEN** no particles show

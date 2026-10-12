## ADDED Requirements

### Requirement: Ender Anchor look
The Chunk Loader SHALL look like an ender anchor: a full block with obsidian panels in a dark steel frame, an ender eye lens in the middle of its front, a chunk grid engraved on its sides and purple accents on its top that brighten while the loader is active. It SHALL show no coloured bar and no yellow window. The item SHALL show the block with the eye shut.

#### Scenario: Placed
- **WHEN** a player places a Chunk Loader
- **THEN** it shows the obsidian panels, the eye lens on the side facing the player and the chunk grid on the other sides

#### Scenario: Item
- **WHEN** the player looks at a Chunk Loader in the inventory
- **THEN** it shows the anchor with the eye shut

### Requirement: Eye shows whether the chunk is held
The eye SHALL be shut while the loader does not hold its chunk, whatever the reason (no power, team cap, FTB Chunks, no owner). When the loader becomes active the eye SHALL open with a short animation of about a second; when it stops being active the eye SHALL close the same way. While active, the eye SHALL blink now and then at irregular intervals of several seconds, each blink short, and loaders side by side SHALL NOT blink in step. The eye is look only and does not change what the loader does.

#### Scenario: Placed with power
- **WHEN** a player places a Chunk Loader next to a powered cable within the team cap
- **THEN** the eye opens within about a second and stays open

#### Scenario: Power runs out
- **WHEN** the loader's energy runs out and it releases the chunk
- **THEN** the eye closes

#### Scenario: Over the team cap
- **WHEN** a loader stays inactive because the team cap is reached
- **THEN** its eye stays shut

#### Scenario: Blinking
- **WHEN** a player watches an active loader for half a minute
- **THEN** the eye blinks several times at uneven intervals

#### Scenario: Not in step
- **WHEN** two active loaders stand side by side
- **THEN** they do not blink at the same moments

### Requirement: Eye follows the nearest player
While the loader is active, the iris SHALL turn toward the nearest player within a few blocks (example 8) who is in front of the eye, moving smoothly and staying inside the lens. With no such player the iris SHALL drift back to the centre. While the eye is shut nothing follows.

#### Scenario: Walking past
- **WHEN** a player walks from the left to the right in front of an active loader at 4 blocks
- **THEN** the iris follows from the left to the right side of the lens

#### Scenario: Nobody near
- **WHEN** the nearest player is 20 blocks away
- **THEN** the iris rests in the centre

#### Scenario: Behind the loader
- **WHEN** the only nearby player stands behind the loader
- **THEN** the iris rests in the centre

### Requirement: Portal particles while active
While the loader is active, a few faint purple portal particles SHALL drift around the block; while it is not active none SHALL show. The particles are look only.

#### Scenario: Active
- **WHEN** a loader holds its chunk
- **THEN** a few purple particles drift around it now and then

#### Scenario: Inactive
- **WHEN** the loader has no power
- **THEN** no particles show

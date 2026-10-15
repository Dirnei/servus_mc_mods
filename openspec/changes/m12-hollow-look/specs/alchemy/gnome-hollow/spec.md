## MODIFIED Requirements

### Requirement: Seen only under Elemental Sight
A Gnome Hollow SHALL look like a plain stone block. Only while the local player has Elemental Sight SHALL the client draw the Hollow's true face over it: a darkened, cracked block. Once a gnome is bound, a cave entry SHALL open on the side the Hollow faces; before that, that side is cracked rock like the others. An empty Hollow SHALL show dry, dark cracks. A planted Hollow SHALL fill its cracks with the vein's material from the start of each crack, as far as its ripeness, glowing and slowly flowing in the vein's colour; near ripe, nodules SHALL break out of the cracks, and crystals SHALL line the cave mouth of a bound Hollow. A gifted gnome SHALL make the material flow faster. The client SHALL draw no item icons for the vein. The server SHALL not change any block for this. Picking the block without the sight SHALL give Stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow
- **THEN** they see a plain stone block

#### Scenario: Empty under the sight
- **WHEN** a player with Elemental Sight looks at a Hollow without a sample
- **THEN** they see dark dry cracks on every side, with no glow and no cave entry

#### Scenario: Ripening under the sight
- **WHEN** a planted iron Hollow goes from half ripe to ripe while a player with Elemental Sight watches
- **THEN** the iron coloured material creeps further along the cracks until they are full, still without a cave entry

#### Scenario: The Hollow opens
- **WHEN** a player with Elemental Sight binds a gnome to a ripe Hollow
- **THEN** the rock on the Hollow's facing side crumbles away into a cave entry lined with crystals of the vein

#### Scenario: Pick block
- **WHEN** a creative player without Elemental Sight picks a Gnome Hollow
- **THEN** they get Stone

## ADDED Requirements

### Requirement: The Hollow faces its maker
A Gnome Hollow SHALL face the player who placed it, and the cave entry of a bound Hollow SHALL be on that side. Its look without the sight SHALL be the same stone for every facing.

#### Scenario: Placed from the south
- **WHEN** a player standing south of the spot places a Gnome Hollow and later binds a gnome to it
- **THEN** its cave entry, seen under Elemental Sight, opens to the south

### Requirement: Vein colours are data
Each vein SHALL name the colour its material shows under the sight in its JSON, with a default for veins that name none.

#### Scenario: Gold
- **WHEN** the shipped gold vein is planted
- **THEN** its cracks fill with a golden yellow material

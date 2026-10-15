## MODIFIED Requirements

### Requirement: Binding a gnome
A Hollow without a gnome SHALL be bound when a player with Elemental Sight uses a Salt Crystal on it; the crystal is used up and a gnome moves in. No vein is needed for this. Under the sight, when a gnome already lives in the Hollow, nothing SHALL happen and the player SHALL read why. Without the sight nothing SHALL happen and the player SHALL read nothing, as with stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight uses a Salt Crystal on an empty Hollow
- **THEN** no gnome is bound, the crystal stays in the hand and no message appears

#### Scenario: Not ripe
- **WHEN** a player with Elemental Sight uses a Salt Crystal on an empty Hollow with no vein at all
- **THEN** a gnome is bound and the crystal is used up: binding needs no ripe vein any more

#### Scenario: Gnome already there
- **WHEN** a player with Elemental Sight uses a Salt Crystal on a Hollow that has a gnome
- **THEN** the crystal stays in the hand and the player reads that a gnome already lives here

### Requirement: Seeding with a sample
A player with Elemental Sight who uses a Seed on a Gnome Hollow that has a gnome, or on that gnome, SHALL have the gnome plant the Seed's vein; one Seed SHALL be used up (not in creative mode) and the Hollow SHALL hold that vein at ripeness 0. If the Hollow already holds a vein of another kind, the gnome SHALL dig it out (it is lost) and plant the new one; a Seed of the vein it already holds SHALL be refused. A Hollow without a gnome SHALL take no Seed. Any other item, including raw ores and ore blocks, SHALL plant nothing. The player SHALL read on the action bar why nothing was planted.

#### Scenario: Seed of Iron
- **WHEN** a player under Elemental Sight uses a Seed of Iron on a Hollow with a gnome and no vein
- **THEN** the Hollow holds an iron vein at ripeness 0 and one Seed is used up

#### Scenario: Before Villach
- **WHEN** a player of a team that has not finished Villach uses a Seed of Iron on its Hollow with a gnome under Elemental Sight
- **THEN** the Hollow holds an iron vein: chapters no longer gate planting, the Athanor tier of the Seed's recipe does

#### Scenario: Raw ore
- **WHEN** a player under Elemental Sight uses Raw Gold on a Hollow with a gnome
- **THEN** nothing is planted, the Raw Gold stays in the hand and the player reads that the gnome wants a seed

#### Scenario: No gnome yet
- **WHEN** a player under Elemental Sight uses a Seed of Iron on a Hollow without a gnome
- **THEN** nothing is planted and the player reads that a gnome must live here first

#### Scenario: Changing the vein
- **WHEN** a player under Elemental Sight gives a Seed of Gold to the gnome of a ripe iron Hollow
- **THEN** the iron vein is gone and the Hollow holds a gold vein at ripeness 0; the store keeps what was dug

### Requirement: Ripening with principles
A planted Hollow that is not ripe SHALL, once per step time, take the step's principles from its tanks and grow one step while its gnome tends it. Without the principles it SHALL wait. After the vein's number of steps it is ripe and the gnome starts digging.

#### Scenario: One step
- **WHEN** an iron vein needing 100 mB Salt per step of 100 ticks has 150 mB Salt
- **THEN** after 100 ticks it is one step riper and 50 mB Salt remain

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with a yield item, the number of ripening steps, the ticks per step, the principles per step, the ticks per yielded item and an optional colour. Alchemy SHALL ship iron, copper, gold, redstone and diamond veins; the noble veins SHALL need more principles and time than iron (example numbers in the data). The server SHALL send every player the veins' ids, colours and yields at login and after a reload.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with yield Raw Iron
- **THEN** a Hollow planted with a Seed of Iron yields Raw Iron once ripe

#### Scenario: A vein from a datapack
- **WHEN** a datapack adds a vein `mypack:tin` and an Athanor recipe for its Seed
- **THEN** the Seed of Tin can be made, shows the tin vein's colour and plants a tin vein

## ADDED Requirements

### Requirement: Seeds
A Seed SHALL be one item that names its vein. It SHALL be named "Seed of" followed by the vein's name and tinted in the vein's colour. A Seed whose vein is not loaded SHALL show as an unknown seed and plant nothing.

#### Scenario: Name and colour
- **WHEN** a player hovers a Seed of Gold
- **THEN** it reads "Seed of Gold" and is drawn golden yellow

### Requirement: Seeds come from the Athanor
Alchemy SHALL ship an Athanor recipe per vein that turns its raw material and principles into its Seed, at the tier of its stage: Seed of Iron from Raw Iron and Seed of Copper from Raw Copper on an Albedo Athanor, Seed of Gold from Raw Gold and Seed of Redstone from Redstone Dust on a Citrinitas Athanor, Seed of Diamond from a Diamond on a Rubedo Athanor (principles as example numbers in the data).

#### Scenario: Gold needs Citrinitas
- **WHEN** an Albedo Athanor holds Raw Gold and the principles of the Seed of Gold recipe
- **THEN** it makes no Seed of Gold, and a Citrinitas Athanor with the same contents does

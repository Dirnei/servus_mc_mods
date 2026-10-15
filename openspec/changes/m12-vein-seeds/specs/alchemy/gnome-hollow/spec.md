## MODIFIED Requirements

### Requirement: Seeding with a sample
A player with Elemental Sight who uses a Seed on a Gnome Hollow without a vein SHALL plant the Seed's vein; one Seed SHALL be used up (not in creative mode) and the Hollow SHALL hold that vein at ripeness 0. Any other item, including raw ores and ore blocks, SHALL plant nothing. A Hollow that already holds a vein SHALL take nothing, and the player SHALL read why on the action bar.

#### Scenario: Seed of Iron
- **WHEN** a player under Elemental Sight uses a Seed of Iron on an empty Hollow
- **THEN** the Hollow holds an iron vein at ripeness 0 and one Seed is used up

#### Scenario: Before Villach
- **WHEN** a player of a team that has not finished Villach uses a Seed of Iron on its empty Hollow under Elemental Sight
- **THEN** the Hollow holds an iron vein: chapters no longer gate planting, the Athanor tier of the Seed's recipe does

#### Scenario: Raw ore
- **WHEN** a player under Elemental Sight uses Raw Gold on an empty Hollow
- **THEN** the Hollow stays empty, the Raw Gold stays in the hand and the player reads that a Hollow takes only seeds

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with a yield item, the number of ripening steps, the ticks per step, the principles per step, the ticks per yielded item and an optional colour. Alchemy SHALL ship iron, copper, gold, redstone and diamond veins; the noble veins SHALL need more principles and time than iron (example numbers in the data). The server SHALL send every player the veins' ids, colours and yields at login and after a reload.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with yield Raw Iron
- **THEN** a Hollow planted with a Seed of Iron yields Raw Iron once a gnome is bound

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

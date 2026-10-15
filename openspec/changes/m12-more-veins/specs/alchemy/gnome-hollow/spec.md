# Spec Delta

## MODIFIED Requirements

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with a yield item, the number of ripening steps, the ticks per step, the principles per step, the ticks per yielded item and an optional colour. Alchemy SHALL ship coal, iron, copper, gold, redstone, quartz, diamond and emerald veins; the noble veins SHALL need more principles and time than iron, and coal SHALL be the cheapest (example numbers in the data). The server SHALL send every player the veins' ids, colours and yields at login and after a reload.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with yield Raw Iron
- **THEN** a Hollow planted with a Seed of Iron yields Raw Iron once ripe

#### Scenario: Gold before Schwaz
- **WHEN** a team that finished Villach but not Schwaz has a Seed of Gold and a Hollow with a gnome
- **THEN** the gnome plants the gold vein: the chapter no longer gates it, making the Seed of Gold on a Citrinitas Athanor does

#### Scenario: Gold after Schwaz
- **WHEN** a player under Elemental Sight gives a Seed of Gold to a Hollow with a gnome
- **THEN** the Hollow holds a gold vein at ripeness 0

#### Scenario: Ore of the tag
- **WHEN** a player under Elemental Sight uses a gold ore block on a Hollow with a gnome
- **THEN** nothing is planted: ores of a tag no longer plant veins, only Seeds do

#### Scenario: Diamond
- **WHEN** a ripe diamond vein is worked by its gnome
- **THEN** its store fills with Diamonds, and its Seed came from a Rubedo Athanor

#### Scenario: Quartz
- **WHEN** a ripe quartz vein is worked by its gnome
- **THEN** its store fills with Nether Quartz

#### Scenario: Coal and emerald
- **WHEN** a Hollow is planted with a Seed of Coal, and another with a Seed of Emerald
- **THEN** once ripe the first yields Coal and the second Emerald, and the coal vein ripens with fewer principles than the emerald vein

#### Scenario: A vein from a datapack
- **WHEN** a datapack adds a vein `mypack:tin` and an Athanor recipe for its Seed
- **THEN** the Seed of Tin can be made, shows the tin vein's colour and plants a tin vein

### Requirement: Seeds come from the Athanor
Alchemy SHALL ship an Athanor recipe per vein that turns its raw material and principles into its Seed, at the tier of its stage: Seed of Coal from Coal, Seed of Iron from Raw Iron and Seed of Copper from Raw Copper on an Albedo Athanor; Seed of Gold from Raw Gold, Seed of Redstone from Redstone Dust and Seed of Quartz from Nether Quartz on a Citrinitas Athanor; Seed of Diamond from a Diamond and Seed of Emerald from an Emerald on a Rubedo Athanor (principles as example numbers in the data).

#### Scenario: Gold needs Citrinitas
- **WHEN** an Albedo Athanor holds Raw Gold and the principles of the Seed of Gold recipe
- **THEN** it makes no Seed of Gold, and a Citrinitas Athanor with the same contents does

#### Scenario: Coal on the white Athanor
- **WHEN** an Albedo Athanor holds Coal and the principles of the Seed of Coal recipe
- **THEN** it makes a Seed of Coal

#### Scenario: Quartz needs Citrinitas
- **WHEN** an Albedo Athanor holds Nether Quartz and the principles of the Seed of Quartz recipe
- **THEN** it makes no Seed of Quartz, and a Citrinitas Athanor with the same contents does

#### Scenario: Emerald needs Rubedo
- **WHEN** a Citrinitas Athanor holds an Emerald and the principles of the Seed of Emerald recipe
- **THEN** it makes no Seed of Emerald, and a Rubedo Athanor with the same contents does

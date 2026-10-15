# alchemy/gnome-hollow Specification

## Purpose
The Gnome Hollow grows ore without world generation: seeded with a sample, ripened with principles and worked by a bound gnome, it yields its ore forever. Its vein and gnome are seen only under Elemental Sight.

## Requirements

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with sample item tags, a yield item, an optional journal chapter, the number of ripening steps, the ticks per step, the principles per step and the ticks per yielded item. Alchemy SHALL ship iron and copper veins that need the Villach chapter, and gold, redstone and diamond veins that need the Schwaz chapter, each seeded by common item tags so ores of other mods in those tags seed them too:
- gold: `c:ores/gold` and `c:raw_materials/gold`, yields Raw Gold,
- redstone: `c:ores/redstone` and `c:dusts/redstone`, yields Redstone Dust,
- diamond: `c:ores/diamond` and `c:gems/diamond`, yields a Diamond.
The noble veins SHALL need more principles and time than iron (example numbers in the data).

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with samples `c:ores/iron` and `c:raw_materials/iron`, yield raw iron and chapter Villach
- **THEN** raw iron or any iron ore seeds an iron vein for a team that finished Villach

#### Scenario: Gold before Schwaz
- **WHEN** a player of a team that finished Villach but not Schwaz uses raw gold on its empty Hollow under Elemental Sight
- **THEN** the Hollow stays empty, the raw gold stays in the hand and the player reads that this ore does not take root yet

#### Scenario: Gold after Schwaz
- **WHEN** a player of a team that finished Schwaz uses raw gold on its empty Hollow under Elemental Sight
- **THEN** the Hollow holds a gold vein at ripeness 0

#### Scenario: Ore of the tag
- **WHEN** a player of a team that finished Schwaz uses Nether Gold Ore, which is in `c:ores/gold`, on its empty Hollow under Elemental Sight
- **THEN** the Hollow holds a gold vein

#### Scenario: Diamond
- **WHEN** a bound diamond Hollow works through its yield time
- **THEN** its store holds one Diamond

### Requirement: Seeding with a sample
A Gnome Hollow without a vein SHALL take one item into its sample slot when the item matches a vein whose chapter the owner's team has finished; that item SHALL be used up and the Hollow SHALL hold that vein at ripeness 0. A Hollow that already holds a vein SHALL take no sample.

#### Scenario: Before Villach
- **WHEN** a team that has not finished Villach offers raw iron to its Hollow
- **THEN** the Hollow takes nothing

### Requirement: Ripening with principles
A seeded Hollow that is not ripe SHALL, once per step time, take the step's principles from its tanks and grow one step. Without the principles it SHALL wait. After the vein's number of steps it is ripe.

#### Scenario: One step
- **WHEN** an iron vein needing 100 mB Salt per step of 100 ticks has 150 mB Salt
- **THEN** after 100 ticks it is one step riper and 50 mB Salt remain

### Requirement: Principles by pipe or bucket
The Hollow SHALL hold one tank per principle (example 4,000 mB), accept each principle only into its own tank through the fluid capability and by principle bucket on right click, and never give fluid away.

#### Scenario: Bucket
- **WHEN** a player uses a Salt bucket on a Hollow
- **THEN** the Salt tank gains 1,000 mB

### Requirement: Binding a gnome
A ripe vein without a gnome SHALL be bound when a player with Elemental Sight uses a Salt Crystal on the Hollow; the crystal is used up. Without the sight or before the vein is ripe nothing SHALL happen and the player SHALL be told why.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight uses a Salt Crystal on a ripe Hollow
- **THEN** no gnome is bound and the player reads that they cannot see the spirits of the earth

### Requirement: A bound gnome works forever
A Hollow with a ripe vein and a bound gnome SHALL put one yield item into its output slot every vein yield time while the output has room, without using anything, and SHALL push outputs through its sides by the shared machine rules.

#### Scenario: Raw iron
- **WHEN** a bound iron Hollow with a yield time of 200 ticks runs for 400 ticks with a chest beside it
- **THEN** the chest holds 2 raw iron

### Requirement: Hollow production
A working Hollow SHALL expose the production capability with one recipe: no inputs, the yield out, the yield time as ticks.

#### Scenario: Producer
- **WHEN** the production capability of a working iron Hollow is queried
- **THEN** it reports raw iron with no inputs

### Requirement: Seen only under Elemental Sight
A Gnome Hollow SHALL look like a plain stone block. Only while the local player has Elemental Sight SHALL the client draw ore buds of the vein's yield around it, more and larger as it ripens, and a gnome at work once one is bound. The server SHALL not change any block for this.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow
- **THEN** they see a plain stone block

### Requirement: Gnome Hollow crafting
The Gnome Hollow SHALL be made on an Athanor of at least Albedo from cracked stone bricks with Salt and Mercury.

#### Scenario: Athanor craft
- **WHEN** cracked stone bricks lie on an Albedo Athanor with the principles
- **THEN** a Gnome Hollow is made

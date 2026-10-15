## MODIFIED Requirements

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

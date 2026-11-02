# Spec Delta

## MODIFIED Requirements

### Requirement: Zone properties
A zone SHALL have a name of 1 to 24 characters, unique within its team ignoring case, one of the 16 dye colours, and a filter of up to nine items and up to nine item tags, each chosen in the shared item picker popup with its search, where typing `$` searches tags and the picked tag is shown as `#namespace:path`. Creating or editing a zone with an empty, too long or taken name SHALL be refused with a message in the form.

#### Scenario: Create a zone
- **WHEN** a player creates "Ores" in orange with the tag `c:ores`
- **THEN** the team's list holds "Ores", orange, taking items tagged `c:ores`

#### Scenario: Name taken
- **WHEN** the team already has "Ores" and the player creates "ORES"
- **THEN** the form says the name is taken and no zone is created

#### Scenario: Item from the picker
- **WHEN** the player clicks "Add" in the zone form, types "bread" in the picker and picks Bread
- **THEN** the zone's filter lists Bread, and clicking that entry in the form removes it again

#### Scenario: Tag from the picker
- **WHEN** the player types `$ores` in the picker and presses Tab
- **THEN** the search completes to `$c:ores`, the top entry reads `#c:ores`, and picking it adds the tag to the filter, shown there as `#c:ores`

#### Scenario: Zones saved before keep their tags
- **WHEN** a world with a zone filtering `#c:ores` from before this change is loaded
- **THEN** the zone still takes items tagged `c:ores` and its form shows `#c:ores`

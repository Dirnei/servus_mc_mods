# Spec Delta

## Purpose

Zones give the warehouse a layout: named groups of storage blocks with rules that say what belongs there, so put-away lands items where the player wants them and overflow has a place to go.

## ADDED Requirements

### Requirement: Zone Label item
A Zone Label SHALL be a craftable item made from vanilla materials. Naming it in an anvil SHALL set the zone name it stamps. Using an unnamed label on a storage block SHALL do nothing except tell the player to name it. The label SHALL NOT be consumed by stamping.

#### Scenario: Crafting
- **WHEN** the player places paper and a dye in the label pattern (for example paper above a dye)
- **THEN** the result is a Zone Label

#### Scenario: Unnamed label
- **WHEN** the player right clicks a crate with a label that has no custom name
- **THEN** the crate stays unzoned and a message says the label must be named in an anvil

#### Scenario: Label stays in hand
- **WHEN** the player stamps three racks with a label named "Aisle 3"
- **THEN** the player still holds the label

### Requirement: Stamping a zone
Right clicking a crate, double crate, quad crate or Heavy Duty Rack with a named Zone Label SHALL put that block into the zone of that name. Sneak right clicking a storage block with any Zone Label SHALL remove the block from its zone. Zone names SHALL be compared case insensitively with surrounding whitespace trimmed and SHALL be stored as typed. A block SHALL belong to at most one zone, and the zone SHALL survive reloads and the carrot dismantle and place cycle.

#### Scenario: Stamp a rack
- **WHEN** the player right clicks a Heavy Duty Rack with a label named "Aisle 3"
- **THEN** the rack belongs to zone "Aisle 3"

#### Scenario: Restamp
- **WHEN** the player right clicks that rack with a label named "Food"
- **THEN** the rack belongs to "Food" and no longer to "Aisle 3"

#### Scenario: Clear
- **WHEN** the player sneaks and right clicks the rack with a label
- **THEN** the rack belongs to no zone

#### Scenario: Case and spaces
- **WHEN** one crate is stamped "aisle 3" and another " Aisle 3 "
- **THEN** both crates are in the same zone

#### Scenario: Dismantle keeps the zone
- **WHEN** a zoned crate is dismantled with the Configuration Carrot and placed again
- **THEN** the placed crate is still in its zone

#### Scenario: Label on a controller
- **WHEN** the player right clicks a Warehouse Controller with a named label
- **THEN** nothing is zoned and the controller behaves as if no label was used

### Requirement: Zone shown on the block
A zoned block SHALL show its zone name on its front face, small and above the item display, to every player who can see it. The Heavy Duty Rack GUI title SHALL include the zone name in brackets while the rack is zoned.

#### Scenario: Looking at a zoned crate
- **WHEN** a player looks at a crate in zone "Aisle 3"
- **THEN** "Aisle 3" is drawn on the crate's front above its item and count

#### Scenario: Rack GUI
- **WHEN** a player opens a rack in zone "Food"
- **THEN** the GUI title reads "Heavy Duty Rack (Food)"

#### Scenario: Unzoned
- **WHEN** a crate belongs to no zone
- **THEN** nothing extra is drawn on its front

### Requirement: Zone rules
Each zone of a warehouse SHALL have a rule: up to nine filter items, a list of item tags, and an overflow flag. A stack SHALL match a zone when it is one of the filter items or carries one of the tags. At most one zone of a warehouse SHALL be the overflow zone; flagging another SHALL unflag the previous one. Rules SHALL be kept for zones that currently have no blocks.

#### Scenario: Tag match
- **WHEN** zone "Ores" lists the tag `c:ores` and raw iron arrives
- **THEN** raw iron matches "Ores"

#### Scenario: Item match
- **WHEN** zone "Food" lists bread as a filter item and bread arrives
- **THEN** bread matches "Food"

#### Scenario: One overflow zone
- **WHEN** "Spare" is the overflow zone and the player flags "Back" as overflow
- **THEN** "Back" is the overflow zone and "Spare" is not

#### Scenario: Rule outlives its blocks
- **WHEN** every block of zone "Ores" is broken and one is placed and stamped "Ores" again
- **THEN** "Ores" still lists the tag `c:ores`

### Requirement: Put-away routing
Every insert into a warehouse SHALL pick targets in this order. A stack that matches a filtered zone SHALL go to that zone's blocks; when none has room, to the overflow zone; otherwise it SHALL NOT be stored. A stack matching no filtered zone SHALL go to general storage, which is every unzoned block and every zone without filter items and tags that is not the overflow zone, then to the overflow zone. Among matching zones, one listing the exact item SHALL win over one matching only by tag, then the alphabetically first name SHALL win. Within the chosen blocks, those already holding the item SHALL come first, then empty crates, then other storage blocks, then foreign inventories already in use.

#### Scenario: Ores to aisle 3
- **WHEN** "Aisle 3" lists `c:ores`, an unzoned crate is empty, and 64 raw copper are inserted
- **THEN** the raw copper lands in a block of "Aisle 3" and the unzoned crate stays empty

#### Scenario: Zone full, overflow takes it
- **WHEN** every block of "Aisle 3" is full, "Spare" is the overflow zone and raw copper is inserted
- **THEN** the raw copper lands in "Spare"

#### Scenario: Zone full, no overflow
- **WHEN** every block of "Aisle 3" is full, no overflow zone exists and an unzoned crate is empty
- **THEN** the raw copper is not stored and is returned as not fitting

#### Scenario: Zoned block refuses other items
- **WHEN** "Aisle 3" lists `c:ores`, no other storage exists and bread is inserted
- **THEN** the bread is not stored

#### Scenario: Unfiltered zone is general storage
- **WHEN** zone "Back" has an empty rule and is not the overflow zone and bread is inserted
- **THEN** the bread may land in "Back"

#### Scenario: Exact item beats tag
- **WHEN** "Iron" lists the item raw iron, "Ores" lists `c:ores`, and raw iron is inserted
- **THEN** the raw iron lands in "Iron"

#### Scenario: Same item first within the zone
- **WHEN** "Aisle 3" has an empty crate and a crate holding raw copper and more raw copper is inserted
- **THEN** the copper joins the crate that already holds copper

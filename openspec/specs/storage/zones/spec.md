# storage/zones Specification

## Purpose

Zones give the warehouse a layout: a Zone Label carries a name and a filter, stamps them onto storage blocks, and put-away fills matching zoned blocks before everything else.

## Requirements

### Requirement: Zone Label item
A Zone Label SHALL be a craftable item made from vanilla materials. Right clicking the air with it SHALL open the Zone Label GUI with a name field, nine ghost item slots and a line of item tags. The label SHALL show its zone name as its item name and keep name and filter in its data. The label SHALL NOT be consumed by stamping.

#### Scenario: Crafting
- **WHEN** the player places paper and a dye in the label pattern (for example paper and a dye together)
- **THEN** the result is a Zone Label

#### Scenario: Define a zone
- **WHEN** the player right clicks the air with a label, types "Ores" as the name and `c:ores` in the tag line
- **THEN** the label is named "Ores" and carries the tag `c:ores`

#### Scenario: Ghost item
- **WHEN** the player clicks a ghost slot of the label GUI with bread on the cursor
- **THEN** the slot shows bread and the cursor still holds the bread

#### Scenario: Label stays in hand
- **WHEN** the player stamps three racks with a label named "Aisle 3"
- **THEN** the player still holds the label

### Requirement: Stamping a zone
Right clicking a crate, double crate, quad crate or Heavy Duty Rack with a named Zone Label SHALL copy the label's name and filter onto that block. Sneak right clicking a storage block with any Zone Label SHALL clear its zone. Using a label without a name SHALL do nothing except tell the player to name it. A block SHALL keep the copy it was stamped with until it is stamped again or cleared, and the zone SHALL survive reloads and the carrot dismantle and place cycle.

#### Scenario: Stamp a rack
- **WHEN** the player right clicks a Heavy Duty Rack with a label named "Ores" carrying `c:ores`
- **THEN** the rack belongs to zone "Ores" and takes only items tagged `c:ores`

#### Scenario: Restamp
- **WHEN** the player right clicks that rack with a label named "Food"
- **THEN** the rack belongs to "Food" with the Food filter and no longer to "Ores"

#### Scenario: Clear
- **WHEN** the player sneaks and right clicks the rack with a label
- **THEN** the rack belongs to no zone and takes any item again

#### Scenario: Unnamed label
- **WHEN** the player right clicks a crate with a label that has no name
- **THEN** the crate stays unzoned and a message says the label needs a name

#### Scenario: Label edited later
- **WHEN** a crate was stamped "Ores" with `c:ores` and the label is then changed to carry only raw iron
- **THEN** the crate still takes everything tagged `c:ores` until it is stamped again

#### Scenario: Dismantle keeps the zone
- **WHEN** a zoned crate is dismantled with the Configuration Carrot and placed again
- **THEN** the placed crate is still in its zone with its filter

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

### Requirement: Zone filters on blocks
A zoned block with a filter SHALL take only items that are one of its filter items or carry one of its tags. A zoned block whose filter is empty and an unzoned block SHALL take any item. Zones are optional: a warehouse without any label works as before.

#### Scenario: Filtered block refuses other items
- **WHEN** a crate is zoned "Ores" with `c:ores` and bread is inserted while no other storage exists
- **THEN** the bread is not stored

#### Scenario: Name only zone
- **WHEN** a rack is zoned "Back" with no filter items and no tags and bread is inserted
- **THEN** the rack may take the bread

### Requirement: Put-away routing
Every insert into a warehouse SHALL try targets in this order: zoned blocks whose filter lists the exact item, then zoned blocks matching by tag, then general storage, which is every unzoned block and every zoned block with an empty filter. A stack with no room in any of these SHALL NOT be stored. Within each tier, blocks already holding the item SHALL come first, then empty crates, then other storage blocks, then foreign inventories already in use.

#### Scenario: Ores to aisle 3
- **WHEN** "Aisle 3" blocks carry `c:ores`, an unzoned crate is empty, and 64 raw copper are inserted
- **THEN** the raw copper lands in a block of "Aisle 3" and the unzoned crate stays empty

#### Scenario: Zone full, unzoned storage takes it
- **WHEN** every "Aisle 3" block is full, an unzoned crate is empty and raw copper is inserted
- **THEN** the raw copper lands in the unzoned crate

#### Scenario: Zone and general storage full
- **WHEN** every "Aisle 3" block is full and no unzoned block has room
- **THEN** the raw copper is not stored and is returned as not fitting

#### Scenario: Exact item beats tag
- **WHEN** one rack carries the item raw iron, another carries `c:ores`, and raw iron is inserted
- **THEN** the raw iron lands in the rack listing raw iron

#### Scenario: Same item first within the tier
- **WHEN** "Aisle 3" has an empty crate and a crate holding raw copper and more raw copper is inserted
- **THEN** the copper joins the crate that already holds copper

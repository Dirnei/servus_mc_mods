# shuttles/zones Specification

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
Right clicking a Heavy Duty Rack with a named Zone Label SHALL copy the label's name and filter onto that rack. Sneak right clicking a rack with any Zone Label SHALL clear its zone. Using a label without a name SHALL do nothing except tell the player to name it. A rack SHALL keep the copy it was stamped with until it is stamped again or cleared, and the zone SHALL survive reloads and the carrot dismantle and place cycle.

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
- **WHEN** the player right clicks a rack with a label that has no name
- **THEN** the rack stays unzoned and a message says the label needs a name

#### Scenario: Label edited later
- **WHEN** a rack was stamped "Ores" with `c:ores` and the label is then changed to carry only raw iron
- **THEN** the rack still takes everything tagged `c:ores` until it is stamped again

#### Scenario: Dismantle keeps the zone
- **WHEN** a zoned rack is dismantled with the Configuration Carrot and placed again
- **THEN** the placed rack is still in its zone with its filter

#### Scenario: Label on a controller
- **WHEN** the player right clicks a Warehouse Controller with a named label
- **THEN** nothing is zoned and the controller behaves as if no label was used

### Requirement: Zone filters on blocks
A zoned block with a filter SHALL take only items that are one of its filter items or carry one of its tags. A zoned block whose filter is empty and an unzoned block SHALL take any item. Zones are optional: a warehouse without any label works as before.

#### Scenario: Filtered block refuses other items
- **WHEN** a rack is zoned "Ores" with `c:ores` and bread waits for put-away while no other storage exists
- **THEN** the bread is not stored

#### Scenario: Name only zone
- **WHEN** a rack is zoned "Back" with no filter items and no tags and bread is put away
- **THEN** the rack may take the bread

### Requirement: Put-away routing
Every put-away into a warehouse SHALL try racks in this order: zoned racks whose filter lists the exact item, then zoned racks matching by tag, then general storage, which is every unzoned rack and every zoned rack with an empty filter. A stack with no room in any of these SHALL NOT be stored. Within each tier, racks already holding the item SHALL come first, then the other racks.

#### Scenario: Ores to aisle 3
- **WHEN** "Aisle 3" racks carry `c:ores`, an unzoned rack is empty, and 64 raw copper are put away
- **THEN** the raw copper lands in a rack of "Aisle 3" and the unzoned rack stays empty

#### Scenario: Zone full, unzoned storage takes it
- **WHEN** every "Aisle 3" rack is full, an unzoned rack is empty and raw copper is put away
- **THEN** the raw copper lands in the unzoned rack

#### Scenario: Zone and general storage full
- **WHEN** every "Aisle 3" rack is full and no unzoned rack has room
- **THEN** the raw copper is not stored and waits as not fitting

#### Scenario: Exact item beats tag
- **WHEN** one rack carries the item raw iron, another carries `c:ores`, and raw iron is put away
- **THEN** the raw iron lands in the rack listing raw iron

#### Scenario: Same item first within the tier
- **WHEN** "Aisle 3" has an empty rack and a rack holding raw copper and more raw copper is put away
- **THEN** the copper joins the rack that already holds copper

### Requirement: Zone shown on the rack
A zoned rack SHALL show its zone name on its front face, small and above the item display, to every player who can see it. The Heavy Duty Rack GUI title SHALL include the zone name in brackets while the rack is zoned.

#### Scenario: Looking at a zoned rack
- **WHEN** a player looks at a rack in zone "Aisle 3"
- **THEN** "Aisle 3" is drawn on the rack's front above the items on its shelves

#### Scenario: Rack GUI
- **WHEN** a player opens a rack in zone "Food"
- **THEN** the GUI title reads "Heavy Duty Rack (Food)"

#### Scenario: Unzoned
- **WHEN** a rack belongs to no zone
- **THEN** nothing extra is drawn on its front

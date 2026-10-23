## MODIFIED Requirements

### Requirement: Put-away routing
Every put-away into a warehouse SHALL try racks in this order: zoned racks whose filter lists the exact item, then zoned racks matching by tag, then general storage, which is every unzoned rack and every zoned rack with an empty filter. A stack with no room in any of these SHALL NOT be stored. Only room in slots that no job has locked SHALL count. Within each tier the racks already holding the item SHALL come first: a rack with a partial stack of the item before the others, then the cheapest by travel cost. Then every other rack with room SHALL follow, the ones with the most free slots first, counted in buckets of `free_slot_bucket` of the bay's slots (in `warehouse.json`, example 0.25 for quarters), and within a bucket the cheapest. The order in which racks joined the warehouse SHALL only break exact ties. A put-away SHALL go to the first rack in this order and lock its room there; when that rack takes only part of the stack, the job carries that part and the rest is planned once the shuttle has loaded.

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

#### Scenario: Partial stack first
- **WHEN** two racks hold cobblestone, the nearer one only full stacks and the farther one a stack of 20, and 30 cobblestone are put away
- **THEN** the cobblestone goes to the farther rack, tops up its 20 and fills a free slot there

#### Scenario: Emptier racks first
- **WHEN** no rack holds bread, a near rack has 2 of 27 slots free and a far rack 20 of 27 free
- **THEN** the bread goes to the far rack

#### Scenario: Cheapest among equals
- **WHEN** no rack holds bread and two empty racks are 4 and 9 rail blocks from the station
- **THEN** the bread goes to the rack 4 blocks away

#### Scenario: Locked room does not count
- **WHEN** the only rack with room has its last free slots IN locked for another put-away
- **THEN** the next stack waits as not fitting until room appears

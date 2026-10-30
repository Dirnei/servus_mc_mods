# Spec Delta

## ADDED Requirements

### Requirement: One zone list per team
Each team SHALL have one list of zones, kept with the world. A zone SHALL belong to the team of the player who created it and SHALL keep its identity when renamed, recoloured or refiltered. Shelves SHALL point at a zone, not hold a copy of it. Zones of other teams SHALL keep working on shelves they are set on but SHALL NOT be listed in the player's handheld.

#### Scenario: Shared within the team
- **WHEN** player A creates the zone "Ores" and player B on the same team opens the handheld GUI
- **THEN** "Ores" is listed for player B

#### Scenario: Other team
- **WHEN** a player of another team opens the handheld GUI
- **THEN** "Ores" is not listed

#### Scenario: Kept after restart
- **WHEN** the server restarts
- **THEN** the team's zones and every shelf's zone are unchanged

### Requirement: Zone properties
A zone SHALL have a name of 1 to 24 characters, unique within its team ignoring case, one of the 16 dye colours, and a filter of up to nine items and up to nine item tags, each chosen in the shared item picker popup with its search, where typing `#` searches tags. Creating or editing a zone with an empty, too long or taken name SHALL be refused with a message in the form.

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
- **WHEN** the player types `#ores` in the picker and presses Tab
- **THEN** the search completes to `#c:ores`, and picking it adds the tag to the filter

### Requirement: Any team member edits zones
Any member of a zone's team SHALL be able to change its name, colour and filter. The change SHALL apply to every shelf in that zone at once, for display and for put-away.

#### Scenario: Rename
- **WHEN** a team member renames "Ores" to "Raw ores"
- **THEN** every loaded shelf in that zone shows "Raw ores" on its beam without being touched

#### Scenario: New filter applies at once
- **WHEN** a team member removes `c:ores` from "Ores" and adds only raw iron, then raw copper is put away
- **THEN** the "Ores" shelves no longer take the raw copper

### Requirement: Deleting a zone
A team member SHALL be able to delete a zone. The handheld GUI SHALL show how many loaded shelves are in it and ask once more before deleting a zone that loaded shelves use. Every shelf in a deleted zone SHALL be unzoned, also shelves in unloaded areas once they load.

#### Scenario: Delete a used zone
- **WHEN** a player deletes "Bulk" that three loaded shelves use and confirms
- **THEN** "Bulk" is gone and the three shelves take any item and show no zone

### Requirement: Clients know every zone
Every client SHALL receive all zones with their team, name, colour and filter when the player joins and after every create, edit or delete, so rack beams and the handheld show current names and colours.

#### Scenario: Join
- **WHEN** a player joins a world with zoned racks
- **THEN** the racks show their zone names and colours before any GUI is opened

### Requirement: One zone per shelf
A zone SHALL be set on a whole shelf of Heavy Duty Racks or of Fluid Racks, never on a single bay of it: every bay of a shelf SHALL be in the same zone or in none. When a shelf loses a bay, the bays left behind SHALL keep the zone, also when they fall apart into two shelves.

#### Scenario: Shelf split by a broken bay
- **WHEN** the middle bay of a shelf of three in "Ores" is broken
- **THEN** both bays left behind are in "Ores"

#### Scenario: New layer on top
- **WHEN** a layer of racks is placed on top of a shelf in "Ores"
- **THEN** the new shelf on top has no zone

### Requirement: Zone on join
A bay that joins a zoned shelf SHALL take the shelf's zone, whatever zone it carried. A zoned bay that joins an unzoned shelf SHALL put the whole shelf into its zone; an unzoned bay joining an unzoned shelf SHALL leave it unzoned. This SHALL hold for Heavy Duty Racks and Fluid Racks alike.

#### Scenario: Unzoned bay joins a zoned shelf
- **WHEN** a new rack without a zone is placed beside a shelf of two bays in "Ores" and joins it
- **THEN** the shelf of three is in "Ores"

#### Scenario: Bay with another zone joins a zoned shelf
- **WHEN** a rack item dismantled from a shelf in "Food" is placed so that it joins a shelf in "Ores"
- **THEN** the shelf of three is in "Ores", the shelf's zone wins

#### Scenario: Zoned bay joins an unzoned shelf
- **WHEN** a rack item dismantled from a shelf in "Food" is placed so that it joins an unzoned shelf of two
- **THEN** the whole shelf of three is in "Food"

#### Scenario: Fluid Rack bay joins a zoned shelf
- **WHEN** an empty Fluid Rack without a zone joins a Fluid Rack shelf in "Cooling"
- **THEN** the Fluid Rack shelf is in "Cooling"

### Requirement: Zone kept through dismantling
A shelf's zone SHALL survive reloads. A bay dismantled with the Configuration Carrot SHALL carry its shelf's zone; placed as a shelf of its own it SHALL bring that zone back, and placed so that it joins a shelf it SHALL follow the join rule of "Zone on join". Rack bays and Fluid Racks saved with a zone stamped by a Zone Label of an earlier version SHALL load unzoned.

#### Scenario: Dismantle keeps the zone
- **WHEN** a single bay shelf in "Ores" is dismantled with the Configuration Carrot and placed again on its own
- **THEN** the placed bay is a shelf in "Ores"

#### Scenario: Old stamped zone
- **WHEN** a world saved with a rack stamped "Ores" by a Zone Label is loaded
- **THEN** the rack has no zone

### Requirement: Zone list logic is pure
Name checks, ordering, the search of the handheld's zone list and resolving a deleted zone to no zone SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover name length and uniqueness per team, ordering by name, searching by text and resolving a deleted zone id

## MODIFIED Requirements

### Requirement: Zone filters on blocks
A zoned block with a filter SHALL take only items that are one of its zone's filter items or carry one of its zone's tags. A zoned block whose zone has an empty filter and an unzoned block SHALL take any item. Zones are optional: a warehouse without any zone works as before.

#### Scenario: Filtered block refuses other items
- **WHEN** a rack is zoned "Ores" with `c:ores` and bread waits for put-away while no other storage exists
- **THEN** the bread is not stored

#### Scenario: Name only zone
- **WHEN** a rack is zoned "Back" with no filter items and no tags and bread is put away
- **THEN** the rack may take the bread

### Requirement: Zone shown on the rack
A zoned shelf of Heavy Duty Racks SHALL show a stripe in its zone's colour along the top beam of the whole shelf and its zone name once along the shelf, small and above the item display, to every player who can see it. The Heavy Duty Rack GUI title SHALL include the zone name in brackets while the shelf is zoned.

#### Scenario: Looking at a zoned rack
- **WHEN** a player looks at a shelf of three bays in the orange zone "Aisle 3"
- **THEN** one orange stripe runs along the top beam of all three bays and "Aisle 3" is drawn once on the shelf, above the items on its boards

#### Scenario: Rack GUI
- **WHEN** a player opens a shelf in zone "Food"
- **THEN** the GUI title reads "Heavy Duty Rack (Food)"

#### Scenario: Unzoned
- **WHEN** a shelf belongs to no zone
- **THEN** no stripe and no zone name are drawn on its front

### Requirement: Fluid filters on Fluid Racks
On a Fluid Rack a zone's filter SHALL be the fluids of the filled fluid containers in its ghost slots, for example a water bucket meaning water. A zoned Fluid Rack whose filter lists fluids SHALL take only those fluids; one whose filter lists none, and an unzoned Fluid Rack, SHALL take any fluid. Item tags and ghost items that hold no fluid SHALL be ignored on Fluid Racks and SHALL keep their meaning on Heavy Duty Racks.

#### Scenario: Water only
- **WHEN** a Fluid Rack is zoned "Cooling" with a water bucket in a ghost slot and lava is offered to it
- **THEN** it takes none

#### Scenario: Name only zone takes anything
- **WHEN** a Fluid Rack is zoned "Back" with a filter of bread and the tag `c:ores`
- **THEN** it takes any fluid

#### Scenario: Same label on both racks
- **WHEN** the zone "Mixed" carries a water bucket and bread and holds a Heavy Duty Rack and a Fluid Rack
- **THEN** the Heavy Duty Rack takes only water buckets and bread, and the Fluid Rack takes only water

### Requirement: Zone shown on the Fluid Rack
A zoned Fluid Rack shelf SHALL show its zone name once, small, on the top beam of its port bay, to every player who can see it. An unzoned shelf SHALL show nothing there.

#### Scenario: Looking at a zoned section
- **WHEN** a player looks at a three bay shelf in zone "Cooling"
- **THEN** "Cooling" is drawn on the top beam of the middle bay

## REMOVED Requirements

### Requirement: Zone Label item
**Reason**: Zones are team data edited in the Warehouse Handheld; a label item that carries a copy of a zone no longer exists.
**Migration**: None. Labels in inventories disappear on load; players create their zones in the handheld GUI.

### Requirement: Stamping a zone
**Reason**: Replaced by scanning a shelf with the handheld in ZONE mode, see `shuttles/handheld`.
**Migration**: None. Racks stamped by labels load unzoned and are assigned again with the handheld.

### Requirement: Stamping a Fluid Rack
**Reason**: Replaced by scanning a Fluid Rack with the handheld in ZONE mode, which assigns its whole shelf, see `shuttles/handheld`; the join rule moves to "Zone on join".
**Migration**: None. Fluid Racks stamped by labels load unzoned and are assigned again with the handheld.

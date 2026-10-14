# Spec Delta

## ADDED Requirements

### Requirement: Supply buffer
Each Field Head SHALL keep a buffer of three parts: seeds of its crop up to `seed_buffer` (in `field.json`, example 64), its fertilizer up to `fertilizer_buffer` (example 64), and anti-corrosion items up to `anti_corrosion_level` (example 8). Only supply runs SHALL fill it, and only the field's Harvester and Spreader SHALL take from it. Players, hoppers and pipes SHALL neither put items in nor take them out: the head SHALL register no item capability, no packet inlet and no upkeep. The buffer SHALL be kept when the world is saved, and its items SHALL drop when the head is broken.

#### Scenario: Buffer survives a reload
- **WHEN** a head with 40 wheat seeds, 12 bone meal and 5 honeycomb in its buffer is saved and loaded
- **THEN** its buffer holds the same items

#### Scenario: Broken head drops its buffer
- **WHEN** a player breaks a head with 40 wheat seeds in its buffer
- **THEN** the 40 wheat seeds drop with the head

#### Scenario: Not on the data line
- **WHEN** a data line duct touches a Field Head
- **THEN** the duct finds no packet inlet and no upkeep on the head

## MODIFIED Requirements

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with four tabs on the left edge (Field, Water, Fertilizer, Upgrades) of its panel and the player inventory below the page of the selected tab. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have a crop picker: a button that shows the field's crop with its icon and name, or "None, harvest only" while the field is in harvest only mode or has no crop, and that opens a scrollable list with a search field over the GUI. The list SHALL hold every item that places a crop block, mods' crops included, sorted by name, after a first entry "None, harvest only". Typing in the search field SHALL narrow the list to the entries whose name or id contains the text. Choosing a crop entry SHALL set that crop and turn harvest only off; choosing "None, harvest only" SHALL set harvest only mode and clear the crop. The player SHALL NOT need the item to choose it. The server SHALL refuse an item that places no crop block and a player who can no longer use the head. The crop and the mode SHALL be kept when the world is saved.

Each crop entry SHALL show how many of that item the warehouse of the served field holds. The counts SHALL reach the player when the GUI opens and again whenever they change while it is open. An entry with no stock SHALL stay selectable but be drawn greyed, with a hint that the first planting needs seeds in the warehouse. A head whose field is not on a warehouse SHALL show no counts.

The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the seeds in the head's buffer out of their level, the harvest threshold with its buttons, whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL show the water level and capacity and whether the field is hydrated or dry. It SHALL show the anti-corrosion items in the head's buffer out of `anti_corrosion_level`, and SHALL show the corrosion stage of the served field with the progress toward the next aging step, or, for a corroded field, that the rails are corroded and the field is not watered.

The **Fertilizer** tab SHALL have a fertilizer picker like the crop picker: "Off" first, then every item of the fertilizer tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's fertilizer, "Off" clears it. It SHALL have mode buttons Off, After planting and Until ripe share, the fertilize threshold with - and + buttons, the Keep in stock reserve with -10, -1, +1 and +10 buttons, the fertilizer in the head's buffer out of its level, and a status line: fertilizing off, Spreader at work (naming the Spreader when it has a name plate), waiting, no Spreader shuttle, no Spreader that reaches the field without a lift, no shuttle that can bring supplies, no fertilizer in stock, stock at or below the reserve, or the buffer empty while a supply run is on its way. The server SHALL refuse an item outside the tag and a player who can no longer use the head.

The **Upgrades** tab SHALL have the Range Upgrade slot of the field radius and SHALL show the radius and the field size in rails and plots.

Shift clicking a Range Upgrade in the player inventory SHALL move it into its slot whatever tab is open, and shift clicking it in its slot SHALL move it back to the inventory. The head's buffer SHALL take no items from the player and give none back.

A tab SHALL carry a red marker while its page holds a problem, so it is seen from every tab: the Field tab while the head serves no field or the work status is a warning, the Water tab while the served field is dry or corroded, the Fertilizer tab while fertilizing is on and the status says there is no Spreader, no Spreader that reaches the field or no shuttle that can bring supplies. No fertilizer in stock, or stock at or below the reserve, is not a problem: the status line says so without a red marker.

#### Scenario: Opens on the Field tab
- **WHEN** a player opens a Field Head
- **THEN** the Field tab is selected and shows the crop picker and the seeds in the buffer, while the Range Upgrade slot is hidden

#### Scenario: Set wheat
- **WHEN** a player with no wheat seeds in the inventory chooses Wheat Seeds in the crop picker
- **THEN** the field's crop is wheat and harvest only is off

#### Scenario: Harvest only clears the crop
- **WHEN** wheat is set and the player chooses "None, harvest only"
- **THEN** the field has no crop and is in harvest only mode

#### Scenario: Refuse a non seed
- **WHEN** a client sends the id of cobblestone as the chosen crop
- **THEN** the crop and the mode do not change

#### Scenario: Modded crops listed
- **WHEN** a mod adds a seed item that places a crop block
- **THEN** that seed appears in the crop picker list in name order

#### Scenario: Search narrows the list
- **WHEN** the player types "carr" in the picker's search field
- **THEN** the list shows Carrot and the "None, harvest only" entry

#### Scenario: Stock shown and greyed
- **WHEN** the warehouse holds 40 wheat seeds and no beetroot seeds and the player opens the picker
- **THEN** Wheat Seeds shows 40 and Beetroot Seeds is greyed with the hint that the first planting needs seeds in the warehouse

#### Scenario: Stock follows the warehouse
- **WHEN** wheat seeds are put into the warehouse while the GUI is open
- **THEN** the Wheat Seeds count in the picker updates without reopening the GUI

#### Scenario: No warehouse, no counts
- **WHEN** a player opens the picker of a head whose field is not on a warehouse
- **THEN** the entries show no counts and none is greyed

#### Scenario: Fertilizer tab
- **WHEN** a player opens the Fertilizer tab of a head on a warehouse holding 30 bone meal
- **THEN** the fertilizer picker lists Bone Meal with 30 after "Off", and the mode buttons, the threshold, the reserve and the status line are shown

#### Scenario: Refuse a non fertilizer
- **WHEN** a client sends the id of cobblestone as the chosen fertilizer
- **THEN** the head's fertilizer does not change

#### Scenario: Below reserve not marked
- **WHEN** fertilizing after planting is on with bone meal, the reserve is 64 and the warehouse holds 64 bone meal
- **THEN** the Fertilizer tab shows no red marker and its status says the stock is at or below the reserve

#### Scenario: Radius shown
- **WHEN** a player opens the Upgrades tab of a head holding one Range Upgrade with `radius` 2
- **THEN** the tab shows radius 3

#### Scenario: Corroded field shown
- **WHEN** a player opens the GUI of a head whose field is corroded
- **THEN** the Water tab has a red marker and its page says the rails are corroded and the field is not watered

#### Scenario: Honeycomb by hand
- **WHEN** a player shift clicks honeycomb in the head's GUI while the Field tab is open
- **THEN** the honeycomb stays in the player inventory and the head's buffer is unchanged

#### Scenario: Range Upgrade back to the inventory
- **WHEN** a player shift clicks the Range Upgrade in its slot on the Upgrades tab
- **THEN** it moves back to the player inventory

#### Scenario: Dry field marked
- **WHEN** a head serving a field runs out of water while the player looks at the Upgrades tab
- **THEN** the Water tab shows a red marker

### Requirement: Field work status
The Field Head SHALL show in its GUI, and the field command SHALL print, what its served field is doing or the first thing that keeps a harvester away, checked in this order: rails not linked to a warehouse, warehouse without exactly one controller, no crop and no harvest only, no Harvester shuttle in a garage of that warehouse, no Harvester shuttle that can reach the field without riding a lift car, a run queued or under way, harvest due, harvest only waiting for ripe crops, plots to work but no water, plots to work, farmland but no seeds in the head's buffer and none in stock, farmland waiting for a supply run with seeds, farmland but no shuttle that can bring seeds, everything growing, no plot to till or plant. The warehouse SHALL NOT send a run to a field no harvester can reach without a lift.

#### Scenario: Field without a controller
- **WHEN** a served field's farm rails are linked to rails, a garage and racks but no Warehouse Controller
- **THEN** the Field Head GUI says the warehouse has no controller

#### Scenario: Ready field
- **WHEN** the warehouse is online, a Harvester shuttle sits in its garage, wheat is set, the head has water and grass plots lie in the strip
- **THEN** the GUI says work is ready and a harvester is coming

#### Scenario: Field on another floor
- **WHEN** the only Harvester shuttle's garage stands on the floor below the field and an elevator shaft is the only way up
- **THEN** the GUI says no harvester can reach the field without a lift, and no harvester leaves its garage

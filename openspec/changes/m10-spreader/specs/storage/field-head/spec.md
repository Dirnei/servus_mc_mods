## MODIFIED Requirements

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with four tabs on the left edge (Field, Water, Fertilizer, Upgrades) of its panel and the player inventory below the page of the selected tab. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have a crop picker: a button that shows the field's crop with its icon and name, or "None, harvest only" while the field is in harvest only mode or has no crop, and that opens a scrollable list with a search field over the GUI. The list SHALL hold every item that places a crop block, mods' crops included, sorted by name, after a first entry "None, harvest only". Typing in the search field SHALL narrow the list to the entries whose name or id contains the text. Choosing a crop entry SHALL set that crop and turn harvest only off; choosing "None, harvest only" SHALL set harvest only mode and clear the crop. The player SHALL NOT need the item to choose it. The server SHALL refuse an item that places no crop block and a player who can no longer use the head. The crop and the mode SHALL be kept when the world is saved.

Each crop entry SHALL show how many of that item the warehouse of the served field holds. The counts SHALL reach the player when the GUI opens and again whenever they change while it is open. An entry with no stock SHALL stay selectable but be drawn greyed, with a hint that the first planting needs seeds in the warehouse. A head whose field is not on a warehouse SHALL show no counts.

The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the harvest threshold with its buttons, whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL show the water level and capacity and whether the field is hydrated or dry. It SHALL have the anti-corrosion upkeep slot and SHALL show the corrosion stage of the served field with the progress toward the next aging step, or, for a corroded field, that the rails are corroded and the field is not watered.

The **Fertilizer** tab SHALL have a fertilizer picker like the crop picker: "Off" first, then every item of the fertilizer tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's fertilizer, "Off" clears it. It SHALL have mode buttons Off, After planting and Until ripe share, the fertilize threshold with - and + buttons, the Keep in stock reserve with -10, -1, +1 and +10 buttons, and a status line: fertilizing off, Spreader at work (naming the Spreader when it has a name plate), waiting, no Spreader shuttle, no Spreader that reaches the field without a lift, no fertilizer in stock, or stock at or below the reserve. The server SHALL refuse an item outside the tag and a player who can no longer use the head.

The **Upgrades** tab SHALL have the Range Upgrade slot of the field radius and SHALL show the radius and the field size in rails and plots.

Shift clicking an anti-corrosion item or a Range Upgrade in the player inventory SHALL move it into its slot whatever tab is open, and shift clicking it in its slot SHALL move it back to the inventory.

A tab SHALL carry a red marker while its page holds a problem, so it is seen from every tab: the Field tab while the head serves no field or the work status is a warning, the Water tab while the served field is dry or corroded, the Fertilizer tab while fertilizing is on and the status says there is no Spreader or no Spreader that reaches the field. No fertilizer in stock, or stock at or below the reserve, is not a problem: the status line says so without a red marker.

#### Scenario: Opens on the Field tab
- **WHEN** a player opens a Field Head
- **THEN** the Field tab is selected and shows the crop picker, while the Range Upgrade and anti-corrosion slots are hidden

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
- **THEN** the honeycomb moves into the anti-corrosion slot

#### Scenario: Range Upgrade back to the inventory
- **WHEN** a player shift clicks the Range Upgrade in its slot on the Upgrades tab
- **THEN** it moves back to the player inventory

#### Scenario: Dry field marked
- **WHEN** a head serving a field runs out of water while the player looks at the Upgrades tab
- **THEN** the Water tab shows a red marker

## ADDED Requirements

### Requirement: Fertilize settings per head
Each Field Head SHALL keep its own fertilizer (an item of the tag `servus_storage:fertilizer`, bone meal by default in the tag, or none), a fertilize mode (Off, After planting, Until ripe share), a fertilize threshold in percent (default from `fertilize_share` in `field.json`, example 0.5) and a Keep in stock reserve (default from `fertilizer_reserve`, example 64, never below 0). Fertilizing SHALL be on only while a fertilizer is chosen and the mode is not Off. The settings SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** fertilizing is off, no fertilizer is chosen and the reserve is `fertilizer_reserve`

#### Scenario: Reserve never below zero
- **WHEN** the reserve is 5 and the player presses -10
- **THEN** the reserve is 0

#### Scenario: Settings saved
- **WHEN** a head with bone meal, Until ripe share at 70 percent and reserve 20 is saved and loaded
- **THEN** it has the same fertilizer, mode, threshold and reserve

### Requirement: Plots that need a dose
The head SHALL know which plots need a dose, from the same events and passes as ripeness and never by scanning per tick. In After planting mode these SHALL be the plots planted since their last dose whose crop is not ripe; a plot counts as planted when the head sees a crop appear on an empty plot, and a dose round by a Spreader clears it. Plots already planted when a field is loaded or changed SHALL NOT count as newly planted. In Until ripe share mode, while the ripe share is below the head's fertilize threshold, these SHALL be all planted plots whose crop is not ripe; at or above the threshold none. Ripe plots SHALL never need a dose.

#### Scenario: Planted after the head was set
- **WHEN** a field in After planting mode has an old growing wheat and a player plants two new wheat plots
- **THEN** the head counts two plots that need a dose

#### Scenario: Dosed plots are done
- **WHEN** a Spreader gives a newly planted plot its round of doses and the crop is still not ripe
- **THEN** the head no longer counts it as needing a dose

#### Scenario: Until ripe share stops at the threshold
- **WHEN** a field in Until ripe share mode with threshold 50 percent has four planted plots and two of them are ripe
- **THEN** the head counts no plot that needs a dose

# Spec Delta

## ADDED Requirements

### Requirement: Produce learned from harvests
A Field Head SHALL learn its field's produce from its harvests: after a run that harvested, the produce SHALL be the item the harvest brought home the most of, leaving out the field's crop item whenever another item came home too. Choosing another crop SHALL forget the produce until the next harvest. The produce SHALL be kept when the world is saved.

#### Scenario: Wheat
- **WHEN** a wheat field's harvest brings home 12 wheat and 20 wheat seeds
- **THEN** the head's produce is wheat

#### Scenario: Carrots
- **WHEN** a carrot field's harvest brings home only carrots
- **THEN** the head's produce is carrots

#### Scenario: New crop
- **WHEN** a head whose produce is wheat gets beetroot as its crop
- **THEN** the head has no produce until its first beetroot harvest

### Requirement: Stock levels per head
Each Field Head SHALL keep a harvest limit and a fertilize level, both counted in items of its produce in the warehouse of the served field, each 0 for none (defaults `harvest_limit` and `fertilize_below` in `field.json`, example 0). While both are set, the fertilize level SHALL never be above the harvest limit: moving one past the other SHALL move the other with it. While the head has no produce, both levels SHALL do nothing. The levels SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** its harvest limit and fertilize level are 0 and the field harvests and fertilizes as before

#### Scenario: Levels stay in order
- **WHEN** the harvest limit is 512 and the player raises the fertilize level to 640
- **THEN** the harvest limit is 640 as well

### Requirement: Harvesting stops at the limit
While a head has a produce and a harvest limit, and the warehouse holds at least that many of the produce, the field SHALL harvest nothing: the warehouse SHALL queue no run for ripe plots, and a run that goes out to till or plant SHALL leave ripe plots standing. Once the stock drops below the limit, harvesting SHALL resume at the next field check. One run MAY bring the stock above the limit.

#### Scenario: Limit reached
- **WHEN** a wheat field with harvest limit 1,000 is fully ripe and the warehouse holds 1,024 wheat
- **THEN** no harvester leaves for that field and the wheat stays ripe on the field

#### Scenario: Stock used up
- **WHEN** an order takes 100 of those 1,024 wheat
- **THEN** a harvester leaves for the field at the next field check

### Requirement: Fertilizing only below the level
While a head has a produce and a fertilize level, the field SHALL call no Spreader while the warehouse holds at least that many of the produce, and no Spreader either while harvesting is stopped at the limit. Below the level, the head's fertilize mode, threshold and reserve SHALL apply as before.

#### Scenario: Enough wheat
- **WHEN** a wheat field fertilizes after planting with fertilize level 500, has newly planted plots, and the warehouse holds 640 wheat
- **THEN** no Spreader is called

#### Scenario: Wheat runs low
- **WHEN** the same warehouse drops to 400 wheat
- **THEN** the field calls a Spreader at the next field check

### Requirement: Stock levels in the GUI
The Stock tab SHALL show the harvest limit as "Stop harvesting at" (0 for no limit), the fertilize level as "Fertilize below" (0 for always), the fertilizer reserve as "Keep fertilizer in stock" and the anti-corrosion reserve as "Keep anti-corrosion in stock" (with the name of the anti-corrosion item beside it), each as a number field the player types into. A field SHALL take digits only, and SHALL send its number to the server when the player presses Enter or the field loses focus; an empty field counts as 0. The server SHALL clamp a level to 0 to 99,999 and a reserve to 0 to 9,999, SHALL keep the two levels in order as for the stock levels, and SHALL refuse a player who can no longer use the head. Each field SHALL then show the value the head keeps, but SHALL NOT be overwritten while the player types in it.

While harvesting is stopped at the limit, the Field tab's work status SHALL say so with the stock and the limit; while fertilizing waits on the level, the Fertilizer tab's status SHALL say so. Neither SHALL be a problem: no red marker and no red ripe share bar. The field command SHALL print the produce and both levels.

#### Scenario: Paused shown
- **WHEN** a player opens the GUI of a wheat field stopped at its limit of 1,000 with 1,024 wheat in stock
- **THEN** the Field tab says harvesting is paused with 1,024 of 1,000 wheat, and no tab has a red marker

#### Scenario: Typed limit
- **WHEN** the harvest limit is 0 and the player types 1000 into Stop harvesting at and presses Enter
- **THEN** the harvest limit is 1,000 and the field shows 1000

#### Scenario: Typed value clamped
- **WHEN** the player types 250000 into Fertilize below and clicks elsewhere in the GUI
- **THEN** the fertilize level is 99,999 and the field shows 99999

#### Scenario: Field not overwritten while typing
- **WHEN** the player has typed 12 into Stop harvesting at and the head's values reach the client before Enter is pressed
- **THEN** the field still shows 12

#### Scenario: Far player refused
- **WHEN** a client sends a harvest limit of 500 for a head its player can no longer use
- **THEN** the head's harvest limit does not change

### Requirement: Anti-corrosion item and reserve per head
Each Field Head SHALL keep its own anti-corrosion item: an item of the tag `servus_storage:anti_corrosion`, the first item of the tag until the player chooses one, or Off. While it is Off the anti-corrosion part of the buffer SHALL have no level, the items it holds SHALL count as stale, and the field SHALL age without upkeep. The choice SHALL be kept when the world is saved, and the server SHALL refuse an item outside the tag.

Each Field Head SHALL also keep an anti-corrosion Keep in stock reserve (default from `anti_corrosion_reserve` in `field.json`, example 0, never below 0), kept when the world is saved. Supply runs SHALL never take the warehouse stock of the anti-corrosion item at or below it, and a low anti-corrosion part SHALL call a supply run only while the warehouse holds more than the reserve.

#### Scenario: Honeycomb kept
- **WHEN** a head's anti-corrosion reserve is 5, its anti-corrosion part is empty, `anti_corrosion_level` is 8 and the warehouse holds 10 honeycomb
- **THEN** a supply run brings 5 honeycomb, 5 stay in the warehouse, and no further supply run comes for honeycomb

#### Scenario: Default keeps nothing
- **WHEN** a head's anti-corrosion reserve was never set and `anti_corrosion_reserve` is 0
- **THEN** supply runs may take the last honeycomb of the warehouse

#### Scenario: Anti-corrosion Off
- **WHEN** the player chooses Off for the anti-corrosion item of a head whose buffer holds 3 honeycomb
- **THEN** no supply run brings honeycomb, the next supply run takes the 3 honeycomb back, and the field ages at its next step

#### Scenario: Refuse a non anti-corrosion item
- **WHEN** a client chooses dirt as the anti-corrosion item
- **THEN** the head keeps its anti-corrosion item

#### Scenario: Anti-corrosion reserve saved
- **WHEN** a head with anti-corrosion Off and an anti-corrosion reserve of 6 is saved and loaded
- **THEN** its anti-corrosion item is Off and its reserve is 6

## MODIFIED Requirements

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with five tabs on the left edge (Field, Water, Fertilizer, Stock, Upgrades) of its panel and the player inventory below the page of the selected tab. The panel SHALL be large enough that no row is squeezed and long item names fit, and SHALL fit a 1280 by 720 window at GUI scale 3. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have a crop picker across the width of the page: a button that shows the field's crop with its icon and name, or "None, harvest only" while the field is in harvest only mode or has no crop, and that opens a scrollable list with a search field over the GUI. The list SHALL hold every item that places a crop block, mods' crops included, sorted by name, after a first entry "None, harvest only". Typing in the search field SHALL narrow the list to the entries whose name or id contains the text. Choosing a crop entry SHALL set that crop and turn harvest only off; choosing "None, harvest only" SHALL set harvest only mode and clear the crop. The player SHALL NOT need the item to choose it. The server SHALL refuse an item that places no crop block and a player who can no longer use the head. The crop and the mode SHALL be kept when the world is saved.

Each crop entry SHALL show how many of that item the warehouse of the served field holds. The counts SHALL reach the player when the GUI opens and again whenever they change while it is open. An entry with no stock SHALL stay selectable but be drawn greyed, with a hint that the first planting needs seeds in the warehouse. A head whose field is not on a warehouse SHALL show no counts.

The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the harvest threshold with its buttons, whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL have an anti-corrosion picker like the fertilizer picker, across the width of the page: "Off" first, then every item of the anti-corrosion tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's anti-corrosion item. Below it the tab SHALL show the water level and capacity and whether the field is hydrated or dry. It SHALL show the corrosion stage of the served field with the progress toward the next aging step, or, for a corroded field, that the rails are corroded and the field is not watered.

The **Fertilizer** tab SHALL have a fertilizer picker like the crop picker, across the width of the page: "Off" first, then every item of the fertilizer tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's fertilizer, "Off" clears it. It SHALL have mode buttons Off, After planting and Until ripe share, the fertilize threshold with - and + buttons, and a status line: fertilizing off, Spreader at work (naming the Spreader when it has a name plate), waiting, no Spreader shuttle, no Spreader that reaches the field without a lift, no shuttle that can bring supplies, no fertilizer in stock, stock at or below the reserve, or the buffer empty while a supply run is on its way. The server SHALL refuse an item outside the tag and a player who can no longer use the head.

The **Stock** tab SHALL show the field's produce with its icon, its name and its stock in the warehouse, or that the produce is learned at the first harvest. It SHALL have the harvest limit, the fertilize level and the Keep in stock reserve as number fields the player types into, and SHALL show the head's buffer: for the seeds, the fertilizer and the anti-corrosion items an icon with the items held out of their level, each with a tooltip that names the part and says that supply runs fill it.

The **Upgrades** tab SHALL have the Range Upgrade slot of the field radius and SHALL show the radius and the field size in rails and plots.

Shift clicking a Range Upgrade in the player inventory SHALL move it into its slot whatever tab is open, and shift clicking it in its slot SHALL move it back to the inventory. The head's buffer SHALL take no items from the player and give none back.

A tab SHALL carry a red marker while its page holds a problem, so it is seen from every tab: the Field tab while the head serves no field or the work status is a warning, the Water tab while the served field is dry or corroded, the Fertilizer tab while fertilizing is on and the status says there is no Spreader, no Spreader that reaches the field or no shuttle that can bring supplies. No fertilizer in stock, or stock at or below the reserve, is not a problem: the status line says so without a red marker. The Stock tab and the Upgrades tab SHALL carry no marker.

#### Scenario: Opens on the Field tab
- **WHEN** a player opens a Field Head
- **THEN** the Field tab is selected and shows the crop picker across the page, while the Range Upgrade slot, the level fields and the buffer are hidden

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
- **THEN** the fertilizer picker lists Bone Meal with 30 after "Off", and the mode buttons, the threshold and the status line are shown, while the reserve is on the Stock tab

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

#### Scenario: Stock tab
- **WHEN** a player opens the Stock tab of a wheat field's head with 1,024 wheat in the warehouse and 40 wheat seeds, 12 bone meal and 5 honeycomb in its buffer, at levels 64, 64 and 8
- **THEN** the tab shows Wheat with 1,024 in stock, the fields Stop harvesting at, Fertilize below, Keep fertilizer in stock and Keep anti-corrosion in stock, and 40 / 64, 12 / 64 and 5 / 8 beside the seed, bone meal and honeycomb icons

#### Scenario: Produce not learned yet
- **WHEN** a player opens the Stock tab of a head that has not harvested yet
- **THEN** the tab says the produce is learned at the first harvest

#### Scenario: Stock tab never marked
- **WHEN** a head's buffer holds no seeds and its field is dry
- **THEN** the Water tab has a red marker and the Stock tab has none

### Requirement: Fertilize settings per head
Each Field Head SHALL keep its own fertilizer (an item of the tag `servus_storage:fertilizer`, bone meal by default in the tag, or none), a fertilize mode (Off, After planting, Until ripe share), a fertilize threshold in percent (default from `fertilize_share` in `field.json`, example 0.5) and a Keep in stock reserve (default from `fertilizer_reserve`, example 64, never below 0). Fertilizing SHALL be on only while a fertilizer is chosen and the mode is not Off. The settings SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** fertilizing is off, no fertilizer is chosen and the reserve is `fertilizer_reserve`

#### Scenario: Reserve never below zero
- **WHEN** the reserve is 5 and the player clears its field and presses Enter, or a client sends -10 as the reserve
- **THEN** the reserve is 0

#### Scenario: Settings saved
- **WHEN** a head with bone meal, Until ripe share at 70 percent and reserve 20 is saved and loaded
- **THEN** it has the same fertilizer, mode, threshold and reserve

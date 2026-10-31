## MODIFIED Requirements

### Requirement: Hydration
While its field is hydrated, the head SHALL keep wet every farmland block within the field radius of each wet farm rail of the field (see the rail corrosion spec), one block below the rail: the square of radius blocks to each side of every wet rail. Farmland in that square SHALL behave as if water were near it: it does not dry out and crops on it grow at the moistened rate. Hydration SHALL end when the buffer runs dry, when the head is removed or unloaded, or for the part of the field a removed rail, a smaller radius or a corroded rail took away. It SHALL resume after a world reload without any action from the player.

#### Scenario: Farmland stays wet
- **WHEN** a head with water and radius 2 serves a straight field and farmland lies two blocks beside a rail, one block below it
- **THEN** that farmland counts as near water

#### Scenario: Out of reach
- **WHEN** farmland lies three blocks beside the nearest rail of a field with radius 2
- **THEN** the head does not keep it wet

#### Scenario: Rail removed
- **WHEN** the only rail beside a patch of farmland is broken
- **THEN** that farmland no longer counts as near water

#### Scenario: Behind a corroded rail
- **WHEN** a patch of farmland lies only beside rails behind a corroded rail of the field
- **THEN** that farmland no longer counts as near water

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with five tabs on the left edge (Field, Water, Fertilizer, Stock, Upgrades) of its panel and the player inventory below the page of the selected tab. The panel SHALL be large enough that no row is squeezed and long item names fit, and SHALL fit a 1280 by 720 window at GUI scale 3. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have a crop picker across the width of the page: a button that shows the field's crop with its icon and name, or "None, harvest only" while the field is in harvest only mode or has no crop, and that opens a scrollable list with a search field over the GUI. The list SHALL hold every item that places a crop block, mods' crops included, sorted by name, after a first entry "None, harvest only". Typing in the search field SHALL narrow the list to the entries whose name or id contains the text. Choosing a crop entry SHALL set that crop and turn harvest only off; choosing "None, harvest only" SHALL set harvest only mode and clear the crop. The player SHALL NOT need the item to choose it. The server SHALL refuse an item that places no crop block and a player who can no longer use the head. The crop and the mode SHALL be kept when the world is saved.

Each crop entry SHALL show how many of that item the warehouse of the served field holds. The counts SHALL reach the player when the GUI opens and again whenever they change while it is open. An entry with no stock SHALL stay selectable but be drawn greyed, with a hint that the first planting needs seeds in the warehouse. A head whose field is not on a warehouse SHALL show no counts.

The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the harvest threshold with its buttons, and the ripe share bar SHALL also be a slider: clicking or dragging on it SHALL set the harvest threshold to the step under the mouse, its mark following while dragged. It SHALL show whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL have an anti-corrosion picker like the fertilizer picker, across the width of the page: "Off" first, then every item of the anti-corrosion tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's anti-corrosion item. Below it the tab SHALL show the water level and capacity and the water the served field uses per minute while watered, or that it is dry. It SHALL show how many rails of the served field are in each corrosion stage, listing only stages that have rails, and the progress toward the next aging step; the tooltip of the progress SHALL say roughly how much watering time is left until that step and whether an anti-corrosion item in the buffer will hold the stages then. While a corroded rail blocks water, the tab SHALL say that water is blocked and how many rails are dry.

The **Fertilizer** tab SHALL have a fertilizer picker like the crop picker, across the width of the page: "Off" first, then every item of the fertilizer tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's fertilizer, "Off" clears it. It SHALL have mode buttons Off, After planting and Until ripe share, the fertilize threshold with - and + buttons, and a status line: fertilizing off, Spreader at work (naming the Spreader when it has a name plate), waiting, no Spreader shuttle, no Spreader that reaches the field without a lift, no shuttle that can bring supplies, no fertilizer in stock, stock at or below the reserve, or the buffer empty while a supply run is on its way. The server SHALL refuse an item outside the tag and a player who can no longer use the head.

The **Stock** tab SHALL be a table with one column each for the produce, the seeds of the crop, the chosen fertilizer and the anti-corrosion item, headed by the item's icon ("?" while the produce is not learned yet, "off" for a part without an item). Its rows SHALL be, top to bottom: **Warehouse**, the warehouse stock of each item (no count without an item or a warehouse); **Field Head**, the items the head's buffer holds out of their level for the seeds, the fertilizer and the anti-corrosion item; then, below a rule, the typed settings: **Keep at least** (the two reserves), **Harvest up to** and **Fertilize below**. Large counts SHALL be shortened (123k, 1.2M) with the full count in the tooltip. Explanations SHALL be in tooltips only: each header names the item and its role, each count names the item and the full count, each buffer cell names the part and says that supply runs fill it, and each row label explains the row.

The **Upgrades** tab SHALL have the Range Upgrade slot of the field radius and SHALL show the radius and the field size in rails and plots.

Shift clicking a Range Upgrade in the player inventory SHALL move it into its slot whatever tab is open, and shift clicking it in its slot SHALL move it back to the inventory. The head's buffer SHALL take no items from the player and give none back.

A tab SHALL carry a red marker while its page holds a problem, so it is seen from every tab: the Field tab while the head serves no field or the work status is a warning, the Water tab while the served field is dry or a corroded rail blocks water, the Fertilizer tab while fertilizing is on and the status says there is no Spreader, no Spreader that reaches the field or no shuttle that can bring supplies. No fertilizer in stock, or stock at or below the reserve, is not a problem: the status line says so without a red marker. The Stock tab and the Upgrades tab SHALL carry no marker.

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

#### Scenario: Ripe bar as slider
- **WHEN** a player clicks the ripe share bar of a head at 70 percent of its width
- **THEN** the harvest threshold is 70 percent and the bar's mark moves there

#### Scenario: Water use and aging estimate
- **WHEN** a player opens the Water tab of a watered field of five rails with `water_per_rail` 1, `water_interval` 100 and `corrosion_water` 6000, half way to its next aging step
- **THEN** the tab says the field uses 60 mB per minute, and the tooltip of the aging progress says the next stage comes in about 50m of watering

#### Scenario: Rail counts shown
- **WHEN** a player opens the Water tab of a head whose field has twelve copper, five exposed and three oxidized rails
- **THEN** the tab lists Copper 12, Exposed 5 and Oxidized 3 and no other stage

#### Scenario: Corroded field shown
- **WHEN** a player opens the GUI of a head whose field of twenty rails has its thirteenth rail from the head corroded
- **THEN** the Water tab has a red marker, lists Corroded 1, and its page says water is blocked and eight rails are dry

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
- **THEN** the Warehouse row shows 1,024 under the wheat icon, the Field Head row shows 40/64, 12/64 and 5/8 under the seed, bone meal and honeycomb icons, and the rows Keep at least, Harvest up to and Fertilize below have number fields with no explanation text beside them

#### Scenario: Produce not learned yet
- **WHEN** a player opens the Stock tab of a head that has not harvested yet
- **THEN** the tab says the produce is learned at the first harvest

#### Scenario: Stock tab never marked
- **WHEN** a head's buffer holds no seeds and its field is dry
- **THEN** the Water tab has a red marker and the Stock tab has none

### Requirement: Field diagnostics
`/servus_warehouse field <pos>` SHALL print, for the Field Head at that position, its status, its field kind, rails, plots, planted and ripe counts, water level, crop, sapling or mode (2x2 included), whether a harvest is due, the number of rails in each corrosion stage, the number of dry rails and the water counted toward the next aging step. `/servus_warehouse field <pos> recheck` SHALL run the pass over all plots again. `/servus_warehouse field <pos> age <mB>` SHALL count that much water as used for aging, with the same upkeep and aging rules as water used by hydration, so a stage can be reached quickly for testing.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails, the head's water level and how many rails are in each corrosion stage

#### Scenario: Age by command
- **WHEN** an operator runs the age command with `corrosion_water` mB on a head of a field of copper rails without anti-corrosion items
- **THEN** every wet rail becomes exposed

#### Scenario: Print a Trees field
- **WHEN** an operator runs the field command on a head of a 2x2 spruce field
- **THEN** the output names the Trees kind and Spruce Sapling 2x2

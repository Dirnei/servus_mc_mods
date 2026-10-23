# shuttles/field-head Specification

## Purpose

The Field Head waters a field of farm rails and keeps track of which plots are ripe, so harvester shuttles know when a run is worth it.

## Requirements

### Requirement: One head per field
A Field Head SHALL be a craftable block. It SHALL serve the field of a farm rail touching one of its four horizontal sides; when it touches farm rails of more than one field it SHALL serve the field of the first one in the order north, east, south, west. A field SHALL have at most one serving head: when several heads touch the same field, only one serves it and the others SHALL show that the field has another head. A field larger than the maximum size (`max_rails` in `data/<ns>/shuttles/field.json`, example 128 rails) SHALL NOT be served, and the head SHALL show that the field is too large. A head without a farm rail beside it SHALL show that it has no field.

#### Scenario: Head beside a run
- **WHEN** a Field Head is placed beside the end of a run of four farm rails
- **THEN** its GUI shows a field of four rails

#### Scenario: Second head
- **WHEN** a second Field Head is placed beside another rail of the same run
- **THEN** one of the two heads serves the field and the other shows that the field has another head

#### Scenario: No rail
- **WHEN** a Field Head stands with no farm rail beside it
- **THEN** its GUI shows that it has no field

### Requirement: Water buffer
A Field Head SHALL expose a fluid handler on every side that accepts water and refuses every other fluid, so the Foundation fluid pipe and other mods can fill it. Its buffer SHALL hold up to `capacity` mB (example 8000). It SHALL NOT give water back out. While it serves a field, the head SHALL use `water_per_rail` mB (example 1) per rail of its field every `water_interval` ticks (example 100). When the buffer holds less than one interval's use, the field SHALL stop being hydrated until enough water is in the buffer again. The water level SHALL be kept when the world is saved.

#### Scenario: Fill with water
- **WHEN** 1000 mB of water are offered to a Field Head
- **THEN** it takes them and its GUI shows the new level

#### Scenario: Lava refused
- **WHEN** lava is offered to a Field Head
- **THEN** it takes none

#### Scenario: Water through the pipe
- **WHEN** a Utility Duct with a fluid line runs from a pump on water to a Field Head
- **THEN** the head's buffer fills

#### Scenario: Water runs out
- **WHEN** a head serving a field of three rails holds exactly one interval's use and gets no more water
- **THEN** the field is hydrated for one interval and then dries

### Requirement: Water by bucket
Using an item that holds or takes fluid on a Field Head SHALL try to fill the head's buffer from it, the way a Fluid Tank is filled: water goes in as far as the buffer has room and the item gives up what was taken (a water bucket becomes an empty bucket). A player in creative mode SHALL keep the item unchanged. A fluid item that gives no water to the head (an empty bucket, a lava bucket, or any item while the buffer is full) SHALL change nothing and SHALL NOT open the GUI. The head SHALL never fill an item from its buffer. Every other item and the empty hand SHALL open the GUI as before.

#### Scenario: Water bucket fills the head
- **WHEN** a player uses a water bucket on a Field Head with an empty buffer
- **THEN** the buffer holds 1000 mB more and the player holds an empty bucket

#### Scenario: Lava bucket refused
- **WHEN** a player uses a lava bucket on a Field Head
- **THEN** the buffer is unchanged, the player still holds the lava bucket and no GUI opens

#### Scenario: Empty bucket gets nothing
- **WHEN** a player uses an empty bucket on a Field Head holding water
- **THEN** the buffer is unchanged and the player still holds the empty bucket

#### Scenario: Full head keeps the bucket
- **WHEN** a player uses a water bucket on a Field Head whose buffer is full
- **THEN** the buffer is unchanged and the player still holds the water bucket

#### Scenario: Creative keeps the bucket
- **WHEN** a player in creative mode uses a water bucket on a Field Head with room
- **THEN** the buffer holds 1000 mB more and the player still holds the water bucket

#### Scenario: Other items open the GUI
- **WHEN** a player uses a wheat seed on a Field Head
- **THEN** the head's GUI opens

### Requirement: Hydration
While its field is hydrated, the head SHALL keep wet every farmland block within the field radius of each farm rail of the field, one block below the rail: the square of radius blocks to each side of every rail. Farmland in that square SHALL behave as if water were near it: it does not dry out and crops on it grow at the moistened rate. Hydration SHALL end when the buffer runs dry, when the head is removed or unloaded, or for the part of the field a removed rail or a smaller radius took away. It SHALL resume after a world reload without any action from the player.

#### Scenario: Farmland stays wet
- **WHEN** a head with water and radius 2 serves a straight field and farmland lies two blocks beside a rail, one block below it
- **THEN** that farmland counts as near water

#### Scenario: Out of reach
- **WHEN** farmland lies three blocks beside the nearest rail of a field with radius 2
- **THEN** the head does not keep it wet

#### Scenario: Rail removed
- **WHEN** the only rail beside a patch of farmland is broken
- **THEN** that farmland no longer counts as near water

### Requirement: Ripeness tracking
The plots of a field SHALL be the positions at the rail's level inside the hydrated squares, except the rail positions. The head SHALL know which plots hold an age crop (wheat, carrots, potatoes, beetroot and other crops of that kind) and which of those are ripe. It SHALL learn this from crop growth, bone meal, a block being broken or placed on a plot and farmland being trampled, and from one pass over all plots when the field is loaded, changed or rechecked by command. It SHALL NOT scan plots every tick. The ripe share SHALL be the ripe plots divided by the planted plots. The head SHALL signal harvest due when its field has a crop set or is in harvest only mode, at least one plot is planted and the ripe share reaches `harvest_share` (example 0.5).

#### Scenario: Crop grows ripe
- **WHEN** a wheat crop on a plot grows to its last stage
- **THEN** the head counts one more ripe plot

#### Scenario: Bone meal
- **WHEN** a player uses bone meal on a carrot crop of a plot until it is ripe
- **THEN** the head counts it ripe a tick later

#### Scenario: Harvested by hand
- **WHEN** a player breaks a ripe crop on a plot
- **THEN** the head counts one ripe plot less

#### Scenario: Due at the threshold
- **WHEN** a field with wheat set has four planted plots and the second one becomes ripe
- **THEN** the head signals harvest due

#### Scenario: Nothing set
- **WHEN** a field has neither a crop nor harvest only set and all its crops are ripe
- **THEN** the head does not signal harvest due

### Requirement: Field Head GUI
Right clicking a Field Head SHALL open a GUI with five tabs on the left edge (Field, Water, Fertilizer, Stock, Upgrades) of its panel and the player inventory below the page of the selected tab. The panel SHALL be large enough that no row is squeezed and long item names fit, and SHALL fit a 1280 by 720 window at GUI scale 3. The GUI SHALL open on the Field tab, and clicking a tab SHALL show its page; slots of the other pages SHALL be hidden and SHALL NOT take clicks.

The **Field** tab SHALL have a crop picker across the width of the page: a button that shows the field's crop with its icon and name, or "None, harvest only" while the field is in harvest only mode or has no crop, and that opens a scrollable list with a search field over the GUI. The list SHALL hold every item that places a crop block, mods' crops included, sorted by name, after a first entry "None, harvest only". Typing in the search field SHALL narrow the list to the entries whose name or id contains the text. Choosing a crop entry SHALL set that crop and turn harvest only off; choosing "None, harvest only" SHALL set harvest only mode and clear the crop. The player SHALL NOT need the item to choose it. The server SHALL refuse an item that places no crop block and a player who can no longer use the head. The crop and the mode SHALL be kept when the world is saved.

Each crop entry SHALL show how many of that item the warehouse of the served field holds. The counts SHALL reach the player when the GUI opens and again whenever they change while it is open. An entry with no stock SHALL stay selectable but be drawn greyed, with a hint that the first planting needs seeds in the warehouse. A head whose field is not on a warehouse SHALL show no counts.

The Field tab SHALL show the planted and ripe plot counts with the ripe share bar, the harvest threshold with its buttons, and the ripe share bar SHALL also be a slider: clicking or dragging on it SHALL set the harvest threshold to the step under the mouse, its mark following while dragged. It SHALL show whether a harvest is due through the work status line (naming the harvester working the field), or why the head serves no field.

The **Water** tab SHALL have an anti-corrosion picker like the fertilizer picker, across the width of the page: "Off" first, then every item of the anti-corrosion tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's anti-corrosion item. Below it the tab SHALL show the water level and capacity and the water the served field uses per minute while watered, or that it is dry. It SHALL show the corrosion stage of the served field with the progress toward the next aging step; its tooltip SHALL say roughly how much watering time is left until that step, and whether an anti-corrosion item in the buffer will hold the stage then, or, for a corroded field, that the rails are corroded and the field is not watered.

The **Fertilizer** tab SHALL have a fertilizer picker like the crop picker, across the width of the page: "Off" first, then every item of the fertilizer tag, each with its warehouse stock, greyed without stock; choosing an entry sets the head's fertilizer, "Off" clears it. It SHALL have mode buttons Off, After planting and Until ripe share, the fertilize threshold with - and + buttons, and a status line: fertilizing off, Spreader at work (naming the Spreader when it has a name plate), waiting, no Spreader shuttle, no Spreader that reaches the field without a lift, no shuttle that can bring supplies, no fertilizer in stock, stock at or below the reserve, or the buffer empty while a supply run is on its way. The server SHALL refuse an item outside the tag and a player who can no longer use the head.

The **Stock** tab SHALL be a table with one column each for the produce, the seeds of the crop, the chosen fertilizer and the anti-corrosion item, headed by the item's icon ("?" while the produce is not learned yet, "off" for a part without an item). Its rows SHALL be, top to bottom: **Warehouse**, the warehouse stock of each item (no count without an item or a warehouse); **Field Head**, the items the head's buffer holds out of their level for the seeds, the fertilizer and the anti-corrosion item; then, below a rule, the typed settings: **Keep at least** (the two reserves), **Harvest up to** and **Fertilize below**. Large counts SHALL be shortened (123k, 1.2M) with the full count in the tooltip. Explanations SHALL be in tooltips only: each header names the item and its role, each count names the item and the full count, each buffer cell names the part and says that supply runs fill it, and each row label explains the row.

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

#### Scenario: Ripe bar as slider
- **WHEN** a player clicks the ripe share bar of a head at 70 percent of its width
- **THEN** the harvest threshold is 70 percent and the bar's mark moves there

#### Scenario: Water use and aging estimate
- **WHEN** a player opens the Water tab of a watered field of five rails with `water_per_rail` 1, `water_interval` 100 and `corrosion_water` 6000, half way to its next aging step
- **THEN** the tab says the field uses 60 mB per minute, and the tooltip of the corrosion stage says the next stage comes in about 50m of watering

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
- **THEN** the Warehouse row shows 1,024 under the wheat icon, the Field Head row shows 40/64, 12/64 and 5/8 under the seed, bone meal and honeycomb icons, and the rows Keep at least, Harvest up to and Fertilize below have number fields with no explanation text beside them

#### Scenario: Produce not learned yet
- **WHEN** a player opens the Stock tab of a head that has not harvested yet
- **THEN** the tab says the produce is learned at the first harvest

#### Scenario: Stock tab never marked
- **WHEN** a head's buffer holds no seeds and its field is dry
- **THEN** the Water tab has a red marker and the Stock tab has none

### Requirement: Field diagnostics
`/servus_warehouse field <pos>` SHALL print, for the Field Head at that position, its status, rails, plots, planted and ripe counts, water level, crop or mode, whether a harvest is due, the corrosion stage and the water counted toward the next aging step. `/servus_warehouse field <pos> recheck` SHALL run the pass over all plots again. `/servus_warehouse field <pos> age <mB>` SHALL count that much water as used for aging, with the same upkeep and stage rules as water used by hydration, so a stage can be reached quickly for testing.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails, the head's water level and the corrosion stage

#### Scenario: Age by command
- **WHEN** an operator runs the age command with `corrosion_water` mB on a head of a copper field without anti-corrosion items
- **THEN** the field becomes exposed

### Requirement: Open plots tracking
Besides planted and ripe plots, the head SHALL know which plots are open: a plot is to till when it is empty and stands on a dirt or grass block, and to plant when it is empty and stands on farmland. It SHALL learn this from the same events and passes as ripeness, and also from blocks broken or placed one below a plot and from blocks changed by a tool one below a plot. It SHALL NOT scan plots every tick.

#### Scenario: Dirt under an empty plot
- **WHEN** a field is loaded with dirt under one empty plot and farmland under another
- **THEN** the head counts one plot to till and one plot to plant

#### Scenario: Hoed by a player
- **WHEN** a player tills the dirt under an empty plot with a hoe
- **THEN** a tick later the head counts that plot to plant instead of to till

#### Scenario: Planted by a player
- **WHEN** a player plants wheat seeds on farmland under a plot to plant
- **THEN** a tick later the head counts that plot planted and not open

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

### Requirement: Plots in sight of the rails
A field's plots SHALL be the positions at rail level within the field radius of a farm rail that the rails see in straight lines. Every rail looks straight out, at 90 degrees, on each side without a farm rail next to it; the first plot that is not open ends that line. Where a rail has two such sides at a right angle, at the end of a run or on the outside of a bend, it also sees the corner between them along an L: straight out on one side, then 90 degrees towards the other, every plot of the L open. Nothing is seen around a blocker. A plot is open when it holds air, a crop or a replaceable plant such as short grass, over dirt, a grass block or farmland; pipes, garages, the Field Head, paths, water and gaps without soil are not. Filling a gap brings the plots behind it back. The harvester clears a replaceable plant before it tills or plants.

#### Scenario: Dirt behind a gap
- **WHEN** dirt lies two plots beside a rail and the plot between holds no soil
- **THEN** the far dirt is not part of the field and is not counted to till

#### Scenario: Pipe beside the rail
- **WHEN** a pipe stands on the plot next to a middle rail of a run
- **THEN** the plots straight behind the pipe are out of sight, even when soil around the pipe would join them

#### Scenario: Corner at the end of a run
- **WHEN** the plots past the end rail and beside it are soil
- **THEN** the corner square between them is part of the field

#### Scenario: Gap filled
- **WHEN** the player puts dirt into the gap
- **THEN** the far plot joins the field and counts to till

#### Scenario: Overgrown grass
- **WHEN** a grass block beside a rail has short grass on it
- **THEN** the plot counts to till and the harvester clears the grass and tills it

#### Scenario: Beyond the radius
- **WHEN** a field has radius 2 and open soil lies three blocks beside a rail
- **THEN** that soil is not part of the field

### Requirement: Harvest threshold per head
Each Field Head SHALL keep its own harvest threshold, the ripe share of the planted plots that makes a harvest due, set in its GUI with down and up buttons in steps of 10 percent from 10 to 100 percent and saved with the head. A head that was never set follows `harvest_share` from `field.json`. The GUI SHALL show the ripe and planted counts with a bar of the ripe share and a mark at the threshold; the bar turns green once the harvest is due.

#### Scenario: Lower threshold
- **WHEN** one of four planted plots is ripe and the player lowers the threshold from 50 to 20 percent
- **THEN** the harvest becomes due

#### Scenario: Default threshold
- **WHEN** a Field Head is placed and `harvest_share` is 0.5
- **THEN** its GUI shows "Harvest at 50%"

### Requirement: Field radius
How far a field reaches to each side of its farm rails SHALL be the radius of its Field Head: `radius` from `field.json` (example 2), plus one for each Range Upgrade in the head, at most `max_radius` from `field.json` (example 4). The Range Upgrade SHALL be a crafted item. The Field Head GUI SHALL have one upgrade slot that takes only Range Upgrades, and only as many as the radius can still grow. Putting an upgrade in or taking one out SHALL change the field at once: its plots, hydration, ripe share and the plots the harvester works follow the new radius without the player touching a rail. The upgrades SHALL be kept when the world is saved and SHALL drop when the head is broken.

#### Scenario: A new head reaches two blocks
- **WHEN** a player places a Field Head beside a farm rail with no upgrade and `radius` is 2
- **THEN** the field reaches 2 blocks to each side of every rail

#### Scenario: Two upgrades reach four blocks
- **WHEN** a player puts two Range Upgrades into the head with `radius` 2 and `max_radius` 4
- **THEN** the field reaches 4 blocks to each side and soil 3 and 4 blocks out joins it

#### Scenario: No upgrade past the maximum
- **WHEN** the head holds two Range Upgrades with `radius` 2 and `max_radius` 4 and the player tries to put in a third
- **THEN** the slot refuses it

#### Scenario: Taking an upgrade out shrinks the field
- **WHEN** the head with two upgrades loses one
- **THEN** farmland 4 blocks out is no longer kept wet and its plots leave the field

#### Scenario: Upgrades drop with the head
- **WHEN** a player breaks a Field Head holding two Range Upgrades
- **THEN** the head and both upgrades drop

### Requirement: Field Head wood and facing
The Field Head SHALL look like a station: board panels in the wood of its recipe under the station iron frame. Its recipe SHALL take planks, the crafted item SHALL carry the wood picked from them, a placed head SHALL show that wood, and the drop of a broken head SHALL carry it again. The head SHALL have a horizontal facing, and its front SHALL face the player who places it.

#### Scenario: Wood from the recipe
- **WHEN** a player crafts a Field Head with birch planks
- **THEN** the item names birch as its wood, the placed head shows birch boards, and breaking it drops a Field Head with birch

#### Scenario: Front faces the player
- **WHEN** a player looking north places a Field Head
- **THEN** the head's front faces south, toward the player

### Requirement: Ripe share bar
The front of the Field Head SHALL show a ripe share bar in its lower panel: a small dark window that fills from the left with wheat gold in a few steps as the ripe share of the planted plots approaches the head's harvest threshold, and that is full and green while a harvest is due. The ripe share shown SHALL be empty when nothing is ripe or the head serves no field. The head SHALL change the ripe share shown only when its ripe count, planted count, crop, harvest only mode or threshold changes, never by looking at the field per tick. While the head has a problem, the same problems that put a red marker on a tab of its GUI (it serves no field, its work status is a warning such as no controller, no crop, no Harvester shuttle or no way to the field, the field is dry or corroded, or the fertilizing is held up), the bar SHALL be full and red instead, so the player sees it without opening the GUI; once the problem is gone the bar SHALL show the ripe share again. The head SHALL notice problems that come from the warehouse (controller, shuttles, the way to the field) within about a second, by asking its warehouse now and then, not by looking at the field.

#### Scenario: Bar follows the ripe share
- **WHEN** a head without a problem, with wheat set and a threshold of 50 percent, sees one of four planted plots ripe
- **THEN** its bar shows wheat gold, part way filled, and not green

#### Scenario: Bar turns green when due
- **WHEN** the ripe share reaches the head's threshold and a harvest is due
- **THEN** the bar is full and green

#### Scenario: Lowering the threshold
- **WHEN** the threshold is lowered in the GUI to the current ripe share
- **THEN** the bar turns green without any crop changing

#### Scenario: Problem turns the bar red
- **WHEN** a head's field has water, wheat set and a Harvester shuttle in a garage of its warehouse, and the player then clears the crop
- **THEN** the bar on the head turns full and red, and the Field tab of its GUI has a red marker

#### Scenario: Unreachable field
- **WHEN** the only Harvester shuttle cannot reach the head's field without riding a lift car
- **THEN** the bar on the head is red

### Requirement: Water window
Each side of the Field Head SHALL show a dark glass window in its upper panel, with the head's water drawn as real water (its still texture and the biome's water tint) from the bottom of the window up to the fill share of the buffer. An empty buffer SHALL show the dark glass only. The client SHALL learn the water level in a few steps of the window height (for example eighths), and only when the step changes, not on every mB.

#### Scenario: Water rises in the window
- **WHEN** a pipe fills an empty head to half of its capacity
- **THEN** the windows show water up to half their height

#### Scenario: Small changes stay quiet
- **WHEN** the field uses a few mB and the level stays within the same step
- **THEN** nothing is sent to the clients for the water

### Requirement: Crop icon on top
The top of the Field Head SHALL show a dark inset plate with the field's crop item drawn flat on it. The plate SHALL stay empty when no crop is set or harvest only is on. Players near the head SHALL see a new crop on the plate as soon as it is set.

#### Scenario: Crop shown on top
- **WHEN** a player sets carrots as the field's crop
- **THEN** the head's top shows a carrot on the plate

#### Scenario: Harvest only clears the plate
- **WHEN** harvest only is turned on
- **THEN** the plate is empty

### Requirement: Look steps are pure logic
The mapping from planted count, ripe count, threshold and due state to a bar step, and from water amount and capacity to a window level, SHALL be pure Java without Minecraft imports and covered by JUnit tests. Any water above nothing SHALL show at least the lowest level, and only a full buffer SHALL show the top level.

#### Scenario: Water levels
- **WHEN** a buffer of 8000 mB holds 1 mB, 4000 mB or 8000 mB
- **THEN** the window level is the lowest step, half, and full

### Requirement: Fertilize settings per head
Each Field Head SHALL keep its own fertilizer (an item of the tag `servus_shuttles:fertilizer`, bone meal by default in the tag, or none), a fertilize mode (Off, After planting, Until ripe share), a fertilize threshold in percent (default from `fertilize_share` in `field.json`, example 0.5) and a Keep in stock reserve (default from `fertilizer_reserve`, example 64, never below 0). Fertilizing SHALL be on only while a fertilizer is chosen and the mode is not Off. The settings SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** fertilizing is off, no fertilizer is chosen and the reserve is `fertilizer_reserve`

#### Scenario: Reserve never below zero
- **WHEN** the reserve is 5 and the player clears its field and presses Enter, or a client sends -10 as the reserve
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

### Requirement: Supply buffer
Each Field Head SHALL keep a buffer of three parts: seeds of its crop up to `seed_buffer` (in `field.json`, example 64), its fertilizer up to `fertilizer_buffer` (example 64), and anti-corrosion items up to `anti_corrosion_level` (example 8). Only supply runs SHALL fill it, and only the field's Harvester and Spreader SHALL take from it. Players, hoppers and pipes SHALL neither put items in nor take them out: the head SHALL register no item capability, no packet inlet and no upkeep. Each part SHALL take slot locks (see `shuttles/slot-locks`): a supply run reserves the room it fills and a field shuttle reserves the items it takes, and nobody else fills reserved room or takes reserved items. The buffer and its locks SHALL be kept when the world is saved, a head saved without locks SHALL load with its buffer unlocked, and the buffer's items SHALL drop when the head is broken.

#### Scenario: Buffer survives a reload
- **WHEN** a head with 40 wheat seeds, 12 bone meal and 5 honeycomb in its buffer is saved and loaded
- **THEN** its buffer holds the same items

#### Scenario: Locks survive a reload
- **WHEN** a supply run has locked room for 24 wheat seeds in a head's buffer and the server restarts before the shuttle arrives
- **THEN** after the restart the room is still reserved for that run and the shuttle fills it

#### Scenario: Broken head drops its buffer
- **WHEN** a player breaks a head with 40 wheat seeds in its buffer
- **THEN** the 40 wheat seeds drop with the head

#### Scenario: Not on the data line
- **WHEN** a data line duct touches a Field Head
- **THEN** the duct finds no packet inlet and no upkeep on the head

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
The Stock tab SHALL show the harvest limit in the row "Harvest up to" (0 for no limit), the fertilize level in the row "Fertilize below" (0 for always), both in the produce column, and the fertilizer and anti-corrosion reserves in the row "Keep at least" in their columns, each as a number field the player types into. What a field does and what 0 means SHALL be in its tooltip, not on the page. A field SHALL take digits only, and SHALL send its number to the server when the player presses Enter or the field loses focus; an empty field counts as 0. The server SHALL clamp a level to 0 to 99,999 and a reserve to 0 to 9,999, SHALL keep the two levels in order as for the stock levels, and SHALL refuse a player who can no longer use the head. Each field SHALL then show the value the head keeps, but SHALL NOT be overwritten while the player types in it.

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
Each Field Head SHALL keep its own anti-corrosion item: an item of the tag `servus_shuttles:anti_corrosion`, the first item of the tag until the player chooses one, or Off. While it is Off the anti-corrosion part of the buffer SHALL have no level, the items it holds SHALL count as stale, and the field SHALL age without upkeep. The choice SHALL be kept when the world is saved, and the server SHALL refuse an item outside the tag.

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

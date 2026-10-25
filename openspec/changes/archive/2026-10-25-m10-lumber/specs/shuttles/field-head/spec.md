## ADDED Requirements

### Requirement: Field kind
Each Field Head SHALL keep a field kind, Crops or Trees, chosen with two buttons on the Field tab above the picker and saved with the head; a head saved without one SHALL load as Crops. In a Trees field the chosen sapling SHALL take the place of the crop wherever the field specs speak of the crop or its seeds: the picker, the seed part of the supply buffer, supply runs, the Stock tab and the crop icon on top. Switching the kind SHALL clear the crop or sapling.

#### Scenario: Switch to Trees
- **WHEN** a player opens a head with wheat set and presses Trees on the Field tab
- **THEN** the field is a Trees field with no sapling set, and the picker lists saplings

#### Scenario: Saplings in the supply buffer
- **WHEN** a Trees field has oak saplings set, its buffer holds none and the warehouse holds oak saplings
- **THEN** a supply run brings oak saplings into the head's seed part

#### Scenario: Old head
- **WHEN** a head saved before field kinds existed is loaded
- **THEN** it is a Crops field with its crop as before

### Requirement: Sapling picker
In a Trees field the picker SHALL list "None, fell only" first, then every item of the item tag `minecraft:saplings` that places a block, mods' saplings included, in picker order, with warehouse stock and search like the crop picker. An item of the tag `servus_shuttles:saplings_2x2` SHALL get a second entry for 2x2 squares; an item of `servus_shuttles:saplings_2x2_only` SHALL have only the 2x2 entry. The server SHALL refuse an item outside the lists.

#### Scenario: Spruce twice
- **WHEN** a player opens the picker of a Trees field
- **THEN** it lists Spruce Sapling and Spruce Sapling 2x2, Dark Oak Sapling only as 2x2, and Oak Sapling once

#### Scenario: Fell only
- **WHEN** the player chooses "None, fell only"
- **THEN** the field has no sapling and is in fell only mode, so grown trees are felled and nothing is planted

#### Scenario: Refuse a crop in a Trees field
- **WHEN** a client sends wheat seeds as the sapling of a Trees field
- **THEN** the field's sapling does not change

### Requirement: Picker order
The crop, sapling, fertilizer and anti-corrosion pickers of the Field Head SHALL list their first entry ("None" or "Off") first, then the entries that can be chosen now before the ones that cannot (such as a 2x2 sapling without a Range Upgrade), each part by warehouse stock, most first, then by name. Without a warehouse the stock SHALL not count. The order SHALL be set when the list opens or the search changes, so rows do not move while stock updates.

#### Scenario: Stock first
- **WHEN** the warehouse holds 300 birch saplings, 12 oak saplings and no spruce saplings and a head without a Range Upgrade opens its sapling picker
- **THEN** the list shows None, fell only, then Birch Sapling, Oak Sapling, then the saplings without stock by name, and the 2x2 entries last

#### Scenario: Search keeps the order
- **WHEN** the player types "oak" in that picker
- **THEN** Oak Sapling comes before Dark Oak Sapling 2x2 and Pale Oak Sapling 2x2

### Requirement: Big trees need a Range Upgrade
A 2x2 entry SHALL be chosen only while the head holds at least one Range Upgrade; without one it SHALL be drawn greyed with a hint that it needs a Range Upgrade and the server SHALL refuse it. When the last Range Upgrade leaves a head with a 2x2 sapling set, the setting SHALL stay, no square SHALL be planted and the field work status SHALL say that 2x2 trees need a Range Upgrade, as a problem.

#### Scenario: No upgrade, no 2x2
- **WHEN** a head without a Range Upgrade shows its sapling picker
- **THEN** Dark Oak Sapling 2x2 is greyed and choosing it changes nothing

#### Scenario: Upgrade taken out
- **WHEN** a 2x2 dark oak field loses its only Range Upgrade
- **THEN** the Field tab has a red marker and its status says 2x2 trees need a Range Upgrade

### Requirement: Tree spots
In a Trees field only spots SHALL be planted: for single saplings every other plot in both directions (plots whose x and z are both even), for 2x2 saplings squares of four plots with one plot between squares (squares whose smaller corner has x and z divisible by 3) whose four plots all belong to the field. Plots that are no spot SHALL be left alone and SHALL NOT count as planted or open.

#### Scenario: Every other plot
- **WHEN** a Trees field with oak saplings set has a strip of empty grass plots from x 0 to x 5 at z 0 beside a rail
- **THEN** only the plots at x 0, 2 and 4 count as spots to plant

#### Scenario: Square cut off
- **WHEN** one plot of a square lies under the Field Head instead of on the field
- **THEN** that square is not planted

### Requirement: Trees ready to fell
In a Trees field a spot holding a sapling SHALL count as planted, a spot holding a log of a grown tree as planted and ripe, and an empty spot whose block below lets the sapling grow as open to plant. A log on a plot that is no spot SHALL count as a tree ready to fell too, so trees standing when the field is set up are felled. Ripe share, harvest threshold, the ripe share bar, fertilizing and the stock levels SHALL use these counts.

#### Scenario: Sapling grows
- **WHEN** an oak sapling on a spot of a Trees field grows into a tree
- **THEN** the head counts one more ripe plot

#### Scenario: Old forest
- **WHEN** a Field Head is placed beside farm rails between three standing birch trees
- **THEN** after its first pass the head counts three trees ready to fell

#### Scenario: Harvest limit in logs
- **WHEN** a Trees field has learned oak logs as its produce, a harvest limit of 500 and the warehouse holds 640 oak logs
- **THEN** no Lumber shuttle is sent for grown trees

## MODIFIED Requirements

### Requirement: Water buffer
A Field Head SHALL expose a fluid handler on every side that accepts water and refuses every other fluid, so the Foundation fluid pipe and other mods can fill it. Its buffer SHALL hold up to `capacity` mB (example 8000). It SHALL NOT give water back out. While it serves a Crops field, the head SHALL use `water_per_rail` mB (example 1) per rail of its field every `water_interval` ticks (example 100). When the buffer holds less than one interval's use, the field SHALL stop being hydrated until enough water is in the buffer again. A Trees field SHALL never be hydrated and SHALL use no water, so its rails do not age. The water level SHALL be kept when the world is saved.

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

#### Scenario: Trees stay dry
- **WHEN** a head serving a Trees field of five rails holds 4000 mB of water
- **THEN** after ten minutes it still holds 4000 mB and the field's corrosion stage is unchanged

### Requirement: Ripeness tracking
The plots of a field SHALL be the positions at the rail's level inside the hydrated squares, except the rail positions; a Trees field SHALL have the same squares without being hydrated. The head SHALL know which plots hold an age crop (wheat, carrots, potatoes, beetroot and other crops of that kind) and which of those are ripe, and in a Trees field which spots hold a sapling and which a grown tree. It SHALL learn this from crop growth, a sapling growing into a tree, bone meal, a block being broken or placed on a plot and farmland being trampled, and from one pass over all plots when the field is loaded, changed or rechecked by command. It SHALL NOT scan plots every tick. The ripe share SHALL be the ripe plots divided by the planted plots. The head SHALL signal harvest due when its field has a crop set or is in harvest only mode, at least one plot is planted and the ripe share reaches `harvest_share` (example 0.5).

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

#### Scenario: Bone meal grows a tree
- **WHEN** a player uses bone meal on a birch sapling on a spot until it grows into a tree
- **THEN** a tick later the head counts that spot ripe

#### Scenario: Square grows
- **WHEN** a square of four dark oak saplings grows into a dark oak
- **THEN** the head counts the four plots of the square ripe

### Requirement: Plots in sight of the rails
A field's plots SHALL be the positions at rail level within the field radius of a farm rail that the rails see in straight lines. Every rail looks straight out, at 90 degrees, on each side without a farm rail next to it; the first plot that is not open ends that line. Where a rail has two such sides at a right angle, at the end of a run or on the outside of a bend, it also sees the corner between them along an L: straight out on one side, then 90 degrees towards the other, every plot of the L open. Nothing is seen around a blocker. A plot is open when it holds air, a crop or a replaceable plant such as short grass, over dirt, a grass block or farmland; in a Trees field a sapling, a log, leaves or a hanging part of a tree also keep a plot open, over any block a sapling grows on. Pipes, garages, the Field Head, paths, water and gaps without soil are not. Filling a gap brings the plots behind it back. The harvester clears a replaceable plant before it tills or plants.

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

#### Scenario: Trunk in the line
- **WHEN** a Trees field has a grown oak on the spot next to a rail and an empty spot behind it
- **THEN** the spot behind the oak is part of the field

### Requirement: Field work status
The Field Head SHALL show in its GUI, and the field command SHALL print, what its served field is doing or the first thing that keeps a harvester away, checked in this order: rails not linked to a warehouse, warehouse without exactly one controller, no crop and no harvest only, no Harvester shuttle in a garage of that warehouse, no Harvester shuttle that can reach the field without riding a lift car, a run queued or under way, harvest due, harvest only waiting for ripe crops, plots to work but no water, plots to work, farmland but no seeds in the head's buffer and none in stock, farmland waiting for a supply run with seeds, farmland but no shuttle that can bring seeds, everything growing, no plot to till or plant. The warehouse SHALL NOT send a run to a field no harvester can reach without a lift.

A Trees field SHALL use the same order with the Lumber shuttle in place of the Harvester, saplings in place of seeds and spots in place of farmland, SHALL skip the water check, and SHALL add, right after the way check, that 2x2 trees need a Range Upgrade and that a tree is too large to fell; both are warnings and only the first is a problem.

#### Scenario: Field without a controller
- **WHEN** a served field's farm rails are linked to rails, a garage and racks but no Warehouse Controller
- **THEN** the Field Head GUI says the warehouse has no controller

#### Scenario: Ready field
- **WHEN** the warehouse is online, a Harvester shuttle sits in its garage, wheat is set, the head has water and grass plots lie in the strip
- **THEN** the GUI says work is ready and a harvester is coming

#### Scenario: Field on another floor
- **WHEN** the only Harvester shuttle's garage stands on the floor below the field and an elevator shaft is the only way up
- **THEN** the GUI says no harvester can reach the field without a lift, and no harvester leaves its garage

#### Scenario: Trees without a Lumber shuttle
- **WHEN** a Trees field with oak saplings set has grown trees and the warehouse has only a Harvester shuttle
- **THEN** the GUI says there is no Lumber shuttle in a garage and the Field tab has a red marker

#### Scenario: Dry trees are fine
- **WHEN** a Trees field with birch saplings set has empty spots, saplings in the head's buffer and no water
- **THEN** the GUI says work is ready and no tab has a red marker for water

### Requirement: Field diagnostics
`/servus_warehouse field <pos>` SHALL print, for the Field Head at that position, its status, its field kind, rails, plots, planted and ripe counts, water level, crop, sapling or mode (2x2 included), whether a harvest is due, the corrosion stage and the water counted toward the next aging step. `/servus_warehouse field <pos> recheck` SHALL run the pass over all plots again. `/servus_warehouse field <pos> age <mB>` SHALL count that much water as used for aging, with the same upkeep and stage rules as water used by hydration, so a stage can be reached quickly for testing.

#### Scenario: Print a field
- **WHEN** an operator runs the field command on a head serving four rails
- **THEN** the output names four rails, the head's water level and the corrosion stage

#### Scenario: Age by command
- **WHEN** an operator runs the age command with `corrosion_water` mB on a head of a copper field without anti-corrosion items
- **THEN** the field becomes exposed

#### Scenario: Print a Trees field
- **WHEN** an operator runs the field command on a head of a 2x2 spruce field
- **THEN** the output names the Trees kind and Spruce Sapling 2x2

## Purpose

Defines the Brewhouse of Servus: Brewery: the bricked firebox multiblock with its two fire doors, the copper Brew Kettle that brews grist, hops and water into wort, the Malt Kiln that dries green malt into pale, dark or wheat malt, the wort fluids, and how players and automation use them.

## ADDED Requirements

### Requirement: Brewhouse parts
Servus: Brewery SHALL add two blocks: Brewhouse Bricks, which look like vanilla bricks with an iron top plate and are crafted from Bricks and Iron, and the Brew Kettle, a copper block crafted from Copper Ingots. Each SHALL drop itself when broken.

#### Scenario: Crafting the parts
- **WHEN** a player crafts eight Bricks blocks around one Iron Ingot
- **THEN** they get Brewhouse Bricks (example: eight)

#### Scenario: Loose parts
- **WHEN** Brewhouse Bricks and a Brew Kettle are placed on their own
- **THEN** they are plain full blocks, the bricks with an iron top and the kettle block in copper, and they heat and brew nothing

### Requirement: Forming the Brewhouse
A Brewhouse SHALL form when a player uses the Configuration Carrot on one of its loose parts and the parts stand in the Brewhouse layout: six Brewhouse Bricks in a row three wide and two deep, and four Brew Kettle blocks directly on the two by two of bricks on the left as seen from the front. The player SHALL be told whether it formed or what the layout needs. Formed parts SHALL never join a second Brewhouse.

#### Scenario: Missing kettle
- **WHEN** the six bricks stand in the layout without the kettle blocks and the carrot is used on one of them
- **THEN** nothing forms and the player is told the layout

### Requirement: Brewhouse front
The front of a forming Brewhouse SHALL be the clicked face when it is a side face, otherwise the side facing the player.

#### Scenario: Forming from the front
- **WHEN** the six bricks and four kettle blocks stand in the layout and a player uses the carrot on the front face of a front brick
- **THEN** all ten parts form one Brewhouse facing that way, the front shows two fire doors, the kettle stands on the left and the kiln tray on the right, and the player is told the Brewhouse formed

#### Scenario: Clicking a kettle block
- **WHEN** the carrot is used on a side face of one of the kettle blocks of a complete layout
- **THEN** the Brewhouse forms with its front on that face

### Requirement: Unforming the Brewhouse
Breaking any part of a formed Brewhouse, by hand, by an explosion or by dismantling it with a sneaking carrot click, SHALL unform the whole Brewhouse: every other part SHALL become a loose block again, and the fuel, the kettle's items and the kiln's malt SHALL drop as items. The fluids in the kettle SHALL be lost. A formed Brewhouse SHALL not be pushed by pistons.

#### Scenario: Breaking a brick
- **WHEN** a player breaks a brick of a formed Brewhouse that holds coal, grist and green malt
- **THEN** that brick drops itself, the other nine parts become loose blocks, and the coal, grist and green malt drop as items

### Requirement: Brewhouse firebox
A formed Brewhouse SHALL have one firebox with one fuel slot that takes any item a vanilla furnace burns. A player SHALL add fuel by right clicking any brick with it, and automation SHALL insert fuel through the item capability of the four bricks under the kettle; containers left by burnt fuel SHALL be extractable there. An empty hand right click on a brick under the kettle SHALL tell whether the fire burns and how much fuel is left.

#### Scenario: Fuel by hand
- **WHEN** a player right clicks a brick of a formed Brewhouse with Coal
- **THEN** the Coal goes into the fuel slot

### Requirement: Brewhouse fire
The firebox SHALL light a new fuel item only while the kettle or a kiln tray has work, SHALL burn a lit item for its vanilla burn time, and SHALL be hot while it burns, with the same fire logic as the Tiled Stove. Every formed brick SHALL offer the Delicacies firebox capability, so a heat source that runs on FE can later heat the Brewhouse without changing the kettle or the kiln.

#### Scenario: No work, no fire
- **WHEN** a formed Brewhouse holds fuel and neither the kettle nor a tray has anything to do
- **THEN** no fuel burns and the Brewhouse stays cold

### Requirement: Brewhouse look
While the firebox burns, both fire doors SHALL glow with light around them and flicker, and flames and crackling SHALL come from them now and then; when the fire goes out they SHALL return to cold. While the kettle brews, steam SHALL rise from the top of the vent pipe. A kiln tray SHALL show whether it is empty or holds green, pale or dark malt, and SHALL give off a little vapour while it dries over a lit fire. The kettle, its dome and its vent pipe SHALL be copper on every face.

#### Scenario: Steam while brewing
- **WHEN** the kettle brews over a lit fire
- **THEN** steam rises from the vent pipe, and it stops when the brew is done or the fire goes out

### Requirement: Brew Kettle
The Brew Kettle SHALL hold up to four input stacks (one item kind per stack), a water tank and a wort tank (example size: 4000 mB each, from data). It SHALL run a brewing recipe when its stacks hold exactly the recipe's ingredients, the water tank holds the recipe's fluid and the wort tank has room. It SHALL count the time up only while the firebox is hot; at the time it SHALL take the ingredients and the fluid and add the wort.

#### Scenario: Brewing pale wort
- **WHEN** the kettle holds 4 Pale Malt Grist, 1 Hops and 1000 mB Water and the firebox is hot
- **THEN** after the recipe's time (example: 20 seconds) the grist and hops are gone, the water tank holds 1000 mB less and the wort tank holds 1000 mB Pale Wort

#### Scenario: Only while hot
- **WHEN** the kettle holds a full recipe and the Brewhouse has no fuel
- **THEN** no wort is brewed and the steam stays off

#### Scenario: A stray item
- **WHEN** the kettle holds a full recipe and one more item kind no recipe with these ingredients uses
- **THEN** it does not brew

### Requirement: Brewing recipes are data
Brewing recipes SHALL be data of the type `servus_brewery:brewing`: counted item ingredients, a counted fluid ingredient, a fluid result and a time.

#### Scenario: A datapack recipe
- **WHEN** a datapack adds a brewing recipe of 3 Wheat Malt Grist and 1000 mB Water to 1000 mB Wheat Wort
- **THEN** the kettle brews it over a hot fire

### Requirement: Wort fluids
Servus: Brewery SHALL add three fluids, Pale Wort, Dark Wort and Wheat Wort, each with a bucket. Brewery SHALL ship brewing recipes for each: Pale Malt Grist and Hops to Pale Wort, Dark Malt Grist and Hops to Dark Wort, Wheat Malt Grist, Pale Malt Grist and Hops to Wheat Wort, each with water (example amounts: 1000 mB water to 1000 mB wort).

#### Scenario: Worts are told apart
- **WHEN** a recipe asks for Dark Wort
- **THEN** Pale Wort does not match it

### Requirement: Brew Kettle by hand
Right click on a kettle block of a formed Brewhouse SHALL: pour a filled water container into the water tank, fill an empty bucket from the wort tank, and put an item some brewing recipe takes into the input stacks. An empty hand SHALL tell the player the water, the wort and whether it brews; sneaking with an empty hand SHALL take back the last filled input stack.

#### Scenario: Water in, wort out by bucket
- **WHEN** a player right clicks the kettle with a Water Bucket and later, after a brew, with an empty Bucket
- **THEN** the water tank gains 1000 mB and the player gets an empty Bucket, then a Pale Wort Bucket comes out of the wort tank

### Requirement: Malt Kiln
The two bricks on the right of a formed Brewhouse SHALL each be a kiln tray that holds one stack. A tray SHALL dry a stack a kilning recipe takes, keeping the count, only while the firebox is hot. Adding to a tray SHALL start its drying over. Kilning recipes SHALL be data of the type `servus_brewery:kilning` (ingredient, result, time, optional dark result with extra time); Brewery SHALL ship Green Malt to Pale Malt (dark: Dark Malt) and Green Wheat Malt to Wheat Malt.

#### Scenario: Pale malt
- **WHEN** a tray in pale mode holds 16 Green Malt over a lit fire for the recipe's time
- **THEN** it holds 16 Pale Malt and shows pale malt

### Requirement: Kiln modes
Each tray SHALL have a mode, pale or dark, that a player toggles with the Configuration Carrot on it and is told. In pale mode the stack SHALL become the recipe's result after the recipe's time; in dark mode, for a recipe with a dark result, it SHALL become the dark result after the recipe's time plus its dark time. A recipe without a dark result SHALL give its result in both modes.

#### Scenario: Dark malt
- **WHEN** a player toggles a tray to dark mode with the carrot and it dries 16 Green Malt over a lit fire
- **THEN** the malt is still green after the pale time and becomes 16 Dark Malt after the pale and the dark time

#### Scenario: Wheat malt in dark mode
- **WHEN** a tray in dark mode dries Green Wheat Malt
- **THEN** it becomes Wheat Malt after the recipe's time

### Requirement: Malt Kiln by hand
Right click on a tray with an item some kilning recipe takes SHALL put as much of it on the tray as fits, when the tray is empty or holds the same item. An empty hand SHALL take the whole stack from the tray.

#### Scenario: Collecting pale malt
- **WHEN** a player right clicks a tray holding Pale Malt with an empty hand
- **THEN** the player gets the Pale Malt and the tray is empty

### Requirement: Brewhouse automation
Each kettle block of a formed Brewhouse SHALL expose the standard item capability on every side, inserting only items some brewing recipe takes and extracting nothing, and the standard fluid capability, filling only the water tank with fluids some brewing recipe uses and draining only the wort tank. Loose parts SHALL expose no capability.

#### Scenario: Pipes run the kettle
- **WHEN** pipes insert grist and hops into a kettle block, fill it with water and later drain it
- **THEN** the items and water go in, and the pipe drains wort, never water

### Requirement: Kiln automation
Each kiln tray SHALL expose the standard item capability on every side, inserting only what a kilning recipe takes and extracting only finished malt.

#### Scenario: Pipes run the kiln
- **WHEN** a pipe inserts Green Malt into a tray and tries to extract before and after it is dry
- **THEN** nothing comes out while it is green, and the Pale Malt comes out once it is dry

### Requirement: Brewhouse recipes in JEI
When JEI is installed it SHALL show a Brewing category (ingredients, water, wort, time) and a Kilning category (input, result, time, dark result and its extra time), with the Brew Kettle and the Brewhouse Bricks as catalysts.

#### Scenario: Looking up Pale Wort
- **WHEN** a player looks up the recipes of the Pale Wort Bucket's fluid in JEI
- **THEN** the Brewing category shows Pale Malt Grist, Hops and Water making Pale Wort

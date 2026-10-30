# alchemy/venter-equinus Specification

## Purpose
The Venter Equinus is the alchemist's horse's belly: a warm straw bed with a sealed cucurbit in which rot slowly putrefies into Humus, the rotted life that, mixed with Sand, Clay and Water, gives Dirt in bulk.

## Requirements

### Requirement: Humus
Alchemy SHALL add the item Humus. Only the Venter Equinus SHALL make it; it SHALL have no crafting recipe and no calcination recipe, and it SHALL NOT be putrefiable.

#### Scenario: Humus from the bed
- **WHEN** a formed Venter Equinus finishes a batch and a player empties its Bed Hatch
- **THEN** the player holds Humus

#### Scenario: Not calcined
- **WHEN** a pipe offers Humus to a formed Calciner
- **THEN** the Calciner accepts none

#### Scenario: Only one way
- **WHEN** a player looks up Humus in JEI
- **THEN** the only way shown to make it is the Putrefaction category

### Requirement: Putrefiable rot
Alchemy SHALL ship the item tag `servus_alchemy:putrefiable` for the rot the Cucurbit takes. The shipped tag SHALL hold Rotten Flesh, Spider Eye, Poisonous Potato, Brown and Red Mushroom, the crops of `c:crops` except Wheat, which goes into Hay Bales for the bed, and the leaves (`minecraft:leaves`). A datapack MAY add or remove items.

#### Scenario: Flesh is rot
- **WHEN** a player uses Rotten Flesh on the Cucurbit of a formed Venter Equinus
- **THEN** the Rotten Flesh goes into the Cucurbit

#### Scenario: Stone is not rot
- **WHEN** a player uses Cobblestone on the Cucurbit of a formed Venter Equinus
- **THEN** the Cucurbit takes nothing

### Requirement: Putrefaction recipes are data
A putrefaction recipe SHALL be a datapack recipe of type `servus_alchemy:putrefaction` with one counted item ingredient, one result with count, the time in ticks in a full bed of fresh straw and the time in ticks in an empty bed. When the Cucurbit holds rot for several recipes, the recipe whose id sorts first among those with enough rot SHALL run.

#### Scenario: Shipped recipe
- **WHEN** the shipped data holds a recipe of 8 putrefiable items for 1 Humus, 1,200 ticks warm and 4,800 ticks cold
- **THEN** a formed Venter Equinus with 8 Rotten Flesh and a full bed of fresh straw makes 1 Humus in 60 seconds

#### Scenario: Mixed rot
- **WHEN** the Cucurbit holds 5 Rotten Flesh and 3 Spider Eyes
- **THEN** one batch takes all eight and makes 1 Humus

### Requirement: Bedding is data
Which items are bedding SHALL be a data map `servus_alchemy:bedding` on items, giving for each item the work ticks one item keeps the bed warm. Alchemy SHALL ship the Hay Bale as bedding (example 6,000 ticks). Items without an entry SHALL NOT be bedding.

#### Scenario: Hay Bale
- **WHEN** a player uses a Hay Bale on a Dung Bed of a formed Venter Equinus with room for straw
- **THEN** the bed holds one more bale of straw

#### Scenario: Wheat is not bedding
- **WHEN** a pipe offers Wheat to a Dung Bed
- **THEN** the Dung Bed takes nothing

### Requirement: Venter Equinus layout
The Venter Equinus SHALL be a bed of 3 by 3 blocks on one level: the Cucurbit in the middle, the Bed Hatch in the middle of one side, which is the build's front, and a Dung Bed on each of the other seven cells. Nothing SHALL be required above or below the bed.

#### Scenario: Layout from above
- **WHEN** a player looks down on a complete Venter Equinus with its front at the bottom
- **THEN** they see the rows `D D D`, `D C D`, `D H D` (D Dung Bed, C Cucurbit, H Bed Hatch)

### Requirement: Forming the Venter Equinus
Right clicking the Cucurbit, a Dung Bed or the Bed Hatch with Paracelsus' Journal SHALL check the bed in each of the four horizontal facings and form it in the first that matches, making the player its owner, and SHALL NOT open the book. When nothing matches it SHALL name the first missing block of the best facing and where it belongs. The Configuration Carrot SHALL NOT form, turn or dismantle any block of the build.

#### Scenario: Formed
- **WHEN** a player right clicks the Cucurbit of a complete bed with the Journal
- **THEN** the bed forms and the player sees "The Venter Equinus is ready."

#### Scenario: Form from the frame
- **WHEN** a player right clicks a Dung Bed of a complete bed with the Journal
- **THEN** the bed forms as from the Cucurbit

#### Scenario: Any side may be the front
- **WHEN** the Bed Hatch stands east of the Cucurbit and Dung Beds fill the other cells
- **THEN** the bed forms with its front facing east

#### Scenario: Corner missing
- **WHEN** only the Dung Bed in the back left corner is missing
- **THEN** nothing forms and the player is told a Dung Bed is needed in the back left corner

### Requirement: Breaking unforms the Venter Equinus
Breaking the Cucurbit, the Bed Hatch or a Dung Bed of a formed Venter Equinus SHALL unform it; an unformed Venter Equinus SHALL NOT work. A Cucurbit that stays in place SHALL keep its rot, its straw and its Humus while unformed. A broken Cucurbit SHALL drop its rot and its Humus; its straw is lost.

#### Scenario: Dung Bed broken
- **WHEN** a player breaks a Dung Bed of a working Venter Equinus
- **THEN** the bed unforms, stops, and keeps its rot and straw for when it is formed again

#### Scenario: Cucurbit broken
- **WHEN** a player breaks the Cucurbit of a formed Venter Equinus holding 40 Rotten Flesh and 3 Humus
- **THEN** the Cucurbit, the 40 Rotten Flesh and the 3 Humus drop

### Requirement: The Venter Equinus putrefies
The Cucurbit SHALL hold rot up to a capacity from machine stats (example 256 items) and the Bed Hatch one stack of the result. A batch SHALL run only while formed, while the Cucurbit holds the recipe's count of rot and the hatch has room for the result. The rot SHALL be taken and the result added when the batch completes. It SHALL need no power and no fuel.

#### Scenario: Big batch
- **WHEN** a formed Venter Equinus holds 64 Rotten Flesh in a bed kept full of straw
- **THEN** after eight batches the hatch holds 8 Humus and the Cucurbit is empty

#### Scenario: Hatch full
- **WHEN** the hatch holds 64 Humus
- **THEN** no batch starts until Humus is taken out

#### Scenario: Not enough rot
- **WHEN** the Cucurbit holds 7 Rotten Flesh
- **THEN** no batch runs

### Requirement: Warmth from the straw
The bed SHALL hold up to one bedding item per frame block (eight). While a batch runs, the oldest bedding item SHALL rot down by one work tick per tick, and when it is spent the next one burns. Bedding SHALL NOT rot while no batch runs. A batch SHALL advance at a speed between the cold time with no straw and the warm time with a full bed, linear in the straw left.

#### Scenario: Full bed
- **WHEN** a batch runs in a bed holding eight fresh Hay Bales
- **THEN** it completes in the recipe's warm time

#### Scenario: Empty bed
- **WHEN** a batch runs in a bed without straw
- **THEN** it completes in the recipe's cold time and does not stop

#### Scenario: Half full
- **WHEN** a batch of the shipped recipe runs in a bed holding four fresh Hay Bales
- **THEN** it advances at the mean of the warm and the cold speed, so the batch takes about 1,920 ticks

#### Scenario: Idle straw
- **WHEN** a bed full of straw has no rot for an hour
- **THEN** it still holds the same straw

### Requirement: Venter Equinus ports
A formed Venter Equinus SHALL take only putrefiable items through the item capability on every face of the Cucurbit and SHALL give nothing there. Each Dung Bed SHALL take only bedding through the item capability while the bed has room and SHALL give nothing. The Bed Hatch SHALL give only the result through the item capability and SHALL take nothing. Unformed blocks SHALL expose nothing.

#### Scenario: Rot by pipe
- **WHEN** an Item Pipe pushes Spider Eyes and Hay Bales into the Cucurbit
- **THEN** the Spider Eyes go in and the Hay Bales stay in the pipe

#### Scenario: Straw by pipe
- **WHEN** an Item Pipe pushes Hay Bales into a Dung Bed of a bed holding six bales
- **THEN** two Hay Bales go in

#### Scenario: Humus by hopper
- **WHEN** a hopper pulls from the Bed Hatch of a Venter Equinus that has made 3 Humus
- **THEN** the hopper holds 3 Humus

#### Scenario: Nothing in at the hatch
- **WHEN** a pipe pushes Rotten Flesh or Hay Bales into the Bed Hatch
- **THEN** the hatch takes nothing

### Requirement: The Venter Equinus by hand
On a formed Venter Equinus, using putrefiable items on any of its blocks SHALL put them into the Cucurbit as many as fit; using bedding on any of its blocks SHALL lay in as many as the bed has room for; an empty hand on the Bed Hatch SHALL give the Humus; sneaking with an empty hand on the Cucurbit SHALL give the rot back and reset the batch. The Venter Equinus SHALL have no GUI.

#### Scenario: Rot by hand
- **WHEN** a player holding 16 Spider Eyes uses them on the Cucurbit of a formed bed with an empty Cucurbit
- **THEN** the 16 Spider Eyes go in

#### Scenario: Hay by hand
- **WHEN** a player holding 10 Hay Bales uses them on a Dung Bed of a formed bed without straw
- **THEN** 8 Hay Bales go into the bed and the player keeps 2

#### Scenario: Anywhere on the bed
- **WHEN** a player uses Rotten Flesh on the Bed Hatch and a Hay Bale on the Cucurbit of a formed bed
- **THEN** the Rotten Flesh goes into the Cucurbit and the Hay Bale into the straw

#### Scenario: Humus by hand
- **WHEN** a player uses the Bed Hatch with an empty hand and the hatch holds 5 Humus
- **THEN** the player receives the 5 Humus

#### Scenario: Rot back
- **WHEN** a player sneaks and uses the Cucurbit with an empty hand while it holds 12 Rotten Flesh
- **THEN** the player receives the 12 Rotten Flesh

### Requirement: The Venter Equinus shows its work
A formed bed SHALL show its straw as a fill whose height follows the straw left, from rotted litter when the bed is empty to a fill below the wall tops when it is full, with a few dung mats lying on top. Steam SHALL rise from the bed while a batch runs. With Jade, the Cucurbit, the Bed Hatch and every Dung Bed SHALL show the rot, the straw, the warmth, the progress and the Humus.

#### Scenario: Straw level
- **WHEN** the straw of a full bed rots down to half
- **THEN** the straw fill shows at half its height above the litter

#### Scenario: Empty bed
- **WHEN** the last bale has rotted away
- **THEN** the bed shows dark rotted litter and no straw

#### Scenario: Steam
- **WHEN** a batch runs
- **THEN** steam rises from the bed, and none rises while it waits

#### Scenario: Jade
- **WHEN** a player with Jade looks at a Dung Bed of a bed holding 24 Rotten Flesh, four bales and 2 Humus
- **THEN** the tooltip shows Rot: 24, Straw: 4 / 8, the warmth, the progress and Humus: 2

### Requirement: Formed Venter Equinus looks like one bed
A formed bed SHALL read as one frame: four spruce log corner posts, spruce plank walls between them, the hatch in the middle of the front wall, and the Cucurbit, a terracotta gourd with a neck and a cut copper seal, bedded in the straw. The pieces SHALL turn with the build's front. Unformed, each block SHALL show its own plain model.

#### Scenario: Formed look
- **WHEN** a player forms a Venter Equinus
- **THEN** the walls run without a gap from post to post on all four sides and the hatch sits in the front wall

#### Scenario: Unformed look
- **WHEN** a player breaks the Bed Hatch of a formed bed
- **THEN** the Dung Beds and the Cucurbit show their plain models

### Requirement: Venter Equinus crafting
The Cucurbit, the Dung Bed and the Bed Hatch SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Cucurbit recipe
- **WHEN** the player crafts the Cucurbit pattern of Terracotta around a Glass Bottle under a Cut Copper
- **THEN** one Cucurbit is produced

#### Scenario: Dung Bed recipe
- **WHEN** the player crafts the Dung Bed pattern of spruce logs, spruce planks and a Hay Bale
- **THEN** four Dung Beds are produced

### Requirement: Venter Equinus schematic
The journal's Venter Equinus entry SHALL show the bed as a multiblock page with Visualize, standing on the clicked ground, and the crafting recipes of the Cucurbit, the Dung Bed and the Bed Hatch. The schematic SHALL be generated from the same layout that forming checks.

#### Scenario: Visualize
- **WHEN** a player presses Visualize on the Venter Equinus page and clicks the ground
- **THEN** the ghost of the bed lies on that ground

### Requirement: Venter Equinus entry lists its recipes
The journal's Venter Equinus entry SHALL list every shipped putrefaction recipe and the Dirt recipes Alchemy ships for the Mixer from the same data that generates the recipes, one line per recipe. Formatting the lines SHALL be pure Java covered by JUnit.

#### Scenario: Putrefaction line
- **WHEN** the player reads the Venter Equinus entry
- **THEN** a page lists "8 rot: 1 Humus, 60 s in fresh straw, 240 s cold"

#### Scenario: Dirt line
- **WHEN** the player reads the Venter Equinus entry
- **THEN** a page lists "4 Sand, 1 Clay Ball, 1 Humus, 250 mB water: 8 Dirt"

### Requirement: Venter Equinus reports to the journal
The Venter Equinus SHALL remember the player who formed it as its owner and SHALL report every finished batch to the journal of the owner's team as a `putrefy` event with the count it made.

#### Scenario: First Humus
- **WHEN** a player's Venter Equinus finishes its first batch
- **THEN** the team's journal has counted one `putrefy` event of 1

### Requirement: Dirt from the Mixer
Alchemy SHALL ship two `servus_foundation:mixing` recipes that make Dirt (amounts are datapack examples): 4 Sand, 1 Clay Ball, 1 Humus and 250 mB Water give 8 Dirt; 2 Sand, 1 Clay Ball, 1 Bone Meal and 250 mB Water give 2 Dirt. Any Sand of the `c:sands` tag SHALL do.

#### Scenario: Bulk Dirt
- **WHEN** a driven Mixer holds 4 Sand, 1 Clay Ball, 1 Humus and 250 mB Water
- **THEN** it makes 8 Dirt

#### Scenario: Quick Dirt
- **WHEN** a driven Mixer holds 2 Red Sand, 1 Clay Ball, 1 Bone Meal and 250 mB Water
- **THEN** it makes 2 Dirt

### Requirement: Putrefaction logic is pure
The warmth rule (speed from the straw left between the warm and the cold time, bedding rotting only while working) and the batch accounting (a mixed rot store with a capacity, taking a batch's count from the kinds that match) SHALL be pure Java without Minecraft types, covered by JUnit.

#### Scenario: Warmth in a test
- **WHEN** a unit test asks for the ticks of a 1,200 / 4,800 batch with half the straw
- **THEN** it gets 1,920 ticks without starting Minecraft

# foundation/materials Specification

## Purpose

Materials let Servus blocks show the material they were crafted from, such as a wood or a stone, so a base can match its own blocks, with one shared mechanism for every block and every kind of material.

## Requirements

### Requirement: Material kinds
Every material block SHALL belong to exactly one kind. A kind SHALL define which items count as its material in a recipe, the material a block shows when it has none, and the name its tooltip line starts with. There SHALL be two kinds: wood (any planks, default Oak Planks, "Wood") and stone (any item of the stone tag, default Bricks, "Stone").

#### Scenario: Wood kind
- **WHEN** a mod registers a block of the wood kind
- **THEN** its recipe takes any planks and the block shows oak when it has no wood

#### Scenario: Stone kind
- **WHEN** a mod registers a block of the stone kind
- **THEN** its recipe takes any item of `#servus_foundation:stones` and the block shows bricks when it has no stone

### Requirement: Stone tag
Foundation SHALL ship the item tag `#servus_foundation:stones` with the vanilla full cube stone blocks: bricks, stone and its bricks and smooth forms, cobblestone, andesite, diorite and granite with their polished forms, the deepslate, blackstone and tuff families, mud bricks, the nether bricks, the sandstones, quartz, end stone bricks and the prismarine blocks. Datapacks SHALL be able to add or remove entries.

#### Scenario: Vanilla stone in the tag
- **WHEN** the game loads without datapacks
- **THEN** Stone Bricks, Polished Deepslate and Cut Sandstone are in `#servus_foundation:stones` and Stone Brick Slab is not

#### Scenario: Datapack adds a stone
- **WHEN** a datapack adds a modded marble block to `#servus_foundation:stones`
- **THEN** stone kind recipes accept marble and the result carries marble

### Requirement: Material from the recipe
The recipes of the wood blocks (Crafting CPU, Packing Station, Inbound Station, Outbound Station, Storage Terminal, Production Terminal, Pattern Storage, Crafting Station, Warehouse Controller, Shuttle Garage, Shuttle Workshop, Machine Adapter, Field Head, Warehouse Rail, Farm Rail) SHALL accept any planks. Every material recipe SHALL record on the result the material of its kind in the grid: the block that appears most often, and on a tie the middle one in reading order.

#### Scenario: Birch
- **WHEN** the player crafts a Storage Terminal with birch planks
- **THEN** the result carries birch

#### Scenario: Mixed planks
- **WHEN** the player crafts a Shuttle Garage with two spruce planks and one oak plank
- **THEN** the result carries spruce

#### Scenario: Workshop
- **WHEN** the player crafts a Shuttle Workshop with five acacia planks
- **THEN** the result carries acacia

### Requirement: Material pick is pure logic
Choosing the material from the grid SHALL be pure Java without Minecraft classes, covered by unit tests, and the same for every kind.

#### Scenario: Tie picks the middle
- **WHEN** a unit test picks from three different ids in reading order
- **THEN** it returns the second one

### Requirement: Material stays with the block
A placed material block SHALL keep the material of its item, SHALL drop an item carrying that material when mined or dismantled with the Configuration Carrot, and SHALL use its kind's default when no material is set.

#### Scenario: Break and place
- **WHEN** a birch Packing Station is mined and placed again
- **THEN** the placed Packing Station is birch

#### Scenario: No wood
- **WHEN** a Pattern Storage is given by command without a material
- **THEN** it shows oak

### Requirement: Material look
A material block SHALL draw every part of its model that uses its kind's placeholder texture in its material, at the same spot, on the block and on the item; every other part SHALL stay as it is. The placeholder of the wood kind SHALL be the Servus board texture, that of the stone kind the vanilla bricks texture. The item tooltip SHALL name the material after the kind's name.

#### Scenario: Stone kind retextures bricks
- **WHEN** a stone kind block whose model draws a face in the vanilla bricks texture carries Stone Bricks
- **THEN** that face shows stone bricks and its iron parts are unchanged

#### Scenario: Stone tooltip
- **WHEN** the player hovers a stone kind item that carries Polished Deepslate
- **THEN** the tooltip reads "Stone: Polished Deepslate"

### Requirement: Iron and wood look
Each wood block SHALL show a dark riveted iron frame with wooden board panels in its wood and teal accents for lamps, screens and data, on the block and on the item. The rails are the exception: they show their wood on their sleepers. The Shuttle Workshop, which is not on the warehouse network, SHALL show no teal and no lamp; its accent is the yellow of its tool handles. The item tooltip SHALL name the wood. The recipe viewer SHALL show the recipe with any planks.

#### Scenario: Item tooltip
- **WHEN** the player hovers a dark oak Crafting Station
- **THEN** the tooltip reads "Wood: Dark Oak Planks" and the item shows dark oak boards

#### Scenario: Rail sleepers
- **WHEN** the player hovers a stack of jungle Warehouse Rails
- **THEN** the tooltip names Jungle Planks and the item shows jungle sleepers

#### Scenario: Workshop without teal
- **WHEN** the player hovers a birch Shuttle Workshop
- **THEN** the tooltip names Birch Planks and the item shows birch boards with the hanging tools and no teal

#### Scenario: Recipe viewer
- **WHEN** the player looks up the Storage Terminal recipe in the recipe viewer
- **THEN** the planks slots cycle through the planks of every wood

### Requirement: Further material swaps
Besides its material, a material block SHALL be able to swap further textures of its model per block: its block entity names, in its model data, textures of its model and the textures to show in their place. Quads with a named texture SHALL be drawn with the replacement at the same spot; every other quad SHALL stay as it is. Items SHALL show the model without these swaps. Foundation SHALL NOT need to know the blocks or textures that use this.

#### Scenario: Weathered rail head
- **WHEN** a farm rail's block entity names its fresh head texture with the weathered one as replacement
- **THEN** the rail is drawn with weathered heads and its sleepers keep its wood

### Requirement: Stone blocks
The Sink block and the Chimney block SHALL be blocks of the stone kind. Their recipes SHALL accept any item of `#servus_foundation:stones` and record the stone of the grid on the result by the material pick rule; the parts of their models drawn in the vanilla bricks texture SHALL show that stone.

#### Scenario: Mixed stones in a Sink recipe
- **WHEN** the player crafts Sink blocks from five Stone Bricks and three Cobblestone around iron bars
- **THEN** the result carries Stone Bricks

#### Scenario: Chimney from tuff
- **WHEN** the player crafts Chimney blocks from six Tuff Bricks
- **THEN** the result carries Tuff Bricks and its tooltip reads "Stone: Tuff Bricks"

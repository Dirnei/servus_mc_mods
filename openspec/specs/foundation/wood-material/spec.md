# foundation/wood-material Specification

## Purpose
The wood material lets Servus blocks show the wood they were crafted from, so a base can match its own woods, with one shared mechanism for every block that uses it.

## Requirements

### Requirement: Wood from the recipe
The recipes of the wood blocks (Crafting CPU, Packing Station, Inbound Station, Outbound Station, Storage Terminal, Production Terminal, Pattern Storage, Crafting Station, Warehouse Controller, Shuttle Garage, Shuttle Workshop, Machine Adapter, Field Head, Warehouse Rail, Farm Rail) SHALL accept any planks and SHALL record on the result the wood of the planks in the grid: the planks block that appears most often, and on a tie the middle one in reading order.

#### Scenario: Birch
- **WHEN** the player crafts a Storage Terminal with birch planks
- **THEN** the result carries birch

#### Scenario: Mixed planks
- **WHEN** the player crafts a Shuttle Garage with two spruce planks and one oak plank
- **THEN** the result carries spruce

#### Scenario: Workshop
- **WHEN** the player crafts a Shuttle Workshop with five acacia planks
- **THEN** the result carries acacia

### Requirement: Wood stays with the block
A placed wood block SHALL keep the wood of its item, SHALL drop an item carrying that wood when mined or dismantled with the Configuration Carrot, and SHALL use oak when no wood is set.

#### Scenario: Break and place
- **WHEN** a birch Packing Station is mined and placed again
- **THEN** the placed Packing Station is birch

#### Scenario: No wood
- **WHEN** a Pattern Storage is given by command without a wood
- **THEN** it shows oak

### Requirement: Iron and wood look
Each wood block SHALL show a dark riveted iron frame with wooden board panels in its wood and teal accents for lamps, screens and data, on the block and on the item. The rails are the exception: they show their wood on their sleepers. The Shuttle Workshop, which is not on the warehouse network, SHALL show no teal and no lamp; its accent is the yellow of its tool handles. The item tooltip SHALL name the wood. The recipe viewer SHALL show the recipe with any planks.

#### Scenario: Item tooltip
- **WHEN** the player hovers a dark oak Crafting Station
- **THEN** the tooltip names Dark Oak Planks and the item shows dark oak boards

#### Scenario: Rail sleepers
- **WHEN** the player hovers a stack of jungle Warehouse Rails
- **THEN** the tooltip names Jungle Planks and the item shows jungle sleepers

#### Scenario: Workshop without teal
- **WHEN** the player hovers a birch Shuttle Workshop
- **THEN** the tooltip names Birch Planks and the item shows birch boards with the hanging tools and no teal

### Requirement: Further material swaps
Besides the wood, a wood block SHALL be able to swap further textures of its model per block: its block entity names, in its model data, textures of its model and the textures to show in their place. Quads with a named texture SHALL be drawn with the replacement at the same spot; every other quad SHALL stay as it is. Items SHALL show the model without these swaps. Foundation SHALL NOT need to know the blocks or textures that use this.

#### Scenario: Weathered rail head
- **WHEN** a farm rail's block entity names its fresh head texture with the weathered one as replacement
- **THEN** the rail is drawn with weathered heads and its sleepers keep its wood

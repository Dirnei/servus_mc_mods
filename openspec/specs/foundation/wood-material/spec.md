# foundation/wood-material Specification

## Purpose
The wood material lets Servus blocks show the wood they were crafted from, so a base can match its own woods, with one shared mechanism for every block that uses it.

## Requirements

### Requirement: Wood from the recipe
The recipes of the wood blocks (Crafting CPU, Packing Station, Inbound Station, Outbound Station, Storage Terminal, Production Terminal, Pattern Storage, Crafting Station, Warehouse Controller, Shuttle Garage, Machine Adapter) SHALL accept any planks and SHALL record on the result the wood of the planks in the grid: the planks block that appears most often, and on a tie the middle one in reading order.

#### Scenario: Birch
- **WHEN** the player crafts a Storage Terminal with birch planks
- **THEN** the result carries birch

#### Scenario: Mixed planks
- **WHEN** the player crafts a Shuttle Garage with two spruce planks and one oak plank
- **THEN** the result carries spruce

### Requirement: Wood stays with the block
A placed wood block SHALL keep the wood of its item, SHALL drop an item carrying that wood when mined or dismantled with the Configuration Carrot, and SHALL use oak when no wood is set.

#### Scenario: Break and place
- **WHEN** a birch Packing Station is mined and placed again
- **THEN** the placed Packing Station is birch

#### Scenario: No wood
- **WHEN** a Pattern Storage is given by command without a wood
- **THEN** it shows oak

### Requirement: Iron and wood look
Each wood block SHALL show a dark riveted iron frame with wooden board panels in its wood and teal accents for lamps, screens and data, on the block and on the item. The item tooltip SHALL name the wood. The recipe viewer SHALL show the recipe with any planks.

#### Scenario: Item tooltip
- **WHEN** the player hovers a dark oak Crafting Station
- **THEN** the tooltip names Dark Oak Planks and the item shows dark oak boards

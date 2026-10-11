## MODIFIED Requirements

### Requirement: Wood from the recipe
The recipes of the wood blocks (Crafting CPU, Packing Station, Inbound Station, Outbound Station, Storage Terminal, Production Terminal, Pattern Storage, Crafting Station, Warehouse Controller, Shuttle Garage, Machine Adapter, Field Head, Warehouse Rail, Farm Rail) SHALL accept any planks and SHALL record on the result the wood of the planks in the grid: the planks block that appears most often, and on a tie the middle one in reading order.

#### Scenario: Birch
- **WHEN** the player crafts a Storage Terminal with birch planks
- **THEN** the result carries birch

#### Scenario: Mixed planks
- **WHEN** the player crafts a Shuttle Garage with two spruce planks and one oak plank
- **THEN** the result carries spruce

### Requirement: Iron and wood look
Each wood block SHALL show a dark riveted iron frame with wooden board panels in its wood and teal accents for lamps, screens and data, on the block and on the item. The rails are the exception: they show their wood on their sleepers. The item tooltip SHALL name the wood. The recipe viewer SHALL show the recipe with any planks.

#### Scenario: Item tooltip
- **WHEN** the player hovers a dark oak Crafting Station
- **THEN** the tooltip names Dark Oak Planks and the item shows dark oak boards

#### Scenario: Rail sleepers
- **WHEN** the player hovers a stack of jungle Warehouse Rails
- **THEN** the tooltip names Jungle Planks and the item shows jungle sleepers

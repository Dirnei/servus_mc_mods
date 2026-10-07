## MODIFIED Requirements

### Requirement: Rack is obtainable and dismantled
The rack SHALL be craftable from vanilla materials, SHALL spill its contents when broken, and SHALL keep them on the dropped item when dismantled by sneaking with the Configuration Carrot. The Configuration Carrot SHALL NOT turn a rack; its facing is set when it is placed. The crafted rack SHALL remember the wood of the planks it was made from: the planks that appear most often, or the middle one when all three differ.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and planks in the rack pattern (for example ingots in the corners and planks across the middle)
- **THEN** the result is one Heavy Duty Rack

#### Scenario: Spruce rack
- **WHEN** the player crafts the rack with three spruce planks
- **THEN** the rack remembers spruce

#### Scenario: Majority wins
- **WHEN** the planks row is oak, spruce, oak
- **THEN** the rack remembers oak

#### Scenario: Three different woods
- **WHEN** the planks row is birch, spruce, oak
- **THEN** the rack remembers spruce, the middle one

#### Scenario: Dismantle
- **WHEN** a player sneak dismantles a rack with two stacks and places it again
- **THEN** the new rack holds the two stacks

#### Scenario: Carrot click
- **WHEN** a player uses the Configuration Carrot on a rack without sneaking
- **THEN** the rack keeps its facing and its joins and nothing else happens

## ADDED Requirements

### Requirement: Boards show the crafting wood
A rack's boards (back panel, floor and shelves) SHALL show the texture of the planks it remembers, for any block in the planks tag including modded woods. A rack that remembers no wood SHALL show oak. The wood SHALL stay with the rack when it is placed, broken or dismantled, and SHALL show on the item in hand and in inventories, in the item tooltip and in the breaking particles.

#### Scenario: Placed spruce rack
- **WHEN** a player places a rack crafted from spruce planks
- **THEN** its boards show the spruce planks texture

#### Scenario: Modded wood
- **WHEN** a rack is crafted from a modded planks block in the planks tag
- **THEN** its boards show that block's texture

#### Scenario: Item look
- **WHEN** the player holds a spruce rack
- **THEN** the item shows spruce boards and its tooltip names the spruce planks

#### Scenario: Wood survives breaking
- **WHEN** a spruce rack is broken and the dropped item is placed again
- **THEN** the new rack shows spruce boards

#### Scenario: No stored wood
- **WHEN** a rack is given by command without a wood
- **THEN** it shows oak boards

#### Scenario: Mixed woods in a row
- **WHEN** an oak rack and a spruce rack stand side by side with the same facing
- **THEN** they join as one rack and each bay keeps its own boards

### Requirement: Blackened iron frame
Every rack's frame (posts, beams and edges) SHALL show a blackened wrought iron texture, whatever its wood. The recipe cost SHALL stay iron ingots and planks.

#### Scenario: Frame look
- **WHEN** the player looks at any rack
- **THEN** its posts and beams are dark iron, not orange steel

### Requirement: One rack in the creative tab and JEI
The creative tab and the recipe viewer SHALL list one Heavy Duty Rack, the oak one; the recipe viewer SHALL show the rack recipe accepting any planks.

#### Scenario: Creative tab
- **WHEN** the player opens the Servus Storage creative tab
- **THEN** one Heavy Duty Rack with oak boards is listed

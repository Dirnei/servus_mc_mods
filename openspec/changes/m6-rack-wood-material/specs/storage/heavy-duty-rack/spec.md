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

### Requirement: Bays connect visually
Racks with the same facing and the same wood that stand in a row SHALL form sections of up to `bays_per_section` bays (`rack.json`, example 3). A bay joins the section beside it on placement only while that section has room; otherwise it starts a new section. Joins SHALL be decided when a bay is placed and SHALL NOT move when further bays are placed anywhere. A bay placed on top of a row SHALL copy the section boundaries of the row below wherever a rack stands below it or below its neighbour. Within a section the shared posts disappear and the floor board, the middle shelf and the beams run through; posts stand at section ends. A rack directly above or below another rack of the same wood SHALL continue the posts without a top or bottom frame between them. A rack placed directly on top of another rack SHALL take that rack's facing, whatever its wood. Racks of different woods SHALL NOT join, side by side or stacked. Each rack SHALL keep its own inventory. The frame SHALL render without flickering faces.

#### Scenario: Row of three
- **WHEN** three racks facing the same way are placed one after another in a row
- **THEN** only the two outer edges show posts and the shelves and beams run along all three

#### Scenario: Row of five
- **WHEN** five racks are placed one after another from left to right
- **THEN** the first three form a section, the fourth and fifth form a second section, and a post stands between the third and the fourth

#### Scenario: Fourth bay stands alone
- **WHEN** a fourth rack is placed at either end of a row of three
- **THEN** the three keep their joins and the fourth shows posts on both sides

#### Scenario: Bay removed from a row
- **WHEN** the middle bay of a section of three is broken
- **THEN** the two remaining bays each show posts towards the gap and nothing else changes

#### Scenario: Shifted upper row
- **WHEN** a row of three and a row of two stand on the floor and a second row is placed on top, shifted by one bay
- **THEN** the upper row splits above the same post as the lower row

#### Scenario: Different facing
- **WHEN** two racks stand side by side but face different directions
- **THEN** each renders as its own bay

#### Scenario: Separate inventories
- **WHEN** a player fills the left rack of a row
- **THEN** the right rack's GUI stays empty

#### Scenario: Placed on top
- **WHEN** a player looking south places a rack on top of a rack that faces east
- **THEN** the new rack faces east and the two render as one stacked bay

#### Scenario: Different woods
- **WHEN** a spruce rack is placed beside an oak rack with the same facing
- **THEN** each renders as its own bay with posts between them

#### Scenario: Different wood stacked
- **WHEN** a spruce rack is placed on top of an oak rack
- **THEN** it takes the oak rack's facing and both keep their top and bottom frames

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

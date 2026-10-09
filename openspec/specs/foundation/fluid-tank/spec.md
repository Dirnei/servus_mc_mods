# foundation/fluid-tank Specification

## Purpose

The Fluid Tank stores fluids in the world: glass blocks that join into one large rectangular tank, filled through Fluid Pipes, buckets or any mod's fluid handling.

## Requirements

### Requirement: Rectangular tanks form
Fluid Tank blocks SHALL group into tanks that each fill a complete rectangular box, with one shared content and a capacity of the block count times `capacity_per_block` from data. Two touching tanks whose touching faces match exactly SHALL join into one larger tank. A block that completes no box SHALL work as its own tank and SHALL NOT change the tanks it touches. The grouping SHALL survive saving and reloading.

#### Scenario: Two blocks
- **WHEN** a player places a tank block on top of another and `capacity_per_block` is 16,000 mB (example value)
- **THEN** both blocks report one tank of 32,000 mB

#### Scenario: Building a 2 by 2 floor
- **WHEN** a player places three blocks of a 2 by 2 floor in an L shape
- **THEN** two of them form a 32,000 mB tank and the third works on its own, and placing the fourth block forms one 64,000 mB tank holding the fluid of all four

#### Scenario: Block on top of a tank
- **WHEN** a player places one tank block on top of a formed 2 by 2 tank
- **THEN** the 2 by 2 tank stays formed with its content, and the new block works as its own tank

#### Scenario: Completing a layer
- **WHEN** the player completes a 2 by 2 layer of single blocks on top of a formed 2 by 2 tank
- **THEN** the eight blocks form one 2 by 2 by 2 tank holding the fluid of both layers

#### Scenario: Neighbour does not break a tank
- **WHEN** a single tank block touches the side of a formed 2 by 2 by 2 tank
- **THEN** the 2 by 2 by 2 tank stays formed and the single block works on its own

#### Scenario: Box broken open
- **WHEN** one corner block of a formed 2 by 2 by 2 tank is broken
- **THEN** that tank breaks up, its seven remaining blocks group again into boxes by the same rules, and no fluid besides the broken block's share is lost

#### Scenario: Reload
- **WHEN** a world with a formed 2 by 2 tank and a single block on top of it is saved and loaded again
- **THEN** the 2 by 2 tank is still formed and the block on top still works on its own

### Requirement: Placing a whole layer
Using a tank item on the top face of a tank's top layer SHALL place a complete new layer over that tank's footprint when the player is not sneaking, the held stack holds empty tank items for every block of the layer (always in creative), and every spot of the layer is free. It SHALL use one item per block. Otherwise a single block SHALL be placed as usual.

#### Scenario: Grow a 2 by 2 tank
- **WHEN** a player holding 16 empty tank items uses one on the top of a formed 2 by 2 tank
- **THEN** four blocks are placed above it, the player holds 12, and the tank is now 2 by 2 by 2

#### Scenario: Too few items
- **WHEN** the player holds only 3 empty tank items
- **THEN** one block is placed on the clicked block

#### Scenario: Sneaking
- **WHEN** a sneaking player uses a tank item on the top of a formed 2 by 2 tank
- **THEN** one block is placed on the clicked block

#### Scenario: Blocked spot
- **WHEN** one spot of the new layer is taken by another block
- **THEN** one block is placed on the clicked block

### Requirement: Connected look
Blocks of a formed tank SHALL hide their frame edges toward each other so the tank looks like one glass box with a copper frame around its outside. A block SHALL show its frame toward blocks of another tank, and a block working on its own SHALL show its full frame. Instead of a glass bottom, a block with no block of its own tank below it SHALL show its floor block as a thin plate, visible from below and from inside. Tank blocks SHALL let light pass like air and cast no ambient occlusion shadow.

#### Scenario: Formed tank
- **WHEN** four blocks form a 2 by 2 tank
- **THEN** no frame shows between the blocks, only around the outside of the box

#### Scenario: Block on top
- **WHEN** a single block stands on top of a formed 2 by 2 tank
- **THEN** a frame shows between the single block and the tank below it

#### Scenario: Floor
- **WHEN** a player looks down into an empty tank crafted with stone bricks, or up at it from below
- **THEN** they see a stone brick floor, not the blocks beneath the tank

#### Scenario: Two layers
- **WHEN** a 2 by 2 tank is two layers high
- **THEN** only the lower layer shows its floor and no floor shows between the layers

#### Scenario: Even light
- **WHEN** a 3 by 3 tank stands in daylight
- **THEN** its middle column is as bright as its edges

### Requirement: One fluid per tank
A tank SHALL hold one fluid at a time and SHALL refuse a different fluid until it is empty. A tank block SHALL not be placed when the touching tank blocks and the placed item hold more than one fluid, and the player SHALL be told why.

#### Scenario: Wrong fluid
- **WHEN** a tank holding water is offered lava
- **THEN** it accepts none

#### Scenario: Joining two fluids
- **WHEN** a player places a tank block touching a water tank and a lava tank
- **THEN** the block is not placed and the player sees a message that the tanks hold different fluids

### Requirement: Fills from the bottom
A formed tank's fluid SHALL settle in its lowest layer first and rise layer by layer, every block of a layer holding the same amount. Each block SHALL render its own fill level with the fluid's texture and color, and the client SHALL follow the content within a second.

#### Scenario: Tower half full
- **WHEN** a tower of four tank blocks holds half its capacity
- **THEN** the lower two blocks render full and the upper two empty

#### Scenario: Partly filled layer
- **WHEN** a 2 by 2 by 2 tank holds six blocks' worth of fluid
- **THEN** the bottom layer renders full and every top block half full

### Requirement: Fluid handler on every face
Every block of a tank SHALL expose its tank's content as a fluid handler on every face, so pipes and other mods' blocks can fill and drain it from any block of the tank.

#### Scenario: Fill from the top, drain from the bottom
- **WHEN** water is filled into the top block of a formed tower and drained from the bottom block
- **THEN** both act on the same content

### Requirement: Buckets
Using a filled bucket on a tank SHALL empty it into the tank when the tank has room and holds that fluid or nothing. Using an empty bucket SHALL fill it from the tank when the tank holds at least one bucket's worth; a player in creative mode SHALL keep the empty bucket and get the filled bucket into the inventory unless it holds one already, like vanilla buckets. Using an empty hand SHALL tell the player the fluid, the amount and the capacity.

#### Scenario: Water bucket
- **WHEN** a player uses a water bucket on an empty tank
- **THEN** the tank holds 1,000 mB of water and the player holds an empty bucket

#### Scenario: Creative scoop
- **WHEN** a creative player uses an empty bucket on a tank holding Salt
- **THEN** the tank loses 1,000 mB, the player still holds the empty bucket and a Salt bucket is in the inventory

#### Scenario: Read the tank
- **WHEN** a player right clicks a tank with an empty hand
- **THEN** the player sees the fluid name and the amount and capacity in mB

### Requirement: Broken block keeps its share
A broken tank block SHALL drop as a tank item carrying the fluid that was in that block and its floor block, show the fluid and amount and the floor in its tooltip, and add that fluid back and show that floor when placed. Fluid that was in the other blocks SHALL stay in them.

#### Scenario: Move a full block
- **WHEN** a single full tank block of water is broken and placed elsewhere
- **THEN** the new tank holds the same amount of water

#### Scenario: Top of a tower
- **WHEN** the empty top block of a half full tower is broken
- **THEN** it drops an empty tank item and the remaining tower keeps all its fluid

#### Scenario: Floor travels with the item
- **WHEN** a tank block with a stone brick floor is broken and placed elsewhere
- **THEN** the new block shows a stone brick floor

### Requirement: Crafting
The Fluid Tank SHALL be crafted from copper ingots in the corners, glass on the top, left and right edges, an empty centre, and one solid full block without a block entity in the bottom middle, which becomes the tank's floor. Glass and other blocks that are not solid full cubes SHALL NOT be accepted as a floor.

#### Scenario: Recipe
- **WHEN** the player crafts copper ingots in the corners, glass on the three edges and stone bricks in the bottom middle around an empty centre
- **THEN** one Fluid Tank with a stone brick floor is produced

#### Scenario: Glass floor refused
- **WHEN** the bottom middle holds glass
- **THEN** nothing is crafted
